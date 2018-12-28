package com.greencloud.gateway.context;

import com.greencloud.gateway.constants.GatewayHeaders;
import com.greencloud.gateway.util.DeepCopy;
import com.netflix.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请求上下文
 * @author leejianhao
 */
public class RequestContext extends ConcurrentHashMap<String, Object> {

    private static final Logger LOG = LoggerFactory.getLogger(RequestContext.class);

    protected static Class<? extends RequestContext> contextClass = RequestContext.class;

    private static RequestContext testContext = null;

    protected static final ThreadLocal<? extends RequestContext> threadLocal = new ThreadLocal<RequestContext>() {
        @Override
        protected RequestContext initialValue() {
            try {
                return contextClass.newInstance();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    };


    public RequestContext() {
        super();
    }

    /**
     * Override the default RequestContext
     *
     * @param clazz
     */
    public static void setContextClass(Class<? extends RequestContext> clazz) {
        contextClass = clazz;
    }

    /**
     * set an overriden "test" context
     *
     * @param context
     */
    public static void testSetCurrentContext(RequestContext context) {
        testContext = context;
    }

    /**
     * Get the current RequestContext
     *
     * @return the current RequestContext
     */
    public static RequestContext getCurrentContext() {
        if (testContext != null) {
            return testContext;
        }

        RequestContext context = threadLocal.get();
        return context;
    }

    /**
     * Convenience method to return a boolean value for a given key
     *
     * @param key
     * @return true or false depending what was set. default is false
     */
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * Convenience method to return a boolean value for a given key
     *
     * @param key
     * @param defaultResponse
     * @return true or false depending what was set. default defaultResponse
     */
    public boolean getBoolean(String key, boolean defaultResponse) {
        Boolean b = (Boolean) get(key);
        if (b != null) {
            return b.booleanValue();
        }
        return defaultResponse;
    }

    /**
     * sets a key value to Boolen.TRUE
     *
     * @param key
     */
    public void set(String key) {
        put(key, Boolean.TRUE);
    }

    /**
     * puts the key, value into the map. a null value will remove the key from the map
     *
     * @param key
     * @param value
     */
    public void set(String key, Object value) {
        if (value != null) {
            put(key, value);
        }
        else {
            remove(key);
        }
    }

    /**
     * true if  gatewayEngineRan
     *
     * @return
     */
    public boolean getGatewayEngineRan() {
        return getBoolean("gatewayEngineRan");
    }

    /**
     * sets gatewayEngineRan to true
     */
    public void setGatewayEngineRan() {
        put("gatewayEngineRan", true);
    }

    /**
     * @return the HttpServletRequest from the "request" key
     */
    public HttpServletRequest getRequest() {
        return (HttpServletRequest) get("request");
    }

    /**
     * sets the HttpServletRequest into the "request" key
     *
     * @param request
     */
    public void setRequest(HttpServletRequest request) {
        put("request", request);
    }

    /**
     * @return the HttpServletResponse from the "response" key
     */
    public HttpServletResponse getResponse() {
        return (HttpServletResponse) get("response");
    }

    /**
     * sets the "response" key to the HttpServletResponse passed in
     *
     * @param response
     */
    public void setResponse(HttpServletResponse response) {
        set("response", response);
    }

    /**
     * returns a set throwable
     *
     * @return a set throwable
     */
    public Throwable getThrowable() {
        return (Throwable) get("throwable");

    }

    /**
     * sets a throwable
     *
     * @param th
     */
    public void setThrowable(Throwable th) {
        put("throwable", th);

    }

    /**
     * sets  debugRouting
     *
     * @param bDebug
     */
    public void setDebugRouting(boolean bDebug) {
        set("debugRouting", bDebug);
    }

    /**
     * @return "debugRouting"
     */
    public boolean debugRouting() {
        return getBoolean("debugRouting");
    }

    /**
     * sets "debugRequestHeadersOnly" to bHeadersOnly
     *
     * @param bHeadersOnly
     */
    public void setDebugRequestHeadersOnly(boolean bHeadersOnly) {
        set("debugRequestHeadersOnly", bHeadersOnly);

    }

    /**
     * @return "debugRequestHeadersOnly"
     */
    public boolean debugRequestHeadersOnly() {
        return getBoolean("debugRequestHeadersOnly");
    }

    /**
     * sets "debugRequest"
     *
     * @param bDebug
     */
    public void setDebugRequest(boolean bDebug) {
        set("debugRequest", bDebug);
    }

    /**
     * gets debugRequest
     *
     * @return debugRequest
     */
    public boolean debugRequest() {
        return getBoolean("debugRequest");
    }

    /**
     * removes "routeHost" key
     */
    public void removeRouteHost() {
        remove("routeHost");
    }

    /**
     * sets routeHost
     *
     * @param routeHost a URL
     */
    public void setRouteHost(URL routeHost) {
        set("routeHost", routeHost);
    }

    /**
     * @return "routeHost" URL
     */
    public URL getRouteHost() {
        return (URL) get("routeHost");
    }

    public void setRouteUrl(String url) {
        set("routeUrl", url);
    }

    public String getRouteUrl() {
        return (String)get("routeUrl");
    }

    /**
     * appends filter name and status to the filter execution history for the
     * current request
     * 
     * @param name   filter name
     * @param status execution status
     * @param time   execution time in milliseconds
     */
    public void addFilterExecutionSummary(String name, String status, long time) {
            StringBuilder sb = getFilterExecutionSummary();
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(name).append('[').append(status).append(']').append('[').append(time).append("ms]");
    }

    /**
     * @return String that represents the filter execution history for the current request
     */
    public StringBuilder getFilterExecutionSummary() {
        if (get("executedFilters") == null) {
            putIfAbsent("executedFilters", new StringBuilder());
        }
        return (StringBuilder) get("executedFilters");
    }
    
    /**
     * sets the "responseBody" value as a String. This is the response sent back to the client.
     *
     * @param body
     */
    public void setResponseBody(String body) {
        set("responseBody", body);
    }

    /**
     * @return the String response body to be snt back to the requesting client
     */
    public String getResponseBody() {
        return (String) get("responseBody");
    }

    /**
     * sets the InputStream of the response into the responseDataStream
     *
     * @param responseDataStream
     */
    public void setResponseDataStream(InputStream responseDataStream) {
        set("responseDataStream", responseDataStream);
    }

    /**
     * sets the flag responseGZipped if the response is gzipped
     *
     * @param gzipped
     */
    public void setResponseGZipped(boolean gzipped) {
        put("responseGZipped", gzipped);
    }

    /**
     * @return true if responseGZipped is true (the response is gzipped)
     */
    public boolean getResponseGZipped() {
        return getBoolean("responseGZipped", true);
    }

    /**
     * sets the flag errorHandled if there is an exception.
     *
     * @param handled
     */
    public void setErrorHandled(boolean handled) {
        put("errorHandled", handled);
    }

    /**
     * @return true if there is an exception and has been handled.
     */
    public boolean errorHandled() {
        return getBoolean("errorHandled", false);
    }

    /**
     * @return the InputStream Response
     */
    public InputStream getResponseDataStream() {
        return (InputStream) get("responseDataStream");
    }

    /**
     * If this value is true then the response should be sent to the client.
     *
     * @return
     */
    public boolean sendGatewayResponse() {
        return getBoolean("sendGatewayResponse", true);
    }

    /**
     * sets the sendGatewayResponse boolean
     *
     * @param bSend
     */
    public void setSendGatewayResponse(boolean bSend) {
        set("sendGatewayResponse", Boolean.valueOf(bSend));
    }

    /**
     * returns the response status code. Default is 200
     *
     * @return
     */
    public int getResponseStatusCode() {
        return get("responseStatusCode") != null ? (Integer) get("responseStatusCode") : 500;
    }


    /**
     * Use this instead of response.setStatusCode()
     *
     * @param nStatusCode
     */
    public void setResponseStatusCode(int nStatusCode) {
        getResponse().setStatus(nStatusCode);
        set("responseStatusCode", nStatusCode);
    }

    /**
     * add a header to be sent to the origin
     *
     * @param name
     * @param value
     */
    public void addGatewayRequestHeader(String name, String value) {
        getGatewayRequestHeaders().put(name.toLowerCase(), value);
    }

    /**
     * return the list of requestHeaders to be sent to the origin
     *
     * @return the list of requestHeaders to be sent to the origin
     */
    public Map<String, String> getGatewayRequestHeaders() {
        if (get("gatewayRequestHeaders") == null) {
            HashMap<String, String> gatewayRequestHeaders = new HashMap<String, String>();
            putIfAbsent("gatewayRequestHeaders", gatewayRequestHeaders);
        }
        return (Map<String, String>) get("gatewayRequestHeaders");
    }

    /**
     * add a header to be sent to the response
     *
     * @param name
     * @param value
     */
    public void addGatewayResponseHeader(String name, String value) {
        getGatewayResponseHeaders().add(new Pair<String, String>(name, value));
    }

    /**
     * returns the current response header list
     *
     * @return a List<Pair<String, String>>  of response headers
     */
    public List<Pair<String, String>> getGatewayResponseHeaders() {
        if (get("gatewayResponseHeaders") == null) {
            List<Pair<String, String>> gatewayRequestHeaders = new ArrayList<Pair<String, String>>();
            putIfAbsent("gatewayResponseHeaders", gatewayRequestHeaders);
        }
        return (List<Pair<String, String>>) get("gatewayResponseHeaders");
    }

    /**
     * the Origin response headers
     *
     * @return the List<Pair<String, String>> of headers sent back from the origin
     */
    public List<Pair<String, String>> getOriginResponseHeaders() {
        if (get("originResponseHeaders") == null) {
            List<Pair<String, String>> originResponseHeaders = new ArrayList<Pair<String, String>>();
            putIfAbsent("originResponseHeaders", originResponseHeaders);
        }
        return (List<Pair<String, String>>) get("originResponseHeaders");
    }

    /**
     * adds a header to the origin response headers
     *
     * @param name
     * @param value
     */
    public void addOriginResponseHeader(String name, String value) {
        getOriginResponseHeaders().add(new Pair<String, String>(name, value));
    }

    /**
     * returns the content-length of the origin response
     *
     * @return the content-length of the origin response
     */
    public Long getOriginContentLength() {
        return (Long) get("originContentLength");
    }

    /**
     * sets the content-length from the origin response
     *
     * @param v
     */
    public void setOriginContentLength(Long v) {
        set("originContentLength", v);
    }

    /**
     * sets the content-length from the origin response
     *
     * @param v parses the string into an int
     */
    public void setOriginContentLength(String v) {
        try {
            final Long i = Long.valueOf(v);
            set("originContentLength", i);
        } catch (NumberFormatException e) {
            LOG.warn("error parsing origin content length", e);
        }
    }

    /**
     * @return true if the request body is chunked
     */
    public boolean isChunkedRequestBody() {
        final Object v = get("chunkedRequestBody");
        return (v != null) ? (Boolean) v : false;
    }

    /**
     * sets chunkedRequestBody to true
     */
    public void setChunkedRequestBody() {
        this.set("chunkedRequestBody", Boolean.TRUE);
    }

    /**
     * @return true is the client request can accept gzip encoding. Checks the "accept-encoding" header
     */
    public boolean isGzipRequested() {
        final String requestEncoding = this.getRequest().getHeader(GatewayHeaders.ACCEPT_ENCODING);
        return requestEncoding != null && requestEncoding.toLowerCase().contains("gzip");
    }

    /**
     * unsets the threadLocal context. Done at the end of the request.
     */
    public void unset() {
        threadLocal.remove();
    }

    /**
     * Mkaes a copy of the RequestContext. This is used for debugging.
     *
     * @return
     */
    public RequestContext copy() {
        RequestContext copy = new RequestContext();
        Iterator<String> it = keySet().iterator();
        String key = it.next();
        while (key != null) {
            Object orig = get(key);
            try {
                Object copyValue = DeepCopy.copy(orig);
                if (copyValue != null) {
                    copy.set(key, copyValue);
                } else {
                    copy.set(key, orig);
                }
            } catch (NotSerializableException e) {
                copy.set(key, orig);
            }
            if (it.hasNext()) {
                key = it.next();
            } else {
                key = null;
            }
        }
        return copy;
    }

    /**
     * @return Map<String, List<String>>  of the request Query Parameters
     */
    public Map<String, List<String>> getRequestQueryParams() {
        return (Map<String, List<String>>) get("requestQueryParams");
    }

    /**
     * sets the request query params list
     *
     * @param qp Map<String, List<String>> qp
     */
    public void setRequestQueryParams(Map<String, List<String>> qp) {
        put("requestQueryParams", qp);
    }

}
