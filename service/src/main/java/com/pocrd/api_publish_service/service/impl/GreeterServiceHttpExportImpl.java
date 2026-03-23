package com.pocrd.api_publish_service.service.impl;

import com.pocrd.api_publish_service.api.GreeterServiceHttpExport;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcContext;

import java.util.concurrent.TimeUnit;

/**
 * GreeterService HTTP Export Implementation
 * 
 * 此实现类暴露到 Triple 协议（端口 50051）：供 Higress 网关 HTTP 调用
 */
@DubboService(version = "1.0.0", group = "public", registry = "nacos-public", protocol = "tri", path = "api")
public class GreeterServiceHttpExportImpl implements GreeterServiceHttpExport {

    @Override
    public String greet(String name) {
        // Get current RPC context information
        String remoteAddress = RpcContext.getContext().getRemoteAddressString();
        String localAddress = RpcContext.getContext().getLocalAddressString();
        
        // Return greeting message with additional context info
        return String.format("Hello %s, from %s (to %s)", 
                name, remoteAddress, localAddress);
    }

    @Override
    public String greet2(String name1, String name2) {
        // Get current RPC context information
        String remoteAddress = RpcContext.getContext().getRemoteAddressString();
        String localAddress = RpcContext.getContext().getLocalAddressString();
        
        // Return greeting message for two names
        return String.format("Hello %s and %s, from %s (to %s)", 
                name1, name2, remoteAddress, localAddress);
    }

    @Override
    public void greetStream(String name, StreamObserver<String> observer) {
        try {
            // Send greetings multiple times with delay
            for (int i = 1; i <= 5; i++) {
                String greeting = String.format("Hello %s! This is greeting #%d from %s", 
                        name, i, RpcContext.getContext().getLocalAddressString());
                observer.onNext(greeting);
                
                // Simulate some processing time
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(e);
        }
    }

}
