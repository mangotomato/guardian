package com.greencloud.gateway.ratelimit.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author leejianhao
 */
public class RateLimitRuleManager {
	private static final ConcurrentMap<String, RateLimitRule> rateLimitRules = new ConcurrentHashMap<>();

	static {
		RateLimitRule rule = new RateLimitRule();
		rule.setLimit(10);
		rule.setLimitClient(5);
		rule.setLimitAPP(1);
		rule.setTimeUnit(TimeUnit.MINUTES);
		rule.setApi("/s/weather");

		rateLimitRules.put("/s/weather", rule);
	}

	public static void loadRules(List<RateLimitRule> rules) {
		for (RateLimitRule rateLimitRule : rules) {
			rateLimitRules.put(rateLimitRule.getApi(), rateLimitRule);
		}
	}

	public static Map<String, RateLimitRule> getRules() {
		return rateLimitRules;
	}
}
