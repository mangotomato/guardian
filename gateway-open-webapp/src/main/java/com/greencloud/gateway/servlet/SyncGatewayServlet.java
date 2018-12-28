package com.greencloud.gateway.servlet;

import com.dianping.cat.Cat;
import com.greencloud.gateway.GatewayRunner;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * sync servlet
 * @author leejianhao
 */
public class SyncGatewayServlet extends HttpServlet {
	
	private static final long serialVersionUID = -7314825620092836092L;

	private static Logger logger = LoggerFactory.getLogger(SyncGatewayServlet.class);
	
    private GatewayRunner gatewayRunner = new GatewayRunner();

    @Override
    public void service(javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) {
        try {

            init((HttpServletRequest) req, (HttpServletResponse) res);

            // marks this request as having passed through the "Gateway engine", as opposed to servlets
            // explicitly bound in web.xml, for which requests will not have the same data attached
            RequestContext.getCurrentContext().setGatewayEngineRan();

            try {
                preRoute();
            } catch (GatewayException e) {
                error(e);
                postRoute();
                return;
            }
            try {
                route();
            } catch (GatewayException e) {
                error(e);
                postRoute();
                return;
            }
            try {
                postRoute();
            } catch (GatewayException e) {
                error(e);
                return;
            }

        } catch (Throwable e) {
            error(new GatewayException(e, 500, "UNHANDLED_EXCEPTION_" + e.getClass().getName()));
        } finally {
        	RequestContext.getCurrentContext().unset();
        }
    }

    /**
     * executes "post" ZuulFilters
     *
     * @throws GatewayException
     */
    void postRoute() throws GatewayException {
        gatewayRunner.postRoute();
    }

    /**
     * executes "route" filters
     *
     * @throws GatewayException
     */
    void route() throws GatewayException {
        gatewayRunner.route();
    }

    /**
     * executes "pre" filters
     *
     * @throws GatewayException
     */
    void preRoute() throws GatewayException {
        gatewayRunner.preRoute();
    }

    /**
     * initializes request
     *
     * @param servletRequest
     * @param servletResponse
     */
    void init(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        gatewayRunner.init(servletRequest, servletResponse);
    }

	/**
	 * sets error context info and executes "error" filters
	 *
	 * @param e
	 * @throws GatewayException
	 */
	void error(GatewayException e) {
		try {
			RequestContext.getCurrentContext().setThrowable(e);
            gatewayRunner.error();
		} catch (Throwable t) {
			Cat.logError(t);
			logger.error(e.getMessage(), e);
		}finally{
			Cat.logError(e);
		}
	}

}
