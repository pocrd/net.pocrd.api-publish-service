package com.pocrd.api_publish_service.api.entity;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;
import java.util.List;

/**
 * 订单信息实体
 */
@Description("订单信息实体，包含订单详情和商品列表")
public record OrderInfo(
    @Description("订单ID") long orderId,
    @Description("订单编号") String orderNo,
    @Description("买家信息") UserInfo buyer,
    @Description("订单商品列表") List<OrderItem> items,
    @Description("订单状态") String status,
    @Description("订单总金额") String totalAmount
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
