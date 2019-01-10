package com.greencloud.gateway.constants;

/**
 * @author leejianhao
 */
public class SystemHeader {
    /* -------------- 系统级Header -------------- */

    /*
        防重放： timestamp + nonce （15分钟内AppKey+API+Nonce不能重复）
        首先判断客户端传入的timestamp和网关服务器的时间差在15分钟内，再比较nonce是否重复。
        客户端第一次访问时，会将timestamp, api, nonce放在缓存中，缓存有效时间和timestamp一致。
     */

    // AppKey
    public static final String X_GW_KEY = "x-gw-key";
    // 签名字符串
    public static final String X_GW_SIGNATURE = "x-gw-signature";

    // API调用这传递时间戳，值为时间的毫秒数，1970年1月1日至今的时间转为毫秒，时间戳有效时间为15分钟
    public static final String X_GW_TIMESTAMP = "x-gw-timestamp";

    // API调用者生成的UUID，结合时间戳防止重放入
    public static final String X_GW_NONCE = "x-gw-nonce";

    // 指定要加入签名计算的Header,多个用英文逗号,分割
    public static final String X_GW_SIGNATURE_HEADERS = "x-gw-signature-headers";

    // API的环境，支持（TEST、PRE、RELEASE），默认为RELEASE
    public static final String X_GW_STAGE = "x-gw-stage";

    // API调试模式
    public static final String X_GW_REQUEST_MODE = "x-gw-request-mode";
    // API版本号，暂时不用
    public static final String X_GW_VERSION = "x-gw-version";

    public static final String X_GW_ERROR_CAUSE = "x-gw-error-cause";

    /* -------------- 系统级Header -------------- */

    /**
     * Prevent instantiation
     */
    private SystemHeader() {
        throw new AssertionError("Must not instantiate constant utility class");
    }


}
