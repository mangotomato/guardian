package com.greencloud.gateway;

import com.greencloud.gateway.exception.GatewayException;

/**
 * leejianhao
 */
public interface IGatewayFilter {

    boolean shouldFilter();

    Object run() throws GatewayException;
}
