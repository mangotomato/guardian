package com.greencloud.gateway.ratelimit.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author leejianhao
 */
public class RateLimitRuleManager {
	private static final ConcurrentMap<String, List<RateLimitRule>> rateLimitRules = new ConcurrentHashMap<>();

	static {
		RateLimitRule rule = new RateLimitRule();
		rule.setLimit(10);
		rule.setLimitClient(5);
		rule.setLimitAPP(1);
		rule.setTimeUnit(TimeUnit.MINUTES);
		rule.setApi("/s/weather");

		List<RateLimitRule> rules = new ArrayList<>();
		rules.add(rule);

		rateLimitRules.put("/s/weather", rules);
	}

	public static void loadRules() {
	}

	public static Map<String, List<RateLimitRule>> getRules() {
		return rateLimitRules;
	}
}
