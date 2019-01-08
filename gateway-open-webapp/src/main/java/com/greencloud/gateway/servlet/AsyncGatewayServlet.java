package com.greencloud.gateway.servlet;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.greencloud.gateway.GatewayCallable;
import com.greencloud.gateway.GatewayRunner;
import com.greencloud.gateway.common.CatContext;
import com.greencloud.gateway.constants.GatewayConstants;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Async servlet
 * @author leejianhao
 */
public class AsyncGatewayServlet extends HttpServlet {
    private static Logger logger = LoggerFactory.getLogger(AsyncGatewayServlet.class);

    private DynamicIntProperty asyncTimeout = DynamicPropertyFactory.getInstance().getIntProperty(GatewayConstants.GATEWAY_SERVLET_ASYNC_TIMEOUT, 20000);
    private DynamicIntProperty coreSize = DynamicPropertyFactory.getInstance().getIntProperty(GatewayConstants.GATEWAY_THREADPOOL_CODE_SIZE, 200);
    private DynamicIntProperty maximumSize = DynamicPropertyFactory.getInstance().getIntProperty(GatewayConstants.GATEWAY_THREADPOOL_MAX_SIZE, 2000);
    private DynamicLongProperty aliveTime = DynamicPropertyFactory.getInstance().getLongProperty(GatewayConstants.GATEWAY_THREADPOOL_ALIVE_TIME, 1000 * 60 * 5);

    private GatewayRunner gatewayRunner = new GatewayRunner();
    private AtomicReference<ThreadPoolExecutor> poolExecutorRef = new AtomicReference<>();
    private AtomicLong rejectedRequests = new AtomicLong(0);

    @Override
    public void init() throws ServletException {
        reNewThreadPool();

        Runnable c = new Runnable() {
            @Override
            public void run() {
                ThreadPoolExecutor p = poolExecutorRef.get();
                p.setCorePoolSize(coreSize.get());
                p.setMaximumPoolSize(maximumSize.get());
                p.setKeepAliveTime(aliveTime.get(), TimeUnit.MILLISECONDS);
            }
        };

        coreSize.addCallback(c);
        maximumSize.addCallback(c);
        aliveTime.addCallback(c);

        // TODO metrics reporting

    }

    private void reNewThreadPool() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("gateway-pool-%d").build();
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(coreSize.get(), maximumSize.get(), aliveTime.get(), TimeUnit.MILLISECONDS, new SynchronousQueue<>(), namedThreadFactory);
        ThreadPoolExecutor old = poolExecutorRef.getAndSet(poolExecutor);
        if (old != null) {
            shutdownPoolExecutor(old);
        }
    }

    private void shutdownPoolExecutor(ThreadPoolExecutor old) {
        try {
            old.awaitTermination(5, TimeUnit.MINUTES);
            old.shutdown();
        } catch (InterruptedException e) {
            old.shutdownNow();
            logger.error("Shutdown Gateway Thread Pool:", e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Transaction t = Cat.newTransaction("AsyncGatewayServlet", req.getRequestURL().toString());
        req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(asyncTimeout.get());
        asyncContext.addListener(new AsyncGatewayListener());
        try {
            Cat.Context ctx = new CatContext();
            Cat.logRemoteCallClient(ctx);
            poolExecutorRef.get().submit(new GatewayCallable(ctx, asyncContext, gatewayRunner, req));
            t.setStatus(Transaction.SUCCESS);
        } catch (RuntimeException e) {
            Cat.logError(e);
            t.setStatus(e);
            rejectedRequests.incrementAndGet();
            throw e;
        } finally {
            t.complete();
        }
    }


    @Override
    public void destroy() {
        shutdownPoolExecutor(poolExecutorRef.get());
    }

}
