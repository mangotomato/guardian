package com.greencloud.gateway.ratelimit.config;

import java.util.concurrent.TimeUnit;

/**
 * 流控规则
 *<pre>
 * 单位时间：秒／分钟／小时／天
 * API流量限制: 10000
 * 用户流量限制: 1000 (< API流量限制)
 * APP流量限制:  100 (< 用户流量限制)
 * </pre>
 * @author leejianhao
 */
public class RateLimitRule {
	/**
	 * 资源名称(api)
	 */
	private String api;
	/**
	 * 流控(api)
	 */
	private int limit;
	/**
	 * 流控(client)
	 */
	private int limitClient;
	/**
	 * 流控(app)
	 */
	private int limitAPP;
	/**
	 * 单位 （支持秒、分钟、小时、天）
	 */
	private TimeUnit timeUnit = TimeUnit.SECONDS;

	public RateLimitRule() {
	}

	public RateLimitRule(String api, int limit, int limitClient, int limitAPP, TimeUnit timeUnit) {
		this.api = api;
		this.limit = limit;
		this.limitClient = limitClient;
		this.limitAPP = limitAPP;
		this.timeUnit = timeUnit;
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

	public String getApi() {
		return api;
	}

	public void setApi(String api) {
		this.api = api;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getLimitClient() {
		return limitClient;
	}

	public void setLimitClient(int limitClient) {
		this.limitClient = limitClient;
	}

	public int getLimitAPP() {
		return limitAPP;
	}

	public void setLimitAPP(int limitAPP) {
		this.limitAPP = limitAPP;
	}
}
