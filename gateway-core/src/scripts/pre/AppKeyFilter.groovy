package com.greencloud.gateway.filters.pre

import com.google.common.base.Strings
import com.greencloud.gateway.GatewayFilter
import com.greencloud.gateway.common.datasource.DataSourceHolder
import com.greencloud.gateway.constants.Constants
import com.greencloud.gateway.constants.HttpHeader
import com.greencloud.gateway.constants.SystemHeader
import com.greencloud.gateway.context.RequestContext
import com.greencloud.gateway.exception.GatewayException
import com.greencloud.gateway.util.HTTPRequestUtil
import com.greencloud.gateway.util.MessageDigestUtil
import com.greencloud.gateway.util.RedisUtil
import com.greencloud.gateway.util.SignUtil
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils

import javax.servlet.http.HttpServletRequest
import javax.sql.DataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * @author leejianhao
 */
public class AppKeyFilter extends GatewayFilter {
    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        return !RequestContext.getCurrentContext().isHealthCheckRequest();
    }

    @Override
    public Object run() throws GatewayException {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();

        String method = request.getMethod().toUpperCase();
        String path = request.getRequestURI();

        String appKey = request.getHeader(SystemHeader.X_GW_KEY);
        String appSecret = getAppSecret(appKey);

        Map<String, String> headers = getHeadersForSign();
        Map<String, String> querys = getQuerysForSign();
        Map<String, String> formBodys = null;

        // Body里Form表单数据是否需要签名，当content-type为'application/x-www-form-urlencoded'时需要签名
        if (formRequest(request)) {
            formBodys = getFormBodysForSign(request);
        } else if (contentMD5(request)) {
            // 如果Body为非Form表单时，客户端可以通过请求头Content-MD5校验body
            assertContentMD5Equal(request);
        }

        List<String> customSignHeaders = getCustomSignHeaders(request);

        assertSignature(request, appSecret, method, path, headers, querys, formBodys, customSignHeaders);

        // go further
        return null;
    }

    private void assertContentMD5Equal(HttpServletRequest request) throws GatewayException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
            String contentMD5Real = MessageDigestUtil.base64AndMD5(IOUtils.toString(reader));
            String contentMD5Expect = request.getHeader(HttpHeader.CONTENT_MD5);
            if (!contentMD5Expect.equals(contentMD5Real)) {
                throw new GatewayException("Invalid Signature", 400, "Invalid Request Body");
            }
        } catch (IOException e) {
        }
    }

    private void assertSignature(HttpServletRequest request, String secret, String method, String path,
                                 Map<String, String> headers, Map<String, String> querys, Map<String, String> bodys,
                                 List<String> customSignHeaders) throws GatewayException {

        String signatureReal = SignUtil.sign(secret, method, path, headers, querys, bodys, customSignHeaders);
        String signatureExpect = request.getHeader(SystemHeader.X_GW_SIGNATURE);
        if (!signatureReal.equals(signatureExpect)) {
            throw new GatewayException("Invalid Signature", 403,
                    "Invalid Signature,stringToSign: " + RequestContext.getCurrentContext().get("stringToSign"));
        }

    }

    private Map<String, String> getHeadersForSign() {
        return HTTPRequestUtil.getInstance().getRequestHeaders();
    }

    private Map<String, String> getQuerysForSign() {
        return HTTPRequestUtil.getInstance().leaveFirstIfMultiQueryParamValue();
    }

    private boolean contentMD5(HttpServletRequest request) {
        return request.getHeader(HttpHeader.CONTENT_MD5) != null;
    }

    private boolean formRequest(HttpServletRequest request) {
        String contentType = request.getHeader(HttpHeader.CONTENT_TYPE);
        return Constants.CONTENT_TYPE_X_WWW_FORM_URLENCODED.equalsIgnoreCase(contentType);
    }

    private Map<String, String> getFormBodysForSign(HttpServletRequest request) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
            String formQueryString = IOUtils.toString(reader);
            Map<String, String> formParams = HTTPRequestUtil.getInstance().leaveFirstIfMultiFormParamValue
            (formQueryString);
            return formParams;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final String SQL = "select app_secret from app where app_key = ?";

    private String getAppSecret(String appKey) {
        String appSecret = "";

        if (Strings.isNullOrEmpty(appKey)) {
            return appSecret;
        }

        // from cache
        String redisKey = Constants.APP_KEY_REDIS_PREFIX + appKey;
        appSecret = RedisUtil.getInstance().get(redisKey);
        if (!Strings.isNullOrEmpty(appSecret)) {
            return appSecret;
        }

        // from db
        try {
            DataSource dataSource = DataSourceHolder.getInstance().getDataSource();
            Connection connection = dataSource.getConnection();

            PreparedStatement ps = null;
            ResultSet r = null;
            try {
                connection.setAutoCommit(true);
                ps = connection.prepareStatement(SQL);
                ps.setString(1, appKey);
                r = ps.executeQuery();

                appSecret = r.next() ? r.getString(1) : appSecret;

                if (!Strings.isNullOrEmpty(appSecret)) {
                    RedisUtil.getInstance().set(redisKey, appSecret);
                }

            } finally {
                try {
                    if (r != null) {
                        r.close();
                    }

                    if (ps != null) {
                        ps.close();
                    }
                } finally {
                    connection.close();
                }
            }
        } catch (Exception ignored) {
        }

        return appSecret;
    }

    private List<String> getCustomSignHeaders(HttpServletRequest request) {
        String signatureHeaderString = request.getHeader(SystemHeader.X_GW_SIGNATURE_HEADERS);
        if (Strings.isNullOrEmpty(signatureHeaderString)) {
            return Collections.emptyList();
        }
        String[] signatureHeaders = StringUtils.split(signatureHeaderString, ',');
        return Arrays.asList(signatureHeaders);
    }

}
