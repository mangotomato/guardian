package com.greencloud.gateway.filters.pre;

import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author leejianhao
 */
public class HealthCheckFilter extends GatewayFilter {
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
		HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
		return request.getRequestURI().endsWith("/healthcheck");
	}

	private String responseBody() {
		return "ok";
	}

	@Override
	public Object run() throws GatewayException {
		RequestContext ctx = RequestContext.getCurrentContext();
		ctx.setHealthCheckRequest();
		ctx.getResponse().setStatus(HttpServletResponse.SC_OK);
		if (ctx.getResponseBody() == null) {
			ctx.setResponseBody(responseBody());
			ctx.setSendGatewayResponse(false);
		}
		return null;
	}
}
