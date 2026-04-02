package com.pocrd.api_publish_service.service.impl;

import com.pocrd.api_publish_service.api.GreeterServiceHttpExport;
import com.pocrd.api_publish_service.api.entity.AddressInfo;
import com.pocrd.api_publish_service.api.entity.ApiInfo;
import com.pocrd.api_publish_service.api.entity.OrderInfo;
import com.pocrd.api_publish_service.api.entity.OrderItem;
import com.pocrd.api_publish_service.api.entity.ServiceInfo;
import com.pocrd.api_publish_service.api.entity.UserInfo;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        String remoteAddress = RpcContext.getCurrentServiceContext().getRemoteAddressString();
        String localAddress = RpcContext.getCurrentServiceContext().getLocalAddressString();
        
        // Return greeting message with additional context info
        return String.format("Hello %s, from %s (to %s)", 
                name, remoteAddress, localAddress);
    }

    @Override
    public String greet2(String name1, String name2) {
        // Get current RPC context information
        String remoteAddress = RpcContext.getCurrentServiceContext().getRemoteAddressString();
        String localAddress = RpcContext.getCurrentServiceContext().getLocalAddressString();
        
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
                        name, i, RpcContext.getCurrentServiceContext().getLocalAddressString());
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
