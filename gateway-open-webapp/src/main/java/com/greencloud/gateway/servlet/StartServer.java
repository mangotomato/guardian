package com.greencloud.gateway.servlet;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.greencloud.gateway.FilterFileManager;
import com.greencloud.gateway.FilterLoader;
import com.greencloud.gateway.common.LogConfigurator;
import com.greencloud.gateway.constants.GatewayConstants;
import com.greencloud.gateway.groovy.GroovyCompiler;
import com.greencloud.gateway.groovy.GroovyFileFilter;
import com.greencloud.gateway.ratelimit.config.RateLimitRule;
import com.greencloud.gateway.ratelimit.config.RateLimitRuleManager;
import com.greencloud.gateway.ratelimit.config.load.IRateLimitRuleDAO;
import com.greencloud.gateway.ratelimit.config.load.RateLimitRuleDAO;
import com.greencloud.gateway.scriptManage.GatewayFilterPoller;
import com.greencloud.gateway.util.IPUtil;
import com.greencloud.gateway.util.RedisUtil;
import com.netflix.appinfo.*;
import com.netflix.config.*;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryManager;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import static com.greencloud.gateway.ratelimit.algorithm.RedisFixTimeWindowRateLimitAlgo.*;

/**
 * @author leejianhao
 */
public class StartServer implements ServletContextListener {

    private Logger logger = LoggerFactory.getLogger(StartServer.class);

    private String appName = GatewayConstants.APPLICATION_NAME;

    private LogConfigurator logConfigurator;

    private static final String[] namespaces = {"application", "db", "httpclient", "redis", "eureka"};

    public StartServer() {

        //System.setProperty(GatewayConstants.DEPLOYMENT_APPLICATION_ID, "gateway");
        //System.setProperty(GatewayConstants.DEPLOYMENT_ENVIRONMENT, "test");
        // https://github.com/Netflix/archaius/wiki/Getting-Started
        // {config_server_url}/configfiles/json/{appId}/{clusterName}/{namespaceName}?ip={clientIp}
        String configServer = System.getProperty("configServer");
        if (Strings.isNullOrEmpty(configServer)) {
            configServer = "http://localhost:8080";
        }

        StringBuilder configUrls = new StringBuilder();
        for (String namespace : namespaces) {
            configUrls.append(configServer + "/configfiles/gateway/default/" + namespace + ",");
        }
        String configUrlConcat = StringUtils.removeEnd(configUrls.toString(), ",");

        System.setProperty(GatewayConstants.DEPLOYMENT_CONFIG_URL, configUrlConcat);

        // System.setProperty("archaius.fixedDelayPollingScheduler.initialDelayMills", "30000");
        // System.setProperty("archaius.fixedDelayPollingScheduler.delayMills", "60000");

        System.setProperty(DynamicPropertyFactory.ENABLE_JMX, "true");

        loadConfiguration();
        configLog();
        registerEureka();

    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            //initInfoBoard();
            //initMonitor();
            initGateway();
            updateInstanceStatusToEureka();

        } catch (Exception e) {
            logger.error("Error while initializing gateway.", e);
            throw new RuntimeException(e);
        }
    }

    private void initGateway() throws Exception {
        logger.info("Starting Groovy Filter file manager");
        final AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        final String preFiltersPath = config.getString(GatewayConstants.GATEWAY_FILTER_PRE_PATH);
        final String postFiltersPath = config.getString(GatewayConstants.GATEWAY_FILTER_POST_PATH);
        final String routeFiltersPath = config.getString(GatewayConstants.GATEWAY_FILTER_ROUTE_PATH);
        final String errorFiltersPath = config.getString(GatewayConstants.GATEWAY_FILTER_ERROR_PATH);
        final String customPath = config.getString(GatewayConstants.GATEWAY_FILTER_CUSTOM_PATH);

        //load local filter files
        FilterLoader.getInstance().setCompiler(new GroovyCompiler());
        FilterFileManager.setFilenameFilter(new GroovyFileFilter());

        DynamicIntProperty pollerInterval = DynamicPropertyFactory.getInstance()
                .getIntProperty(GatewayConstants.GATEWAY_FILTER_MANAGER_POLLER_INTERVAL_SECONDS, 5);

        if (customPath == null) {
            FilterFileManager.init(pollerInterval.get(), preFiltersPath, postFiltersPath, routeFiltersPath, errorFiltersPath);
        } else {
            FilterFileManager.init(pollerInterval.get(), preFiltersPath, postFiltersPath, routeFiltersPath, errorFiltersPath, customPath);
        }
        //load filters in DB
        startGatewayFilterPoller();
        initFlowRule();
        logger.info("Groovy Filter file manager started");
    }

    private void initFlowRule() {
        // eager load
        loadRateLimitRule();
        initFlowRedisLuaScript();
    }

    private void loadRateLimitRule() {
        try {
            IRateLimitRuleDAO dao = new RateLimitRuleDAO();
            List<RateLimitRule> rules = dao.getAllRelation();
            RateLimitRuleManager.loadRules(rules);
            logger.info("Rate limit rule loaded");
        } catch (Exception e) {
            logger.info("Rate limit rule loaded fail", e);
        }
    }

    private void initFlowRedisLuaScript() {
        RedisUtil.getInstance().scriptLoad(REDIS_LIMIT_SCRIPT_SECOND);
        RedisUtil.getInstance().scriptLoad(REDIS_LIMIT_SCRIPT_MINUTE);
        RedisUtil.getInstance().scriptLoad(REDIS_LIMIT_SCRIPT_HOUR);
        RedisUtil.getInstance().scriptLoad(REDIS_LIMIT_SCRIPT_DAY);
    }

    private void updateInstanceStatusToEureka() {
        DynamicBooleanProperty eurekaEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty("eureka.enabled", true);
        if (!eurekaEnabled.get()) {
            return;
        }
        ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.UP);
    }

    private void startGatewayFilterPoller() {
        GatewayFilterPoller.start();
        logger.info("GatewayFilterPoller Started.");
    }

    private void loadConfiguration() {
        // Loading properties via archaius.
        try {
            ConfigurationManager.loadCascadedPropertiesFromResources(appName);
        } catch (IOException e) {
            logger.error(String.format(
                    "Failed to load properties for application id: %s and environment: %s. This is ok, if you do not have application level properties.",
                    appName, ConfigurationManager.getDeploymentContext().getDeploymentEnvironment()), e);
        }
    }

    private void configLog() {
        logConfigurator = new LogConfigurator(appName, ConfigurationManager.getDeploymentContext().getDeploymentEnvironment());
        logConfigurator.config();
    }

    private void registerEureka() {
        DynamicBooleanProperty eurekaEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty("eureka.enabled", true);
        if (!eurekaEnabled.get()) {
            return;
        }

        EurekaInstanceConfig eurekaInstanceConfig = new PropertiesInstanceConfig() {
            @Override
            public String getHostName(boolean refresh) {
                try {

                    return InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    return super.getHostName(refresh);
                }
            }
        };
        // ConfigurationManager.getConfigInstance().setProperty("eureka.statusPageUrl","http://"+ getTurbineInstance());

        DiscoveryManager.getInstance().initComponent(eurekaInstanceConfig, new DefaultEurekaClientConfig());

        final DynamicStringProperty serverStatus = DynamicPropertyFactory.getInstance()
                .getStringProperty("server." + IPUtil.getLocalIP() + ".status", "up");
        DiscoveryManager.getInstance().getDiscoveryClient().registerHealthCheckCallback(new HealthCheckCallback() {
            @Override
            public boolean isHealthy() {
                return "up".toLowerCase().equals(serverStatus.get());
            }
        });

        String version = String.valueOf(System.currentTimeMillis());
        String group = ConfigurationManager.getConfigInstance().getString("server.group", "default");
        String dataCenter = ConfigurationManager.getConfigInstance().getString("server.data-center", "default");

        Map<String, String> metadata = Maps.newHashMap();
        metadata.put("version", version);
        metadata.put("group", group);
        metadata.put("dataCenter", dataCenter);

//        String turbineInstance = getTurbineInstance();
//        if (turbineInstance != null) {
//            metadata.put("turbine.instance", turbineInstance);
//        }

        ApplicationInfoManager.getInstance().registerAppMetadata(metadata);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
