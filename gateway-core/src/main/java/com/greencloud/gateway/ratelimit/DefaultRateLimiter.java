package com.greencloud.gateway.ratelimit;

import com.google.common.base.Strings;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.ratelimit.algorithm.RateLimitAlgo;
import com.greencloud.gateway.ratelimit.config.RateLimitRepository;
import com.greencloud.gateway.ratelimit.config.RateLimitRule;
import com.greencloud.gateway.ratelimit.config.RateLimitRuleManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

import java.util.Map;

/**
 * @author leejianhao
 */
public class DefaultRateLimiter implements RateLimiter {

	private static final DynamicStringProperty REPOSITORY = DynamicPropertyFactory.getInstance()
			.getStringProperty("ratelimit.repository", "redis");

	@Override
	public void entry() throws ThrottledException {

		// API对应的限流规则
		Map<String /* API */, RateLimitRule> rules = RateLimitRuleManager.getRules();

		// 根据API查找对应流控规则
		String api = RequestContext.getCurrentContext().getAPIIdentity();
		if (Strings.isNullOrEmpty(api)) {
			return;
		}

		RateLimitRule currentRule = rules.get(api);
		if (currentRule == null) {
			return;
		}

		// 根据流控算法，尝试获取资源
		getRateLimitAlgo().tryAcquire(currentRule);

	}

	private RateLimitAlgo getRateLimitAlgo() {
		return RateLimitRepository.valueOf(REPOSITORY.get().toUpperCase()).getAlgo();
	}

}