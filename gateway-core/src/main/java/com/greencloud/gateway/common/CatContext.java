package com.greencloud.gateway.common;

import com.dianping.cat.Cat;
import com.google.common.collect.Maps;

import java.util.Map;

public class CatContext implements Cat.Context {
	private Map<String, String> properties = Maps.newHashMap();
	@Override
	public void addProperty(String key, String value) {
		this.properties.put(key, value);
	}

	@Override
	public String getProperty(String key) {
		return this.properties.get(key);
	}

}
