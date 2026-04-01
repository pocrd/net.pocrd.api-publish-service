package com.pocrd.api_publish_service.sdk.apidefine;

import java.util.List;

/**
 * 方法定义
 * 
 * @param name 方法名
 * @param returnType 返回类型
 * @param description 方法描述
 * @param errorCodes 设计的错误码列表
 * @param parameters 参数列表
 */
public record MethodDefinition(
    String name,
    String returnType,
    String returnContainerType,
    String description,
    int[] errorCodes,
    List<ParameterDefinition> parameters
) {}
