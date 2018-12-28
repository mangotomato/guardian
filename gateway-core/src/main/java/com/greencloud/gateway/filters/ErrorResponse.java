package com.greencloud.gateway.filters;

import com.google.common.base.Strings;
import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author leejianhao
 */
public class ErrorResponse extends GatewayFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorResponse.class);

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
        String errorCause="Gateway-Error-Unknown-Cause";
        int responseStatusCode = 500;

        if (ex instanceof GatewayException) {
            GatewayException gatewayException = (GatewayException) ex;
            String cause = gatewayException.errorCause;
            responseStatusCode = gatewayException.nStatusCode;
            if (!Strings.isNullOrEmpty(cause)) {
                errorCause = cause;
            }
        }

        RequestContext.getCurrentContext().getResponse().addHeader("X-Gateway-Error-Cause", "Gateway Error: " + errorCause);

        if (getOverrideStatusCode()) {
            RequestContext.getCurrentContext().setResponseStatusCode(200);
        } else {
            RequestContext.getCurrentContext().setResponseStatusCode(responseStatusCode);
        }

        //Don't continue
        ctx.setSendGatewayResponse(false);
        ctx.setResponseBody(getErrorMessage(errorCause, responseStatusCode));
        //ErrorResponse was handled
        ctx.errorHandled();
        return null;
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