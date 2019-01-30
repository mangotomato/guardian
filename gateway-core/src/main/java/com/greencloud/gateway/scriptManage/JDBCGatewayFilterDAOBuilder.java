package com.greencloud.gateway.scriptManage;

import com.greencloud.gateway.common.datasource.DataSourceHolder;
import com.greencloud.gateway.constants.GatewayConstants;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class JDBCGatewayFilterDAOBuilder implements IGatewayFilterDAOBuilder {

//	private static final DynamicStringProperty environment = DynamicPropertyFactory.getInstance()
//			.getStringProperty(GatewayConstants.DEPLOY_ENVIRONMENT, "test");

    private static final DynamicStringProperty filterTableName = DynamicPropertyFactory.getInstance()
            .getStringProperty(GatewayConstants.GATEWAY_FILTER_TABLE_NAME, "gateway_filter");

    private static final DynamicStringProperty appName = DynamicPropertyFactory.getInstance()
            .getStringProperty(GatewayConstants.DEPLOYMENT_APPLICATION_ID, GatewayConstants.APPLICATION_NAME);


    private DataSource dataSource;
    private String filterTable;

    public JDBCGatewayFilterDAOBuilder() {
        dataSource = DataSourceHolder.getInstance().getDataSource();
        //+ "_" + environment.get();
        this.filterTable = filterTableName.get();
    }

    @Override
    public IGatewayFilterDAO build() {
        return new JDBCGatewayFilterDAO(filterTable, (HikariDataSource) dataSource, appName.get());
    }

}