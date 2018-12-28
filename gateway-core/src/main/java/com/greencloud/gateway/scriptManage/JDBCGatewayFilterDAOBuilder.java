package com.greencloud.gateway.scriptManage;

import com.greencloud.gateway.constants.GatewayConstants;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class JDBCGatewayFilterDAOBuilder implements IGatewayFilterDAOBuilder {
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
	private static final DynamicLongProperty idleTimeout = DynamicPropertyFactory.getInstance()
			.getLongProperty(GatewayConstants.DATA_SOURCE_IDLE_TIMEOUT, 600000);
	private static final DynamicLongProperty maxLifetime = DynamicPropertyFactory.getInstance()
			.getLongProperty(GatewayConstants.DATA_SOURCE_MAX_LIFETIME, 1800000);

//	private static final DynamicStringProperty environment = DynamicPropertyFactory.getInstance()
//			.getStringProperty(GatewayConstants.DEPLOY_ENVIRONMENT, "test");

	private static final DynamicStringProperty filterTableName = DynamicPropertyFactory.getInstance()
			.getStringProperty(GatewayConstants.GATEWAY_FILTER_TABLE_NAME, "gateway_filter");
	
	private static final DynamicStringProperty appName = DynamicPropertyFactory.getInstance()
			.getStringProperty(GatewayConstants.DEPLOYMENT_APPLICATION_ID, GatewayConstants.APPLICATION_NAME);

	private HikariDataSource dataSource;
	private String filterTable;

	public JDBCGatewayFilterDAOBuilder() {
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

		this.dataSource = new HikariDataSource(config);
		//+ "_" + environment.get();
		this.filterTable = filterTableName.get();
	}

	@Override
	public IGatewayFilterDAO build() {
        return new JDBCGatewayFilterDAO(filterTable, dataSource, appName.get());
	}

}