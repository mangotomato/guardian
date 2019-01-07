package com.greencloud.gateway.util;

import org.junit.Test;

/**
 * @author leejianhao
 */
public class RedisUtilTest {

    @Test
    public void set() {
        RedisUtil.getInstance().set("test", "test");
    }

    @Test
    public void get() {
        String val = RedisUtil.getInstance().get("test");
        System.out.println(val);
    }

    @Test
    public void setNull() {
        RedisUtil.getInstance().setex("null", 1000, "");
    }

    @Test
    public void o() {
        System.out.println(System.currentTimeMillis());
    }
}
