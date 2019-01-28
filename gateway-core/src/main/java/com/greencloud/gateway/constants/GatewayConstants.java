package com.greencloud.gateway.constants;

public class GatewayConstants {

    public static final String APPLICATION_NAME = "gateway";
    public static final String DEPLOYMENT_APPLICATION_ID = "archaius.deployment.applicationId";
    public static final String DEPLOYMENT_CONFIG_URL = "archaius.configurationSource.additionalUrls";
    public static final String DEPLOYMENT_CONFIG_FILENAME = "archaius.configurationSource.defaultFileName";
    public static final String DEPLOYMENT_CONFIG_INITIALDELAYMILLS = "archaius.fixedDelayPollingScheduler.initialDelayMills";
    public static final String DEPLOYMENT_CONFIG_DELAYMILLS = "archaius.fixedDelayPollingScheduler.delayMills";
    public static final String DEPLOYMENT_ENVIRONMENT = "archaius.deployment.environment";

    /**
     * filters
     */
    public static final String GATEWAY_FILTER_PRE_PATH = "gateway.filter.pre.path";
    public static final String GATEWAY_FILTER_ROUTE_PATH = "gateway.filter.route.path";
    public static final String GATEWAY_FILTER_POST_PATH = "gateway.filter.post.path";
    public static final String GATEWAY_FILTER_ERROR_PATH = "gateway.filter.error.path";
    public static final String GATEWAY_FILTER_CUSTOM_PATH = "gateway.filter.custom.path";

    /**
     * dynamic filters
     */
    public static final String GATEWAY_FILTER_MANAGER_POLLER_INTERVAL_SECONDS = "gateway.filter.manager.poller.interval.seconds";
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

    public static final String GATEWAY_INITIAL_STREAM_BUFFER_SIZE = "gateway.initial-stream-buffer-size";
    public static final String GATEWAY_SET_CONTENT_LENGTH = "gateway.set-content-length";

    /**
     * async servlet
     */
    public static final String GATEWAY_SERVLET_ASYNC_TIMEOUT = "gateway.servlet.async.timeout";
    public static final String GATEWAY_THREADPOOL_CODE_SIZE = "gateway.thread-pool.core-size";
    public static final String GATEWAY_THREADPOOL_MAX_SIZE = "gateway.thread-pool.maximum-size";
    public static final String GATEWAY_THREADPOOL_ALIVE_TIME = "gateway.thread-pool.alive-time";

    /**
     * datasource
     */
    public static final String DATA_SOURCE_CLASS_NAME = "gateway.data-source.class-name";
    public static final String DATA_SOURCE_URL = "gateway.data-source.url";
    public static final String DATA_SOURCE_USER = "gateway.data-source.user";
    public static final String DATA_SOURCE_PASSWORD = "gateway.data-source.password";
    public static final String DATA_SOURCE_MIN_POOL_SIZE = "gateway.data-source.min-pool-size";
    public static final String DATA_SOURCE_MAX_POOL_SIZE = "gateway.data-source.max-pool-size";
    public static final String DATA_SOURCE_CONNECT_TIMEOUT = "gateway.data-source.connection-timeout";
    public static final String DATA_SOURCE_IDLE_TIMEOUT = "gateway.data-source.idle-timeout";
    public static final String DATA_SOURCE_MAX_LIFETIME = "gateway.data-source.max-lifetime";
    public static final String DATA_SOURCE_INITIALIZATION_FAIL_TIMEOUT = "1000";

    /**
     * redis
     */
    // 操作超时时间
    public static final String REDIS_TIMEOUT = "redis.timeout";
    // redis url接口以";"分割多个地址
    public static final String REDIS_JEDISPOOLCONFIG_URLS = "redis.jedisPoolConfig.urls";
    // jedis池最大连接数总数,默认8
    public static final String REDIS_JEDISPOOLCONFIG_MAXTOTAL = "redis.jedisPoolConfig.maxTotal";
    // jedis池最大空闲连接数，默认8
    public static final String REDIS_JEDISPOOLCONFIG_MINIDLE = "redis.jedisPoolConfig.minIdle";
    // jedis池最少空闲连接数
    public static final String REDIS_JEDISPOOLCONFIG_MAXIDLE = "redis.jedisPoolConfig.maxIdle";
    // jedis池没有对象返回时，最大等待时间单位为毫秒
    public static final String REDIS_JEDISPOOLCONFIG_MAXWAITTIME = "redis.jedisPoolConfig.maxWaitTime";
    // 在borrow一个jedis实例时，是否提前进行validate操作
    public static final String REDIS_JEDISPOOLCONFIG_TESTONBORROW = "redis.jedisPoolConfig.testOnBorrow";
    // 密码
    public static final String REDIS_JEDISPOOLCONFIG_PASSWORD = "redis.jedisPoolConfig.password";
    // 数据库
    public static final String REDIS_JEDISPOOLCONFIG_DATABASE = "redis.jedisPoolConfig.database";

    /**
     * http client
     */
    public static final String GATEWAY_CLIENT_MAX_CONNECTIONS = "gateway.client.max.connections";
    public static final String GATEWAY_CLIENT_ROUTE_MAX_CONNECTIONS = "gateway.client.route.max.connections";
    public static final String GATEWAY_CLIENT_SOCKET_TIMEOUT_MILLIS = "gateway.client.socket.timeout.millis";
    public static final String GATEWAY_CLIENT_CONNECT_TIMEOUT_MILLIS = "gateway.client.connect.timeout.millis";

    /**
     * filters
     */
    public static final String GATEWAY_NONCE_ENABLE = "gateway.nonce.enable";
    public static final String GATEWAY_NONCE_TIMESTAMP_VALIDITY_MINUTES = "gateway.nonce.timestamp_validity_minutes";
    public static final String GATEWAY_ROUTES_TABLE = "gateway.routes.table";

    public static final String DEFAULT_CONTENT_TYPE = "application/json;charset=utf-8";
    public static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";

    public static final String IGNORED_HEADERS = "ignoredHeaders";

    /**
     * flow rules
     */
    public static final String GATEWAY_FLOW_RULE_POLLER_ENABLED = "gateway.flow.rule.poller.enabled";
    public static final String GATEWAY_FLOW_RULE_INTERVAL = "gateway.flow.rule.poller.interval";

    public static final String GATEWAY_UPSTREAM_CHECK_ENABLE = "gateway.upstream.check.enable";
    public static final String GATEWAY_UPSTREAM_CHECK_CONFIG = "gateway.upstream.check.config";


    /**
     * Prevent instantiation
     */
    private GatewayConstants() {
        throw new AssertionError("Must not instantiate constant utility class");
    }

}
