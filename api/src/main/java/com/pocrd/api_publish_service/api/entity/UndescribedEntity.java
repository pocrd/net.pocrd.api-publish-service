package com.pocrd.api_publish_service.api.entity;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;

/**
 * 缺少 @Description 注解的实体类
 */
// @Description("此实体缺少Description注解")  // 故意缺少
public record UndescribedEntity(
    @Description("字段1") String field1,
    @Description("字段2") int field2
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
