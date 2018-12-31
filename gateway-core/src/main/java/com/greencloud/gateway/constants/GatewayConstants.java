package com.greencloud.gateway.constants;

public class GatewayConstants {

    public static final String APPLICATION_NAME = "gateway";
    public static final String DEPLOYMENT_APPLICATION_ID = "archaius.deployment.applicationId";
    public static final String DEPLOYMENT_CONFIG_URL = "archaius.configurationSource.additionalUrls";
    public static final String DEPLOYMENT_ENVIRONMENT = "archaius.deployment.environment";

    // filters
    public static final String GATEWAY_FILTER_PRE_PATH = "gateway.filter.pre.path";
    public static final String GATEWAY_FILTER_ROUTE_PATH = "gateway.filter.route.path";
    public static final String GATEWAY_FILTER_POST_PATH = "gateway.filter.post.path";
    public static final String GATEWAY_FILTER_ERROR_PATH = "gateway.filter.error.path";
    public static final String GATEWAY_FILTER_CUSTOM_PATH = "gateway.filter.custom.path";

    // dynamic filters
    public static final String GATEWAY_FILTER_POLLER_ENABLED = "gateway.filter.poller.enabled";
    public static final String GATEWAY_FILTER_POLLER_INTERVAL = "gateway.filter.poller.interval";
    public static final String GATEWAY_FILTER_TABLE_NAME = "gateway.filter.table.name";
    public static final String GATEWAY_FILTER_DAO_TYPE = "gateway.filter.dao.type";
    public static final String GATEWAY_FILTER_REPO = "gateway.filter.repository";
    public static final String GATEWAY_USE_ACTIVE_FILTERS = "gateway.use.active.filters";
    public static final String GATEWAY_USE_CANARY_FILTERS = "gateway.use.canary.filters";

    //This property turns on the admin page. Note that the admin page should be only accessed internally
    //and should be protected from general access
    public static final String GATEWAY_FILTER_ADMIN_ENABLED = "gateway.filter.admin.enabled";
    public static final String GATEWAY_FILTER_ADMIN_REDIRECT = "gateway.filter.admin.redirect.path";

    public static final String GATEWAY_DEBUG_REQUEST = "gateway.debug.request";
    public static final String GATEWAY_DEBUG_PARAMETER = "gateway.debug.parameter";
    public static final String GATEWAY_ROUTER_ALT_ROUTE_VIP = "gateway.router.alt.route.vip";
    public static final String GATEWAY_ROUTER_ALT_ROUTE_HOST = "gateway.router.alt.route.host";
    public static final String GATEWAY_DEFAULT_HOST = "gateway.default.host";
    public static final String GATEWAY_HOST_SOCKET_TIMEOUT_MILLIS = "gateway.host.socket-timeout-millis";
    public static final String GATEWAY_HOST_CONNECT_TIMEOUT_MILLIS = "gateway.host.connect-timeout-millis";
    public static final String GATEWAY_INCLUDE_DEBUG_HEADER = "gateway.include-debug-header";
    public static final String GATEWAY_INITIAL_STREAM_BUFFER_SIZE = "gateway.initial-stream-buffer-size";
    public static final String GATEWAY_SET_CONTENT_LENGTH = "gateway.set-content-length";
    public static final String GATEWAY_DEBUGFILTERS_DISABLED = "gateway.debugFilters.disabled";
    public static final String GATEWAY_DEBUG_VIP = "gateway.debug.vip";
    public static final String GATEWAY_DEBUG_HOST = "gateway.debug.host";

    // async servlet
    public static final String GATEWAY_SERVLET_ASYNC_TIMEOUT = "gateway.servlet.async.timeout";
    public static final String GATEWAY_THREADPOOL_CODE_SIZE = "gateway.thread-pool.core-size";
    public static final String GATEWAY_THREADPOOL_MAX_SIZE = "gateway.thread-pool.maximum-size";
    public static final String GATEWAY_THREADPOOL_ALIVE_TIME = "gateway.thread-pool.alive-time";

    // datasource
    public static final String DATA_SOURCE_CLASS_NAME = "gateway.data-source.class-name";
    public static final String DATA_SOURCE_URL = "gateway.data-source.url";
    public static final String DATA_SOURCE_USER = "gateway.data-source.user";
    public static final String DATA_SOURCE_PASSWORD = "gateway.data-source.password";
    public static final String DATA_SOURCE_MIN_POOL_SIZE = "gateway.data-source.min-pool-size";
    public static final String DATA_SOURCE_MAX_POOL_SIZE = "gateway.data-source.max-pool-size";
    public static final String DATA_SOURCE_CONNECT_TIMEOUT = "gateway.data-source.connection-timeout";
    public static final String DATA_SOURCE_IDLE_TIMEOUT = "gateway.data-source.idle-timeout";
    public static final String DATA_SOURCE_MAX_LIFETIME = "gateway.data-source.max-lifetime";

    // http client
    public static final String GATEWAY_CLIENT_MAX_CONNECTIONS = "gateway.client.max.connections";
    public static final String GATEWAY_CLIENT_ROUTE_MAX_CONNECTIONS = "gateway.client.route.max.connections";
    public static final String GATEWAY_CLIENT_SOCKET_TIMEOUT_MILLIS = "gateway.client.socket.timeout.millis";
    public static final String GATEWAY_CLIENT_CONNECT_TIMEOUT_MILLIS = "gateway.client.connect.timeout.millis";

    public static final String DEFAULT_CONTENT_TYPE = "application/json";
    public static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";

    public static final String IGNORED_HEADERS = "ignoredHeaders";

    // Prevent instantiation
    private GatewayConstants() {
        throw new AssertionError("Must not instantiate constant utility class");
    }

}
