package com.pocrd.api_publish_service.service;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Publish Service Application
 * This application exposes services via Dubbo Triple protocol
 * and registers with Nacos service discovery
 */
@SpringBootApplication
@EnableDubbo
public class ApiPublishServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiPublishServiceApplication.class, args);
    }
}
