package com.pocrd.api_publish_service.api;

import com.pocrd.api_publish_service.api.constant.OrderStatusEnum;
import com.pocrd.api_publish_service.api.constant.TestServiceReturnCode;
import static com.pocrd.api_publish_service.api.constant.TestServiceReturnCode.INVALID_PARAMETER_CODE;
import static com.pocrd.api_publish_service.api.constant.TestServiceReturnCode.RESOURCE_NOT_FOUND_CODE;
import static com.pocrd.api_publish_service.api.constant.TestServiceReturnCode.OPERATION_FAILED_CODE;
import static com.pocrd.api_publish_service.api.constant.TestServiceReturnCode.VALIDATION_ERROR_CODE;
import com.pocrd.api_publish_service.api.entity.AddressInfo;
import com.pocrd.api_publish_service.api.entity.ApiInfo;
import com.pocrd.api_publish_service.api.entity.CircularEntityA;
import com.pocrd.api_publish_service.api.entity.CircularEntityB;
import com.pocrd.api_publish_service.api.entity.EnumFieldEntity;
import com.pocrd.api_publish_service.api.entity.OrderInfo;
import com.pocrd.api_publish_service.api.entity.OrderItem;
import com.pocrd.api_publish_service.api.entity.ServiceInfo;

import com.pocrd.api_publish_service.api.entity.UserInfo;
import com.pocrd.api_publish_service.sdk.annotation.ApiGroup;
import com.pocrd.api_publish_service.sdk.annotation.ApiParameter;
import com.pocrd.api_publish_service.sdk.annotation.Description;
import com.pocrd.api_publish_service.sdk.annotation.DesignedErrorCode;
import com.pocrd.api_publish_service.sdk.annotation.EnumDef;
import org.apache.dubbo.common.stream.StreamObserver;

import java.util.List;
import java.util.Set;

/**
 * 有效API测试服务接口
 *
 * <p>此接口用于测试各种正确的接口/实体组合，验证SDK能够正确提取元数据。</p>
 * <p>包含以下测试场景：</p>
 * <ul>
 *   <li>基本类型参数（String, int, long, boolean）</li>
 *   <li>集合类型参数（List, Set, 数组）</li>
 *   <li>实体类型参数（单层、嵌套、循环依赖）</li>
 *   <li>多种返回类型（基本类型、实体、集合、分页）</li>
 *   <li>流式接口（StreamObserver）</li>
 *   <li>可选参数（required=false）</li>
 *   <li>参数验证（verifyRegex）</li>
 *   <li>错误码声明（@DesignedErrorCode）</li>
 *   <li>枚举参数（@EnumDef）</li>
 *   <li>包装类返回（Integer, Long, Boolean）</li>
 * </ul>
 */
@ApiGroup(
    name = "ValidApiTestService",
    minCode = 2000,
    maxCode = 2999,
    codeDefine = TestServiceReturnCode.class
)
@Description("有效API测试服务 - 用于验证正确的接口声明")
public interface ValidApiTestService {

    // ==================== 基本类型测试 ====================

    /**
     * 基本类型参数测试
     */
    @Description("基本类型参数测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    String testBasicTypes(
        @ApiParameter(name = "strParam", required = true, desc = "字符串参数", exampleValue = "hello") String strParam,
        @ApiParameter(name = "intParam", required = true, desc = "整数参数", exampleValue = "100") int intParam,
        @ApiParameter(name = "longParam", required = true, desc = "长整数参数", exampleValue = "1000000") long longParam,
        @ApiParameter(name = "boolParam", required = true, desc = "布尔参数", exampleValue = "true") boolean boolParam
    );

    /**
     * 可选参数测试
     */
    @Description("可选参数测试")
    String testOptionalParams(
        @ApiParameter(name = "requiredParam", required = true, desc = "必填参数") String requiredParam,
        @ApiParameter(name = "optionalParam", required = false, desc = "可选参数", defaultValue = "default") String optionalParam
    );

    // ==================== 集合类型测试 ====================

    /**
     * List参数测试
     */
    @Description("List参数测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    List<String> testListParam(
        @ApiParameter(name = "items", required = true, desc = "字符串列表") List<String> items
    );

    /**
     * 数组参数测试
     */
    @Description("数组参数测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    String[] testArrayParam(
        @ApiParameter(name = "ids", required = true, desc = "ID数组") long[] ids
    );

    /**
     * 返回List测试
     */
    @Description("返回List测试")
    List<UserInfo> testReturnList(
        @ApiParameter(name = "count", required = true, desc = "返回数量") int count
    );

    /**
     * 返回数组测试
     */
    @Description("返回数组测试")
    OrderItem[] testReturnArray(
        @ApiParameter(name = "size", required = true, desc = "数组大小") int size
    );

    // ==================== 实体类型测试 ====================

    /**
     * 单层实体参数测试
     */
    @Description("单层实体参数测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE, VALIDATION_ERROR_CODE})
    AddressInfo testSingleEntity(
        @ApiParameter(name = "address", required = true, desc = "地址信息") AddressInfo address
    );

    /**
     * 嵌套实体参数测试
     */
    @Description("嵌套实体参数测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE, RESOURCE_NOT_FOUND_CODE})
    UserInfo testNestedEntity(
        @ApiParameter(name = "user", required = true, desc = "用户信息") UserInfo user
    );

    /**
     * 多层嵌套实体测试
     */
    @Description("多层嵌套实体测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE, OPERATION_FAILED_CODE})
    OrderInfo testDeepNestedEntity(
        @ApiParameter(name = "order", required = true, desc = "订单信息") OrderInfo order
    );

    // ==================== 分页测试 ====================

    /**
     * 分页查询测试
     */
    @Description("分页查询测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    List<UserInfo> testPageQuery(
        @ApiParameter(name = "pageNum", required = true, desc = "页码", exampleValue = "1") int pageNum,
        @ApiParameter(name = "pageSize", required = true, desc = "每页大小", exampleValue = "10") int pageSize
    );

    /**
     * 分页返回嵌套实体测试
     */
    @Description("分页返回嵌套实体测试")
    List<OrderInfo> testPageWithNestedEntity(
        @ApiParameter(name = "status", required = true, desc = "订单状态") String status,
        @ApiParameter(name = "pageNum", required = true, desc = "页码") int pageNum,
        @ApiParameter(name = "pageSize", required = true, desc = "每页大小") int pageSize
    );

    // ==================== 参数验证测试 ====================

    /**
     * 正则验证测试
     */
    @Description("正则验证测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE, VALIDATION_ERROR_CODE})
    String testRegexValidation(
        @ApiParameter(
            name = "email",
            required = true,
            desc = "邮箱地址",
            verifyRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            verifyMsg = "邮箱格式不正确",
            exampleValue = "test@example.com"
        ) String email
    );

    /**
     * 多参数验证测试
     */
    @Description("多参数验证测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE, VALIDATION_ERROR_CODE})
    String testMultipleValidation(
        @ApiParameter(
            name = "phone",
            required = true,
            desc = "手机号",
            verifyRegex = "^1[3-9]\\d{9}$",
            verifyMsg = "手机号格式不正确",
            exampleValue = "13800138000"
        ) String phone,
        @ApiParameter(
            name = "idCard",
            required = true,
            desc = "身份证号",
            verifyRegex = "^\\d{17}[\\dXx]$",
            verifyMsg = "身份证号格式不正确",
            exampleValue = "110101199001011234"
        ) String idCard
    );

    // ==================== 流式接口测试 ====================

    /**
     * 服务端流式推送测试
     */
    @Description("服务端流式推送测试")
    @DesignedErrorCode({RESOURCE_NOT_FOUND_CODE})
    void testServerStream(
        @ApiParameter(name = "userId", required = true, desc = "用户ID") long userId,
        @ApiParameter(name = "observer", required = true, desc = "流式响应观察者") StreamObserver<UserInfo> observer
    );

    // ==================== 综合场景测试 ====================

    /**
     * 复杂参数组合测试
     */
    @Description("复杂参数组合测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE, RESOURCE_NOT_FOUND_CODE, OPERATION_FAILED_CODE})
    OrderInfo testComplexParams(
        @ApiParameter(name = "userId", required = true, desc = "用户ID") long userId,
        @ApiParameter(name = "items", required = true, desc = "订单项列表") List<OrderItem> items,
        @ApiParameter(name = "address", required = true, desc = "配送地址") AddressInfo address,
        @ApiParameter(name = "couponCode", required = false, desc = "优惠券码", exampleValue = "SAVE2024") String couponCode
    );

    /**
     * 无参数测试
     */
    @Description("无参数测试")
    String testNoParams();

    /**
     * 无返回值测试
     */
    @Description("无返回值测试")
    @DesignedErrorCode({OPERATION_FAILED_CODE})
    void testVoidReturn(
        @ApiParameter(name = "action", required = true, desc = "操作类型") String action
    );

    // ==================== 新增测试场景 ====================

    /**
     * 枚举参数测试
     */
    @Description("枚举参数测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    String testEnumParam(
        @ApiParameter(name = "status", required = true, desc = "订单状态") @EnumDef(OrderStatusEnum.class) String status
    );

    /**
     * Set参数测试
     */
    @Description("Set参数测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    Set<String> testSetParam(
        @ApiParameter(name = "tags", required = true, desc = "标签集合") Set<String> tags
    );

    /**
     * Set实体类型返回测试
     */
    @Description("Set实体类型返回测试")
    Set<UserInfo> testSetEntityReturn(
        @ApiParameter(name = "userIds", required = true, desc = "用户ID列表") List<Long> userIds
    );

    /**
     * 循环依赖实体测试
     */
    @Description("循环依赖实体测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    CircularEntityA testCircularEntity(
        @ApiParameter(name = "entityB", required = true, desc = "循环依赖实体B") CircularEntityB entityB
    );

    /**
     * StreamObserver返回复杂实体测试
     */
    @Description("StreamObserver返回复杂实体测试")
    @DesignedErrorCode({RESOURCE_NOT_FOUND_CODE})
    void testStreamObserverComplex(
        @ApiParameter(name = "orderId", required = true, desc = "订单ID") long orderId,
        @ApiParameter(name = "observer", required = true, desc = "流式响应观察者") StreamObserver<OrderInfo> observer
    );

    /**
     * 包装类返回测试
     */
    @Description("包装类返回测试")
    Integer testWrapperReturn(
        @ApiParameter(name = "value", required = true, desc = "输入值") int value
    );

    /**
     * 字段级枚举实体测试
     */
    @Description("字段级枚举实体测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    EnumFieldEntity testEnumFieldEntity(
        @ApiParameter(name = "entity", required = true, desc = "枚举字段实体") EnumFieldEntity entity
    );

    // ==================== 复杂入参测试 ====================

    /**
     * 复杂多层嵌套实体入参测试
     * 测试场景：实体中包含多层嵌套对象和集合
     */
    @Description("复杂多层嵌套实体入参测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE, VALIDATION_ERROR_CODE})
    ServiceInfo testComplexNestedEntityInput(
        @ApiParameter(name = "serviceInfo", required = true, desc = "服务信息，包含嵌套API信息数组和错误码列表") ServiceInfo serviceInfo
    );

    /**
     * 集合嵌套实体入参测试
     * 测试场景：List参数中包含嵌套实体
     */
    @Description("集合嵌套实体入参测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE, VALIDATION_ERROR_CODE})
    int testListNestedEntityInput(
        @ApiParameter(name = "orders", required = true, desc = "订单列表，每个订单包含买家和商品列表") List<OrderInfo> orders
    );

    /**
     * 多参数复杂组合测试
     * 测试场景：基本类型、实体、集合混合参数
     */
    @Description("多参数复杂组合测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE, RESOURCE_NOT_FOUND_CODE, VALIDATION_ERROR_CODE})
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
    @DesignedErrorCode({INVALID_PARAMETER_CODE, OPERATION_FAILED_CODE})
    int testBatchEntityArrayInput(
        @ApiParameter(name = "items", required = true, desc = "订单项数组") OrderItem[] items
    );

    // ==================== 复杂返回值测试 ====================

    /**
     * 深层嵌套实体返回测试
     * 测试场景：返回包含多层嵌套的复杂实体
     */
    @Description("深层嵌套实体返回测试")
    @DesignedErrorCode({RESOURCE_NOT_FOUND_CODE})
    ServiceInfo testDeepNestedEntityReturn(
        @ApiParameter(name = "serviceName", required = true, desc = "服务名称") String serviceName
    );

    /**
     * 分页嵌套实体返回测试
     * 测试场景：返回包含嵌套实体的分页数据
     */
    @Description("分页嵌套实体返回测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    List<OrderInfo> testPagedNestedEntityReturn(
        @ApiParameter(name = "pageNum", required = true, desc = "页码") int pageNum,
        @ApiParameter(name = "pageSize", required = true, desc = "每页大小") int pageSize
    );

    /**
     * 复杂集合组合返回测试
     * 测试场景：返回Set集合中包含嵌套实体
     */
    @Description("复杂集合组合返回测试")
    @DesignedErrorCode({RESOURCE_NOT_FOUND_CODE})
    Set<OrderInfo> testComplexSetEntityReturn(
        @ApiParameter(name = "userIds", required = true, desc = "用户ID列表") List<Long> userIds
    );

    /**
     * 分组订单项返回测试
     * 测试场景：返回按分类分组的订单项Map结构（使用实体包装）
     */
    @Description("分组订单项返回测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    OrderInfo testGroupedItemsReturn(
        @ApiParameter(name = "categoryId", required = true, desc = "分类ID") long categoryId
    );

    /**
     * 实体数组返回测试
     * 测试场景：返回复杂实体数组
     */
    @Description("实体数组返回测试")
    @DesignedErrorCode({RESOURCE_NOT_FOUND_CODE})
    UserInfo[] testEntityArrayReturn(
        @ApiParameter(name = "departmentId", required = true, desc = "部门ID") long departmentId
    );

    /**
     * 综合复杂场景测试
     * 测试场景：复杂入参 + 复杂返回值组合
     */
    @Description("综合复杂场景测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE, RESOURCE_NOT_FOUND_CODE, OPERATION_FAILED_CODE, VALIDATION_ERROR_CODE})
    ServiceInfo testComplexScenario(
        @ApiParameter(name = "apiInfos", required = true, desc = "API信息列表") List<ApiInfo> apiInfos,
        @ApiParameter(name = "config", required = true, desc = "地址配置信息") AddressInfo config,
        @ApiParameter(name = "version", required = false, desc = "版本号", defaultValue = "1.0.0") String version
    );
}
