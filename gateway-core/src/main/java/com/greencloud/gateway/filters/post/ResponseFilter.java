package com.greencloud.gateway.filters.post;

import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;

/**
 * @author leejianhao
 */
public class ResponseFilter extends GatewayFilter {

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext context = RequestContext.getCurrentContext();
        return (context.get("RouteHandled") != null && context.getResponseStatusCode() != 200);
    }

    @Override
    public Object run() throws GatewayException {
        RequestContext context = RequestContext.getCurrentContext();
        context.set("errorFlush");
        int statusCode = context.getResponseStatusCode();
        String errorCasue = String.format("Route Error, fail to invoke target: %s, statusCode: %s", context.getRouteUrl(), statusCode);
        GatewayException exception = new GatewayException("Route error", statusCode, errorCasue);
        context.setThrowable(exception);
        throw exception;
    }

}
