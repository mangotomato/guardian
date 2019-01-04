package com.greencloud.gateway.constants;

/**
 * http header case-insensitive, tomcat will transform headers to lowercase
 * @author
 */
public class HttpHeader {
    public static final String TRANSFER_ENCODING = "transfer-encoding";
    public static final String CHUNKED = "chunked";
    public static final String ORIGIN = "origin";
    public static final String CONTENT_ENCODING = "content-encoding";
    public static final String ACCEPT_ENCODING = "accept-encoding";
    public static final String CONNECTION = "connection";
    public static final String KEEP_ALIVE = "keep-alive";
    public static final String X_ORIGINATING_URL = "x-originating-url";
    public static final String X_FORWARDED_PROTO = "x-forwarded-proto";
    public static final String X_FORWARDED_FOR = "x-forwarded-for";

    public static final String HOST = "host";
    public static final String USER_AGENT= "user-agent";
    public static final String CONTENT_TYPE= "content-type";
    public static final String ACCEPT = "accept";
    public static final String DATE = "date";

    // 当请求Body为非Form(content-type: application/x-www-form-urlencoded)表单时,可以计算Body的MD5，作为签名的参数
    // RFC1864
    public static final String CONTENT_MD5 = "content-md5";

    /** Prevent instantiation */
    private HttpHeader() {
        throw new AssertionError("Must not instantiate constant utility class");
    }

}