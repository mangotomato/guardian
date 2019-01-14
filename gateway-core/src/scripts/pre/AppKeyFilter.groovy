package com.greencloud.gateway.filters.pre;

import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.constants.Constants;
import com.greencloud.gateway.constants.HttpHeader;
import com.greencloud.gateway.constants.SystemHeader;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;
import com.greencloud.gateway.util.HTTPRequestUtil;
import com.greencloud.gateway.util.MessageDigestUtil;
import com.greencloud.gateway.util.SignUtil;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        String secret = getAppSecret();

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

        List<String> customSignHeaders = getCustomSignHeaders();

        assertSignature(request, secret, method, path, headers, querys, formBodys, customSignHeaders);

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
            throw new GatewayException("Invalid Signature", 403, "Invalid Signature");
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
            Map<String, String> formParams = HTTPRequestUtil.getInstance().leaveFirstIfMultiFormParamValue(formQueryString);
            return formParams;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getAppSecret() {
        return "default";
    }

    private List<String> getCustomSignHeaders(HttpServletRequest request) {
        String signatureHeaderString= request.getHeader(SystemHeader.X_GW_SIGNATURE_HEADERS);
        String[] signatureHeaders = StringUtils.split(signatureHeaderString, ',');
        return Arrays.asList(signatureHeaders);
    }

}
