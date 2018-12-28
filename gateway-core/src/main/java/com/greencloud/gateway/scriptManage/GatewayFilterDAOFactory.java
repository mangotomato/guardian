package com.greencloud.gateway.scriptManage;

import com.google.common.collect.Maps;
import com.greencloud.gateway.constants.GatewayConstants;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

import java.util.concurrent.ConcurrentMap;

public class GatewayFilterDAOFactory {
    private static final DynamicStringProperty daoType = DynamicPropertyFactory.getInstance().getStringProperty(GatewayConstants.GATEWAY_FILTER_DAO_TYPE, "jdbc");

    private static ConcurrentMap<String, IGatewayFilterDAO> daoCache = Maps.newConcurrentMap();

    private GatewayFilterDAOFactory() {

    }

    public static IGatewayFilterDAO getZuulFilterDAO() {
        IGatewayFilterDAO dao = daoCache.get(daoType.get());

        if (dao != null) {
            return dao;
        }

        if ("jdbc".equalsIgnoreCase(daoType.get())) {
            dao = new JDBCGatewayFilterDAOBuilder().build();
        } else if ("http".equalsIgnoreCase(daoType.get())) {
            dao = new GatewayGatewayFilterDAOBuilder().build();
        } else {
            dao = new JDBCGatewayFilterDAOBuilder().build();
        }

        daoCache.putIfAbsent(daoType.get(), dao);

        return dao;
    }

    public static String getCurrentType() {
        return daoType.get();
    }

}