package com.greencloud.gateway.http;

import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author leejianhao
 */
public class HttpServletRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {
    protected HttpServletRequest request;

    private byte[] body = null;

    public HttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
        Preconditions.checkNotNull(request);
        this.request = request;
        this.body = readBody(request);
    }

    private byte[] readBody(HttpServletRequest request) {
        if (this.request.getContentLength() > 0) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(this.request.getInputStream(), baos);
                return baos.toByteArray();
            } catch (IOException e) {
                return new byte[0];
            }
        }
        return new byte[0];
    }
}
