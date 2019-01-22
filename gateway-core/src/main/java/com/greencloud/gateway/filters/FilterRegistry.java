package com.greencloud.gateway.filters;

import com.greencloud.gateway.GatewayFilter;

import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author leejianhao
 */
public class FilterRegistry {

    private static final FilterRegistry INSTANCE = new FilterRegistry();

    public static final FilterRegistry instance() {
        return INSTANCE;
    }

    private final ConcurrentHashMap<String, GatewayFilter> filters = new ConcurrentHashMap<String, GatewayFilter>();

    private FilterRegistry() {
    }

    public GatewayFilter remove(String key) {
        return this.filters.remove(key);
    }

    public GatewayFilter get(String key) {
        return this.filters.get(key);
    }

    public void put(String key, GatewayFilter filter) {
        this.filters.putIfAbsent(key, filter);
    }

    public int size() {
        return this.filters.size();
    }

    public Collection<GatewayFilter> getAllFilters() {
        return this.filters.values();
    }

    public Collection<String> getAllFilterKeys() {
        return this.filters.keySet();
    }

}
