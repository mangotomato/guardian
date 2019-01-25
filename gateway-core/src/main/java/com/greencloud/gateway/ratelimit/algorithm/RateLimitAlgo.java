package com.greencloud.gateway.ratelimit.algorithm;


import com.greencloud.gateway.ratelimit.config.RateLimitRule;
import com.greencloud.gateway.ratelimit.ThrottledException;

import java.util.List;

public interface RateLimitAlgo {

	void tryAcquire(List<RateLimitRule> rules) throws ThrottledException;
}
