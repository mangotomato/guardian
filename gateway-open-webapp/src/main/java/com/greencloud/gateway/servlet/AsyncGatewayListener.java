package com.greencloud.gateway.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class AsyncGatewayListener implements AsyncListener {
    private static final Logger logger = LoggerFactory.getLogger(AsyncGatewayListener.class);

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        logger.error("Access {} timeout in AsyncServlet.", ((HttpServletRequest)event.getAsyncContext().getRequest()).getRequestURL());
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
        logger.error("Error while access {} in AsyncServlet.", ((HttpServletRequest)event.getAsyncContext().getRequest()).getRequestURL());
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
    }
}