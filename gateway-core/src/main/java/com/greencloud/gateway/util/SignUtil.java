package com.greencloud.gateway.util;

import com.greencloud.gateway.constants.Constants;
import com.greencloud.gateway.constants.HttpHeader;
import com.greencloud.gateway.constants.SystemHeader;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

/**
 * @author leejianhao
 */
public class SignUtil {

    /**
     * @param secret               AppKey
     * @param method               {@link com.greencloud.gateway.constants.HttpMethod}
     * @param path
     * @param querys
     * @param headers
     * @param bodys
     * @param signHeaderPrefixList 自定义参与签名Header前缀
     * @return 签名后的字符串
     */
    public static String sign(String secret, String method, String path,
                              Map<String, String> headers,
                              Map<String, String> querys,
                              Map<String, String> bodys,
                              List<String> signHeaderPrefixList) {
        try {
            Mac hmacSha256 = Mac.getInstance(Constants.HMAC_SHA256);
            byte[] keyBytes = secret.getBytes(Constants.ENCODING);
            hmacSha256.init(new SecretKeySpec(keyBytes, 0, keyBytes.length, Constants.HMAC_SHA256));
            return new String(Base64.encodeBase64(
                    hmacSha256.doFinal(buildStringToSign(method, path, headers, querys, bodys, signHeaderPrefixList).getBytes(Constants.ENCODING))),
                    Constants.ENCODING);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 构建待签名字符串
     * <p>
     * stringToSign: HttpMethod+"\n"+Accept+"\n"+Content-MD5+"\n"+Content-Type+"\n"+Date+"\n"+Headers+Url
     * <p>
     * 建议显示设置 Accept Header。当 Accept 为空时，部分 Http 客户端会给 Accept 设置默认值为 *\/*，导致签名校验失败
     *
     * @param method               {@link com.greencloud.gateway.constants.HttpMethod}
     * @param path
     * @param headers
     * @param querys
     * @param bodys
     * @param signHeaderPrefixList
     * @return
     */
    private static String buildStringToSign(String method, String path,
                                            Map<String, String> headers,
                                            Map<String, String> querys,
                                            Map<String, String> bodys,
                                            List<String> signHeaderPrefixList) {
        StringBuilder sb = new StringBuilder();

        sb.append(method.toUpperCase()).append(Constants.LF);
        if (null != headers) {
            if (null != headers.get(HttpHeader.ACCEPT)) {
                sb.append(headers.get(HttpHeader.ACCEPT));
            }
            sb.append(Constants.LF);
            if (null != headers.get(HttpHeader.CONTENT_MD5)) {
                sb.append(headers.get(HttpHeader.CONTENT_MD5));
            }
            sb.append(Constants.LF);
            if (null != headers.get(HttpHeader.CONTENT_TYPE)) {
                sb.append(headers.get(HttpHeader.CONTENT_TYPE));
            }
            sb.append(Constants.LF);
            if (null != headers.get(HttpHeader.DATE)) {
                sb.append(headers.get(HttpHeader.DATE));
            }
        }
        sb.append(Constants.LF);
        sb.append(buildHeaders(headers, signHeaderPrefixList));
        sb.append(buildResource(path, querys, bodys));
        return sb.toString();
    }

    /**
     * 构建待签名Path+Query+BODY
     * <p>
     * Url 指 Path + Query + Body 中 Form 参数，组织方法：对 Query+Form 参数按照字典对 Key 进行排序后按照如下方法拼接，如果 Query 或 Form 参数为空，则 Url = Path，不需要添加 ？，如果某个参数的 Value 为空只保留 Key 参与签名，等号不需要再加入签名。
     * <p>
     * 注意这里 Query 或 Form 参数的 Value 可能有多个，多个的时候只取第一个 Value 参与签名计算。
     *
     * @param path
     * @param querys
     * @param bodys
     * @return 待签名
     */
    private static String buildResource(String path, Map<String, String> querys, Map<String, String> bodys) {
        StringBuilder sb = new StringBuilder();

        if (!StringUtils.isBlank(path)) {
            sb.append(path);
        }
        Map<String, String> sortMap = new TreeMap<>();
        if (null != querys) {
            for (Map.Entry<String, String> query : querys.entrySet()) {
                if (!StringUtils.isBlank(query.getKey())) {
                    sortMap.put(query.getKey(), query.getValue());
                }
            }
        }

        if (null != bodys) {
            for (Map.Entry<String, String> body : bodys.entrySet()) {
                if (!StringUtils.isBlank(body.getKey())) {
                    sortMap.put(body.getKey(), body.getValue());
                }
            }
        }

        StringBuilder sbParam = new StringBuilder();
        for (Map.Entry<String, String> item : sortMap.entrySet()) {
            if (!StringUtils.isBlank(item.getKey())) {
                if (0 < sbParam.length()) {
                    sbParam.append(Constants.SPE3);
                }
                sbParam.append(item.getKey());
                if (!StringUtils.isBlank(item.getValue())) {
                    sbParam.append(Constants.SPE4).append(item.getValue());
                }
            }
        }
        if (0 < sbParam.length()) {
            sb.append(Constants.SPE5);
            sb.append(sbParam);
        }

        return sb.toString();
    }

    /**
     * 构建待签名Http头
     * <p>
     * HeaderKey1 + ":" + HeaderValue1 + "\n"\+HeaderKey2 + ":" + HeaderValue2 + "\n"\+...+HeaderKeyN + ":" + HeaderValueN + "\n"
     * <p>
     * 将 Headers 签名中 Header 的 Key 使用英文逗号分割放到 Request 的 Header 中，Key为：X-Ca-Signature-Headers
     *
     * @param headers              请求中所有的Http头
     * @param signHeaderPrefixList 自定义参与签名Header前缀
     * @return 待签名Http头
     */
    private static String buildHeaders(Map<String, String> headers, List<String> signHeaderPrefixList) {
        StringBuilder sb = new StringBuilder();

        if (null != signHeaderPrefixList) {
            signHeaderPrefixList.remove(SystemHeader.X_GW_SIGNATURE);
            signHeaderPrefixList.remove(SystemHeader.X_GW_SIGNATURE_HEADERS);
            signHeaderPrefixList.remove(HttpHeader.ACCEPT);
            signHeaderPrefixList.remove(HttpHeader.CONTENT_MD5);
            signHeaderPrefixList.remove(HttpHeader.CONTENT_TYPE);
            signHeaderPrefixList.remove(HttpHeader.DATE);
            Collections.sort(signHeaderPrefixList);
            if (null != headers) {
                headers = new HashMap<>(headers);
                Map<String, String> sortMap = new TreeMap<>();
                sortMap.putAll(headers);
                StringBuilder signHeadersStringBuilder = new StringBuilder();
                for (Map.Entry<String, String> header : sortMap.entrySet()) {
                    if (isHeaderToSign(header.getKey(), signHeaderPrefixList)) {
                        sb.append(header.getKey());
                        sb.append(Constants.SPE2);
                        if (!StringUtils.isBlank(header.getValue())) {
                            sb.append(header.getValue());
                        }
                        sb.append(Constants.LF);
                        if (0 < signHeadersStringBuilder.length()) {
                            signHeadersStringBuilder.append(Constants.SPE1);
                        }
                        signHeadersStringBuilder.append(header.getKey());
                    }
                }
                headers.put(SystemHeader.X_GW_SIGNATURE_HEADERS, signHeadersStringBuilder.toString());
            }
        }

        return sb.toString();
    }

    /**
     * Http头是否参与签名 return
     */
    private static boolean isHeaderToSign(String headerName, List<String> signHeaderPrefixList) {
        if (StringUtils.isBlank(headerName)) {
            return false;
        }

        if (headerName.equals(SystemHeader.X_GW_SIGNATURE) || headerName.equals(SystemHeader.X_GW_SIGNATURE_HEADERS) ) {
            return false;
        }

        if (headerName.startsWith(Constants.GW_HEADER_TO_SIGN_PREFIX_SYSTEM)) {
            return true;
        }

        if (null != signHeaderPrefixList) {
            for (String signHeaderPrefix : signHeaderPrefixList) {
                if (headerName.equalsIgnoreCase(signHeaderPrefix)) {
                    return true;
                }
            }
        }

        return false;
    }

}
