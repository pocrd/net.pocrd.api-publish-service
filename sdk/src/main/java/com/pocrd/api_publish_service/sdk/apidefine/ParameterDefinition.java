package com.pocrd.api_publish_service.sdk.apidefine;

/**
 * 参数定义
 * 
 * @param name 参数名
 * @param type 参数类型
 * @param isStream 是否为流式参数
 * @param required 是否必填
 * @param defaultValue 默认值
 * @param verifyRegex 验证正则表达式
 * @param verifyMsg 验证失败提示
 * @param ignoreForSecurity 是否因安全原因忽略
 * @param desc 参数描述
 * @param exampleValue 示例值
 * @param enumDef 枚举定义类
 * @param description 详细描述（来自 @Description 注解）
 */
public record ParameterDefinition(
    String name,
    String type,
    String containerType,
    boolean required,
    String verifyRegex,
    String verifyMsg,
    String desc,
    String exampleValue,
    String enumDef,
    String description
) {}
