package com.pocrd.clientsdk;

/**
 * API 方法基类
 * <p>
 * 所有生成的 API 方法类都应继承此类，提供通用的请求参数和方法信息
 */
public abstract class ApiMethod<R> {

    /**
     * 获取方法名
     */
    public abstract String getMethodName();

    /**
     * 获取服务名
     */
    public abstract String getServiceName();

    /**
     * 获取返回类型
     */
    public abstract Class<R> getReturnType();

    /**
     * 获取超时时间（毫秒），默认 5000ms
     */
    public int getTimeout() {
        return 5000;
    }

    /**
     * 获取重试次数，默认 0 次
     */
    public int getRetryTimes() {
        return 0;
    }
}
