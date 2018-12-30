package com.greencloud.gateway.filters;

import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;

/**
 * @author leejianhao
 */
public class MockFilter extends GatewayFilter {
    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws GatewayException {
        RequestContext.getCurrentContext().setRouteUrl("http://api.ihotel.cn/s/weather?city=呼伦贝尔");
        return null;
    }
}

