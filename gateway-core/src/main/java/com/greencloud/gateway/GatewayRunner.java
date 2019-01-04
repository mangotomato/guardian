package com.greencloud.gateway;

import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;
import com.greencloud.gateway.http.HttpServletRequestWrapper;
import com.greencloud.gateway.http.HttpServletResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author leejianhao
 */
public class GatewayRunner {
    private boolean bufferRequests;

    /**
     * Creates a new <code>GatewayRunner</code> instance.
     */
    public GatewayRunner() {
        this.bufferRequests = true;
    }

    /**
     * @param bufferRequests - whether to wrap the ServletRequest in HttpServletRequestWrapper and buffer the body.
     */
    public GatewayRunner(boolean bufferRequests) {
        this.bufferRequests = bufferRequests;
    }

    /**
     * sets HttpServlet request and HttpResponse
     *
     * @param servletRequest
     * @param servletResponse
     */
    public void init(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {

        RequestContext ctx = RequestContext.getCurrentContext();
        if (bufferRequests) {
            ctx.setRequest(new HttpServletRequestWrapper(servletRequest));
        } else {
            ctx.setRequest(servletRequest);
        }

        ctx.setResponse(new HttpServletResponseWrapper(servletResponse));
    }

    /**
     * executes "post" filterType  GatewayFilters
     *
     * @throws GatewayException
     */
    public void postRoute() throws GatewayException {
        FilterProcessor.getInstance().postRoute();
    }

    /**
     * executes "route" filterType  GatewayFilters
     *
     * @throws GatewayException
     */
    public void route() throws GatewayException {
        FilterProcessor.getInstance().route();
    }

    /**
     * executes "pre" filterType  GatewayFilters
     *
     * @throws GatewayException
     */
    public void preRoute() throws GatewayException {
        FilterProcessor.getInstance().preRoute();
    }

    /**
     * executes "error" filterType  GatewayFilters
     */
    public void error() {
        FilterProcessor.getInstance().error();
    }
}
