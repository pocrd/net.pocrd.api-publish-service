package com.pocrd.api_publish_service.api.entity;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;
import java.util.List;

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
@Description("服务信息实体，包含服务名称、版本、运行时长等元数据")
public record ServiceInfo(
    @Description("服务名称") String serviceName,
    @Description("服务版本号") String version,
    @Description("服务运行时长(毫秒)") long uptime,
    @Description("请求计数") int requestCount,
    @Description("API信息") ApiInfo[] apiInfos,
    // @Description("错误码信息") Map<Integer, String> errorCodes
    @Description("错误码信息") List<String> errorCodes
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
