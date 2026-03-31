package com.pocrd.api_publish_service.service.impl;

import com.pocrd.api_publish_service.api.GreeterServiceInternal;
import com.pocrd.api_publish_service.api.entity.ApiInfo;
import com.pocrd.api_publish_service.api.entity.ServiceInfo;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GreeterService Internal Implementation
 * 
 * 此实现类提供仅在内网供其他 Dubbo 服务调用的接口
 * 不会通过 Higress 网关暴露给公网
 * 使用 Dubbo 原生 RPC 协议（端口 50052）进行高效内部通信
 */
@DubboService(version = "1.0.0", group = "internal", registry = "nacos-internal", protocol = "dubbo")
public class GreeterServiceInternalImpl implements GreeterServiceInternal {
    
    // 模拟服务启动时间
    private final long startTime = System.currentTimeMillis();
    
    // 请求计数器（线程不安全，仅用于演示）
    private int requestCount = 0;

    @Override
    public String greetInternal(String name) {
        incrementRequestCount();
        
        String remoteAddress = RpcContext.getServerContext().getRemoteAddressString();
        String localAddress = RpcContext.getServerContext().getLocalAddressString();
        
        return String.format("[Internal] Hello %s, from %s (to %s)", 
                name, remoteAddress, localAddress);
    }

    @Override
    public List<String> greetBatch(List<String> names) {
        incrementRequestCount();
        
        return names.stream()
                .map(name -> {
                    String remoteAddress = RpcContext.getServerContext().getRemoteAddressString();
                    return String.format("[Internal Batch] Hello %s, from %s", name, remoteAddress);
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean healthCheck() {
        incrementRequestCount();
        
        // 简单的健康检查逻辑
        // 实际项目中可以检查数据库连接、外部服务状态等
        long uptime = System.currentTimeMillis() - startTime;
        
        System.out.println("Health check executed. Uptime: " + uptime + "ms");
        
        return true;
    }

    @Override
    public ServiceInfo getServiceInfo() {
        incrementRequestCount();
        
        long uptime = System.currentTimeMillis() - startTime;
        
        return new ServiceInfo(
            "GreeterServiceInternal",
            "1.0.0",
            uptime,
            requestCount,
            new ApiInfo[] {
                new ApiInfo("greetInternal", "Greets a user internally", "POST /greetInternal", "application/json"),
                new ApiInfo("greetBatch", "Greets a list of users internally", "POST /greetBatch", "application/json")
            },
            null
        );
    }
    
    /**
     * 增加请求计数
     */
    private synchronized void incrementRequestCount() {
        requestCount++;
    }
}
