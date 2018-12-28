package com.greencloud.gateway.context;

import javax.servlet.*;
import java.io.IOException;

/**
 * Manages Gateway <code>RequestContext</code> lifecycle.
 */
public class ContextLifecycleFilter implements Filter {

    @Override
    public void destroy() {}

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
        throws IOException, ServletException {
        try {
            chain.doFilter(req, res);
        } finally {
            RequestContext.getCurrentContext().unset();
        }
    }

}