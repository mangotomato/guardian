package com.greencloud.gateway.util;

import com.greencloud.gateway.constants.HttpHeader;
import com.greencloud.gateway.constants.HttpMethod;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author leejianhao
 */
public class SignUtilTest {
    //APP KEY
    private final static String APP_KEY = "app_key";
    // APP密钥
    private final static String APP_SECRET = "APP_SECRET";
    //自定义参与签名Header前缀（可选,默认只有"X-Gw-"开头的参与到Header签名）
    private final static List<String> CUSTOM_HEADERS_TO_SIGN_PREFIX = new ArrayList<>();

    @Test
    public void testSign() {

        //请求path
        String path = "/s/weather";

        Map<String, String> headers = new HashMap<>();
        //（必填）根据期望的Response内容类型设置
        headers.put(HttpHeader.ACCEPT, "application/json");
        headers.put("a-header1", "header1Value");
        headers.put("b-header2", "header2Value");

        CUSTOM_HEADERS_TO_SIGN_PREFIX.clear();
        CUSTOM_HEADERS_TO_SIGN_PREFIX.add("a-header1");
        CUSTOM_HEADERS_TO_SIGN_PREFIX.add("a-header2");

        //请求的query
        Map<String, String> querys = new HashMap<>();
        querys.put("a-query1", "query1Value");
        querys.put("b-query2", "query2Value");

        String signature = SignUtil.sign(APP_SECRET, HttpMethod.GET, path, headers, querys, null, CUSTOM_HEADERS_TO_SIGN_PREFIX);

        System.out.println(signature);

    }
}
