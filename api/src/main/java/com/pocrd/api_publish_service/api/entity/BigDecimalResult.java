package com.pocrd.api_publish_service.api.entity;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * BigDecimal返回结果实体
 */
@Description("BigDecimal返回结果实体，用于测试BigDecimal类型")
public record BigDecimalResult(
    @Description("金额数值") BigDecimal amount,
    @Description("货币单位") String currency
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
