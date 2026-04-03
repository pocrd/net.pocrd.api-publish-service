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
     * 获取接口全限定名（用于构建 URL）
     */
    public abstract String getInterfaceName();

    /**
     * 获取返回类型
     */
    public abstract Class<R> getReturnType();

    /**
     * 获取返回类型的容器类型（如 list, set）
     * @return 容器类型，如果没有则为空字符串
     */
    public abstract String getReturnContainerType();

    /**
     * 构建请求 URL
     *
     * @param baseUrl 基础 URL，如 https://api.example.com
     * @return 完整请求 URL
     */
    public String buildUrl(String baseUrl) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/dapi/" + getInterfaceName() + "/" + getMethodName();
    }

    /**
     * 将当前请求参数序列化为 JSON 请求体字符串。
     * <p>
     * 单个实体参数直接返回该实体的 JSON；
     * 多个参数或基本类型参数封装为 JSON 对象。
     */
    public abstract String buildRequestBody();

    /**
     * 是否使用 POST 方式发送请求（默认 false 即 GET）
     */
    public abstract boolean isPost();

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
