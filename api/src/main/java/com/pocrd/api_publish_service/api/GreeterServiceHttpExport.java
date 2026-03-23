package com.pocrd.api_publish_service.api;

import org.apache.dubbo.common.stream.StreamObserver;

/**
 * Greeter service interface definition - HTTP Export via Higress Gateway
 *
 * 此接口定义的方法将通过 Higress 网关暴露给公网 HTTP 访问
 * 使用 Dubbo Triple 协议（兼容 gRPC/HTTP2）
 */
public interface GreeterServiceHttpExport {
    /**
     * Unary call - simple greeting
     * @param name the person's name
     * @return greeting message
     */
    String greet(String name);

    String greet2(String name1, String name2);
    
    /**
     * Server streaming - greet multiple times
     * @param name the person's name
     * @param observer the stream observer to send multiple greetings
     */
    void greetStream(String name, StreamObserver<String> observer);
    
}
