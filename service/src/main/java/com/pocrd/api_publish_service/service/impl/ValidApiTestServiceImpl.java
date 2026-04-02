package com.pocrd.api_publish_service.service.impl;

import com.pocrd.api_publish_service.api.ValidApiTestService;
import com.pocrd.api_publish_service.api.entity.AddressInfo;
import com.pocrd.api_publish_service.api.entity.ApiInfo;
import com.pocrd.api_publish_service.api.entity.CircularEntityA;
import com.pocrd.api_publish_service.api.entity.CircularEntityB;
import com.pocrd.api_publish_service.api.entity.EnumFieldEntity;
import com.pocrd.api_publish_service.api.entity.OrderInfo;
import com.pocrd.api_publish_service.api.entity.OrderItem;
import com.pocrd.api_publish_service.api.entity.ServiceInfo;
import com.pocrd.api_publish_service.api.entity.UserInfo;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ValidApiTestService 实现类
 *
 * <p>用于测试各种正确的接口/实体组合</p>
 */
@DubboService(version = "1.0.0", group = "public", registry = "nacos-public", protocol = "tri")
public class ValidApiTestServiceImpl implements ValidApiTestService {

    // ==================== 基本类型测试 ====================

    @Override
    public String testBasicTypes(String strParam, int intParam, long longParam, boolean boolParam) {
        return String.format("Received: str=%s, int=%d, long=%d, bool=%b", 
                strParam, intParam, longParam, boolParam);
    }

    @Override
    public String testOptionalParams(String requiredParam, String optionalParam) {
        return String.format("Required: %s, Optional: %s", requiredParam, optionalParam);
    }

    // ==================== 集合类型测试 ====================

    @Override
    public List<String> testListParam(List<String> items) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            result.add(String.format("[%d] %s", i, items.get(i)));
        }
        return result;
    }

    @Override
    public String[] testArrayParam(long[] ids) {
        String[] result = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            result[i] = String.format("ID_%d", ids[i]);
        }
        return result;
    }

    @Override
    public List<UserInfo> testReturnList(int count) {
        List<UserInfo> users = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            users.add(createSampleUser(i));
        }
        return users;
    }

    @Override
    public OrderItem[] testReturnArray(int size) {
        OrderItem[] items = new OrderItem[size];
        for (int i = 0; i < size; i++) {
            items[i] = new OrderItem(
                    1000L + i,
                    "Product_" + i,
                    i + 1,
                    "10.00",
                    String.valueOf((i + 1) * 10.00)
            );
        }
        return items;
    }

    // ==================== 实体类型测试 ====================

    @Override
    public AddressInfo testSingleEntity(AddressInfo address) {
        // 返回处理后的地址（示例：添加前缀）
        return new AddressInfo(
                "Processed_" + address.province(),
                "Processed_" + address.city(),
                "Processed_" + address.district(),
                "Processed_" + address.detail(),
                address.zipCode()
        );
    }

    @Override
    public UserInfo testNestedEntity(UserInfo user) {
        // 返回处理后的用户（示例：添加标记）
        return new UserInfo(
                user.userId(),
                user.username() + "_processed",
                user.email(),
                user.age(),
                user.active(),
                user.address(),
                user.tags(),
                user.roles()
        );
    }

    @Override
    public OrderInfo testDeepNestedEntity(OrderInfo order) {
        // 返回处理后的订单（示例：修改状态）
        return new OrderInfo(
                order.orderId(),
                order.orderNo(),
                order.buyer(),
                order.items(),
                "PROCESSED",
                order.totalAmount()
        );
    }

    // ==================== 分页测试 ====================

    @Override
    public List<UserInfo> testPageQuery(int pageNum, int pageSize) {
        List<UserInfo> users = new ArrayList<>();
        int start = (pageNum - 1) * pageSize;
        for (int i = 1; i <= pageSize; i++) {
            users.add(createSampleUser(start + i));
        }
        return users;
    }

    @Override
    public List<OrderInfo> testPageWithNestedEntity(String status, int pageNum, int pageSize) {
        List<OrderInfo> orders = new ArrayList<>();
        int start = (pageNum - 1) * pageSize;
        for (int i = 1; i <= pageSize; i++) {
            orders.add(createSampleOrder(start + i, status));
        }
        return orders;
    }

    // ==================== 参数验证测试 ====================

    @Override
    public String testRegexValidation(String email) {
        // 正则验证已在注解中声明，这里只做业务处理
        return String.format("Valid email: %s", email);
    }

    @Override
    public String testMultipleValidation(String phone, String idCard) {
        // 正则验证已在注解中声明，这里只做业务处理
        return String.format("Valid phone: %s, idCard: %s", phone, idCard);
    }

    // ==================== 流式接口测试 ====================

    @Override
    public void testServerStream(long userId, StreamObserver<UserInfo> observer) {
        try {
            // 模拟流式推送多个用户信息
            for (int i = 1; i <= 5; i++) {
                UserInfo user = createSampleUser((int) userId + i);
                observer.onNext(user);
                Thread.sleep(200);
            }
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(e);
        }
    }

    // ==================== 综合场景测试 ====================

    @Override
    public OrderInfo testComplexParams(long userId, List<OrderItem> items, AddressInfo address, String couponCode) {
        // 计算订单总价
        double total = 0;
        for (OrderItem item : items) {
            total += Double.parseDouble(item.totalPrice());
        }
        
        // 应用优惠券折扣
        if (couponCode != null && !couponCode.isEmpty()) {
            total = total * 0.9; // 9折优惠
        }
        
        return new OrderInfo(
                System.currentTimeMillis(),
                "ORD_" + System.currentTimeMillis(),
                createSampleUser((int) userId),
                items,
                "CREATED",
                String.format("%.2f", total)
        );
    }

    @Override
    public String testNoParams() {
        return "No parameters received, service is working!";
    }

    @Override
    public void testVoidReturn(String action) {
        // 执行操作但不返回结果
        System.out.println("Executing action: " + action);
    }

    // ==================== 新增测试场景实现 ====================

    @Override
    public String testEnumParam(String status) {
        return "Order status: " + status;
    }

    @Override
    public Set<String> testSetParam(Set<String> tags) {
        Set<String> result = new HashSet<>();
        for (String tag : tags) {
            result.add("[" + tag + "]");
        }
        return result;
    }

    @Override
    public Set<UserInfo> testSetEntityReturn(List<Long> userIds) {
        Set<UserInfo> users = new HashSet<>();
        for (Long userId : userIds) {
            users.add(createSampleUser(userId.intValue()));
        }
        return users;
    }

    @Override
    public CircularEntityA testCircularEntity(CircularEntityB entityB) {
        // 返回一个包含entityB引用的CircularEntityA
        List<CircularEntityB> list = new ArrayList<>();
        list.add(entityB);
        return new CircularEntityA(1L, "EntityA", list);
    }

    @Override
    public void testStreamObserverComplex(long orderId, StreamObserver<OrderInfo> observer) {
        try {
            // 模拟流式推送订单信息
            for (int i = 1; i <= 3; i++) {
                OrderInfo order = createSampleOrder((int) orderId + i, "SHIPPED");
                observer.onNext(order);
                Thread.sleep(200);
            }
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(e);
        }
    }

    @Override
    public Integer testWrapperReturn(int value) {
        return value * 2;
    }

    @Override
    public EnumFieldEntity testEnumFieldEntity(EnumFieldEntity entity) {
        return new EnumFieldEntity(
                entity.orderId(),
                entity.status() + "_processed",
                entity.amount()
        );
    }

    // ==================== 复杂入参测试实现 ====================

    @Override
    public ServiceInfo testComplexNestedEntityInput(ServiceInfo serviceInfo) {
        // 返回处理后的服务信息，添加标记
        List<String> processedErrorCodes = new ArrayList<>();
        if (serviceInfo.errorCodes() != null) {
            for (String code : serviceInfo.errorCodes()) {
                processedErrorCodes.add("PROCESSED_" + code);
            }
        }
        
        return new ServiceInfo(
                serviceInfo.serviceName() + "_processed",
                serviceInfo.version(),
                serviceInfo.uptime(),
                serviceInfo.requestCount() + 1,
                serviceInfo.apiInfos(),
                processedErrorCodes
        );
    }

    @Override
    public int testListNestedEntityInput(List<OrderInfo> orders) {
        // 返回订单总数
        return orders != null ? orders.size() : 0;
    }

    @Override
    public String testComplexParamCombination(long userId, UserInfo user, List<AddressInfo> addresses,
                                               Set<String> tags, String[] statusList, boolean notify) {
        // 组合所有参数信息返回
        StringBuilder sb = new StringBuilder();
        sb.append("UserId: ").append(userId).append(", ");
        sb.append("User: ").append(user != null ? user.username() : "null").append(", ");
        sb.append("Addresses: ").append(addresses != null ? addresses.size() : 0).append(", ");
        sb.append("Tags: ").append(tags != null ? tags.size() : 0).append(", ");
        sb.append("StatusList: ").append(statusList != null ? statusList.length : 0).append(", ");
        sb.append("Notify: ").append(notify);
        return sb.toString();
    }

    @Override
    public int testBatchEntityArrayInput(OrderItem[] items) {
        // 返回订单项总数
        return items != null ? items.length : 0;
    }

    // ==================== 复杂返回值测试实现 ====================

    @Override
    public ServiceInfo testDeepNestedEntityReturn(String serviceName) {
        // 创建包含多层嵌套的ServiceInfo
        ApiInfo[] apiInfos = new ApiInfo[]{
            new ApiInfo("api1", "/api/v1/test1", "GET", "测试接口1"),
            new ApiInfo("api2", "/api/v1/test2", "POST", "测试接口2"),
            new ApiInfo("api3", "/api/v1/test3", "PUT", "测试接口3")
        };
        
        List<String> errorCodes = List.of("2000:成功", "2001:参数错误", "2002:系统错误");
        
        return new ServiceInfo(
                serviceName,
                "1.0.0",
                System.currentTimeMillis(),
                1000,
                apiInfos,
                errorCodes
        );
    }

    @Override
    public List<OrderInfo> testPagedNestedEntityReturn(int pageNum, int pageSize) {
        // 返回分页的嵌套实体数据
        List<OrderInfo> orders = new ArrayList<>();
        int start = (pageNum - 1) * pageSize;
        for (int i = 1; i <= pageSize; i++) {
            orders.add(createSampleOrder(start + i, "PENDING"));
        }
        return orders;
    }

    @Override
    public Set<OrderInfo> testComplexSetEntityReturn(List<Long> userIds) {
        // 返回Set集合中包含嵌套实体
        Set<OrderInfo> orders = new HashSet<>();
        int index = 1;
        for (Long userId : userIds) {
            OrderInfo order = createSampleOrder(index++, "COMPLETED");
            orders.add(order);
        }
        return orders;
    }

    @Override
    public OrderInfo testGroupedItemsReturn(long categoryId) {
        // 返回包含分组订单项的OrderInfo实体（使用实体包装代替嵌套List）
        List<OrderItem> allItems = new ArrayList<>();
        allItems.add(new OrderItem(categoryId * 100 + 1, "Category_" + categoryId + "_Item1", 2, "10.00", "20.00"));
        allItems.add(new OrderItem(categoryId * 100 + 2, "Category_" + categoryId + "_Item2", 1, "15.00", "15.00"));
        allItems.add(new OrderItem(categoryId * 100 + 3, "Category_" + categoryId + "_Item3", 3, "8.00", "24.00"));
        
        return new OrderInfo(
                categoryId,
                "ORDER_CAT_" + categoryId,
                createSampleUser((int) categoryId),
                allItems,
                "GROUPED",
                "59.00"
        );
    }

    @Override
    public UserInfo[] testEntityArrayReturn(long departmentId) {
        // 返回复杂实体数组
        int size = 3;
        UserInfo[] users = new UserInfo[size];
        for (int i = 0; i < size; i++) {
            users[i] = createSampleUser((int) (departmentId * 100 + i));
        }
        return users;
    }

    @Override
    public ServiceInfo testComplexScenario(List<ApiInfo> apiInfos, AddressInfo config, String version) {
        // 综合复杂场景：复杂入参 + 复杂返回值
        ApiInfo[] apiArray = apiInfos != null ? apiInfos.toArray(new ApiInfo[0]) : new ApiInfo[0];
        List<String> errorCodes = List.of(
            "Service: " + config.city(),
            "Version: " + version,
            "API Count: " + apiArray.length
        );
        
        return new ServiceInfo(
                "ComplexService",
                version,
                System.currentTimeMillis(),
                apiArray.length * 100,
                apiArray,
                errorCodes
        );
    }

    // ==================== 辅助方法 ====================

    private UserInfo createSampleUser(int id) {
        AddressInfo address = new AddressInfo(
                "Province_" + id,
                "City_" + id,
                "District_" + id,
                "Street_" + id + " No." + id,
                "1000" + id
        );
        
        return new UserInfo(
                id,
                "user_" + id,
                "user" + id + "@example.com",
                20 + (id % 50),
                id % 2 == 0,
                address,
                List.of("tag" + id, "vip"),
                new String[]{"user", "member"}
        );
    }

    private OrderInfo createSampleOrder(int id, String status) {
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem(id * 100L, "Product_A", 2, "50.00", "100.00"));
        items.add(new OrderItem(id * 100L + 1, "Product_B", 1, "80.00", "80.00"));
        
        return new OrderInfo(
                id,
                "ORDER_" + id,
                createSampleUser(id),
                items,
                status,
                "180.00"
        );
    }
}
