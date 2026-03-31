package com.pocrd.api_publish_service.api.entity;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;

/**
 * API 信息实体
 */
@Description("API信息实体，包含API名称、路径、HTTP方法和描述")
public record ApiInfo(
    @Description("API名称") String apiName,
    @Description("API路径") String path,
    @Description("HTTP方法") String httpMethod,
    @Description("API描述") String description
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
