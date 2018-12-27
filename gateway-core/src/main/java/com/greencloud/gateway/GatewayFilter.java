package com.greencloud.gateway;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author leejianhao
 */
public abstract class GatewayFilter implements IGatewayFilter, Comparable<GatewayFilter> {

    private final AtomicReference<DynamicBooleanProperty> filterDisabledRef = new AtomicReference<>();

    /**
     * to classify a filter by type. Standard types in Gateway are "pre" for pre-routing filtering,
     * "route" for routing to an origin, "post" for post-routing filters, "error" for error handling.
     * We also support a "static" type for static responses see  StaticResponseFilter.
     * Any filterType made be created or added and run by calling FilterProcessor.runFilters(type)
     *
     * @return A String representing that type
     */
    abstract public String filterType();

    /**
     * filterOrder() must also be defined for a filter. Filters may have the same  filterOrder if precedence is not
     * important for a filter. filterOrders do not need to be sequential.
     *
     * @return the int order of a filter
     */
    abstract public int filterOrder();

    /**
     * By default GatewayFilters are static; they don't carry state. This may be overridden by overriding the isStaticFilter() property to false
     *
     * @return true by default
     */
    public boolean isStaticFilter() {
        return true;
    }

    /**
     * The name of the Archaius property to disable this filter. by default it is gateway.[classname].[filtertype].disable
     *
     * @return
     */
    public String disablePropertyName() {
        return "gateway." + this.getClass().getSimpleName() + "." + filterType() + ".disable";
    }

    /**
     * If true, the filter has been disabled by archaius and will not be run
     *
     * @return
     */
    public boolean isFilterDisabled() {
        filterDisabledRef.compareAndSet(null, DynamicPropertyFactory.getInstance().getBooleanProperty(disablePropertyName(), false));
        return filterDisabledRef.get().get();
    }

    /**
     * runFilter checks !isFilterDisabled() and shouldFilter(). The run() method is invoked if both are true.
     *
     * @return the return from GatewayFilterResult
     */
    public GatewayFilterResult runFilter() {
        GatewayFilterResult zr = new GatewayFilterResult();
        if (!isFilterDisabled()) {
            if (shouldFilter()) {
                try {
                    Object res = run();
                    zr = new GatewayFilterResult(res, ExecutionStatus.SUCCESS);
                } catch (Throwable e) {
                    zr = new GatewayFilterResult(ExecutionStatus.FAILED);
                    zr.setException(e);
                }
            } else {
                zr = new GatewayFilterResult(ExecutionStatus.SKIPPED);
            }
        }
        return zr;
    }

    @Override
    public int compareTo(GatewayFilter filter) {
        return Integer.compare(this.filterOrder(), filter.filterOrder());
    }
}