package com.greencloud.gateway.servlet;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.greencloud.gateway.FilterFileManager;
import com.greencloud.gateway.FilterLoader;
import com.greencloud.gateway.common.LogConfigurator;
import com.greencloud.gateway.constants.GatewayConstants;
import com.greencloud.gateway.groovy.GroovyCompiler;
import com.greencloud.gateway.groovy.GroovyFileFilter;
import com.greencloud.gateway.scriptManage.GatewayFilterPoller;
import com.greencloud.gateway.util.IPUtil;
import com.netflix.appinfo.*;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
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
import java.util.Map;

/**
 * @author leejianhao
 */
public class StartServer implements ServletContextListener {

    private Logger logger = LoggerFactory.getLogger(StartServer.class);

    private String appName = null;

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
            configUrls.append(configServer+"/configfiles/gateway/default/"+namespace+",");
        }
        String configUrlConcat = StringUtils.removeEnd(configUrls.toString(), ",");

        System.setProperty(GatewayConstants.DEPLOYMENT_CONFIG_URL, configUrlConcat);

        String applicationID = ConfigurationManager.getConfigInstance().getString(GatewayConstants.DEPLOYMENT_APPLICATION_ID);
        if (Strings.isNullOrEmpty(applicationID)) {
            logger.warn("Using default config!");
            ConfigurationManager.getConfigInstance().setProperty(GatewayConstants.DEPLOYMENT_APPLICATION_ID, "gateway");
        }

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
            logger.error("Error while initializing zuul gateway.", e);
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
        if (customPath == null) {
            FilterFileManager.init(5, preFiltersPath, postFiltersPath, routeFiltersPath, errorFiltersPath);
        } else {
            FilterFileManager.init(5, preFiltersPath, postFiltersPath, routeFiltersPath, errorFiltersPath, customPath);
        }
        //load filters in DB
        startZuulFilterPoller();
        logger.info("Groovy Filter file manager started");
    }

    private void updateInstanceStatusToEureka() {
        DynamicBooleanProperty eurekaEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty("eureka.enabled", true);
        if (!eurekaEnabled.get()) {
            return;
        }
        ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.UP);
    }

    private void startZuulFilterPoller() {
        GatewayFilterPoller.start();
        logger.info("GatewayFilterPoller Started.");
    }

    private void loadConfiguration() {
        appName = ConfigurationManager.getDeploymentContext().getApplicationId();
        // Loading properties via archaius.
        if (!Strings.isNullOrEmpty(appName)) {
            try {
                logger.info(String.format("Loading application properties with app id: %s and environment: %s", appName,
                        ConfigurationManager.getDeploymentContext().getDeploymentEnvironment()));
                ConfigurationManager.loadCascadedPropertiesFromResources(appName);
            } catch (IOException e) {
                logger.error(String.format(
                        "Failed to load properties for application id: %s and environment: %s. This is ok, if you do not have application level properties.",
                        appName, ConfigurationManager.getDeploymentContext().getDeploymentEnvironment()), e);
            }
        } else {
            logger.warn(
                    "Application identifier not defined, skipping application level properties loading. You must set a property 'archaius.deployment.applicationId' to be able to load application level properties.");
        }
    }


    private void configLog() {
        logConfigurator = new LogConfigurator(appName,ConfigurationManager.getDeploymentContext().getDeploymentEnvironment());
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

    public String getTurbineInstance() {
        String instance = null;
        String ip = IPUtil.getLocalIP();
        if (ip != null) {
            instance = ip + ":" + ConfigurationManager.getConfigInstance().getString("server.internals.port", "8077");
        } else {
            logger.warn("Can't build turbine instance as can't fetch the ip.");
        }
        return instance;
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
