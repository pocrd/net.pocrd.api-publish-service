package com.pocrd.api_publish_service.api;

import com.pocrd.api_publish_service.api.constant.TestServiceReturnCode;
import static com.pocrd.api_publish_service.api.constant.TestServiceReturnCode.INVALID_PARAMETER_CODE;
import com.pocrd.api_publish_service.api.entity.BigDecimalResult;
import com.pocrd.api_publish_service.api.entity.OrderInfo;
import com.pocrd.api_publish_service.api.entity.PageResult;
import com.pocrd.api_publish_service.api.entity.PartialDescribedEntity;
import com.pocrd.api_publish_service.api.entity.UndescribedEntity;
import com.pocrd.api_publish_service.api.entity.UserInfo;
import com.pocrd.api_publish_service.api.entity.NonRecordEntity;
import com.pocrd.api_publish_service.sdk.annotation.ApiGroup;
import com.pocrd.api_publish_service.sdk.annotation.ApiParameter;
import com.pocrd.api_publish_service.sdk.annotation.Description;
import com.pocrd.api_publish_service.sdk.annotation.DesignedErrorCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 无效API测试服务接口
 *
 * <p>此接口用于测试各种<strong>不正确</strong>的接口声明，验证SDK能够正确检测并报告错误。</p>
 * <p>包含以下错误场景：</p>
 * <ul>
 *   <li>缺少 @Description 注解</li>
 *   <li>参数缺少 @ApiParameter 注解</li>
 *   <li>使用不支持的JDK类型（如 LocalDateTime, Map）</li>
 *   <li>错误码超出 @ApiGroup 声明的范围</li>
 *   <li>错误码未在 @ApiGroup 中定义</li>
 *   <li>实体类缺少 @Description 注解</li>
 *   <li>实体字段缺少 @Description 注解</li>
 *   <li>实体类不是record类型</li>
 *   <li>多层嵌套泛型</li>
 *   <li>泛型返回类型</li>
 * </ul>
 *
 * <p><strong>注意：</strong>此接口故意包含错误，用于测试SDK的错误检测能力。</p>
 */
@ApiGroup(
    name = "InvalidApiTestService",
    minCode = 2000,
    maxCode = 2999,
    codeDefine = TestServiceReturnCode.class
)
@Description("无效API测试服务 - 用于验证错误检测")
public interface InvalidApiTestService {

    // ==================== 缺少 @Description 注解 ====================

    /**
     * 缺少方法 @Description 注解
     */
    // @Description("此方法缺少Description注解")  // 故意注释掉
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    String missingMethodDescription(
        @ApiParameter(name = "name", required = true, desc = "名称") String name
    );

    // ==================== 参数缺少 @ApiParameter 注解 ====================

    /**
     * 参数缺少 @ApiParameter 注解
     */
    @Description("参数缺少ApiParameter注解")
    String missingApiParameter(
        String paramWithoutAnnotation  // 缺少 @ApiParameter
    );

    /**
     * 多个参数，部分缺少注解
     */
    @Description("多个参数部分缺少注解")
    String partialMissingAnnotation(
        @ApiParameter(name = "validParam", required = true, desc = "有效参数") String validParam,
        String invalidParam  // 缺少 @ApiParameter
    );

    // ==================== 使用不支持的JDK类型 ====================

    /**
     * 使用不支持的JDK类型 - LocalDateTime
     */
    @Description("使用不支持的JDK类型LocalDateTime")
    String unsupportedJdkTypeDateTime(
        @ApiParameter(name = "timestamp", required = true, desc = "时间戳") LocalDateTime timestamp
    );

    /**
     * 使用不支持的JDK类型 - Map
     */
    @Description("使用不支持的JDK类型Map")
    String unsupportedJdkTypeMap(
        @ApiParameter(name = "params", required = true, desc = "参数映射") Map<String, Object> params
    );

    /**
     * 返回不支持的JDK类型
     */
    @Description("返回不支持的JDK类型")
    LocalDateTime returnUnsupportedType(
        @ApiParameter(name = "id", required = true, desc = "ID") long id
    );

    /**
     * 使用泛型容器作为返回类型 - PageResult<T> 不被支持
     */
    @Description("使用泛型容器PageResult作为返回类型")
    PageResult<UserInfo> returnGenericContainer(
        @ApiParameter(name = "pageNum", required = true, desc = "页码") int pageNum
    );

    /**
     * 使用泛型容器作为参数类型 - PageResult<T> 不被支持
     */
    @Description("使用泛型容器PageResult作为参数类型")
    String paramGenericContainer(
        @ApiParameter(name = "pageResult", required = true, desc = "分页结果") PageResult<OrderInfo> pageResult
    );

    // ==================== 错误码相关问题 ====================

    /**
     * 错误码超出范围（低于minCode）
     */
    @Description("错误码低于声明范围")
    @DesignedErrorCode({1001})  // 1001 < 2000，超出范围
    String errorCodeBelowRange(
        @ApiParameter(name = "data", required = true, desc = "数据") String data
    );

    /**
     * 错误码超出范围（高于maxCode）
     */
    @Description("错误码高于声明范围")
    @DesignedErrorCode({3001})  // 3001 > 2999，超出范围
    String errorCodeAboveRange(
        @ApiParameter(name = "data", required = true, desc = "数据") String data
    );

    /**
     * 错误码未定义（在范围内但未在codeDefine中声明）
     */
    @Description("错误码未定义")
    @DesignedErrorCode({2500})  // 2500在范围内但未定义
    String undefinedErrorCode(
        @ApiParameter(name = "data", required = true, desc = "数据") String data
    );

    /**
     * 多个错误码，部分无效
     */
    @Description("多个错误码部分无效")
    @DesignedErrorCode({INVALID_PARAMETER_CODE, 3500})  // 3500超出范围
    String partialInvalidErrorCodes(
        @ApiParameter(name = "data", required = true, desc = "数据") String data
    );

    // ==================== 实体类相关问题 ====================

    /**
     * 使用缺少@Description的实体类
     */
    @Description("使用缺少Description的实体类")
    UndescribedEntity testUndescribedEntity(
        @ApiParameter(name = "entity", required = true, desc = "无描述实体") UndescribedEntity entity
    );

    /**
     * 使用字段缺少@Description的实体类
     */
    @Description("使用字段缺少Description的实体类")
    PartialDescribedEntity testPartialDescribedEntity(
        @ApiParameter(name = "entity", required = true, desc = "部分描述实体") PartialDescribedEntity entity
    );

    // ==================== 组合错误场景 ====================

    /**
     * 多种错误组合
     */
    // @Description("多种错误组合")  // 缺少Description
    @DesignedErrorCode({1500})  // 错误码超出范围
    String multipleErrors(
        String param1,  // 缺少@ApiParameter
        @ApiParameter(name = "param2", required = true, desc = "参数2") LocalDateTime param2  // 不支持的类型
    );

    // ==================== 新增错误场景 ====================

    /**
     * 非record类型实体测试
     */
    @Description("非record类型实体测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    NonRecordEntity testNonRecordEntity(
        @ApiParameter(name = "entity", required = true, desc = "非record实体") NonRecordEntity entity
    );

    /**
     * 多层嵌套泛型测试
     */
    @Description("多层嵌套泛型测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    String testNestedGeneric(
        @ApiParameter(name = "nestedList", required = true, desc = "嵌套列表") List<List<String>> nestedList
    );

    /**
     * byte[]数组测试
     */
    @Description("byte数组测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    String testByteArray(
        @ApiParameter(name = "data", required = true, desc = "字节数据") byte[] data
    );

    /**
     * 泛型返回类型测试
     */
    @Description("泛型返回类型测试")
    <T> T testGenericReturn(
        @ApiParameter(name = "value", required = true, desc = "输入值") Object value
    );

    /**
     * BigDecimal类型测试（不支持的JDK类型）
     */
    @Description("BigDecimal类型测试")
    @DesignedErrorCode({INVALID_PARAMETER_CODE})
    BigDecimalResult testBigDecimal(
        @ApiParameter(name = "amount", required = true, desc = "金额") int amount
    );
}


