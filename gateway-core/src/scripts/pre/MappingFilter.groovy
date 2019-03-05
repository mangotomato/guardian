package com.greencloud.gateway.filters.pre;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.common.ant.AntPathMatcher;
import com.greencloud.gateway.common.ant.PathMatcher;
import com.greencloud.gateway.constants.GatewayConstants;
import com.greencloud.gateway.constants.SystemHeader;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;
import com.greencloud.gateway.upstreamCheck.UpStreamCheckListener;
import com.greencloud.gateway.upstreamCheck.UpstreamCheck;
import com.greencloud.gateway.upstreamCheck.config.Status;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author leejianhao
 */
public class MappingFilter extends GatewayFilter implements UpStreamCheckListener {

    private static Logger logger = LoggerFactory.getLogger(com.greencloud.gateway.filters.pre.MappingFilter.class);

    /**
     * /s/** = [http://api.ihotel.cn,http://api.ihotel.cn]
     */
    private static final AtomicReference<HashMap<String /* service group */, List<String>>> serverRef = new AtomicReference<>(Maps.<String, List<String>>newHashMap());
    private static final AtomicReference<HashMap<String /* service group */, List<String>>> activeServerRef = new AtomicReference<>(Maps.<String, List<String>>newHashMap());
    /**
     * /s/** = [stripPrefix=false,config=configValue]
     */
    private static final AtomicReference<HashMap<String, Map<String, String>>> serverConfigRef = new AtomicReference<>(Maps.<String, Map<String, String>>newHashMap());

    /**
     * /s/weather = [http://api.ihotel.cn,http://api.ihotel.cn]
     */
    private static final AtomicReference<HashMap<String, List<String>>> routesTableRef = new AtomicReference<>(Maps.<String, List<String>>newHashMap());

    /**
     * /s/weather=/s/**
     */
    private static final AtomicReference<HashMap<String, String>> matchers = new AtomicReference<>(Maps.<String, String>newHashMap());

    private static final DynamicStringProperty ROUTES_TABLE = DynamicPropertyFactory.getInstance()
            .getStringProperty(GatewayConstants.GATEWAY_ROUTES_TABLE, null);

    private static final DynamicBooleanProperty UPSTREAM_CHECK_ENABLE = DynamicPropertyFactory.getInstance()
            .getBooleanProperty(GatewayConstants.GATEWAY_UPSTREAM_CHECK_ENABLE, false);

    private static final PathMatcher matcher;

    static {
        matcher = new AntPathMatcher();

        buildRoutesTable();

        ROUTES_TABLE.addCallback(new Runnable() {
            @Override
            public void run() {
                buildRoutesTable();
                // Set upstream check servers
                setUpStreamCheckServer();
            }
        });

        if (UPSTREAM_CHECK_ENABLE.get()) {
            startUpStreamCheck();
        }

        UPSTREAM_CHECK_ENABLE.addCallback(new Runnable() {
            @Override
            public void run() {
                if (UPSTREAM_CHECK_ENABLE.get()) {
                    startUpStreamCheck();
                } else {
                    stopUpStreamCheck();
                }
            }
        });
    }

    private static void startUpStreamCheck() {
        UpstreamCheck.start();
        setUpStreamCheckServer();
        UpstreamCheck.getInstance().addListener(new com.greencloud.gateway.filters.pre.MappingFilter());
    }

    private static void stopUpStreamCheck() {
        UpstreamCheck.getInstance().stop();
    }

    private static void setUpStreamCheckServer() {
        for (Map.Entry<String, List<String>> entry : serverRef.get().entrySet()) {
            UpstreamCheck.getInstance().setUpStreamCheckServers(entry.getKey(), entry.getValue());
        }
    }

    private static void buildRoutesTable() {
        logger.info("building routes table");

        final String routesTableString = ROUTES_TABLE.get();
        if (Strings.isNullOrEmpty(routesTableString)) {
            logger.info("routes table string is empty, nothing to build");
            return;
        }

        HashMap<String, List<String>> serverMap = Maps.newHashMap();
        HashMap<String, Map<String, String>> serverConfigMap = Maps.newHashMap();

        String[] routes = routesTableString.split("\n");
        for (String route : routes) {
            if (Strings.isNullOrEmpty(route)) {
                continue;
            }
            String[] parts = StringUtils.split(route, ";");
            if (parts.length != 2) {
                continue;
            }

            String routeMappingString = parts[0];
            if (Strings.isNullOrEmpty(routeMappingString)) {
                continue;
            }

            String[] routeMappings = routeMappingString.split("=");
            if (routeMappings.length != 2) {
                continue;
            }

            String mappingAnt = routeMappings[0];
            String serverString = routeMappings[1];
            if (Strings.isNullOrEmpty(mappingAnt) || Strings.isNullOrEmpty(serverString)) {
                continue;
            }
            String[] servers = serverString.split("\\|");
            serverMap.put(mappingAnt, Lists.newLinkedList(Arrays.asList(servers)));

            String routesConfigString = parts[1];
            if (Strings.isNullOrEmpty(routesConfigString)) {
                continue;
            }
            String[] routesConfigs = routesConfigString.split("&");
            for (String routeConfig : routesConfigs) {
                String[] serverConfigItem = routeConfig.split("=");
                if (serverConfigItem.length != 2) {
                    continue;
                }
                Map<String, String> configs = serverConfigMap.get(mappingAnt);
                if (configs == null) {
                    configs = Maps.newHashMap();
                    serverConfigMap.put(mappingAnt, configs);
                }
                configs.put(serverConfigItem[0], serverConfigItem[1]);
            }
        }
        serverRef.set(serverMap);
        activeServerRef.set(serverMap);

        serverConfigRef.set(serverConfigMap);

        routesTableRef.get().clear();
        matchers.get().clear();
    }

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 20;
    }

    @Override
    public boolean shouldFilter() {
        return !RequestContext.getCurrentContext().isHealthCheckRequest();
    }

    @Override
    public Object run() throws GatewayException {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = StringUtils.removeStart(uri, contextPath);

        List<String> servers = findServers(path);
        if (servers.isEmpty()) {
            throw new GatewayException("API Not Found", 400, "API Not Found");
        }

        int index = (int) (Math.random() * servers.size());
        String server = servers.get(index);

        String mappingAnt = matchers.get().get(path);

        boolean stripPrefix = Boolean.valueOf(serverConfigRef.get().get(mappingAnt).get("stripPrefix"));

        if (stripPrefix) {
            String prefix = mappingAnt.substring(0, mappingAnt.indexOf("/*"));
            path = StringUtils.removeStart(path, prefix);
        }

        boolean appkeyEnable = "T".equalsIgnoreCase(serverConfigRef.get().get(mappingAnt).get("appkey"));
        if (appkeyEnable) {
            RequestContext.getCurrentContext().setAppKeyAuthentification();
        }

        boolean nonceEnable = "T".equalsIgnoreCase(serverConfigRef.get().get(mappingAnt).get("nonce"));
        if (nonceEnable) {
            RequestContext.getCurrentContext().setNonceAuthentification();
        }

        String routeUrl = server + path + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        RequestContext.getCurrentContext().setRouteUrl(routeUrl);
        RequestContext.getCurrentContext().setApp(getApp());
        RequestContext.getCurrentContext().setAPIIdentity(path);
        RequestContext.getCurrentContext().setClient("default");

        return null;
    }

    private List<String> findServers(String path) throws GatewayException {

        List<String> servers = routesTableRef.get().get(path);

        if (servers != null && servers.size() > 0) {
            return servers;
        }

        boolean match = false;
        Map<String, List<String>> serverMap = getServerMap();
        Set<String> mappingAnts = serverMap.keySet();
        for (String mappingAnt : mappingAnts) {
            if (matcher.match(mappingAnt, path)) {
                match = true;
                routesTableRef.get().put(path, getServerMap().get(mappingAnt));
                matchers.get().put(path, mappingAnt);
                break;
            }
        }

        if (!match) {
            throw new GatewayException("API Not Found", 400, "API Not Found");
        }

        return routesTableRef.get().get(path);
    }

    private Map<String, List<String>> getServerMap() {
        return UPSTREAM_CHECK_ENABLE.get() ? activeServerRef.get() : serverRef.get();
    }

    private String getApp() {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();
        String app = request.getHeader(SystemHeader.X_GW_KEY);
        return app;
    }

    @Override
    public void onChange(String serviceGroup, String server, Status status) {
        List<String> servers = activeServerRef.get().get(serviceGroup);

        if (Status.UP == status) {
            if (!servers.contains(server)) {
                servers.add(server);
            }
        } else if (Status.DOWN == status) {
            if (servers.contains(server)) {
                servers.remove(server);
            }
        }
    }
}
