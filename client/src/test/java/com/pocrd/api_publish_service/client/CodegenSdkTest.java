package com.pocrd.api_publish_service.client;

import com.pocrd.clientsdk.autogen.api.GreeterService_Greet;
import com.pocrd.clientsdk.autogen.api.GreeterService_Greet2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基于 Codegen SDK 的接口测试类
 * <p>
 * 使用生成的 API 类进行 HTTP/HTTPS 接口调用测试
 * 支持 mTLS（客户端证书认证）
 */
public class CodegenSdkTest {

    private static CodegenHttpClient httpClient;

    // 默认网关 URL
    private static final String DEFAULT_GATEWAY_URL = "https://api.caringfamily.cn:30443";

    @BeforeAll
    public static void setUp() {
        // 从系统属性获取配置，没有则使用默认值
        String gatewayUrl = System.getProperty("gateway.url", DEFAULT_GATEWAY_URL);
        String resolveIp = System.getProperty("resolve.ip", "127.0.0.1");  // 模拟 --resolve
        int resolvePort = Integer.parseInt(System.getProperty("resolve.port", "30443"));

        // 获取证书路径（从系统属性或动态计算）
        String clientCertPath = getClientCertPath();
        String clientKeyPath = getClientKeyPath();

        // 如果证书存在，使用 mTLS 模式（带 --resolve 功能）
        if (clientCertPath != null && clientKeyPath != null && new java.io.File(clientCertPath).exists()) {
            httpClient = new CodegenHttpClient(gatewayUrl, resolveIp, resolvePort, clientCertPath, clientKeyPath);
            System.out.println("使用 HTTPS + mTLS + --resolve 模式");
            System.out.println("解析: " + extractHost(gatewayUrl) + " -> " + resolveIp + ":" + resolvePort);
            System.out.println("客户端证书: " + clientCertPath);
        } else {
            httpClient = new CodegenHttpClient(gatewayUrl);
            System.out.println("使用 HTTP 模式");
        }
        System.out.println("网关地址: " + gatewayUrl);
    }

    /**
     * 获取客户端证书路径（优先从系统属性，其次动态计算）
     */
    private static String getClientCertPath() {
        // 优先从系统属性获取
        String path = System.getProperty("client.cert.path");
        if (path != null && !path.isEmpty()) {
            return path;
        }

        // 动态计算：从项目根目录推导
        // 项目结构：client/src/test/java/.../CodegenSdkTest.java
        // 证书位置：../../higress/certs/files/bagua/testFactory/devices/device001/device001-fullchain.crt
        String userDir = System.getProperty("user.dir");
        java.io.File projectRoot = new java.io.File(userDir).getParentFile();
        if (projectRoot != null) {
            java.io.File certFile = new java.io.File(projectRoot,
                    "higress/certs/files/bagua/testFactory/devices/device001/device001-fullchain.crt");
            if (certFile.exists()) {
                return certFile.getAbsolutePath();
            }
        }

        return null;
    }

    /**
     * 获取客户端私钥路径（优先从系统属性，其次动态计算）
     */
    private static String getClientKeyPath() {
        // 优先从系统属性获取
        String path = System.getProperty("client.key.path");
        if (path != null && !path.isEmpty()) {
            return path;
        }

        // 动态计算：从项目根目录推导
        String userDir = System.getProperty("user.dir");
        java.io.File projectRoot = new java.io.File(userDir).getParentFile();
        if (projectRoot != null) {
            java.io.File keyFile = new java.io.File(projectRoot,
                    "higress/certs/files/bagua/testFactory/devices/device001/device001.key");
            if (keyFile.exists()) {
                return keyFile.getAbsolutePath();
            }
        }

        return null;
    }

    private static String extractHost(String url) {
        if (url.startsWith("https://")) {
            url = url.substring(8);
        } else if (url.startsWith("http://")) {
            url = url.substring(7);
        }
        int colonIndex = url.indexOf(':');
        if (colonIndex > 0) {
            return url.substring(0, colonIndex);
        }
        int slashIndex = url.indexOf('/');
        if (slashIndex > 0) {
            return url.substring(0, slashIndex);
        }
        return url;
    }

    @Test
    public void testGreet() throws Exception {
        // 使用生成的 API 类构建请求
        GreeterService_Greet request = new GreeterService_Greet("World");

        // 执行请求
        String response = httpClient.execute(request);

        // 验证响应
        System.out.println("testGreet 响应: " + response);
        assertNotNull(response);
        assertTrue(response.contains("Hello") || response.contains("World"));
    }

    @Test
    public void testGreet2() throws Exception {
        // 使用生成的 API 类构建请求（多参数）
        GreeterService_Greet2 request = new GreeterService_Greet2("Alice", "Bob");

        // 执行请求
        String response = httpClient.execute(request);

        // 验证响应
        System.out.println("testGreet2 响应: " + response);
        assertNotNull(response);
    }



    @Test
    public void testApiMethodProperties() {
        // 验证 API 方法类的属性
        GreeterService_Greet request = new GreeterService_Greet("Test");

        assertEquals("greet", request.getMethodName());
        assertEquals("GreeterService", request.getServiceName());
        assertEquals("com.pocrd.dubbo_demo.api.GreeterServiceHttpExport", request.getInterfaceName());
        assertTrue(request.isPost());

        // 验证 URL 构建
        String url = request.buildUrl("http://localhost:8080");
        assertEquals("http://localhost:8080/dapi/com.pocrd.dubbo_demo.api.GreeterServiceHttpExport/greet", url);

        // 验证请求体构建
        String body = request.buildRequestBody();
        assertTrue(body.contains("\"name\""));
        assertTrue(body.contains("\"Test\""));
    }
}
