package com.pocrd.api_publish_service.api.entity;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部分字段缺少 @Description 注解的实体类
 */
@Description("部分字段缺少Description的实体")
public record PartialDescribedEntity(
    @Description("有描述的字段") String describedField,
    String undescribedField,  // 缺少 @Description
    @Description("另一个有描述的字段") int anotherDescribedField,
    LocalDateTime unsupportedField  // 缺少 @Description 且不支持的类型
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
