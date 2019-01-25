package com.greencloud.gateway.common.datasource;

import com.greencloud.gateway.constants.GatewayConstants;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * @author leejianhao
 */
public class DataSourceHolder {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceHolder.class);

    private static final DynamicStringProperty dataSourceClass = DynamicPropertyFactory.getInstance()
            .getStringProperty(GatewayConstants.DATA_SOURCE_CLASS_NAME, null);
    private static final DynamicStringProperty url = DynamicPropertyFactory.getInstance()
            .getStringProperty(GatewayConstants.DATA_SOURCE_URL, null);
    private static final DynamicStringProperty user = DynamicPropertyFactory.getInstance()
            .getStringProperty(GatewayConstants.DATA_SOURCE_USER, null);
    private static final DynamicStringProperty password = DynamicPropertyFactory.getInstance()
            .getStringProperty(GatewayConstants.DATA_SOURCE_PASSWORD, null);
    private static final DynamicIntProperty minPoolSize = DynamicPropertyFactory.getInstance()
            .getIntProperty(GatewayConstants.DATA_SOURCE_MIN_POOL_SIZE, 10);
    private static final DynamicIntProperty maxPoolSize = DynamicPropertyFactory.getInstance()
            .getIntProperty(GatewayConstants.DATA_SOURCE_MAX_POOL_SIZE, 20);
    private static final DynamicLongProperty connectionTimeout = DynamicPropertyFactory.getInstance()
            .getLongProperty(GatewayConstants.DATA_SOURCE_CONNECT_TIMEOUT, 1000);
    private static final DynamicLongProperty initializationFailTimeout = DynamicPropertyFactory.getInstance()
            .getLongProperty(GatewayConstants.DATA_SOURCE_INITIALIZATION_FAIL_TIMEOUT, 1000);
    private static final DynamicLongProperty idleTimeout = DynamicPropertyFactory.getInstance()
            .getLongProperty(GatewayConstants.DATA_SOURCE_IDLE_TIMEOUT, 600000);
    private static final DynamicLongProperty maxLifetime = DynamicPropertyFactory.getInstance()
            .getLongProperty(GatewayConstants.DATA_SOURCE_MAX_LIFETIME, 1800000);

    private static DataSourceHolder holder = new DataSourceHolder();

    private volatile DataSource dataSource;

    private DataSourceHolder() {
    }

    public DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (this) {
                if (dataSource == null) {
                    dataSource = initDataSource();
                }
            }
        }
        return dataSource;
    }

    private DataSource initDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName(dataSourceClass.get());
        config.addDataSourceProperty("url", url.get());
        config.addDataSourceProperty("user", user.get());
        config.addDataSourceProperty("password", password.get());

        config.setMinimumPoolSize(minPoolSize.get());
        config.setMaximumPoolSize(maxPoolSize.get());
        config.setConnectionTimeout(connectionTimeout.get());
        config.setIdleTimeout(idleTimeout.get());
        config.setMaxLifetime(maxLifetime.get());
        //config.addDataSourceProperty("initializationFailTimeout", initializationFailTimeout.get());
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }

    public static DataSourceHolder getInstance() {
        return holder;
    }

}
