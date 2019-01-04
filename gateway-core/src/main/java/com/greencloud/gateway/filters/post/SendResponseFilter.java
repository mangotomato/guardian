package com.greencloud.gateway.filters.post;

import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.constants.GatewayConstants;
import com.greencloud.gateway.constants.HttpHeader;
import com.greencloud.gateway.context.Debug;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.util.HTTPRequestUtil;
import com.netflix.util.Pair;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

/**
 * Send response to client
 * @author leejianhao
 */
public class SendResponseFilter extends GatewayFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SendResponseFilter.class);

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 1000;
    }

    @Override
    public boolean shouldFilter() {
        return !RequestContext.getCurrentContext().getOriginResponseHeaders().isEmpty()
                || RequestContext.getCurrentContext().getResponseDataStream() != null
                || RequestContext.getCurrentContext().getResponseBody() != null;
    }

    @Override
    public Object run() {
        addResponseHeaders();
        try {
            writeResponse();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        } finally {
            RequestContext.getCurrentContext().set("RouteHandled");
        }
        return null;
    }

    private void addResponseHeaders() {
        RequestContext ctx = RequestContext.getCurrentContext();
        List<Pair<String, String>> responseHeaders = ctx.getGatewayResponseHeaders();
        if (responseHeaders != null) {
            for (Pair<String, String> pair : responseHeaders) {
                ctx.getResponse().addHeader(pair.first(), pair.second());
                Debug.addRequestDebug("OUTBOUND: <  " + pair.first() + ":" + pair.second());
            }
        }
        Long contentLength = ctx.getOriginContentLength();
        if (contentLength != null && !ctx.getResponseGZipped()) {
            ctx.getResponse().setContentLength(contentLength.intValue());
        }
    }

    private void writeResponse() throws Exception {
        RequestContext ctx = RequestContext.getCurrentContext();

        // there is no body to send
        if (ctx.getResponseDataStream() == null && ctx.getResponseBody() == null) {
            return;
        }

        HttpServletResponse servletResponse = ctx.getResponse();
        servletResponse.setCharacterEncoding(GatewayConstants.DEFAULT_CHARACTER_ENCODING);

        OutputStream outStream = servletResponse.getOutputStream();
        InputStream is = null;
        try {
            if (RequestContext.getCurrentContext().getResponseBody() != null) {
                String body = RequestContext.getCurrentContext().getResponseBody();
                IOUtils.copy(new ByteArrayInputStream(body.getBytes(GatewayConstants.DEFAULT_CHARACTER_ENCODING)), outStream);
                return;
            }
            boolean isGzipRequest = HTTPRequestUtil.getInstance().isGzippedRequest(ctx.getRequest());
            is = ctx.getResponseDataStream();
            InputStream inputStream = is;
            if (is != null) {
                // if response is gzipped, but is not gzip request
                // decompress before sending response to client
                if (ctx.getResponseGZipped() && !isGzipRequest) {
                    final Long contentLength = ctx.getOriginContentLength();
                    if (contentLength == null || contentLength > 0) {
                        try {
                            // decompress stream from gzip input
                            inputStream = new GZIPInputStream(is);
                        } catch (ZipException ex) {
                            LOG.debug("gzip decompress exception, received response is not gzip compressed. service url: {}",
                                    RequestContext.getCurrentContext().getRequest().getRequestURL());
                            inputStream = is;
                        }
                    }
                } else if (ctx.getResponseGZipped() && isGzipRequest) {
                    servletResponse.setHeader(HttpHeader.CONTENT_ENCODING, "gzip");
                }
                IOUtils.copy(inputStream, outStream);
            }
        } finally {
            try {
                if (is != null) {
                        is.close();
                }
                outStream.flush();
            } catch (IOException ignored) {
            }
        }
    }

}
