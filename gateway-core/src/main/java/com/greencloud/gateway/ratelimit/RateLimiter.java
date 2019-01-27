package com.greencloud.gateway.ratelimit;

/**
 * @author leejianhao
 */
public interface RateLimiter {

	/**
	 * entry resource
	 * @throws ThrottledException
	 */
	void entry() throws ThrottledException;

}
