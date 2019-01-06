package com.greencloud.gateway.filters.pre;

import com.google.common.base.Strings;
import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.constants.SystemHeader;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;
import com.greencloud.gateway.util.RedisUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * Timestamp+nonce to prevent retry attach
 *
 * @author leejianhao
 */
public class RAFilter extends GatewayFilter {

    private static final long TIMESTAMP_VALIDITY_MINUTES = 15;

    /**
     * appKey_api_timestamp (appKey, api, timestamp唯一）
     */
    private static final String UNIQUE_KEY = "%s_%s_%s";

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 15;
    }

    @Override
    public boolean shouldFilter() {
        return rAEnabled();
    }

    @Override
    public Object run() throws GatewayException {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();
        String timestamp = request.getHeader(SystemHeader.X_GW_TIMESTAMP);
        String nonce = request.getHeader(SystemHeader.X_GW_NONCE);

        if (Strings.isNullOrEmpty(timestamp) || Strings.isNullOrEmpty(nonce)) {
            return false;
        }

        try {
            long ts = Long.parseLong(timestamp);
            long interval = Math.abs((System.currentTimeMillis() - ts) / (1000 * 60));
            if (interval - TIMESTAMP_VALIDITY_MINUTES > 0) {
                throw new GatewayException("Retry Attach", 403, "Retry attach, request is rejected");
            }

            String appKey = request.getHeader(SystemHeader.X_GW_KEY);
            String api = request.getRequestURI();
            String key = getKey(appKey, api, ts);
            if (exists(key)) {
                throw new GatewayException("Retry Attach", 403, "Retry attach, request is rejected");
            }

        } catch (NumberFormatException nfe) {
            throw new GatewayException("Retry Attach", 403, "The header x-gw-timestamp is illegal");
        }

        return null;
    }

    private String getKey(String appKey, String api, long timestamp) {
        return String.format(UNIQUE_KEY, appKey, api, timestamp);
    }

    private boolean exists(String key) {
        return RedisUtil.getInstance().get(key) != null;
    }

    private boolean rAEnabled() {
        return true;
    }
}
