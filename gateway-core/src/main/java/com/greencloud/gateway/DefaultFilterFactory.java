package com.greencloud.gateway;

/**
 * Default factory for creating instances of GatewayFilter.
 */
public class DefaultFilterFactory implements IFilterFactory {

    /**
     * Returns a new implementation of GatewayFilter as specified by the provided
     * Class. The Class is instantiated using its nullary constructor.
     * 
     * @param clazz the Class to instantiate
     * @return A new instance of GatewayFilter
     */
    @Override
    public GatewayFilter newInstance(Class clazz) throws InstantiationException, IllegalAccessException {
        return (GatewayFilter) clazz.newInstance();
    }

}
