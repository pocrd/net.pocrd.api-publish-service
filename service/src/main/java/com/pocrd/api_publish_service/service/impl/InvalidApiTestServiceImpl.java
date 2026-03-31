package com.pocrd.api_publish_service.service.impl;

import com.pocrd.api_publish_service.api.InvalidApiTestService;
import com.pocrd.api_publish_service.api.UndescribedEntity;
import com.pocrd.api_publish_service.api.PartialDescribedEntity;
import com.pocrd.api_publish_service.api.PageResult;
import com.pocrd.api_publish_service.api.entity.OrderInfo;
import com.pocrd.api_publish_service.api.entity.UserInfo;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * InvalidApiTestService 实现类
 *
 * <p>此实现类用于测试各种不正确的接口声明场景。</p>
 * <p><strong>注意：</strong>此服务故意包含不规范的实现，用于验证SDK的错误检测能力。</p>
 */
@DubboService(version = "1.0.0", group = "public", registry = "nacos-public", protocol = "tri")
public class InvalidApiTestServiceImpl implements InvalidApiTestService {

    // ==================== 缺少 @Description 注解 ====================

    @Override
    public String missingMethodDescription(String name) {
        return "Method without description: " + name;
    }

    // ==================== 参数缺少 @ApiParameter 注解 ====================

    @Override
    public String missingApiParameter(String paramWithoutAnnotation) {
        return "Missing @ApiParameter: " + paramWithoutAnnotation;
    }

    @Override
    public String partialMissingAnnotation(String validParam, String invalidParam) {
        return String.format("Valid: %s, Invalid: %s", validParam, invalidParam);
    }

    // ==================== 使用不支持的JDK类型 ====================

    @Override
    public String unsupportedJdkTypeDateTime(LocalDateTime timestamp) {
        return "Timestamp: " + timestamp.toString();
    }

    @Override
    public String unsupportedJdkTypeMap(Map<String, Object> params) {
        return "Params count: " + params.size();
    }

    @Override
    public LocalDateTime returnUnsupportedType(long id) {
        return LocalDateTime.now();
    }

    @Override
    public PageResult<UserInfo> returnGenericContainer(int pageNum) {
        PageResult<UserInfo> result = new PageResult<>();
        // 模拟填充数据
        return result;
    }

    @Override
    public String paramGenericContainer(PageResult<OrderInfo> pageResult) {
        return "PageResult received";
    }

    // ==================== 错误码相关问题 ====================

    @Override
    public String errorCodeBelowRange(String data) {
        return "Error code 1001 is below range: " + data;
    }

    @Override
    public String errorCodeAboveRange(String data) {
        return "Error code 3001 is above range: " + data;
    }

    @Override
    public String undefinedErrorCode(String data) {
        return "Error code 2500 is undefined: " + data;
    }

    @Override
    public String partialInvalidErrorCodes(String data) {
        return "Partial invalid error codes: " + data;
    }

    // ==================== 实体类相关问题 ====================

    @Override
    public UndescribedEntity testUndescribedEntity(UndescribedEntity entity) {
        return entity;
    }

    @Override
    public PartialDescribedEntity testPartialDescribedEntity(PartialDescribedEntity entity) {
        return entity;
    }

    // ==================== 组合错误场景 ====================

    @Override
    public String multipleErrors(String param1, LocalDateTime param2) {
        return String.format("Multiple errors: %s, %s", param1, param2);
    }
}
