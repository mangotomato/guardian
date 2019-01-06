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
}
