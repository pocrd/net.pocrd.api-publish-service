package com.pocrd.api_publish_service.sdk.apidefine;

import java.util.List;

/**
 * 实体类型定义
 * 
 * @param type 实体类全限定名
 * @param description 实体类描述
 * @param isRecord 是否为 Record 类型
 * @param fields 字段列表
 */
public record EntityDefinition(
    String type,
    String description,
    boolean isRecord,
    List<FieldDefinition> fields
) {}
