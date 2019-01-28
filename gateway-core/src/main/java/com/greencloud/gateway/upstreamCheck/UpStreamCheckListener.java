package com.greencloud.gateway.upstreamCheck;

import com.greencloud.gateway.upstreamCheck.config.Status;

import java.util.List;

/**
 * @author leejianhao
 */
public interface UpStreamCheckListener {
	void onChange(String serviceGroup, String server, Status status);
}
