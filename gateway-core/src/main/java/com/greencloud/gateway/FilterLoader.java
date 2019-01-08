package com.greencloud.gateway;

import com.greencloud.gateway.filters.FilterRegistry;
import com.greencloud.gateway.filters.error.ErrorResponse;
import com.greencloud.gateway.filters.post.ResponseFilter;
import com.greencloud.gateway.filters.post.SendResponseFilter;
import com.greencloud.gateway.filters.pre.AppKeyFilter;
import com.greencloud.gateway.filters.pre.MockFilter;
import com.greencloud.gateway.filters.pre.RAFilter;
import com.greencloud.gateway.filters.pre.SentinelFilter;
import com.greencloud.gateway.filters.route.RoutingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FilterLoader {
    final static FilterLoader INSTANCE = new FilterLoader();

    private static final Logger logger = LoggerFactory.getLogger(FilterLoader.class);

    private final ConcurrentHashMap<String, Long> filterClassLastModified = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> filterClassCode = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> filterCheck = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<GatewayFilter>> hashFiltersByType = new ConcurrentHashMap<>();

    private FilterRegistry filterRegistry = FilterRegistry.instance();

    static IDynamicCodeCompiler COMPILER;

    static IFilterFactory FILTER_FACTORY = new DefaultFilterFactory();

    public FilterLoader() {
        filterRegistry.put("routeFilter", new RoutingFilter());
        filterRegistry.put("responseFilter", new ResponseFilter());
        filterRegistry.put("sendResponseFilter", new SendResponseFilter());
        filterRegistry.put("errorResponse", new ErrorResponse());
        filterRegistry.put("mockFilter", new MockFilter());
        filterRegistry.put("sentinelFilter", new SentinelFilter());
        filterRegistry.put("appKeyFilter", new AppKeyFilter());
        filterRegistry.put("raFilter", new RAFilter());
    }

    /**
     * Sets a Dynamic Code Compiler
     *
     * @param compiler
     */
    public void setCompiler(IDynamicCodeCompiler compiler) {
        COMPILER = compiler;
    }

    /**
     * overidden by tests
     */
    public void setFilterRegistry(FilterRegistry r) {
        this.filterRegistry = r;
    }

    /**
     * Sets a FilterFactory
     *
     * @param factory
     */
    public void setFilterFactory(IFilterFactory factory) {
        FILTER_FACTORY = factory;
    }

    /**
     * @return Singleton FilterLoader
     */
    public static FilterLoader getInstance() {
        return INSTANCE;
    }

    /**
     * Given source and name will compile and store the filter if it detects that the filter code has changed or
     * the filter doesn't exist. Otherwise it will return an instance of the requested GatewayFilter
     *
     * @param sCode source code
     * @param sName name of the filter
     * @return the GatewayFilter
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public GatewayFilter getFilter(String sCode, String sName) throws Exception {

        if (filterCheck.get(sName) == null) {
            filterCheck.putIfAbsent(sName, sName);
            if (!sCode.equals(filterClassCode.get(sName))) {
                logger.info("reloading code " + sName);
                filterRegistry.remove(sName);
            }
        }
        GatewayFilter filter = filterRegistry.get(sName);
        if (filter == null) {
            Class clazz = COMPILER.compile(sCode, sName);
            if (!Modifier.isAbstract(clazz.getModifiers())) {
                filter = FILTER_FACTORY.newInstance(clazz);
            }
        }
        return filter;

    }

    /**
     * @return the total number of Gateway filters
     */
    public int filterInstanceMapSize() {
        return filterRegistry.size();
    }


    /**
     * From a file this will read the GatewayFilter source code, compile it, and add it to the list of current filters
     * a true response means that it was successful.
     *
     * @param file
     * @return true if the filter in file successfully read, compiled, verified and added to Gateway
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IOException
     */
    public boolean putFilter(File file) throws Exception {
        String sName = file.getAbsolutePath() + file.getName();
        if (filterClassLastModified.get(sName) != null && (file.lastModified() != filterClassLastModified.get(sName))) {
            logger.debug("reloading filter " + sName);
            filterRegistry.remove(sName);
        }
        GatewayFilter filter = filterRegistry.get(sName);
        if (filter == null) {
            Class clazz = COMPILER.compile(file);
            if (!Modifier.isAbstract(clazz.getModifiers())) {
                filter = FILTER_FACTORY.newInstance(clazz);
                List<GatewayFilter> list = hashFiltersByType.get(filter.filterType());
                if (list != null) {
                    //rebuild this list
                    hashFiltersByType.remove(filter.filterType());
                }
                filterRegistry.put(file.getAbsolutePath() + file.getName(), filter);
                filterClassLastModified.put(sName, file.lastModified());
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a list of filters by the filterType specified
     *
     * @param filterType
     * @return a List<GatewayFilter>
     */
    public List<GatewayFilter> getFiltersByType(String filterType) {

        List<GatewayFilter> list = hashFiltersByType.get(filterType);
        if (list != null) {
            return list;
        }

        list = new ArrayList<>();

        Collection<GatewayFilter> filters = filterRegistry.getAllFilters();
        for (Iterator<GatewayFilter> iterator = filters.iterator(); iterator.hasNext(); ) {
            GatewayFilter filter = iterator.next();
            if (filter.filterType().equals(filterType)) {
                list.add(filter);
            }
        }
        // sort by priority
        Collections.sort(list);

        hashFiltersByType.putIfAbsent(filterType, list);
        return list;
    }
}