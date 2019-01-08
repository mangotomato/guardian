package com.greencloud.gateway.filters.pre;

import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.constants.GatewayConstants
import com.greencloud.gateway.constants.SystemHeader;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;

/**
 * @author leejianhao
 */
public class DebugFilter extends GatewayFilter {

    private static final DynamicBooleanProperty debugRequest = DynamicPropertyFactory.getInstance().getBooleanProperty(GatewayConstants.GATEWAY_DEBUG_REQUEST, false);

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        if (!debugRequest.get()) {
            return false;
        }
        return "debug".equalsIgnoreCase(RequestContext.getCurrentContext().getRequest().getHeader(SystemHeader.X_GW_REQUEST_MODE));
    }

    @Override
    public Object run() throws GatewayException {
        RequestContext.getCurrentContext().setDebugRequest(true);
        return null;
    }
}
