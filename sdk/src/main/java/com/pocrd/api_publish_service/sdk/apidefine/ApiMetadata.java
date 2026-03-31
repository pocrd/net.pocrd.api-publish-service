package com.pocrd.api_publish_service.sdk.apidefine;

import java.util.List;

/**
 * API 元数据根对象
 * 
 * @param services 服务列表（每个服务包含自己的 entities 和 errorCodes）
 */
public record ApiMetadata(
    List<ServiceDefinition> services
) {}
