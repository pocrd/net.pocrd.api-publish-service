package com.pocrd.api_publish_service.api;

import com.pocrd.api_publish_service.api.constant.GreeterServiceReturnCode;
import static com.pocrd.api_publish_service.api.constant.GreeterServiceReturnCode.GREET_NAME_TOO_LONG_CODE;
import static com.pocrd.api_publish_service.api.constant.GreeterServiceReturnCode.GREET_NAME_CONTAINS_INVALID_CHAR_CODE;
import static com.pocrd.api_publish_service.api.constant.GreeterServiceReturnCode.BATCH_SIZE_EXCEEDED_CODE;
import com.pocrd.api_publish_service.api.entity.AddressInfo;
import com.pocrd.api_publish_service.api.entity.ApiInfo;
import com.pocrd.api_publish_service.api.entity.OrderInfo;
import com.pocrd.api_publish_service.api.entity.OrderItem;
import com.pocrd.api_publish_service.api.entity.ServiceInfo;
import com.pocrd.api_publish_service.api.entity.UserInfo;
import com.pocrd.api_publish_service.sdk.annotation.ApiGroup;
import com.pocrd.api_publish_service.sdk.annotation.ApiParameter;
import com.pocrd.api_publish_service.sdk.annotation.Description;
import com.pocrd.api_publish_service.sdk.annotation.DesignedErrorCode;
import org.apache.dubbo.common.stream.StreamObserver;

import java.util.List;
import java.util.Set;

/**
 * Greeter service interface definition - HTTP Export via Higress Gateway
 *
 * 此接口定义的方法将通过 Higress 网关暴露给公网 HTTP 访问
 * 使用 Dubbo Triple 协议（兼容 gRPC/HTTP2）
 */
@ApiGroup(
    name = "GreeterService",
    minCode = 1000,
    maxCode = 1999,
    codeDefine = GreeterServiceReturnCode.class
)
@Description("问候服务 - 通过 Higress 网关暴露的 HTTP 接口")
public interface GreeterServiceHttpExport {
    /**
     * Unary call - simple greeting
     * @param name the person's name
     * @return greeting message
     */
    @Description("简单问候接口")
    @DesignedErrorCode({GREET_NAME_TOO_LONG_CODE, GREET_NAME_CONTAINS_INVALID_CHAR_CODE})
    String greet(
        @ApiParameter(
            name = "name",
            required = true,
            desc = "问候对象名称，必须包含三个连续字母（如 abc, xyz）",
            verifyRegex = "(?i).*(abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz).*",
            verifyMsg = "名称必须包含三个连续字母（如 abc, xyz）",
            exampleValue = "AxyzB"
        ) String name
    );

    @Description("双重问候接口")
    @DesignedErrorCode({GREET_NAME_TOO_LONG_CODE, GREET_NAME_CONTAINS_INVALID_CHAR_CODE})
    String greet2(
        @ApiParameter(
            name = "name1",
            required = true,
            desc = "第一个问候对象",
            exampleValue = "Hello"
        ) String name1,
        @ApiParameter(
            name = "name2",
            required = true,
            desc = "第二个问候对象",
            exampleValue = "World"
        ) String name2
    );

    /**
     * Server streaming - greet multiple times
     * @param name the person's name
     * @param observer the stream observer to send multiple greetings
     */
    @Description("流式问候接口")
    @DesignedErrorCode({GREET_NAME_TOO_LONG_CODE, GREET_NAME_CONTAINS_INVALID_CHAR_CODE})
    void greetStream(
        @ApiParameter(
            name = "name",
            required = true,
            desc = "问候对象名称",
            exampleValue = "StreamUser"
        ) String name,
        @ApiParameter(
            name = "observer",
            required = true,
            desc = "流式响应观察者"
        ) StreamObserver<String> observer
    );

    // ==================== 复杂入参测试 ====================

    /**
     * 复杂多层嵌套实体入参测试
     * 测试场景：实体中包含多层嵌套对象和集合
     */
    @Description("复杂多层嵌套实体入参测试")
    @DesignedErrorCode({GREET_NAME_TOO_LONG_CODE, GREET_NAME_CONTAINS_INVALID_CHAR_CODE})
    ServiceInfo testComplexNestedEntityInput(
        @ApiParameter(name = "serviceInfo", required = true, desc = "服务信息，包含嵌套API信息数组和错误码列表") ServiceInfo serviceInfo
    );

    /**
     * 集合嵌套实体入参测试
     * 测试场景：List参数中包含嵌套实体
     */
    @Description("集合嵌套实体入参测试")
    @DesignedErrorCode({GREET_NAME_TOO_LONG_CODE, GREET_NAME_CONTAINS_INVALID_CHAR_CODE})
    int testListNestedEntityInput(
        @ApiParameter(name = "orders", required = true, desc = "订单列表，每个订单包含买家和商品列表") List<OrderInfo> orders
    );

    /**
     * 多参数复杂组合测试
     * 测试场景：基本类型、实体、集合混合参数
     */
    @Description("多参数复杂组合测试")
    @DesignedErrorCode({GREET_NAME_TOO_LONG_CODE, GREET_NAME_CONTAINS_INVALID_CHAR_CODE, BATCH_SIZE_EXCEEDED_CODE})
    String testComplexParamCombination(
        @ApiParameter(name = "userId", required = true, desc = "用户ID") long userId,
        @ApiParameter(name = "user", required = true, desc = "用户信息实体") UserInfo user,
        @ApiParameter(name = "addresses", required = true, desc = "地址列表") List<AddressInfo> addresses,
        @ApiParameter(name = "tags", required = true, desc = "标签集合") Set<String> tags,
        @ApiParameter(name = "statusList", required = true, desc = "状态数组") String[] statusList,
        @ApiParameter(name = "notify", required = false, desc = "是否通知", defaultValue = "true") boolean notify
    );

    /**
     * 批量操作入参测试
     * 测试场景：数组参数包含复杂实体
     */
    @Description("批量操作入参测试")
    @DesignedErrorCode({GREET_NAME_TOO_LONG_CODE, BATCH_SIZE_EXCEEDED_CODE})
    int testBatchEntityArrayInput(
        @ApiParameter(name = "items", required = true, desc = "订单项数组") OrderItem[] items
    );

    // ==================== 复杂返回值测试 ====================

    /**
     * 深层嵌套实体返回测试
     * 测试场景：返回包含多层嵌套的复杂实体
     */
    @Description("深层嵌套实体返回测试")
    @DesignedErrorCode({GREET_NAME_CONTAINS_INVALID_CHAR_CODE})
    ServiceInfo testDeepNestedEntityReturn(
        @ApiParameter(name = "serviceName", required = true, desc = "服务名称") String serviceName
    );

    /**
     * 分页嵌套实体返回测试
     * 测试场景：返回包含嵌套实体的分页数据
     */
    @Description("分页嵌套实体返回测试")
    @DesignedErrorCode({GREET_NAME_TOO_LONG_CODE})
    List<OrderInfo> testPagedNestedEntityReturn(
        @ApiParameter(name = "pageNum", required = true, desc = "页码") int pageNum,
        @ApiParameter(name = "pageSize", required = true, desc = "每页大小") int pageSize
    );

    /**
     * 复杂集合组合返回测试
     * 测试场景：返回Set集合中包含嵌套实体
     */
    @Description("复杂集合组合返回测试")
    @DesignedErrorCode({GREET_NAME_CONTAINS_INVALID_CHAR_CODE})
    Set<OrderInfo> testComplexSetEntityReturn(
        @ApiParameter(name = "userIds", required = true, desc = "用户ID列表") List<Long> userIds
    );

    /**
     * 分组订单项返回测试
     * 测试场景：返回按分类分组的订单项Map结构（使用实体包装）
     */
    @Description("分组订单项返回测试")
    @DesignedErrorCode({GREET_NAME_TOO_LONG_CODE})
    OrderInfo testGroupedItemsReturn(
        @ApiParameter(name = "categoryId", required = true, desc = "分类ID") long categoryId
    );

    /**
     * 实体数组返回测试
     * 测试场景：返回复杂实体数组
     */
    @Description("实体数组返回测试")
    @DesignedErrorCode({GREET_NAME_CONTAINS_INVALID_CHAR_CODE})
    UserInfo[] testEntityArrayReturn(
        @ApiParameter(name = "departmentId", required = true, desc = "部门ID") long departmentId
    );

    /**
     * 综合复杂场景测试
     * 测试场景：复杂入参 + 复杂返回值组合
     */
    @Description("综合复杂场景测试")
    @DesignedErrorCode({GREET_NAME_TOO_LONG_CODE, GREET_NAME_CONTAINS_INVALID_CHAR_CODE, BATCH_SIZE_EXCEEDED_CODE})
    ServiceInfo testComplexScenario(
        @ApiParameter(name = "apiInfos", required = true, desc = "API信息列表") List<ApiInfo> apiInfos,
        @ApiParameter(name = "config", required = true, desc = "地址配置信息") AddressInfo config,
        @ApiParameter(name = "version", required = false, desc = "版本号", defaultValue = "1.0.0") String version
    );

}
