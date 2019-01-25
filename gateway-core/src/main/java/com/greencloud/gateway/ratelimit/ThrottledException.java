package com.greencloud.gateway.ratelimit;

/**
 * @author leejianhao
 */
public class ThrottledException extends Exception {
	public ThrottledException(String message) {
		super(message);
	}

	public ThrottledException(String message, Throwable e) {
		super(message, e);
	}
}
