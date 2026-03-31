package com.pocrd.api_publish_service.api.constant;

/**
 * 订单状态枚举
 *
 * <p>用于测试@EnumDef注解功能</p>
 */
public enum OrderStatusEnum {
    PENDING("待支付"),
    PAID("已支付"),
    SHIPPED("已发货"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String description;

    OrderStatusEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
