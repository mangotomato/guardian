package com.greencloud.gateway.upstreamCheck.config;

import com.greencloud.gateway.constants.HttpMethod;

import java.util.List;

/**
 * @author leejianhao
 */
public class UpstreamCheckConfig {

    /**
     * check请求的间隔时间，单位为秒
     */
    private int interval;

    /**
     * check请求次数下落多少，标记upstream为down状态
     */
    private int fallCount;

    /**
     * check请求上升多少，标记upstream为up状态
     */
    private int riseCount;

    /**
     * check请求的超时时间，单位为秒
     */
    private int timeout;

    /**
     * http method
     */
    private String method;

    /**
     * http请求的path
     */
    private String path;

    /**
     * check协议类型
     */
    private CheckTypeEnum type = CheckTypeEnum.HTTP;

    /**
     * 期望的http status 状态码（如匹配，则服务为UP）
     */
    private List<Integer> expectStatus;

    /**
     * 期望返回的响应内容（如匹配，则服务为UP）
     */
    private String expectResponse;

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getFallCount() {
        return fallCount;
    }

    public void setFallCount(int fallCount) {
        this.fallCount = fallCount;
    }

    public int getRiseCount() {
        return riseCount;
    }

    public void setRiseCount(int riseCount) {
        this.riseCount = riseCount;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public CheckTypeEnum getType() {
        return type;
    }

    public void setType(CheckTypeEnum type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<Integer> getExpectStatus() {
        return expectStatus;
    }

    public void setExpectStatus(List<Integer> expectStatus) {
        this.expectStatus = expectStatus;
    }

    public String getExpectResponse() {
        return expectResponse;
    }

    public void setExpectResponse(String expectResponse) {
        this.expectResponse = expectResponse;
    }
}
