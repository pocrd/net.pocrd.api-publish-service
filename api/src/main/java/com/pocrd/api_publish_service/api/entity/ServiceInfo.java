package com.pocrd.api_publish_service.api.entity;

import java.io.Serializable;

/**
 * 服务信息数据记录
 * 
 * 使用 JDK 16+ Record 特性，自动生成：
 * - 构造函数
 * - 访问器方法（serviceName(), version(), uptime(), requestCount()）
 * - equals() 和 hashCode()
 * - toString()
 * 
 * 实现 Serializable 接口以支持 Hessian2 序列化
 */
public record ServiceInfo(
    String serviceName,
    String version,
    long uptime,
    int requestCount
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
