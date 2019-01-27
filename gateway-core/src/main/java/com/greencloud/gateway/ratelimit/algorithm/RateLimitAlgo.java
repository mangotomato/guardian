package com.greencloud.gateway.ratelimit.algorithm;


import com.greencloud.gateway.ratelimit.ThrottledException;
import com.greencloud.gateway.ratelimit.config.RateLimitRule;

public interface RateLimitAlgo {

	void tryAcquire(RateLimitRule rule) throws ThrottledException;
}
