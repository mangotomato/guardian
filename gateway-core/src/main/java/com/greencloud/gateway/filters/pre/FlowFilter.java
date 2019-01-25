package com.greencloud.gateway.filters.pre;

import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.exception.GatewayException;
import com.greencloud.gateway.ratelimit.DefaultRateLimiter;
import com.greencloud.gateway.ratelimit.RateLimiter;
import com.greencloud.gateway.ratelimit.ThrottledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 流控
 * @author leejianhao
 */
public class FlowFilter extends GatewayFilter {

	private static final Logger logger = LoggerFactory.getLogger(FlowFilter.class);

	private RateLimiter limiter = new DefaultRateLimiter();

	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public int filterOrder() {
		return 21;
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() throws GatewayException {
		try {
			limiter.entry();
		} catch (ThrottledException e) {
			throw new GatewayException("Throttled by API flow control", 403, e.getMessage());
		} catch (Exception e) {
			// Traffic flow must not influence main logic
			logger.error("Flow Internal Error", e);
		}
		return null;
	}
}
