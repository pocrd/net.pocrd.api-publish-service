package com.pocrd.api_publish_service.sdk.apidefine;

/**
 * API 组信息
 * 
 * @param name 组名称
 * @param minCode 错误码最小值
 * @param maxCode 错误码最大值
 * @param codeDefine 错误码定义类
 */
public record ApiGroupInfo(
    String name,
    int minCode,
    int maxCode,
    String codeDefine
) {}
