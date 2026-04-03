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
@DubboService(version = "1.0.0", group = "public", registry = "nacos-public", protocol = "tri", path = "dapi")
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
        // 深度验证：检查接收到的复杂嵌套实体字段值
        validateServiceInfo(serviceInfo);

        // 返回处理后的服务信息，添加标记
        List<String> processedErrorCodes = new ArrayList<>();
        if (serviceInfo.errorCodes() != null) {
            for (String code : serviceInfo.errorCodes()) {
                processedErrorCodes.add("PROCESSED_" + code);
            }
        }

        // 构造返回值，包含特殊字符和边界值以测试序列化一致性
        ApiInfo[] apiInfos = serviceInfo.apiInfos() != null ? serviceInfo.apiInfos() : new ApiInfo[0];

        return new ServiceInfo(
                serviceInfo.serviceName() + "_processed",
                serviceInfo.version() + "_verified",
                serviceInfo.uptime() + 1,
                serviceInfo.requestCount() + 100,
                apiInfos,
                processedErrorCodes.isEmpty() ? List.of("VERIFIED") : processedErrorCodes
        );
    }

    /**
     * 深度验证 ServiceInfo 实体的字段值
     */
    private void validateServiceInfo(ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            throw new IllegalArgumentException("ServiceInfo cannot be null");
        }
        // 验证基本字段
        if (serviceInfo.serviceName() == null || serviceInfo.serviceName().isEmpty()) {
            throw new IllegalArgumentException("serviceName cannot be null or empty");
        }
        // 验证嵌套数组
        if (serviceInfo.apiInfos() != null) {
            for (ApiInfo api : serviceInfo.apiInfos()) {
                if (api == null || api.apiName() == null) {
                    throw new IllegalArgumentException("ApiInfo or methodName cannot be null");
                }
            }
        }
    }

    @Override
    public int testListNestedEntityInput(List<OrderInfo> orders) {
        // 深度验证：检查每个订单的嵌套实体字段
        if (orders != null) {
            for (OrderInfo order : orders) {
                validateOrderInfo(order);
            }
        }
        return orders != null ? orders.size() : 0;
    }

    /**
     * 深度验证 OrderInfo 实体的字段值
     */
    private void validateOrderInfo(OrderInfo order) {
        if (order == null) {
            throw new IllegalArgumentException("OrderInfo cannot be null");
        }
        // 验证基本字段
        if (order.orderNo() == null || order.orderNo().isEmpty()) {
            throw new IllegalArgumentException("orderNo cannot be null or empty");
        }
        // 验证嵌套的买家信息
        if (order.buyer() != null) {
            validateUserInfo(order.buyer());
        }
        // 验证订单项列表
        if (order.items() != null) {
            for (OrderItem item : order.items()) {
                if (item == null || item.productName() == null) {
                    throw new IllegalArgumentException("OrderItem or productName cannot be null");
                }
            }
        }
    }

    /**
     * 深度验证 UserInfo 实体的字段值
     */
    private void validateUserInfo(UserInfo user) {
        if (user == null) {
            throw new IllegalArgumentException("UserInfo cannot be null");
        }
        if (user.username() == null || user.username().isEmpty()) {
            throw new IllegalArgumentException("username cannot be null or empty");
        }
        // 验证嵌套地址
        if (user.address() != null) {
            if (user.address().province() == null || user.address().city() == null) {
                throw new IllegalArgumentException("Address province and city cannot be null");
            }
        }
    }

    @Override
    public String testComplexParamCombination(long userId, UserInfo user, List<AddressInfo> addresses,
                                               Set<String> tags, String[] statusList, boolean notify) {
        // 深度验证：检查复杂参数组合中的每个实体
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (user != null) {
            validateUserInfo(user);
        }
        if (addresses != null) {
            for (AddressInfo addr : addresses) {
                if (addr == null || addr.province() == null || addr.city() == null) {
                    throw new IllegalArgumentException("Address province and city cannot be null");
                }
            }
        }

        // 组合所有参数信息返回，包含验证标记
        StringBuilder sb = new StringBuilder();
        sb.append("UserId: ").append(userId).append(", ");
        sb.append("User: ").append(user != null ? user.username() : "null").append(", ");
        sb.append("Addresses: ").append(addresses != null ? addresses.size() : 0).append(", ");
        sb.append("Tags: ").append(tags != null ? tags.size() : 0).append(", ");
        sb.append("StatusList: ").append(statusList != null ? statusList.length : 0).append(", ");
        sb.append("Notify: ").append(notify).append(", ");
        sb.append("VERIFIED: true");
        return sb.toString();
    }

    @Override
    public int testBatchEntityArrayInput(OrderItem[] items) {
        // 深度验证：检查数组中的每个订单项
        if (items != null) {
            for (OrderItem item : items) {
                if (item == null) {
                    throw new IllegalArgumentException("OrderItem in array cannot be null");
                }
                if (item.productName() == null || item.productName().isEmpty()) {
                    throw new IllegalArgumentException("productName cannot be null or empty");
                }
                if (item.quantity() <= 0) {
                    throw new IllegalArgumentException("quantity must be positive");
                }
            }
        }
        return items != null ? items.length : 0;
    }

    // ==================== 复杂返回值测试实现 ====================

    @Override
    public ServiceInfo testDeepNestedEntityReturn(String serviceName) {
        // 创建包含多层嵌套的ServiceInfo，使用特殊字符和边界值测试序列化一致性
        ApiInfo[] apiInfos = new ApiInfo[]{
            new ApiInfo("api1", "/api/v1/test1", "GET", "测试接口1 with special chars: \\ \" \n \t"),
            new ApiInfo("api2", "/api/v1/test2", "POST", "Test with unicode: 中文字符 🎉"),
            new ApiInfo("api3", "/api/v1/test3", "PUT", "Edge case: 0, -1, max int")
        };

        // 使用包含特殊字符的错误码列表，测试序列化/反序列化一致性
        List<String> errorCodes = List.of(
            "2000:成功",
            "2001:参数错误 with \"quotes\"",
            "2002:系统错误 with unicode 🚨",
            "2003:边界测试: 0, -1, 2147483647"
        );

        return new ServiceInfo(
                serviceName + "_verified",
                "1.0.0_test",
                9223372036854775807L, // Long.MAX_VALUE 边界值测试
                2147483647, // Integer.MAX_VALUE 边界值测试
                apiInfos,
                errorCodes
        );
    }

    @Override
    public List<OrderInfo> testPagedNestedEntityReturn(int pageNum, int pageSize) {
        // 返回分页的嵌套实体数据，包含验证标记
        List<OrderInfo> orders = new ArrayList<>();
        int start = (pageNum - 1) * pageSize;
        for (int i = 1; i <= pageSize; i++) {
            OrderInfo order = createSampleOrder(start + i, "PENDING_VERIFIED");
            orders.add(order);
        }
        return orders;
    }

    @Override
    public Set<OrderInfo> testComplexSetEntityReturn(List<Long> userIds) {
        // 返回Set集合中包含嵌套实体，每个订单包含验证标记
        Set<OrderInfo> orders = new HashSet<>();
        int index = 1;
        for (Long userId : userIds) {
            // 使用userId作为订单ID的一部分，便于客户端验证
            OrderInfo order = createSampleOrder((int)(userId * 1000 + index), "COMPLETED_VERIFIED_" + userId);
            orders.add(order);
            index++;
        }
        return orders;
    }

    @Override
    public OrderInfo testGroupedItemsReturn(long categoryId) {
        // 返回包含分组订单项的OrderInfo实体，包含验证标记
        List<OrderItem> allItems = new ArrayList<>();
        // 使用特殊字符测试序列化一致性
        allItems.add(new OrderItem(categoryId * 100 + 1, "Category_" + categoryId + "_Item1_verified", 2, "10.00", "20.00"));
        allItems.add(new OrderItem(categoryId * 100 + 2, "Item with \"quotes\" and \\ backslash", 1, "15.00", "15.00"));
        allItems.add(new OrderItem(categoryId * 100 + 3, "Unicode测试: 中文字符 🎉", 3, "8.00", "24.00"));

        return new OrderInfo(
                categoryId,
                "ORDER_CAT_" + categoryId + "_VERIFIED",
                createSampleUser((int) categoryId),
                allItems,
                "GROUPED_VERIFIED",
                "59.00"
        );
    }

    @Override
    public UserInfo[] testEntityArrayReturn(long departmentId) {
        // 返回复杂实体数组，包含验证标记和边界值
        int size = 3;
        UserInfo[] users = new UserInfo[size];
        for (int i = 0; i < size; i++) {
            int userId = (int) (departmentId * 100 + i);
            AddressInfo address = new AddressInfo(
                    "Province_VERIFIED_" + userId,
                    "City_" + i + "_测试",
                    "District with \"quotes\"",
                    "Street_\\_" + userId,
                    "1000" + i
            );
            users[i] = new UserInfo(
                    userId,
                    "user_verified_" + userId,
                    "user" + userId + "@verified.example.com",
                    20 + (userId % 50),
                    userId % 2 == 0,
                    address,
                    List.of("tag" + userId, "verified", "unicode_测试"),
                    new String[]{"user", "verified", "test_角色"}
            );
        }
        return users;
    }

    @Override
    public ServiceInfo testComplexScenario(List<ApiInfo> apiInfos, AddressInfo config, String version) {
        // 深度验证入参
        if (config == null) {
            throw new IllegalArgumentException("config AddressInfo cannot be null");
        }
        if (config.province() == null || config.city() == null) {
            throw new IllegalArgumentException("Address province and city cannot be null");
        }

        // 综合复杂场景：复杂入参 + 复杂返回值
        ApiInfo[] apiArray = apiInfos != null ? apiInfos.toArray(new ApiInfo[0]) : new ApiInfo[0];

        // 构造包含验证标记的返回值
        List<String> errorCodes = List.of(
            "Service: " + config.city() + "_VERIFIED",
            "Version: " + version + "_VERIFIED",
            "API Count: " + apiArray.length,
            "Province: " + config.province(),
            "District: " + (config.district() != null ? config.district() : "null")
        );

        return new ServiceInfo(
                "ComplexService_VERIFIED",
                version + "_verified",
                9223372036854775807L, // Long.MAX_VALUE 边界值
                apiArray.length * 100 + 999,
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
