package com.pocrd.api_publish_service.api.entity;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;

/**
 * 地址信息实体
 */
@Description("地址信息实体，包含省市区详细地址")
public record AddressInfo(
    @Description("省份") String province,
    @Description("城市") String city,
    @Description("区县") String district,
    @Description("详细地址") String detail,
    @Description("邮政编码") String zipCode
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
