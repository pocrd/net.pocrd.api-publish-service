package com.pocrd.api_publish_service.client;

import com.pocrd.clientsdk.autogen.api.*;
import com.pocrd.clientsdk.autogen.entity.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 所有 API 方法类的综合测试
 * <p>
 * 为 codegen SDK 中生成的所有 ApiMethod 类提供测试用例
 */
public class AllApiMethodsTest {

    private static CodegenHttpClient httpClient;

    // 默认网关 URL
    private static final String DEFAULT_GATEWAY_URL = "https://api.caringfamily.cn";

    @BeforeAll
    public static void setUp() {
        String gatewayUrl = System.getProperty("gateway.url", DEFAULT_GATEWAY_URL);

        // resolve.ip 和 resolve.port 用于控制实际连接目标
        // 如果不设置，则直接连接到 gatewayUrl 中的主机和端口
        String resolveIp = System.getProperty("resolve.ip");
        String resolvePortStr = System.getProperty("resolve.port");

        String clientCertPath = getClientCertPath();
        String clientKeyPath = getClientKeyPath();

        // 读取 debug 模式配置
        boolean debug = Boolean.parseBoolean(System.getProperty("debug", "false"));

        if (resolveIp != null && !resolveIp.isEmpty() && resolvePortStr != null && !resolvePortStr.isEmpty()) {
            int resolvePort = Integer.parseInt(resolvePortStr);
            httpClient = new CodegenHttpClient(gatewayUrl, resolveIp, resolvePort, clientCertPath, clientKeyPath, debug);
        } else {
            httpClient = new CodegenHttpClient(gatewayUrl, debug);
        }
    }

    private static String getClientCertPath() {
        String path = System.getProperty("client.cert.path");
        if (path != null && !path.isEmpty()) return path;

        String userDir = System.getProperty("user.dir");
        // 尝试从当前项目向上找到 deploy 目录，再进入 higress
        File currentDir = new File(userDir);
        while (currentDir != null && !currentDir.getName().equals("deploy")) {
            currentDir = currentDir.getParentFile();
        }
        if (currentDir != null) {
            File certFile = new File(currentDir,
                    "higress/certs/files/bagua/testFactory/devices/device001/device001-fullchain.crt");
            if (certFile.exists()) return certFile.getAbsolutePath();
        }
        return null;
    }

    private static String getClientKeyPath() {
        String path = System.getProperty("client.key.path");
        if (path != null && !path.isEmpty()) return path;

        String userDir = System.getProperty("user.dir");
        // 尝试从当前项目向上找到 deploy 目录，再进入 higress
        File currentDir = new File(userDir);
        while (currentDir != null && !currentDir.getName().equals("deploy")) {
            currentDir = currentDir.getParentFile();
        }
        if (currentDir != null) {
            File keyFile = new File(currentDir,
                    "higress/certs/files/bagua/testFactory/devices/device001/device001.key");
            if (keyFile.exists()) return keyFile.getAbsolutePath();
        }
        return null;
    }

    // ==================== GreeterService 测试 ====================

    @Test
    public void testGreeterService_Greet() throws Exception {
        GreeterService_Greet request = new GreeterService_Greet("World");
        String response = httpClient.execute(request);
        assertNotNull(response);
        assertTrue(response.contains("Hello") || response.contains("World"));
    }

    @Test
    public void testGreeterService_Greet2() throws Exception {
        GreeterService_Greet2 request = new GreeterService_Greet2("张三", "李四");
        String response = httpClient.execute(request);
        assertNotNull(response);
        assertTrue(response.contains("张三") && response.contains("李四"));
    }

    @Test
    public void testGreeterService_GreetStream() throws Exception {
        GreeterService_GreetStream request = new GreeterService_GreetStream("StreamUser");
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testGreeterService_TestComplexNestedEntityInput() throws Exception {
        // 构造复杂嵌套实体，包含特殊字符和边界值
        ApiPublish_ApiInfo apiInfo1 = new ApiPublish_ApiInfo("testMethod1", "/api/v1/test1", "GET", "Test API with \"quotes\"");
        ApiPublish_ApiInfo apiInfo2 = new ApiPublish_ApiInfo("testMethod2", "/api/v1/test2", "POST", "Unicode测试: 中文字符");
        ApiPublish_ServiceInfo serviceInfo = new ApiPublish_ServiceInfo(
                "TestService",
                "1.0.0",
                1234567890L,
                100,
                Arrays.asList(apiInfo1, apiInfo2),
                Arrays.asList("E0001", "E0002:错误\"信息\"")
        );

        // 验证序列化：将实体转为JSON
        String requestJson = serviceInfo.toJson();
        assertNotNull(requestJson);
        assertTrue(requestJson.contains("TestService"));
        assertTrue(requestJson.contains("testMethod1"));

        GreeterService_TestComplexNestedEntityInput request = new GreeterService_TestComplexNestedEntityInput(serviceInfo);
        String response = httpClient.execute(request);
        assertNotNull(response);

        // 验证响应包含服务端添加的验证标记
        assertTrue(response.contains("_processed") || response.contains("_verified"),
                "响应应包含服务端验证标记");

        // 尝试反序列化响应为实体（如果响应是JSON格式）
        if (response.startsWith("{")) {
            ApiPublish_ServiceInfo responseEntity = ApiPublish_ServiceInfo.fromJson(response);
            assertNotNull(responseEntity);
        }
    }

    @Test
    public void testGreeterService_TestListNestedEntityInput() throws Exception {
        // 构造包含多层嵌套的订单实体
        ApiPublish_AddressInfo address = new ApiPublish_AddressInfo("Province", "City", "District", "Street 123", "100000");
        ApiPublish_OrderItem item1 = new ApiPublish_OrderItem(1L, "Product with \"quotes\"", 2, "99.99", "199.98");
        ApiPublish_OrderItem item2 = new ApiPublish_OrderItem(2L, "Unicode产品 🎉", 3, "50.00", "150.00");
        ApiPublish_UserInfo buyer = new ApiPublish_UserInfo(1L, "buyer_test", "buyer@example.com", 30, true, address,
                Arrays.asList("vip", "verified"), Arrays.asList("user", "buyer"));
        ApiPublish_OrderInfo order = new ApiPublish_OrderInfo(1L, "ORDER001_VERIFIED", buyer,
                Arrays.asList(item1, item2), "PAID_VERIFIED", "349.98");

        // 验证序列化
        String orderJson = order.toJson();
        assertNotNull(orderJson);
        assertTrue(orderJson.contains("ORDER001_VERIFIED"));
        assertTrue(orderJson.contains("buyer_test"));

        GreeterService_TestListNestedEntityInput request = new GreeterService_TestListNestedEntityInput(Arrays.asList(order));
        String response = httpClient.execute(request);
        assertNotNull(response);

        // 验证响应是有效的整数（订单数量）
        int orderCount = Integer.parseInt(response.trim());
        assertEquals(1, orderCount, "应返回1个订单");
    }

    @Test
    public void testGreeterService_TestComplexParamCombination() throws Exception {
        // 构造复杂参数组合，包含特殊字符
        ApiPublish_AddressInfo address1 = new ApiPublish_AddressInfo("Province_测试", "City_\"quoted\"", "District", "Street 123", "100000");
        ApiPublish_AddressInfo address2 = new ApiPublish_AddressInfo("Province2", "City2 🎉", "District2", "Street \\ 456", "200000");
        ApiPublish_UserInfo user = new ApiPublish_UserInfo(1L, "user_verified", "user@example.com", 25, true, address1,
                Arrays.asList("tag1", "verified", "unicode_中文"), Arrays.asList("user", "test"));

        java.util.List<ApiPublish_AddressInfo> addresses = Arrays.asList(address1, address2);
        java.util.Set<String> tags = new java.util.HashSet<>(Arrays.asList("tag1", "tag2", "special_测试"));
        java.util.List<String> statusList = Arrays.asList("ACTIVE", "PENDING", "VERIFIED");

        // 验证UserInfo序列化
        String userJson = user.toJson();
        assertTrue(userJson.contains("user_verified"));

        GreeterService_TestComplexParamCombination request = new GreeterService_TestComplexParamCombination(42L, user, addresses, tags, statusList);
        String response = httpClient.execute(request);
        assertNotNull(response);

        // 验证响应包含服务端验证标记
        assertTrue(response.contains("VERIFIED"), "响应应包含服务端验证标记");
        assertTrue(response.contains("UserId: 42"), "响应应包含正确的userId");
    }

    @Test
    public void testGreeterService_TestBatchEntityArrayInput() throws Exception {
        // 构造包含特殊字符的订单项数组
        ApiPublish_OrderItem item1 = new ApiPublish_OrderItem(1L, "Product1_\"quoted\"", 2, "99.99", "199.98");
        ApiPublish_OrderItem item2 = new ApiPublish_OrderItem(2L, "Product2_测试 🎉", 1, "49.99", "49.99");
        ApiPublish_OrderItem item3 = new ApiPublish_OrderItem(3L, "Product3 with \\ backslash", 5, "10.00", "50.00");

        // 验证序列化
        String item1Json = item1.toJson();
        assertTrue(item1Json.contains("Product1"));

        GreeterService_TestBatchEntityArrayInput request = new GreeterService_TestBatchEntityArrayInput(Arrays.asList(item1, item2, item3));
        String response = httpClient.execute(request);
        assertNotNull(response);

        // 验证响应是有效的整数（订单项数量）
        int itemCount = Integer.parseInt(response.trim());
        assertEquals(3, itemCount, "应返回3个订单项");
    }

    @Test
    public void testGreeterService_TestDeepNestedEntityReturn() throws Exception {
        GreeterService_TestDeepNestedEntityReturn request = new GreeterService_TestDeepNestedEntityReturn("mock-service");
        String response = httpClient.execute(request);
        assertNotNull(response);

        // 验证响应是有效的JSON且可以反序列化
        if (response.startsWith("{")) {
            ApiPublish_ServiceInfo serviceInfo = ApiPublish_ServiceInfo.fromJson(response);
            assertNotNull(serviceInfo);

            // 验证服务端返回的验证标记
            assertTrue(serviceInfo.serviceName().contains("_verified"),
                    "serviceName应包含_verified标记");
            assertTrue(serviceInfo.version().contains("_test"),
                    "version应包含_test标记");

            // 验证边界值
            assertEquals(9223372036854775807L, serviceInfo.uptime(),
                    "uptime应为Long.MAX_VALUE边界值");
            assertEquals(2147483647, serviceInfo.requestCount(),
                    "requestCount应为Integer.MAX_VALUE边界值");

            // 验证嵌套数组
            assertNotNull(serviceInfo.apiInfos());
            assertFalse(serviceInfo.apiInfos().isEmpty(), "apiInfos不应为空");

            // 验证错误码列表包含特殊字符
            assertNotNull(serviceInfo.errorCodes());
            boolean hasUnicodeErrorCode = serviceInfo.errorCodes().stream()
                    .anyMatch(code -> code.contains("unicode") || code.contains("\"quotes\""));
            assertTrue(hasUnicodeErrorCode, "错误码应包含unicode或引号等特殊字符");
        }
    }

    @Test
    public void testGreeterService_TestPagedNestedEntityReturn() throws Exception {
        GreeterService_TestPagedNestedEntityReturn request = new GreeterService_TestPagedNestedEntityReturn(1, 10);
        String response = httpClient.execute(request);
        assertNotNull(response);

        // 验证响应是JSON数组且可以反序列化
        if (response.startsWith("[")) {
            // 处理可能的嵌套数组格式 [[...]]
            String jsonToParse = response;
            
            // 使用JsonUtil解析JSON数组
            java.util.List<ApiPublish_OrderInfo> orders = com.pocrd.clientsdk.JsonUtil.parseList(
                jsonToParse, ApiPublish_OrderInfo::fromJson);
            assertFalse(orders.isEmpty(), "应返回至少一个订单");

            ApiPublish_OrderInfo order = orders.get(0);
            assertNotNull(order);
            assertTrue(order.status().contains("VERIFIED"),
                    "订单状态应包含VERIFIED标记");
        } else {
        }
    }

    @Test
    public void testGreeterService_TestComplexSetEntityReturn() throws Exception {
        GreeterService_TestComplexSetEntityReturn request = new GreeterService_TestComplexSetEntityReturn(Arrays.asList(1L, 2L, 3L));
        String response = httpClient.execute(request);
        assertNotNull(response);

        // 验证响应是JSON数组且可以反序列化
        if (response.startsWith("[")) {
            // 使用 JsonUtil 解析数组，然后逐一调用 fromJson
            java.util.List<ApiPublish_OrderInfo> orders = com.pocrd.clientsdk.JsonUtil.parseList(
                    response, ApiPublish_OrderInfo::fromJson);
            assertFalse(orders.isEmpty(), "应返回至少一个订单");
            assertEquals(3, orders.size(), "应返回3个订单（对应3个userId）");

            // 验证每个订单包含VERIFIED标记
            for (ApiPublish_OrderInfo order : orders) {
                assertTrue(order.status().contains("VERIFIED"),
                        "订单状态应包含VERIFIED标记");
            }
        }
    }

    @Test
    public void testGreeterService_TestGroupedItemsReturn() throws Exception {
        GreeterService_TestGroupedItemsReturn request = new GreeterService_TestGroupedItemsReturn(3392L);
        String response = httpClient.execute(request);
        assertNotNull(response);

        // 验证响应是有效的JSON且可以反序列化
        if (response.startsWith("{")) {
            ApiPublish_OrderInfo order = ApiPublish_OrderInfo.fromJson(response);
            assertNotNull(order);

            // 验证服务端返回的验证标记
            assertTrue(order.orderNo().contains("VERIFIED"),
                    "orderNo应包含VERIFIED标记");
            assertTrue(order.status().contains("VERIFIED"),
                    "status应包含VERIFIED标记");

            // 验证订单项包含特殊字符
            assertNotNull(order.items());
            assertFalse(order.items().isEmpty(), "订单项不应为空");

            // 检查是否包含特殊字符的订单项
            boolean hasSpecialChars = order.items().stream()
                    .anyMatch(item -> item.productName().contains("quotes") ||
                            item.productName().contains("Unicode") ||
                            item.productName().contains("测试"));
            assertTrue(hasSpecialChars, "订单项应包含特殊字符（引号、unicode等）");
        }
    }

    @Test
    public void testGreeterService_TestEntityArrayReturn() throws Exception {
        GreeterService_TestEntityArrayReturn request = new GreeterService_TestEntityArrayReturn(5);
        String response = httpClient.execute(request);
        assertNotNull(response);

        // 验证响应是JSON数组且可以反序列化
        if (response.startsWith("[")) {
            // 使用 JsonUtil 解析数组，然后逐一调用 fromJson
            java.util.List<ApiPublish_UserInfo> users = com.pocrd.clientsdk.JsonUtil.parseList(
                    response, ApiPublish_UserInfo::fromJson);
            assertFalse(users.isEmpty(), "应返回至少一个用户");
            assertEquals(3, users.size(), "应返回3个用户");

            // 验证每个用户包含VERIFIED标记
            for (ApiPublish_UserInfo user : users) {
                assertTrue(user.username().contains("_verified_"),
                        "用户名应包含_verified_标记");
            }

        }
    }

    @Test
    public void testGreeterService_TestComplexScenario() throws Exception {
        // 构造复杂场景参数，包含特殊字符
        ApiPublish_ApiInfo apiInfo1 = new ApiPublish_ApiInfo("testMethod1", "/api/v1/test1", "GET", "API with \"quotes\"");
        ApiPublish_ApiInfo apiInfo2 = new ApiPublish_ApiInfo("testMethod2", "/api/v1/test2", "POST", "API 测试 🎉");
        java.util.List<ApiPublish_ApiInfo> apiInfos = Arrays.asList(apiInfo1, apiInfo2);
        ApiPublish_AddressInfo config = new ApiPublish_AddressInfo("Province_测试", "City_VERIFIED", "District", "Street 123", "100000");

        // 验证序列化
        String configJson = config.toJson();
        assertTrue(configJson.contains("Province_测试"));

        GreeterService_TestComplexScenario request = new GreeterService_TestComplexScenario(apiInfos, config);
        String response = httpClient.execute(request);
        assertNotNull(response);

        // 验证响应是有效的JSON且可以反序列化
        if (response.startsWith("{")) {
            ApiPublish_ServiceInfo serviceInfo = ApiPublish_ServiceInfo.fromJson(response);
            assertNotNull(serviceInfo);

            // 验证服务端返回的验证标记
            assertTrue(serviceInfo.serviceName().contains("VERIFIED"),
                    "serviceName应包含VERIFIED标记");
            assertTrue(serviceInfo.version().contains("verified"),
                    "version应包含verified标记");

            // 验证错误码包含输入参数信息
            assertNotNull(serviceInfo.errorCodes());
            boolean hasVerifiedErrorCode = serviceInfo.errorCodes().stream()
                    .anyMatch(code -> code.contains("VERIFIED"));
            assertTrue(hasVerifiedErrorCode, "错误码应包含VERIFIED标记");

            // 验证边界值
            assertEquals(9223372036854775807L, serviceInfo.uptime(),
                    "uptime应为Long.MAX_VALUE边界值");
        }
    }

    // ==================== CRUDService - User 测试 ====================

    @Test
    public void testCRUDService_CreateUser() throws Exception {
        // 使用随机数生成唯一用户名，避免 DuplicateKeyException
        String randomUsername = "user" + System.currentTimeMillis();
        String randomEmail = "user" + System.currentTimeMillis() + "@example.com";
        String randomPhone = "138" + (10000000 + (int)(Math.random() * 89999999));
        DubboDemo_User user = new DubboDemo_User(0L, randomUsername, randomEmail, randomPhone);
        CRUDService_CreateUser request = new CRUDService_CreateUser(user);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_GetUserById() throws Exception {
        CRUDService_GetUserById request = new CRUDService_GetUserById(1L);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_UpdateUser() throws Exception {
        DubboDemo_User user = new DubboDemo_User(1L, "updateduser", "updated@example.com", "13900139000");
        CRUDService_UpdateUser request = new CRUDService_UpdateUser(user);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_DeleteUser() throws Exception {
        CRUDService_DeleteUser request = new CRUDService_DeleteUser(1L);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_GetAllUsers() throws Exception {
        CRUDService_GetAllUsers request = new CRUDService_GetAllUsers();
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    // ==================== CRUDService - Product 测试 ====================

    @Test
    public void testCRUDService_CreateProduct() throws Exception {
        // 使用随机数生成唯一 productCode，避免 DuplicateKeyException
        String randomCode = "PROD" + System.currentTimeMillis();
        DubboDemo_Product product = new DubboDemo_Product(0L, randomCode, "TestProduct", 9999, 100, "Electronics");
        CRUDService_CreateProduct request = new CRUDService_CreateProduct(product);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_GetProductById() throws Exception {
        CRUDService_GetProductById request = new CRUDService_GetProductById(1L);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_UpdateProduct() throws Exception {
        DubboDemo_Product product = new DubboDemo_Product(1L, "PROD001", "UpdatedProduct", 19999, 50, "Electronics");
        CRUDService_UpdateProduct request = new CRUDService_UpdateProduct(product);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_DeleteProduct() throws Exception {
        CRUDService_DeleteProduct request = new CRUDService_DeleteProduct(1L);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_GetAllProducts() throws Exception {
        CRUDService_GetAllProducts request = new CRUDService_GetAllProducts();
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    // ==================== CRUDService - Order 测试 ====================

    @Test
    public void testCRUDService_CreateOrder() throws Exception {
        // 使用随机数生成唯一订单号，避免 DuplicateKeyException
        String randomOrderNo = "ORD" + System.currentTimeMillis();
        DubboDemo_Order order = new DubboDemo_Order(0L, randomOrderNo, 1L, 29999, 1, "Test order");
        CRUDService_CreateOrder request = new CRUDService_CreateOrder(order);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_GetOrderById() throws Exception {
        CRUDService_GetOrderById request = new CRUDService_GetOrderById(1L);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_UpdateOrder() throws Exception {
        DubboDemo_Order order = new DubboDemo_Order(1L, "ORDER001", 1L, 39999, 2, "Updated order");
        CRUDService_UpdateOrder request = new CRUDService_UpdateOrder(order);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_DeleteOrder() throws Exception {
        CRUDService_DeleteOrder request = new CRUDService_DeleteOrder(1L);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_GetAllOrders() throws Exception {
        CRUDService_GetAllOrders request = new CRUDService_GetAllOrders();
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    // ==================== ValidApiTestService - 基础类型测试 ====================

    @Test
    public void testValidApiTestService_TestBasicTypes() throws Exception {
        ValidApiTestService_TestBasicTypes request = new ValidApiTestService_TestBasicTypes("test", 42, 123456789L, true);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestNoParams() throws Exception {
        ValidApiTestService_TestNoParams request = new ValidApiTestService_TestNoParams();
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestSingleEntity() throws Exception {
        ApiPublish_AddressInfo address = new ApiPublish_AddressInfo("Province", "City", "District", "Street 123", "100000");
        ValidApiTestService_TestSingleEntity request = new ValidApiTestService_TestSingleEntity(address);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestNestedEntity() throws Exception {
        ApiPublish_AddressInfo address = new ApiPublish_AddressInfo("Province", "City", "District", "Street 123", "100000");
        ApiPublish_UserInfo user = new ApiPublish_UserInfo(1L, "testuser", "test@example.com", 25, true, address, null, null);
        ValidApiTestService_TestNestedEntity request = new ValidApiTestService_TestNestedEntity(user);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestDeepNestedEntity() throws Exception {
        ApiPublish_OrderItem item = new ApiPublish_OrderItem(1L, "Product", 2, "99.99", "199.98");
        ApiPublish_UserInfo buyer = new ApiPublish_UserInfo(1L, "buyer", "buyer@example.com", 30, true, null, null, null);
        ApiPublish_OrderInfo order = new ApiPublish_OrderInfo(1L, "ORDER001", buyer, Arrays.asList(item), "PAID", "199.98");
        ValidApiTestService_TestDeepNestedEntity request = new ValidApiTestService_TestDeepNestedEntity(order);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    // ==================== ValidApiTestService - 集合类型测试 ====================

    @Test
    public void testValidApiTestService_TestArrayParam() throws Exception {
        java.util.List<Long> ids = Arrays.asList(1L, 2L, 3L);
        ValidApiTestService_TestArrayParam request = new ValidApiTestService_TestArrayParam(ids);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestListParam() throws Exception {
        java.util.List<String> list = Arrays.asList("item1", "item2", "item3");
        ValidApiTestService_TestListParam request = new ValidApiTestService_TestListParam(list);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestSetParam() throws Exception {
        java.util.Set<String> set = new java.util.HashSet<>(Arrays.asList("item1", "item2"));
        ValidApiTestService_TestSetParam request = new ValidApiTestService_TestSetParam(set);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestComplexParams() throws Exception {
        ApiPublish_OrderItem item = new ApiPublish_OrderItem(1L, "Product", 2, "99.99", "199.98");
        java.util.List<ApiPublish_OrderItem> items = Arrays.asList(item);
        ApiPublish_AddressInfo address = new ApiPublish_AddressInfo("Province", "City", "District", "Street 123", "100000");
        ValidApiTestService_TestComplexParams request = new ValidApiTestService_TestComplexParams(1L, items, address);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    // ==================== ValidApiTestService - 枚举和特殊类型测试 ====================

    @Test
    public void testValidApiTestService_TestEnumParam() throws Exception {
        ValidApiTestService_TestEnumParam request = new ValidApiTestService_TestEnumParam("PENDING");
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestEnumFieldEntity() throws Exception {
        ApiPublish_EnumFieldEntity entity = new ApiPublish_EnumFieldEntity(1L, "PENDING", "Description");
        ValidApiTestService_TestEnumFieldEntity request = new ValidApiTestService_TestEnumFieldEntity(entity);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestOptionalParams() throws Exception {
        ValidApiTestService_TestOptionalParams request = new ValidApiTestService_TestOptionalParams("required");
        request.setOptionalparam("optional");
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    // ==================== ValidApiTestService - 验证和返回类型测试 ====================

    @Test
    public void testValidApiTestService_TestRegexValidation() throws Exception {
        ValidApiTestService_TestRegexValidation request = new ValidApiTestService_TestRegexValidation("ABC123");
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestMultipleValidation() throws Exception {
        ValidApiTestService_TestMultipleValidation request = new ValidApiTestService_TestMultipleValidation("test@example.com", "13800138000");
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestReturnArray() throws Exception {
        ValidApiTestService_TestReturnArray request = new ValidApiTestService_TestReturnArray(10);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestReturnList() throws Exception {
        ValidApiTestService_TestReturnList request = new ValidApiTestService_TestReturnList(10);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestVoidReturn() throws Exception {
        ValidApiTestService_TestVoidReturn request = new ValidApiTestService_TestVoidReturn("test");
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestWrapperReturn() throws Exception {
        ValidApiTestService_TestWrapperReturn request = new ValidApiTestService_TestWrapperReturn(42);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    // ==================== ValidApiTestService - 分页和流式测试 ====================

    @Test
    public void testValidApiTestService_TestPageQuery() throws Exception {
        ValidApiTestService_TestPageQuery request = new ValidApiTestService_TestPageQuery(1, 10);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestPageWithNestedEntity() throws Exception {
        ValidApiTestService_TestPageWithNestedEntity request = new ValidApiTestService_TestPageWithNestedEntity("PENDING", 1, 10);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestServerStream() throws Exception {
        ValidApiTestService_TestServerStream request = new ValidApiTestService_TestServerStream(100L);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    // ==================== ValidApiTestService - 循环引用和集合返回测试 ====================

    @Test
    public void testValidApiTestService_TestCircularEntity() throws Exception {
        ApiPublish_CircularEntityA entityA = new ApiPublish_CircularEntityA(1L, "A", null);
        ApiPublish_CircularEntityB entityB = new ApiPublish_CircularEntityB(1L, "B", entityA);
        ValidApiTestService_TestCircularEntity request = new ValidApiTestService_TestCircularEntity(entityB);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestSetEntityReturn() throws Exception {
        java.util.List<Long> ids = Arrays.asList(1L, 2L, 3L);
        ValidApiTestService_TestSetEntityReturn request = new ValidApiTestService_TestSetEntityReturn(ids);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestStreamObserverComplex() throws Exception {
        ValidApiTestService_TestStreamObserverComplex request = new ValidApiTestService_TestStreamObserverComplex(100L);
        String response = httpClient.execute(request);
        assertNotNull(response);
    }
}
