package com.pocrd.api_publish_service.api.entity;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;

/**
 * 订单项实体
 */
@Description("订单项实体，包含商品信息和价格")
public record OrderItem(
    @Description("商品ID") long productId,
    @Description("商品名称") String productName,
    @Description("商品数量") int quantity,
    @Description("商品单价") String unitPrice,
    @Description("商品总价") String totalPrice
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
