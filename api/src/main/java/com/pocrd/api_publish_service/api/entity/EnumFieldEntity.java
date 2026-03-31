package com.pocrd.api_publish_service.api.entity;

import com.pocrd.api_publish_service.api.constant.OrderStatusEnum;
import com.pocrd.api_publish_service.sdk.annotation.Description;
import com.pocrd.api_publish_service.sdk.annotation.EnumDef;

import java.io.Serializable;

/**
 * 字段级枚举测试实体
 *
 * <p>用于测试实体字段上的@EnumDef注解功能</p>
 */
@Description("字段级枚举测试实体")
public record EnumFieldEntity(
    @Description("订单ID") long orderId,
    @Description("订单状态") @EnumDef(OrderStatusEnum.class) String status,
    @Description("订单金额") String amount
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
