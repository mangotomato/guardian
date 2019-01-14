package com.greencloud.gateway.filters.pre;

import com.google.common.base.Strings;
import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.constants.GatewayConstants;
import com.greencloud.gateway.constants.SystemHeader;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;
import com.greencloud.gateway.util.RedisUtil;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Timestamp+nonce to prevent retry attach
 *
 * @author leejianhao
 */
public class RAFilter extends GatewayFilter {

    private static final DynamicIntProperty TIMESTAMP_VALIDITY_MINUTES = DynamicPropertyFactory.getInstance()
            .getIntProperty(GatewayConstants.GATEWAY_RA_TIMESTAMP_VALIDITY_MINUTES, 15);

    private static final DynamicBooleanProperty RA_ENABLE = DynamicPropertyFactory.getInstance()
            .getBooleanProperty(GatewayConstants.GATEWAY_RA_ENABLE, false);

    /**
     * appKey_api_timestamp (默认，15分钟内，appKey, api, nonce 不能重复）
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
        return !RequestContext.getCurrentContext().isHealthCheckRequest() && rAEnabled();
    }

    @Override
    public Object run() throws GatewayException {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();
        String timestamp = request.getHeader(SystemHeader.X_GW_TIMESTAMP);
        String nonce = request.getHeader(SystemHeader.X_GW_NONCE);

        try {
            long ts = Long.parseLong(timestamp);
            long interval = Math.abs((System.currentTimeMillis() - ts) / (1000 * 60));
            if (interval - TIMESTAMP_VALIDITY_MINUTES.get() > 0) {
                throw new GatewayException("Timestamp Expired", 403, "Timestamp Expired");
            }

            String appKey = request.getHeader(SystemHeader.X_GW_KEY);
            String api = request.getRequestURI();
            String key = getKey(appKey, api, nonce);
            if (exists(key)) {
                throw new GatewayException("Nonce Used", 403, "Nonce Used");
            }

            save(key);

        } catch (NumberFormatException nfe) {
            throw new GatewayException("Invalid Timestamp", 403, "Invalid Timestamp");
        }

        return null;
    }

    private void save(String key) {
        RedisUtil.getInstance().setex(key, TIMESTAMP_VALIDITY_MINUTES.get() * 60*1000, "");
    }

    private String getKey(String appKey, String api, String nonce) {
        return String.format(UNIQUE_KEY, appKey, api, nonce);
    }

    private boolean exists(String key) {
        return RedisUtil.getInstance().exists(key);
    }

    private boolean rAEnabled() {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        String appKey = request.getHeader(SystemHeader.X_GW_KEY);
        String timestamp = request.getHeader(SystemHeader.X_GW_TIMESTAMP);
        String nonce = request.getHeader(SystemHeader.X_GW_NONCE);

        return (RA_ENABLE.get() && !Strings.isNullOrEmpty(appKey) && !Strings.isNullOrEmpty(timestamp)
                && !Strings.isNullOrEmpty(nonce));
    }

}
