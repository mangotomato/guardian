package com.greencloud.gateway;

import com.greencloud.gateway.context.Debug;
import com.greencloud.gateway.context.RequestContext;
import com.greencloud.gateway.exception.GatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FilterProcessor {

    static FilterProcessor INSTANCE = new FilterProcessor();
    protected static final Logger logger = LoggerFactory.getLogger(FilterProcessor.class);

    /**
     * @return the singleton FilterProcessor
     */
    public static FilterProcessor getInstance() {
        return INSTANCE;
    }

    /**
     * sets a singleton processor in case of a need to override default behavior
     *
     * @param processor
     */
    public static void setProcessor(FilterProcessor processor) {
        INSTANCE = processor;
    }

    /**
     * runs "post" filters which are called after "route" filters. GatewayException from GatewayFilters are thrown.
     * Any other Throwables are caught and a GatewayException is thrown out with a 500 status code
     *
     * @throws GatewayException
     */
    public void postRoute() throws GatewayException {
        try {
            runFilters("post");
        } catch (GatewayException e) {
            throw e;
        } catch (Throwable e) {
            throw new GatewayException(e, 500, "UNCAUGHT_EXCEPTION_IN_POST_FILTER_" + e.getClass().getName());
        }
    }

    /**
     * runs all "error" filters. These are called only if an exception occurs. Exceptions from this are swallowed and logged so as not to bubble up.
     */
    public void error() {
        try {
            runFilters("error");
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Runs all "route" filters. These filters route calls to an origin.
     *
     * @throws GatewayException if an exception occurs.
     */
    public void route() throws GatewayException {
        try {
            runFilters("route");
        } catch (GatewayException e) {
            throw e;
        } catch (Throwable e) {
            throw new GatewayException(e, 500, "UNCAUGHT_EXCEPTION_IN_ROUTE_FILTER_" + e.getClass().getName());
        }
    }

    /**
     * runs all "pre" filters. These filters are run before routing to the orgin.
     *
     * @throws GatewayException
     */
    public void preRoute() throws GatewayException {
        try {
            runFilters("pre");
        } catch (GatewayException e) {
            throw e;
        } catch (Throwable e) {
            throw new GatewayException(e, 500, "UNCAUGHT_EXCEPTION_IN_PRE_FILTER_" + e.getClass().getName());
        }
    }

    /**
     * runs all filters of the filterType sType/ Use this method within filters to run custom filters by type
     *
     * @param sType the filterType.
     * @return
     * @throws Throwable throws up an arbitrary exception
     */
    public Object runFilters(String sType) throws Throwable {
        if (RequestContext.getCurrentContext().debugRouting()) {
            Debug.addRoutingDebug("Invoking {" + sType + "} type filters");
        }
        boolean bResult = false;
        List<GatewayFilter> list = FilterLoader.getInstance().getFiltersByType(sType);
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                GatewayFilter GatewayFilter = list.get(i);
                Object result = processGatewayFilter(GatewayFilter);
                if (result != null && result instanceof Boolean) {
                    bResult |= ((Boolean) result);
                }
            }
        }
        return bResult;
    }

    /**
     * Processes an individual GatewayFilter. This method adds Debug information. Any uncaught Thowables are caught by this method and converted to a GatewayException with a 500 status code.
     *
     * @param filter
     * @return the return value for that filter
     * @throws GatewayException
     */
    public Object processGatewayFilter(GatewayFilter filter) throws GatewayException {

        RequestContext ctx = RequestContext.getCurrentContext();
        boolean bDebug = ctx.debugRouting();
        final String metricPrefix = "zuul.filter-";
        long execTime = 0;
        String filterName = "";
        try {
            long ltime = System.currentTimeMillis();
            filterName = filter.getClass().getSimpleName();

            RequestContext copy = null;
            Object o = null;
            Throwable t = null;

            if (bDebug) {
                Debug.addRoutingDebug("Filter " + filter.filterType() + " " + filter.filterOrder() + " " + filterName);
                copy = ctx.copy();
            }

            GatewayFilterResult result = filter.runFilter();
            ExecutionStatus s = result.getStatus();
            execTime = System.currentTimeMillis() - ltime;

            switch (s) {
                case FAILED:
                    t = result.getException();
                    ctx.addFilterExecutionSummary(filterName, ExecutionStatus.FAILED.name(), execTime);
                    break;
                case SUCCESS:
                    o = result.getResult();
                    ctx.addFilterExecutionSummary(filterName, ExecutionStatus.SUCCESS.name(), execTime);
                    if (bDebug) {
                        Debug.addRoutingDebug("Filter {" + filterName + " TYPE:" + filter.filterType() + " ORDER:" + filter.filterOrder() + "} Execution time = " + execTime + "ms");
                        Debug.compareContextState(filterName, copy);
                    }
                    break;
                default:
                    break;
            }

            if (t != null) throw t;
            return o;

        } catch (Throwable e) {
            if (bDebug) {
                Debug.addRoutingDebug("Running Filter failed " + filterName + " type:" + filter.filterType() + " order:" + filter.filterOrder() + " " + e.getMessage());
            }
            if (e instanceof GatewayException) {
                throw (GatewayException) e;
            } else {
                GatewayException ex = new GatewayException(e, "Filter threw Exception", 500, filter.filterType() + ":" + filterName);
                ctx.addFilterExecutionSummary(filterName, ExecutionStatus.FAILED.name(), execTime);
                throw ex;
            }
        }
    }

}