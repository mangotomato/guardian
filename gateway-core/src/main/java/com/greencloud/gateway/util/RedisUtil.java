package com.greencloud.gateway.util;

import static com.greencloud.gateway.constants.GatewayConstants.*;
import static com.greencloud.gateway.constants.GatewayConstants.REDIS_JEDISPOOLCONFIG_DATABASE;

import com.google.common.collect.Lists;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author leejianhao
 */
public class RedisUtil {
    private static final Logger logger = LoggerFactory.getLogger(RedisUtil.class);

    private static final String DEFAULT_REDIS_SEPARATOR = ";";
    private static final String HOST_PORT_SEPARATOR = ":";

    private Runnable RENEW_POOL = new Runnable() {
        @Override
        public void run() {
            RedisUtil.this.reNewPool();
        }
    };

    public static RedisUtil getInstance() {
        return INSTANCE;
    }

    private AtomicReference<JedisPool[]> jedisPoolsRef = new AtomicReference<>();

    private static final DynamicIntProperty JEDISPOOLCONFIG_TIMEOUT = DynamicPropertyFactory.getInstance().getIntProperty(REDIS_TIMEOUT, 2000);
    private static final DynamicStringProperty JEDISPOOLCONFIG_URLS = DynamicPropertyFactory.getInstance().getStringProperty(REDIS_JEDISPOOLCONFIG_URLS, "localhost:6379");
    private static final DynamicIntProperty JEDISPOOLCONFIG_MAXTOTAL = DynamicPropertyFactory.getInstance().getIntProperty(REDIS_JEDISPOOLCONFIG_MAXTOTAL, 8);
    private static final DynamicIntProperty JEDISPOOLCONFIG_MAXIDLE = DynamicPropertyFactory.getInstance().getIntProperty(REDIS_JEDISPOOLCONFIG_MAXIDLE, 8);
    private static final DynamicIntProperty JEDISPOOLCONFIG_MINIDLE = DynamicPropertyFactory.getInstance().getIntProperty(REDIS_JEDISPOOLCONFIG_MINIDLE, 0);
    private static final DynamicIntProperty JEDISPOOLCONFIG_MAXWAITTIME = DynamicPropertyFactory.getInstance().getIntProperty(REDIS_JEDISPOOLCONFIG_MAXWAITTIME, -1);
    private static final DynamicBooleanProperty JEDISPOOLCONFIG_TESTONBORROW = DynamicPropertyFactory.getInstance().getBooleanProperty(REDIS_JEDISPOOLCONFIG_TESTONBORROW, true);
    private static final DynamicStringProperty JEDISPOOLCONFIG_PASSWORD = DynamicPropertyFactory.getInstance().getStringProperty(REDIS_JEDISPOOLCONFIG_PASSWORD, "");
    private static final DynamicIntProperty JEDISPOOLCONFIG_DATABASE = DynamicPropertyFactory.getInstance().getIntProperty(REDIS_JEDISPOOLCONFIG_DATABASE, 0);

    private static final RedisUtil INSTANCE = new RedisUtil();

    private RedisUtil() {
        JEDISPOOLCONFIG_TIMEOUT.addCallback(RENEW_POOL);
        JEDISPOOLCONFIG_URLS.addCallback(RENEW_POOL);
        JEDISPOOLCONFIG_MAXTOTAL.addCallback(RENEW_POOL);
        JEDISPOOLCONFIG_MAXIDLE.addCallback(RENEW_POOL);
        JEDISPOOLCONFIG_MINIDLE.addCallback(RENEW_POOL);
        JEDISPOOLCONFIG_MAXWAITTIME.addCallback(RENEW_POOL);
        JEDISPOOLCONFIG_TESTONBORROW.addCallback(RENEW_POOL);
        JEDISPOOLCONFIG_PASSWORD.addCallback(RENEW_POOL);
        JEDISPOOLCONFIG_DATABASE.addCallback(RENEW_POOL);

        initPool();
    }

    private void reNewPool() {
        this.destroy();
        initPool();
    }

    private void initPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(JEDISPOOLCONFIG_MAXTOTAL.get());
        poolConfig.setMaxIdle(JEDISPOOLCONFIG_MAXIDLE.get());
        poolConfig.setMinIdle(JEDISPOOLCONFIG_MINIDLE.get());
        poolConfig.setMaxWaitMillis(JEDISPOOLCONFIG_MAXWAITTIME.get());
        poolConfig.setTestOnBorrow(JEDISPOOLCONFIG_TESTONBORROW.get());

        // 生成连接池
        List<JedisPool> jedisPoolList = new ArrayList<JedisPool>();
        String redisUrls = JEDISPOOLCONFIG_URLS.get();
        for (String redisUrl : redisUrls.split(DEFAULT_REDIS_SEPARATOR)) {
            String[] redisUrlInfo = redisUrl.split(HOST_PORT_SEPARATOR);
            jedisPoolList.add(new JedisPool(poolConfig, redisUrlInfo[0], Integer.parseInt(redisUrlInfo[1]),
                    JEDISPOOLCONFIG_TIMEOUT.get(), JEDISPOOLCONFIG_PASSWORD.get(), JEDISPOOLCONFIG_DATABASE.get()));
        }

        jedisPoolsRef.set(jedisPoolList.toArray(new JedisPool[0]));
    }

    private Jedis getJedis(String key) {
        JedisPool[] jedisPools = jedisPoolsRef.get();
        Jedis jedis = jedisPools[(0x7FFFFFFF & key.hashCode()) % jedisPools.length].getResource();
        return jedis;
    }

    /**
     * 实现jedis连接的获取和释放，具体的redis业务逻辑由executor实现
     *
     * @param executor RedisExecutor接口的实现类
     * @return
     */
    public <T> T execute(String key, HashRedisExecutor<T> executor) {
        Jedis jedis = getJedis(key);
        T result = null;
        try {
            result = executor.execute(jedis);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }

    public String set(final String key, final String value) {
        return execute(key, new HashRedisExecutor<String>() {
            @Override
            public String execute(Jedis jedis) {
                return jedis.set(key, value);
            }
        });
    }

    public String set(final String key, final String value, final String nxxx, final String expx, final long time) {
        return execute(key, new HashRedisExecutor<String>() {
            @Override
            public String execute(Jedis jedis) {
                return jedis.set(key, value, nxxx, expx, time);
            }
        });
    }

    public String get(final String key) {
        return execute(key, new HashRedisExecutor<String>() {
            @Override
            public String execute(Jedis jedis) {
                return jedis.get(key);
            }
        });
    }

    public Boolean exists(final String key) {
        return execute(key, new HashRedisExecutor<Boolean>() {
            @Override
            public Boolean execute(Jedis jedis) {
                return jedis.exists(key);
            }
        });
    }

    public Long setnx(final String key, final String value) {
        return execute(key, new HashRedisExecutor<Long>() {
            @Override
            public Long execute(Jedis jedis) {
                return jedis.setnx(key, value);
            }
        });
    }

    public String setex(final String key, final int seconds, final String value) {
        return execute(key, new HashRedisExecutor<String>() {
            @Override
            public String execute(Jedis jedis) {
                return jedis.setex(key, seconds, value);
            }
        });
    }

    public Long expire(final String key, final int seconds) {
        return execute(key, new HashRedisExecutor<Long>() {
            @Override
            public Long execute(Jedis jedis) {
                return jedis.expire(key, seconds);
            }
        });
    }

    public Long incr(final String key) {
        return execute(key, new HashRedisExecutor<Long>() {
            @Override
            public Long execute(Jedis jedis) {
                return jedis.incr(key);
            }
        });
    }

    public Long decr(final String key) {
        return execute(key, new HashRedisExecutor<Long>() {
            @Override
            public Long execute(Jedis jedis) {
                return jedis.decr(key);
            }
        });
    }

    public Long hset(final String key, final String field, final String value) {
        return execute(key, new HashRedisExecutor<Long>() {
            @Override
            public Long execute(Jedis jedis) {
                return jedis.hset(key, field, value);
            }
        });
    }

    public String hget(final String key, final String field) {
        return execute(key, new HashRedisExecutor<String>() {
            @Override
            public String execute(Jedis jedis) {
                return jedis.hget(key, field);
            }
        });
    }

    public String hmset(final String key, final Map<String, String> hash) {
        return execute(key, new HashRedisExecutor<String>() {
            @Override
            public String execute(Jedis jedis) {
                return jedis.hmset(key, hash);
            }
        });
    }

    public List<String> hmget(final String key, final String... fields) {
        return execute(key, new HashRedisExecutor<List<String>>() {
            @Override
            public List<String> execute(Jedis jedis) {
                return jedis.hmget(key, fields);
            }
        });
    }

    public Long del(final String key) {
        return execute(key, new HashRedisExecutor<Long>() {
            @Override
            public Long execute(Jedis jedis) {
                return jedis.del(key);
            }
        });
    }

    public Map<String, String> hgetAll(final String key) {
        return execute(key, new HashRedisExecutor<Map<String, String>>() {
            @Override
            public Map<String, String> execute(Jedis jedis) {
                return jedis.hgetAll(key);
            }
        });
    }

    public Object evalsha(final String sha1, final List keys, final List params) {
        return execute(sha1, new HashRedisExecutor<Object>() {
            @Override
            public Object execute(Jedis jedis) {
                return jedis.evalsha(sha1, keys, params);
            }
        });
    }

    public Object scriptLoad(final String script) {
        return execute(script, new HashRedisExecutor<Object>() {
            @Override
            public Object execute(Jedis jedis) {
                return jedis.scriptLoad(script);
            }
        });
    }

    public void destroy() {
        JedisPool[] jedisPools = jedisPoolsRef.get();
        for (int i = 0; i < jedisPools.length; i++) {
            jedisPools[i].close();
        }
    }

    // redis具体逻辑接口
    public interface HashRedisExecutor<T> {
        T execute(Jedis jedis);
    }

}
