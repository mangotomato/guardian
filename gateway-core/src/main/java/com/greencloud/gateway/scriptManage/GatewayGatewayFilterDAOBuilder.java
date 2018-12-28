package com.greencloud.gateway.scriptManage;

import com.greencloud.gateway.constants.GatewayConstants;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

public class GatewayGatewayFilterDAOBuilder implements IGatewayFilterDAOBuilder {

	private static final DynamicStringProperty appName = DynamicPropertyFactory.getInstance()
			.getStringProperty(GatewayConstants.DEPLOYMENT_APPLICATION_ID, GatewayConstants.APPLICATION_NAME);

	public GatewayGatewayFilterDAOBuilder() {

	}

	@Override
	public IGatewayFilterDAO build() {
		return new HttpGatewayFilterDAO(appName.get());

	}

}
