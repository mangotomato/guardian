package com.greencloud.gateway;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.message.internal.DefaultMessageProducer;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.Callable;

/**
 * @author leejianhao
 */
public class GatewayCallable implements Callable {
    private static Logger logger = LoggerFactory.getLogger(GatewayCallable.class);

    private AsyncContext ctx;
    private GatewayRunner gatewayRunner;
    private Cat.Context catCtx;
    private HttpServletRequest request;

    public GatewayCallable(Cat.Context catContext, AsyncContext asyncContext, GatewayRunner gatewayRunner,
                           HttpServletRequest request) {
        this.ctx = asyncContext;
        this.gatewayRunner = gatewayRunner;
        this.catCtx = catContext;
        this.request = request;
    }

    @Override
    public Object call() throws Exception {
        Cat.logRemoteCallServer(catCtx);
        Transaction tran = Cat.getProducer().newTransaction("GatewayCallable", request.getRequestURL().toString());
        long start = System.currentTimeMillis();
        try {
            service(ctx.getRequest(), ctx.getResponse());
            tran.setStatus(Transaction.SUCCESS);
        } catch (Throwable t) {
            logger.error("ZuulCallable execute error.", t);
            Cat.logError(t);
            tran.setStatus(t);
        } finally {
            try {
                reportStat(RequestContext.getCurrentContext(), start);
            } catch (Throwable t) {
                Cat.logError("ZuulCallable collect stats error.", t);
            }
            try {
                ctx.complete();
            } catch (Throwable t) {
                Cat.logError("AsyncContext complete error.", t);
            }
            RequestContext.getCurrentContext().unset();
            tran.complete();
        }
        return null;
    }

    protected void service(ServletRequest req, ServletResponse res) {
        try {

            init((HttpServletRequest) req, (HttpServletResponse) res);

            // marks this request as having passed through the "Zuul engine", as
            // opposed to servlets
            // explicitly bound in web.xml, for which requests will not have the
            // same data attached
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
        }
    }

    private void init(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        gatewayRunner.init(servletRequest, servletResponse);
    }

    void preRoute() throws GatewayException {
        gatewayRunner.preRoute();
    }

    void route() throws GatewayException {
        gatewayRunner.route();
    }

    void postRoute() throws GatewayException {
        gatewayRunner.postRoute();
    }

    void error(GatewayException e) {
        RequestContext.getCurrentContext().setThrowable(e);
        gatewayRunner.error();
    }

    private void reportStat(RequestContext gateContext, long start) {

        long remoteServiceCost = 0L;
        Object remoteCallCost = gateContext.get("remoteCallCost");
        if (remoteCallCost != null) {
            try {
                remoteServiceCost = Long.parseLong(remoteCallCost.toString());
            } catch (Exception ignore) {
            }
        }

        long replyClientCost = 0L;
        Object sendResponseCost = gateContext.get("sendResponseCost");
        if (sendResponseCost != null) {
            try {
                replyClientCost = Long.parseLong(sendResponseCost.toString());
            } catch (Exception ignore) {
            }
        }

        long replyClientReadCost = 0L;
        Object sendResponseReadCost = gateContext.get("sendResponseCost:read");
        if (sendResponseReadCost != null) {
            try {
                replyClientReadCost = Long.parseLong(sendResponseReadCost.toString());
            } catch (Exception ignore) {
            }
        }

        long replyClientWriteCost = 0L;
        Object sendResponseWriteCost = gateContext.get("sendResponseCost:write");
        if (sendResponseWriteCost != null) {
            try {
                replyClientWriteCost = Long.parseLong(sendResponseWriteCost.toString());
            } catch (Exception ignore) {
            }
        }

        if (gateContext.sendGatewayResponse()) {
//            URL routeUrl = gateContext.getRouteUrl();
//            if (routeUrl == null) {
//                logger.warn("Unknown Route: [ {" + gateContext.getRequest().getRequestURL() + "} ]");
//            }
        }

        // TODO report metrics
    }
}
