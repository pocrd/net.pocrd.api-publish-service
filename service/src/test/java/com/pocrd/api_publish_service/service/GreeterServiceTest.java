package com.pocrd.api_publish_service.service;

import com.pocrd.api_publish_service.api.GreeterServiceHttpExport;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.bootstrap.builders.ApplicationBuilder;
import org.apache.dubbo.config.bootstrap.builders.ProtocolBuilder;
import org.apache.dubbo.config.bootstrap.builders.ReferenceBuilder;
import org.apache.dubbo.config.bootstrap.builders.RegistryBuilder;
import org.apache.dubbo.config.bootstrap.builders.ServiceBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GreeterService Dubbo 集成测试
 * 
 * 测试方式：启动本地 Dubbo 服务，然后通过 Dubbo 消费者调用
 */
public class GreeterServiceTest {

    private static final String VERSION = "1.0.0";
    private static final String GROUP = "test";
    private static int servicePort;

    @BeforeAll
    public static void setup() {
        // 使用随机端口
        servicePort = findAvailablePort();
        
        // 启动 Dubbo Bootstrap
        DubboBootstrap.getInstance()
            .application(ApplicationBuilder.newBuilder().name("greeter-service-test").build())
            .protocol(ProtocolBuilder.newBuilder().name("tri").port(servicePort).build())
            .registry(RegistryBuilder.newBuilder().address("N/A").build())
            .service(ServiceBuilder.newBuilder()
                .interfaceClass(GreeterServiceHttpExport.class)
                .ref(new com.pocrd.api_publish_service.service.impl.GreeterServiceHttpExportImpl())
                .version(VERSION)
                .group(GROUP)
                .build())
            .start();
    }

    @AfterAll
    public static void teardown() {
        DubboBootstrap.getInstance().stop();
    }

    /**
     * 查找可用端口
     */
    private static int findAvailablePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find available port", e);
        }
    }

    /**
     * 创建消费者引用
     */
    private GreeterServiceHttpExport createConsumer() {
        ReferenceConfig<GreeterServiceHttpExport> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(GreeterServiceHttpExport.class);
        referenceConfig.setVersion(VERSION);
        referenceConfig.setGroup(GROUP);
        referenceConfig.setUrl("tri://localhost:" + servicePort);
        return referenceConfig.get();
    }

    @Test
    public void testGreet() {
        // 创建消费者引用
        GreeterServiceHttpExport greeterService = createConsumer();

        // 调用服务
        String result = greeterService.greet("World");

        // 验证结果
        assertNotNull(result);
        assertTrue(result.contains("Hello World"));
        assertTrue(result.contains("from"));
        System.out.println("Test result: " + result);
    }

    @Test
    public void testGreetWithEmptyName() {
        // 创建消费者引用
        GreeterServiceHttpExport greeterService = createConsumer();

        // 调用服务
        String result = greeterService.greet("");

        // 验证结果
        assertNotNull(result);
        assertTrue(result.contains("Hello"));
        System.out.println("Empty name result: " + result);
    }

    @Test
    public void testGreet2() {
        // 创建消费者引用
        GreeterServiceHttpExport greeterService = createConsumer();

        // 调用服务
        String result = greeterService.greet2("Alice", "Bob");

        // 验证结果
        assertNotNull(result);
        assertTrue(result.contains("Hello Alice and Bob"));
        System.out.println("Greet2 result: " + result);
    }
}
