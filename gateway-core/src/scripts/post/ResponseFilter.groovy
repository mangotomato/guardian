package com.greencloud.gateway.filters.post;

import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;

/**
 * 处理路由响应请求，post filters中优先级最高
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

        context.getResponse().setStatus(500);
        GatewayException exception = new GatewayException("Route error", 500, "Failed To Invoke Backend Service");
        context.setThrowable(exception);
        throw exception;
    }

}
