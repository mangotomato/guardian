package com.greencloud.gateway.ratelimit.config.load;

import com.greencloud.gateway.ratelimit.config.RateLimitRule;

import java.util.List;

/**
 * @author leejianhao
 */
public interface IRateLimitRuleDAO {

    List<RateLimitRule> getAllRelation() throws Exception;

}
