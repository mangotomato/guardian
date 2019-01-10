package com.greencloud.gateway.constants;

/**
 * @author leejianhao
 */
public class Constants {
    //签名算法HmacSha256
    public static final String HMAC_SHA256 = "HmacSHA256";
    //编码UTF-8
    public static final String ENCODING = "UTF-8";
    //换行符
    public static final String LF = "\n";
    //串联符
    public static final String SPE1 = ",";
    //示意符
    public static final String SPE2 = ":";
    //连接符
    public static final String SPE3 = "&";
    //赋值符
    public static final String SPE4 = "=";
    //问号符
    public static final String SPE5 = "?";

    //参与签名的系统Header前缀,只有指定前缀的Header才会参与到签名中
    public static final String GW_HEADER_TO_SIGN_PREFIX_SYSTEM = "x-gw-";

    public static final String CONTENT_TYPE_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

    /**
     * cat
     */
    public static final String CAT_CHILD_MESSAGE_ID = "X-CAT-CHILD-ID";
    public static final String CAT_PARENT_MESSAGE_ID = "X-CAT-PARENT-ID";
    public static final String CAT_ROOT_MESSAGE_ID = "X-CAT-ROOT-ID";


    /**
     * Prevent instantiation
     */
    private Constants() {
        throw new AssertionError("Must not instantiate constant utility class");
    }

}
