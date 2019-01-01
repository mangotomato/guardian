package com.greencloud.gateway.filters;

import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.constants.GatewayConstants;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.util.Pair;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author leejianhao
 */
public class DebugHeader extends GatewayFilter {
    static final DynamicBooleanProperty INCLUDE_DEBUG_HEADER =
            DynamicPropertyFactory.getInstance().getBooleanProperty(GatewayConstants.GATEWAY_INCLUDE_DEBUG_HEADER, false);

    static final DynamicBooleanProperty INCLUDE_ROUTE_URL_HEADER =
            DynamicPropertyFactory.getInstance().getBooleanProperty(GatewayConstants.GATEWAY_INCLUDE_DEBUG_ROUTE_URL_HEADER, true);

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        return INCLUDE_DEBUG_HEADER.get();
    }

    @Override
    public Object run() throws GatewayException {
        RequestContext context = RequestContext.getCurrentContext()
        List<Pair<String, String>> headers = context.getGatewayResponseHeaders();
        headers.add(new Pair("X_GATEWAY", GatewayConstants.APPLICATION_NAME));
        headers.add(new Pair("CONNECTION", "KEEP_ALIVE"))
        headers.add(new Pair("X_GATEWAY_FILTER_EXECUTION_STATUS", context.getFilterExecutionSummary().toString()))
        headers.add(new Pair("X_ORIGINATING_URL", getOriginatingURL()));
        if (INCLUDE_ROUTE_URL_HEADER.get()) {
            // todo route header
        }
        return null;
    }

    String getOriginatingURL() {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        String protocol = request.getHeader("X_FORWARDED_PROTO");
        if (protocol == null) {
            protocol = "http";
        }
        String host = request.getHeader("HOST");
        String uri = request.getRequestURI();
        String url = String.format("%s://%s%s", protocol, host, uri);
        if (request.getQueryString() != null) {
            url = url + request.getQueryString();
        }
        return url;
    }
}
