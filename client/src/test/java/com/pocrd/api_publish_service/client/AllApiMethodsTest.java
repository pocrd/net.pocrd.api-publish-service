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
    private static final String DEFAULT_GATEWAY_URL = "https://api.caringfamily.cn:30443";

    @BeforeAll
    public static void setUp() {
        String gatewayUrl = System.getProperty("gateway.url", DEFAULT_GATEWAY_URL);
        String resolveIp = System.getProperty("resolve.ip", "127.0.0.1");
        int resolvePort = Integer.parseInt(System.getProperty("resolve.port", "30443"));

        String clientCertPath = getClientCertPath();
        String clientKeyPath = getClientKeyPath();

        if (clientCertPath != null && clientKeyPath != null && new File(clientCertPath).exists()) {
            httpClient = new CodegenHttpClient(gatewayUrl, resolveIp, resolvePort, clientCertPath, clientKeyPath);
            System.out.println("使用 HTTPS + mTLS + --resolve 模式");
        } else {
            httpClient = new CodegenHttpClient(gatewayUrl);
            System.out.println("使用 HTTP 模式");
        }
        System.out.println("网关地址: " + gatewayUrl);
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
        System.out.println("GreeterService_Greet 响应: " + response);
        assertNotNull(response);
        assertTrue(response.contains("Hello") || response.contains("World"));
    }

    @Test
    public void testGreeterService_Greet2() throws Exception {
        GreeterService_Greet2 request = new GreeterService_Greet2("张三", "李四");
        String response = httpClient.execute(request);
        System.out.println("GreeterService_Greet2 响应: " + response);
        assertNotNull(response);
        assertTrue(response.contains("张三") && response.contains("李四"));
    }

    @Test
    public void testGreeterService_GreetStream() throws Exception {
        GreeterService_GreetStream request = new GreeterService_GreetStream("StreamUser");
        String response = httpClient.execute(request);
        System.out.println("GreeterService_GreetStream 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testGreeterService_TestComplexNestedEntityInput() throws Exception {
        ApiPublish_ApiInfo apiInfo = new ApiPublish_ApiInfo("testMethod", "/test", "GET", "Test API");
        ApiPublish_ServiceInfo serviceInfo = new ApiPublish_ServiceInfo("TestService", "1.0.0", 0L, 0, Arrays.asList(apiInfo), Arrays.asList("E0001"));
        GreeterService_TestComplexNestedEntityInput request = new GreeterService_TestComplexNestedEntityInput(serviceInfo);
        String response = httpClient.execute(request);
        System.out.println("GreeterService_TestComplexNestedEntityInput 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testGreeterService_TestListNestedEntityInput() throws Exception {
        ApiPublish_OrderItem item = new ApiPublish_OrderItem(1L, "Product", 2, "99.99", "199.98");
        ApiPublish_UserInfo buyer = new ApiPublish_UserInfo(1L, "buyer", "buyer@example.com", 30, true, null, null, null);
        ApiPublish_OrderInfo order = new ApiPublish_OrderInfo(1L, "ORDER001", buyer, Arrays.asList(item), "PAID", "199.98");
        GreeterService_TestListNestedEntityInput request = new GreeterService_TestListNestedEntityInput(Arrays.asList(order));
        String response = httpClient.execute(request);
        System.out.println("GreeterService_TestListNestedEntityInput 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testGreeterService_TestComplexParamCombination() throws Exception {
        ApiPublish_AddressInfo address = new ApiPublish_AddressInfo("Province", "City", "District", "Street 123", "100000");
        ApiPublish_UserInfo user = new ApiPublish_UserInfo(1L, "user", "user@example.com", 25, true, address, null, null);

        java.util.List<ApiPublish_AddressInfo> addresses = Arrays.asList(address);
        java.util.Set<String> tags = new java.util.HashSet<>(Arrays.asList("tag1", "tag2"));
        java.util.List<String> statusList = Arrays.asList("ACTIVE", "PENDING");
        GreeterService_TestComplexParamCombination request = new GreeterService_TestComplexParamCombination(42L, user, addresses, tags, statusList);
        String response = httpClient.execute(request);
        System.out.println("GreeterService_TestComplexParamCombination 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testGreeterService_TestBatchEntityArrayInput() throws Exception {
        ApiPublish_OrderItem item1 = new ApiPublish_OrderItem(1L, "Product1", 2, "99.99", "199.98");
        ApiPublish_OrderItem item2 = new ApiPublish_OrderItem(2L, "Product2", 1, "49.99", "49.99");
        GreeterService_TestBatchEntityArrayInput request = new GreeterService_TestBatchEntityArrayInput(Arrays.asList(item1, item2));
        String response = httpClient.execute(request);
        System.out.println("GreeterService_TestBatchEntityArrayInput 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testGreeterService_TestDeepNestedEntityReturn() throws Exception {
        GreeterService_TestDeepNestedEntityReturn request = new GreeterService_TestDeepNestedEntityReturn("mock-service");
        String response = httpClient.execute(request);
        System.out.println("GreeterService_TestDeepNestedEntityReturn 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testGreeterService_TestPagedNestedEntityReturn() throws Exception {
        GreeterService_TestPagedNestedEntityReturn request = new GreeterService_TestPagedNestedEntityReturn(1, 10);
        String response = httpClient.execute(request);
        System.out.println("GreeterService_TestPagedNestedEntityReturn 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testGreeterService_TestComplexSetEntityReturn() throws Exception {
        GreeterService_TestComplexSetEntityReturn request = new GreeterService_TestComplexSetEntityReturn(Arrays.asList(1L, 2L, 3L));
        String response = httpClient.execute(request);
        System.out.println("GreeterService_TestComplexSetEntityReturn 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testGreeterService_TestGroupedItemsReturn() throws Exception {
        GreeterService_TestGroupedItemsReturn request = new GreeterService_TestGroupedItemsReturn(3392L);
        String response = httpClient.execute(request);
        System.out.println("GreeterService_TestGroupedItemsReturn 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testGreeterService_TestEntityArrayReturn() throws Exception {
        GreeterService_TestEntityArrayReturn request = new GreeterService_TestEntityArrayReturn(5);
        String response = httpClient.execute(request);
        System.out.println("GreeterService_TestEntityArrayReturn 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testGreeterService_TestComplexScenario() throws Exception {
        ApiPublish_ApiInfo apiInfo = new ApiPublish_ApiInfo("testMethod", "/test", "GET", "Test API");
        java.util.List<ApiPublish_ApiInfo> apiInfos = Arrays.asList(apiInfo);
        ApiPublish_AddressInfo config = new ApiPublish_AddressInfo("Province", "City", "District", "Street 123", "100000");
        GreeterService_TestComplexScenario request = new GreeterService_TestComplexScenario(apiInfos, config);
        String response = httpClient.execute(request);
        System.out.println("GreeterService_TestComplexScenario 响应: " + response);
        assertNotNull(response);
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
        System.out.println("CRUDService_CreateUser 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_GetUserById() throws Exception {
        CRUDService_GetUserById request = new CRUDService_GetUserById(1L);
        String response = httpClient.execute(request);
        System.out.println("CRUDService_GetUserById 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_UpdateUser() throws Exception {
        DubboDemo_User user = new DubboDemo_User(1L, "updateduser", "updated@example.com", "13900139000");
        CRUDService_UpdateUser request = new CRUDService_UpdateUser(user);
        String response = httpClient.execute(request);
        System.out.println("CRUDService_UpdateUser 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_DeleteUser() throws Exception {
        CRUDService_DeleteUser request = new CRUDService_DeleteUser(1L);
        String response = httpClient.execute(request);
        System.out.println("CRUDService_DeleteUser 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_GetAllUsers() throws Exception {
        CRUDService_GetAllUsers request = new CRUDService_GetAllUsers();
        String response = httpClient.execute(request);
        System.out.println("CRUDService_GetAllUsers 响应: " + response);
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
        System.out.println("CRUDService_CreateProduct 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_GetProductById() throws Exception {
        CRUDService_GetProductById request = new CRUDService_GetProductById(1L);
        String response = httpClient.execute(request);
        System.out.println("CRUDService_GetProductById 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_UpdateProduct() throws Exception {
        DubboDemo_Product product = new DubboDemo_Product(1L, "PROD001", "UpdatedProduct", 19999, 50, "Electronics");
        CRUDService_UpdateProduct request = new CRUDService_UpdateProduct(product);
        String response = httpClient.execute(request);
        System.out.println("CRUDService_UpdateProduct 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_DeleteProduct() throws Exception {
        CRUDService_DeleteProduct request = new CRUDService_DeleteProduct(1L);
        String response = httpClient.execute(request);
        System.out.println("CRUDService_DeleteProduct 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_GetAllProducts() throws Exception {
        CRUDService_GetAllProducts request = new CRUDService_GetAllProducts();
        String response = httpClient.execute(request);
        System.out.println("CRUDService_GetAllProducts 响应: " + response);
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
        System.out.println("CRUDService_CreateOrder 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_GetOrderById() throws Exception {
        CRUDService_GetOrderById request = new CRUDService_GetOrderById(1L);
        String response = httpClient.execute(request);
        System.out.println("CRUDService_GetOrderById 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_UpdateOrder() throws Exception {
        DubboDemo_Order order = new DubboDemo_Order(1L, "ORDER001", 1L, 39999, 2, "Updated order");
        CRUDService_UpdateOrder request = new CRUDService_UpdateOrder(order);
        String response = httpClient.execute(request);
        System.out.println("CRUDService_UpdateOrder 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_DeleteOrder() throws Exception {
        CRUDService_DeleteOrder request = new CRUDService_DeleteOrder(1L);
        String response = httpClient.execute(request);
        System.out.println("CRUDService_DeleteOrder 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testCRUDService_GetAllOrders() throws Exception {
        CRUDService_GetAllOrders request = new CRUDService_GetAllOrders();
        String response = httpClient.execute(request);
        System.out.println("CRUDService_GetAllOrders 响应: " + response);
        assertNotNull(response);
    }

    // ==================== ValidApiTestService - 基础类型测试 ====================

    @Test
    public void testValidApiTestService_TestBasicTypes() throws Exception {
        ValidApiTestService_TestBasicTypes request = new ValidApiTestService_TestBasicTypes("test", 42, 123456789L, true);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestBasicTypes 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestNoParams() throws Exception {
        ValidApiTestService_TestNoParams request = new ValidApiTestService_TestNoParams();
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestNoParams 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestSingleEntity() throws Exception {
        ApiPublish_AddressInfo address = new ApiPublish_AddressInfo("Province", "City", "District", "Street 123", "100000");
        ValidApiTestService_TestSingleEntity request = new ValidApiTestService_TestSingleEntity(address);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestSingleEntity 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestNestedEntity() throws Exception {
        ApiPublish_AddressInfo address = new ApiPublish_AddressInfo("Province", "City", "District", "Street 123", "100000");
        ApiPublish_UserInfo user = new ApiPublish_UserInfo(1L, "testuser", "test@example.com", 25, true, address, null, null);
        ValidApiTestService_TestNestedEntity request = new ValidApiTestService_TestNestedEntity(user);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestNestedEntity 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestDeepNestedEntity() throws Exception {
        ApiPublish_OrderItem item = new ApiPublish_OrderItem(1L, "Product", 2, "99.99", "199.98");
        ApiPublish_UserInfo buyer = new ApiPublish_UserInfo(1L, "buyer", "buyer@example.com", 30, true, null, null, null);
        ApiPublish_OrderInfo order = new ApiPublish_OrderInfo(1L, "ORDER001", buyer, Arrays.asList(item), "PAID", "199.98");
        ValidApiTestService_TestDeepNestedEntity request = new ValidApiTestService_TestDeepNestedEntity(order);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestDeepNestedEntity 响应: " + response);
        assertNotNull(response);
    }

    // ==================== ValidApiTestService - 集合类型测试 ====================

    @Test
    public void testValidApiTestService_TestArrayParam() throws Exception {
        java.util.List<Long> ids = Arrays.asList(1L, 2L, 3L);
        ValidApiTestService_TestArrayParam request = new ValidApiTestService_TestArrayParam(ids);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestArrayParam 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestListParam() throws Exception {
        java.util.List<String> list = Arrays.asList("item1", "item2", "item3");
        ValidApiTestService_TestListParam request = new ValidApiTestService_TestListParam(list);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestListParam 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestSetParam() throws Exception {
        java.util.Set<String> set = new java.util.HashSet<>(Arrays.asList("item1", "item2"));
        ValidApiTestService_TestSetParam request = new ValidApiTestService_TestSetParam(set);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestSetParam 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestComplexParams() throws Exception {
        ApiPublish_OrderItem item = new ApiPublish_OrderItem(1L, "Product", 2, "99.99", "199.98");
        java.util.List<ApiPublish_OrderItem> items = Arrays.asList(item);
        ApiPublish_AddressInfo address = new ApiPublish_AddressInfo("Province", "City", "District", "Street 123", "100000");
        ValidApiTestService_TestComplexParams request = new ValidApiTestService_TestComplexParams(1L, items, address);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestComplexParams 响应: " + response);
        assertNotNull(response);
    }

    // ==================== ValidApiTestService - 枚举和特殊类型测试 ====================

    @Test
    public void testValidApiTestService_TestEnumParam() throws Exception {
        ValidApiTestService_TestEnumParam request = new ValidApiTestService_TestEnumParam("PENDING");
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestEnumParam 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestEnumFieldEntity() throws Exception {
        ApiPublish_EnumFieldEntity entity = new ApiPublish_EnumFieldEntity(1L, "PENDING", "Description");
        ValidApiTestService_TestEnumFieldEntity request = new ValidApiTestService_TestEnumFieldEntity(entity);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestEnumFieldEntity 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestOptionalParams() throws Exception {
        ValidApiTestService_TestOptionalParams request = new ValidApiTestService_TestOptionalParams("required");
        request.setOptionalparam("optional");
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestOptionalParams 响应: " + response);
        assertNotNull(response);
    }

    // ==================== ValidApiTestService - 验证和返回类型测试 ====================

    @Test
    public void testValidApiTestService_TestRegexValidation() throws Exception {
        ValidApiTestService_TestRegexValidation request = new ValidApiTestService_TestRegexValidation("ABC123");
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestRegexValidation 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestMultipleValidation() throws Exception {
        ValidApiTestService_TestMultipleValidation request = new ValidApiTestService_TestMultipleValidation("test@example.com", "13800138000");
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestMultipleValidation 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestReturnArray() throws Exception {
        ValidApiTestService_TestReturnArray request = new ValidApiTestService_TestReturnArray(10);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestReturnArray 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestReturnList() throws Exception {
        ValidApiTestService_TestReturnList request = new ValidApiTestService_TestReturnList(10);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestReturnList 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestVoidReturn() throws Exception {
        ValidApiTestService_TestVoidReturn request = new ValidApiTestService_TestVoidReturn("test");
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestVoidReturn 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestWrapperReturn() throws Exception {
        ValidApiTestService_TestWrapperReturn request = new ValidApiTestService_TestWrapperReturn(42);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestWrapperReturn 响应: " + response);
        assertNotNull(response);
    }

    // ==================== ValidApiTestService - 分页和流式测试 ====================

    @Test
    public void testValidApiTestService_TestPageQuery() throws Exception {
        ValidApiTestService_TestPageQuery request = new ValidApiTestService_TestPageQuery(1, 10);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestPageQuery 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestPageWithNestedEntity() throws Exception {
        ValidApiTestService_TestPageWithNestedEntity request = new ValidApiTestService_TestPageWithNestedEntity("PENDING", 1, 10);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestPageWithNestedEntity 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestServerStream() throws Exception {
        ValidApiTestService_TestServerStream request = new ValidApiTestService_TestServerStream(100L);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestServerStream 响应: " + response);
        assertNotNull(response);
    }

    // ==================== ValidApiTestService - 循环引用和集合返回测试 ====================

    @Test
    public void testValidApiTestService_TestCircularEntity() throws Exception {
        ApiPublish_CircularEntityA entityA = new ApiPublish_CircularEntityA(1L, "A", null);
        ApiPublish_CircularEntityB entityB = new ApiPublish_CircularEntityB(1L, "B", entityA);
        ValidApiTestService_TestCircularEntity request = new ValidApiTestService_TestCircularEntity(entityB);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestCircularEntity 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestSetEntityReturn() throws Exception {
        java.util.List<Long> ids = Arrays.asList(1L, 2L, 3L);
        ValidApiTestService_TestSetEntityReturn request = new ValidApiTestService_TestSetEntityReturn(ids);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestSetEntityReturn 响应: " + response);
        assertNotNull(response);
    }

    @Test
    public void testValidApiTestService_TestStreamObserverComplex() throws Exception {
        ValidApiTestService_TestStreamObserverComplex request = new ValidApiTestService_TestStreamObserverComplex(100L);
        String response = httpClient.execute(request);
        System.out.println("ValidApiTestService_TestStreamObserverComplex 响应: " + response);
        assertNotNull(response);
    }
}
