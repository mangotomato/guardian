package com.greencloud.gateway.util;

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

    private static final DynamicIntProperty JEDISPOOLCONFIG_TIMEOUT = DynamicPropertyFactory.getInstance().getIntProperty("redis.timeout", 2000);
    private static final DynamicStringProperty JEDISPOOLCONFIG_URLS = DynamicPropertyFactory.getInstance().getStringProperty("redis.jedisPoolConfig.urls", "localhost:6379");
    private static final DynamicIntProperty JEDISPOOLCONFIG_MAXTOTAL = DynamicPropertyFactory.getInstance().getIntProperty("redis.jedisPoolConfig.maxTotal", 8);
    private static final DynamicIntProperty JEDISPOOLCONFIG_MAXIDLE = DynamicPropertyFactory.getInstance().getIntProperty("redis.jedisPoolConfig.maxIdle", 8);
    private static final DynamicIntProperty JEDISPOOLCONFIG_MINIDLE = DynamicPropertyFactory.getInstance().getIntProperty("redis.jedisPoolConfig.minIdle", 0);
    private static final DynamicIntProperty JEDISPOOLCONFIG_MAXWAITTIME = DynamicPropertyFactory.getInstance().getIntProperty("redis.jedisPoolConfig.maxWaitTime", -1);
    private static final DynamicBooleanProperty JEDISPOOLCONFIG_TESTONBORROW = DynamicPropertyFactory.getInstance().getBooleanProperty("redis.jedisPoolConfig.testOnBorrow", true);

    private static final RedisUtil INSTANCE = new RedisUtil();

    private RedisUtil() {
        JEDISPOOLCONFIG_TIMEOUT.addCallback(RENEW_POOL);
        JEDISPOOLCONFIG_URLS.addCallback(RENEW_POOL);
        JEDISPOOLCONFIG_MAXTOTAL.addCallback(RENEW_POOL);
        JEDISPOOLCONFIG_MAXIDLE.addCallback(RENEW_POOL);
        JEDISPOOLCONFIG_MINIDLE.addCallback(RENEW_POOL);
        JEDISPOOLCONFIG_MAXWAITTIME.addCallback(RENEW_POOL);
        JEDISPOOLCONFIG_TESTONBORROW.addCallback(RENEW_POOL);

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
            jedisPoolList.add(new JedisPool(poolConfig, redisUrlInfo[0], Integer.parseInt(redisUrlInfo[1]), JEDISPOOLCONFIG_TIMEOUT.get()));
        }

        jedisPoolsRef.set(jedisPoolList.toArray(new JedisPool[0]));
    }

    private Jedis getJedis(String key) {
        JedisPool[] jedisPools = jedisPoolsRef.get();
        Jedis jedis = jedisPools[(0x7FFFFFFF & key.hashCode()) % jedisPools.length].getResource();
        return jedis;
    }

    public String set(final String key, final String value) {
        return getJedis(key).set(key, value);
    }

    public String set(final String key, final String value, final String nxxx, final String expx, final long time) {
        return getJedis(key).set(key, value, nxxx, expx, time);
    }

    public String get(final String key) {
        return getJedis(key).get(key);
    }

    public Boolean exists(final String key) {
        return getJedis(key).exists(key);
    }

    public Long setnx(final String key, final String value) {
        return getJedis(key).setnx(key, value);
    }

    public String setex(final String key, final int seconds, final String value) {
        return getJedis(key).setex(key, seconds, value);
    }

    public Long expire(final String key, final int seconds) {
        return getJedis(key).expire(key, seconds);
    }

    public Long incr(final String key) {
        return getJedis(key).incr(key);
    }

    public Long decr(final String key) {
        return getJedis(key).decr(key);
    }

    public Long hset(final String key, final String field, final String value) {
        return getJedis(key).hset(key, field, value);
    }

    public String hget(final String key, final String field) {
        return getJedis(key).hget(key, field);
    }

    public String hmset(final String key, final Map<String, String> hash) {
        return getJedis(key).hmset(key, hash);
    }

    public List<String> hmget(final String key, final String... fields) {
        return getJedis(key).hmget(key, fields);
    }

    public Long del(final String key) {
        return getJedis(key).del(key);
    }

    public Map<String, String> hgetAll(final String key) {
        return getJedis(key).hgetAll(key);
    }

    public void destroy() {
        JedisPool[] jedisPools = jedisPoolsRef.get();
        for (int i = 0; i < jedisPools.length; i++) {
            jedisPools[i].close();
        }
    }

}
