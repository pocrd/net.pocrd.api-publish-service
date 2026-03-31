package com.pocrd.api_publish_service.api;

import com.pocrd.api_publish_service.api.constant.TestServiceReturnCode;
import static com.pocrd.api_publish_service.api.constant.TestServiceReturnCode.INVALID_PARAMETER_CODE;
import static com.pocrd.api_publish_service.api.constant.TestServiceReturnCode.RESOURCE_NOT_FOUND_CODE;
import static com.pocrd.api_publish_service.api.constant.TestServiceReturnCode.OPERATION_FAILED_CODE;
import static com.pocrd.api_publish_service.api.constant.TestServiceReturnCode.VALIDATION_ERROR_CODE;
import com.pocrd.api_publish_service.api.entity.AddressInfo;
import com.pocrd.api_publish_service.api.entity.OrderInfo;
import com.pocrd.api_publish_service.api.entity.OrderItem;

import com.pocrd.api_publish_service.api.entity.UserInfo;
import com.pocrd.api_publish_service.sdk.annotation.ApiGroup;
import com.pocrd.api_publish_service.sdk.annotation.ApiParameter;
import com.pocrd.api_publish_service.sdk.annotation.Description;
import com.pocrd.api_publish_service.sdk.annotation.DesignedErrorCode;
import org.apache.dubbo.common.stream.StreamObserver;

import java.util.List;

/**
 * 有效API测试服务接口
 *
 * <p>此接口用于测试各种正确的接口/实体组合，验证SDK能够正确提取元数据。</p>
 * <p>包含以下测试场景：</p>
 * <ul>
 *   <li>基本类型参数（String, int, long, boolean）</li>
 *   <li>集合类型参数（List, 数组）</li>
 *   <li>实体类型参数（单层、嵌套）</li>
 *   <li>多种返回类型（基本类型、实体、集合、分页）</li>
 *   <li>流式接口（StreamObserver）</li>
 *   <li>可选参数（required=false）</li>
 *   <li>参数验证（verifyRegex）</li>
 *   <li>错误码声明（@DesignedErrorCode）</li>
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
}
