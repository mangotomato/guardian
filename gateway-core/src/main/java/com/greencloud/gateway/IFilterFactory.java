package com.greencloud.gateway;

/**
 * Interface to provide instances of GatewayFilter from a given class.
 */
public interface IFilterFactory {

	/**
	 * Returns an instance of the specified class.
	 * 
	 * @param clazz
	 *            the Class to instantiate
	 * @return an instance of GatewayFilter
	 * @throws Exception
	 *             if an error occurs
	 */
	public GatewayFilter newInstance(Class clazz) throws Exception;
}