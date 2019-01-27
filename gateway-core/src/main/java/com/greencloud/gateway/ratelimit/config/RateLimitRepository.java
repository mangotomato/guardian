package com.greencloud.gateway.ratelimit.config;

import com.greencloud.gateway.ratelimit.algorithm.RateLimitAlgo;
import com.greencloud.gateway.ratelimit.algorithm.RedisFixTimeWindowRateLimitAlgo;

/**
 * 存储（内存，mysql，redis）
 * @author leejianhao
 */
public enum RateLimitRepository {
	MEMORY(null), MYSQL(null), REDIS(new RedisFixTimeWindowRateLimitAlgo());
	private RateLimitAlgo algo;
	private RateLimitRepository(RateLimitAlgo algo) {
		this.algo = algo;
	}

	public RateLimitAlgo getAlgo() {
		return this.algo;
	}
}

