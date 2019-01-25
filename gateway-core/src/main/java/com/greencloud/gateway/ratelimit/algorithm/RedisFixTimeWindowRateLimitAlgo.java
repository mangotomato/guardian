package com.greencloud.gateway.ratelimit.algorithm;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.ratelimit.RateLimiter;
import com.greencloud.gateway.ratelimit.config.RateLimitRule;
import com.greencloud.gateway.ratelimit.ThrottledException;
import com.greencloud.gateway.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.JedisNoScriptException;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author leejianhao
 */
public class RedisFixTimeWindowRateLimitAlgo implements RateLimitAlgo {

	private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);

	/** Redis-lua, avoid race condition */
	public static final String REDIS_LIMIT_SCRIPT =
					"local apiKey = KEYS[1] " +
					"if apiKey then " +
					"  local apiLimit = tonumber(ARGV[1]) " +
					"  if apiLimit ~= -1 then " +
					"    local apiCurrent = tonumber(redis.call('incr', apiKey)) " +
					"    if apiCurrent > apiLimit then " +
					"       return -1 " +
					"    elseif apiCurrent == 1 then " +
					"       redis.call('expire', apiKey, '%d') " +
					"    end " +
					"  end " +
					"end " +

					"local userKey = KEYS[2] " +
					"if userKey then " +
					"  local userLimit = tonumber(ARGV[2]) " +
					"  if userLimit ~= -1 then " +
					"    local userCurrent = tonumber(redis.call('incr', userKey)) " +
					"    if userCurrent > userLimit then " +
					"       return -2 " +
					"    elseif userCurrent == 1 then " +
					"       redis.call('expire', userKey, '%d') " +
					"    end " +
					"  end " +
					"end " +

					"local appKey = KEYS[3] " +
					"if appKey then " +
					"  local appLimit = tonumber(ARGV[3]) " +
					"  if appLimit ~= -1 then " +
					"    local appCurrent = tonumber(redis.call('incr', appKey)) " +
					"    if appCurrent > appLimit then " +
					"       return -3 " +
					"    elseif appCurrent == 1 then " +
					"       redis.call('expire', appKey, '%d') " +
					"    end " +
					"  end " +
					"end " +

					"return 1 ";

	public static final String REDIS_LIMIT_SCRIPT_SECOND = String.format(REDIS_LIMIT_SCRIPT, 1, 1, 1);
	public static final String REDIS_LIMIT_SCRIPT_MINUTE = String.format(REDIS_LIMIT_SCRIPT, 60, 60, 60);
	public static final String REDIS_LIMIT_SCRIPT_HOUR = String.format(REDIS_LIMIT_SCRIPT, 3600, 3600, 3600);
	public static final String REDIS_LIMIT_SCRIPT_DAY = String.format(REDIS_LIMIT_SCRIPT, 86400, 86400, 86400);

	/* Redis cache for Lua script. */
	public static final String REDIS_LIMIT_SCRIPT_SHA1_SECOND = sha1(REDIS_LIMIT_SCRIPT_SECOND);
	public static final String REDIS_LIMIT_SCRIPT_SHA1_MINUTE = sha1(REDIS_LIMIT_SCRIPT_MINUTE);
	public static final String REDIS_LIMIT_SCRIPT_SHA1_HOUR = sha1(REDIS_LIMIT_SCRIPT_HOUR);
	public static final String REDIS_LIMIT_SCRIPT_SHA1_DAY = sha1(REDIS_LIMIT_SCRIPT_DAY);

	public static Map<TimeUnit, String> mapping;

	static {
		mapping = new HashMap<>(4);
		mapping.put(TimeUnit.SECONDS, REDIS_LIMIT_SCRIPT_SHA1_SECOND);
		mapping.put(TimeUnit.MINUTES, REDIS_LIMIT_SCRIPT_SHA1_MINUTE);
		mapping.put(TimeUnit.HOURS, REDIS_LIMIT_SCRIPT_SHA1_HOUR);
		mapping.put(TimeUnit.DAYS, REDIS_LIMIT_SCRIPT_SHA1_DAY);
	}

	@Override
	public void tryAcquire(RateLimitRule rule) throws ThrottledException {
		List<String> keys = Lists.newLinkedList();
		List<String> params = Lists.newLinkedList();

		RequestContext context = RequestContext.getCurrentContext();

		try {
			keys.add(rule.getApi() + ":" + rule.getTimeUnit().name());
			keys.add(context.getClient() + ":" + rule.getApi() + ":" + rule.getTimeUnit().name());
			keys.add(context.getClient() + ":" + context.getApp() + ":" + rule.getApi() + ":" + rule.getTimeUnit().name());

			params.add(String.valueOf(rule.getLimit()));
			params.add(String.valueOf(rule.getLimitClient()));
			params.add(String.valueOf(rule.getLimitAPP()));

			long result = (Long) RedisUtil.getInstance().evalsha(mapping.get(rule.getTimeUnit()), keys, params);

			/**
			 * -1: api throttled
			 * -2: client throttled
			 * -3: app throttled
			 */
			if (result < 0) {
				if (result == -1) {
					throw new ThrottledException("API throttled");
				}
				if (result == -2) {
					throw new ThrottledException("Client throttled");
				}
				if (result == -3) {
					throw new ThrottledException("APP throttled");
				}
			}
		} catch (JedisNoScriptException e) {
			logger.error("Redis lua script not found, script will reload", e);

			// Lua script will be loaded when server starting, but redis server will be restart, lua script cache will loss
			reloadScript();
		}
	}

	private static String sha1(String source) {
		HashFunction hashFunction = Hashing.sha1();
		HashCode hashCode = hashFunction.hashString(source, Charset.forName("UTF-8"));
		return hashCode.toString();
	}

	private void reloadScript() {
		RedisUtil.getInstance().scriptLoad(REDIS_LIMIT_SCRIPT_SECOND);
		RedisUtil.getInstance().scriptLoad(REDIS_LIMIT_SCRIPT_MINUTE);
		RedisUtil.getInstance().scriptLoad(REDIS_LIMIT_SCRIPT_HOUR);
		RedisUtil.getInstance().scriptLoad(REDIS_LIMIT_SCRIPT_DAY);
	}

}
