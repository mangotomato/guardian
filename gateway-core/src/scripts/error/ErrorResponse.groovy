package com.greencloud.gateway.filters.error;

import com.google.common.base.Strings;
import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.constants.GatewayConstants;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;

/**
 *
 * @author leejianhao
 */
public class ErrorResponse extends GatewayFilter {

    private static final Logger logger = LoggerFactory.getLogger(com.greencloud.gateway.filters.error.ErrorResponse.class);

    @Override
    public String filterType() {
        return "error";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext context = RequestContext.getCurrentContext();
        return context.getThrowable() != null && !context.errorHandled();
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        Throwable ex = ctx.getThrowable();
        try {
            String errorCause = "GW-Error-Unknown-Cause";
            int responseStatusCode = 500;

            if (ex instanceof GatewayException) {
                GatewayException gatewayException = (GatewayException) ex;
                String cause = gatewayException.errorCause;
                responseStatusCode = gatewayException.nStatusCode;
                if (!Strings.isNullOrEmpty(cause)) {
                    errorCause = cause;
                }
            }

            RequestContext.getCurrentContext().getResponse().addHeader("X-GW-Error-Cause", "Gateway Error: " + errorCause);

            if (getOverrideStatusCode()) {
                RequestContext.getCurrentContext().setResponseStatusCode(200);
            } else {
                RequestContext.getCurrentContext().setResponseStatusCode(responseStatusCode);
            }

            //Don't continue
            ctx.setSendGatewayResponse(false);
            ctx.setResponseBody(getErrorMessage(errorCause, responseStatusCode));
            //The error throws by post filters must output
            tryFlushResponse(ctx);
        } finally {
            //ErrorResponse was handled
            ctx.errorHandled();
        }

        return null;
    }

    void tryFlushResponse(RequestContext ctx) {
        if (ctx.get("errorFlush") != null) {
            try {
                HttpServletResponse servletResponse = ctx.getResponse();
                OutputStream outputStream = servletResponse.getOutputStream();
                String body = RequestContext.getCurrentContext().getResponseBody();
                IOUtils.copy(new ByteArrayInputStream(body.getBytes(GatewayConstants.DEFAULT_CHARACTER_ENCODING)), outputStream);
                outputStream.flush();
            } catch (Exception e) {
                logger.error("error to write response", e);
            }
        }
    }

    String getErrorMessage(String errorCause, int status) {
        String format = getOutputType();
        switch (format) {
            case "json":
                RequestContext.getCurrentContext().getResponse().setContentType("application/json");
                return "{\"msg\":\""+errorCause+"\","+"\"result\":\""+status+"\"}";
            case "xml":
                RequestContext.getCurrentContext().getResponse().setContentType("application/xml");
                return "<result>"+status+"</result><msg>"+errorCause+"</msg>";
            default:
                RequestContext.getCurrentContext().getResponse().setContentType("application/json");
                return "{\"msg\":\""+errorCause+"\","+"\"result\":\""+status+"\"}";
        }
    }

    boolean getOverrideStatusCode() {
        String override = RequestContext.getCurrentContext().getRequest().getParameter("override_error_status");
        if (getCallback() != null) {
            return true;
        }
        if (override == null) {
            return false;
        }
        return Boolean.valueOf(override);
    }

    String getCallback() {
        String callback = RequestContext.getCurrentContext().getRequest().getParameter("callback");
        if (callback == null) {
            return null;
        }
        return callback;
    }

    String getOutputType() {
        String output = RequestContext.getCurrentContext().getRequest().getParameter("output");
        if (output == null) {
            return "json";
        }
        return output;
    }

    String getVersion() {
        String version = RequestContext.getCurrentContext().getRequest().getParameter("v");
        if (version == null) {
            return "1";
        }
        if (getOverrideStatusCode()) {
            return "1";
        }
        return version;
    }

}