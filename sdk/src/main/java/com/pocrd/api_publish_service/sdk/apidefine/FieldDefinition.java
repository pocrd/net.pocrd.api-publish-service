package com.pocrd.api_publish_service.sdk.apidefine;

/**
 * 字段定义
 * 
 * @param name 字段名
 * @param type 字段类型（基础类型或元素类型）
 * @param containerType 容器类型（如 List、Set、Map，为空表示普通类型）
 * @param desc 字段描述
 * @param enumDef 枚举定义类
 */
public record FieldDefinition(
    String name,
    String type,
    String containerType,
    String desc,
    String enumDef
) {}
