package com.greencloud.gateway.filters.pre;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.greencloud.gateway.GatewayFilter;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;

import javax.servlet.http.HttpServletRequest;

/**
 * @author leejianhao
 */
public class SentinelFilter extends GatewayFilter {
    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 30;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws GatewayException {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();
        String resourceName = request.getRequestURI();
        Entry entry = null;
        // 务必保证finally会被执行
        try {
            // 资源名可使用任意有业务语义的字符串

            entry = SphU.entry(resourceName);
            /**
             * 被保护的业务逻辑
             */
        } catch (BlockException e1) {
            // 资源访问阻止，被限流或被降级
            // 进行相应的处理操作
            throw new GatewayException("Throttled by API Flow Control", 403, "Too frequent visits， please try again later");
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
        return null;
    }
}
