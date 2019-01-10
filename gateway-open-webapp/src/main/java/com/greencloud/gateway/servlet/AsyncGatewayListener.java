package com.greencloud.gateway.servlet;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.greencloud.gateway.constants.GatewayConstants;
import com.greencloud.gateway.constants.HttpHeader;
import com.greencloud.gateway.constants.SystemHeader;
import com.greencloud.gateway.context.RequestContext;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class AsyncGatewayListener implements AsyncListener {
    private static final Logger logger = LoggerFactory.getLogger(AsyncGatewayListener.class);

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        String url = ((HttpServletRequest)event.getAsyncContext().getRequest()).getRequestURL().toString();
//        try {
//            Transaction tran = Cat.newTransaction("Timeout", url);
//            HttpServletResponse servletResponse = (HttpServletResponse) event.getAsyncContext().getResponse();
//            OutputStream outputStream = servletResponse.getOutputStream();
//            try {
//                String body = "{\"msg\":\"Timeout\","+"\"result\":\"504\"}";
//                IOUtils.copy(new ByteArrayInputStream(body.getBytes(GatewayConstants.DEFAULT_CHARACTER_ENCODING)), outputStream);
//                outputStream.flush();
//                tran.setStatus(Transaction.SUCCESS);
//            } catch (Exception e) {
//                tran.setStatus(e);
//            } finally {
//                tran.complete();
//            }
//        } catch (Exception ignored) {
//        }
        HttpServletResponse response = (HttpServletResponse) event.getAsyncContext().getResponse();
        response.setStatus(504);
        response.addHeader(SystemHeader.X_GW_ERROR_CAUSE, "Timeout");
        response.addHeader(HttpHeader.CONTENT_TYPE, GatewayConstants.DEFAULT_CONTENT_TYPE);
        logger.error("Access {} timeout in AsyncServlet.", url);
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
        logger.error("Error while access {} in AsyncServlet.", ((HttpServletRequest)event.getAsyncContext().getRequest()).getRequestURL());
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
    }
}