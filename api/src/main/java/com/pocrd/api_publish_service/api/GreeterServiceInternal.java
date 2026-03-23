package com.pocrd.api_publish_service.api;

import com.pocrd.api_publish_service.api.entity.ServiceInfo;

/**
 * Greeter service interface definition - Internal Dubbo RPC Only
 * 
 * 此接口定义的方法仅在内网供其他 Dubbo 服务调用
 * 不会通过 Higress 网关暴露给公网
 * 使用 Dubbo 原生 RPC 协议进行高效内部通信
 */
public interface GreeterServiceInternal {
    /**
     * 内部服务间问候 - 简单问候
     * @param name 问候对象名称
     * @return 问候消息
     */
    String greetInternal(String name);
    
    /**
     * 内部服务间批量问候
     * @param names 问候对象名称列表
     * @return 问候消息列表
     */
    java.util.List<String> greetBatch(java.util.List<String> names);
    
    /**
     * 内部健康检查 - 用于服务监控
     * @return 服务健康状态
     */
    boolean healthCheck();
    
    /**
     * 内部服务信息获取
     * @return 服务详细信息
     */
    ServiceInfo getServiceInfo();
}
