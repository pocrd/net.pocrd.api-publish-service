package com.pocrd.api_publish_service.sdk.apidefine;

import java.util.List;
import java.util.Map;

/**
 * 服务定义（包含完整的接口元数据）
 * 
 * @param interfaceName 接口全限定名
 * @param apiGroup API 组信息
 * @param description 服务描述
 * @param methods 方法列表
 * @param entities 实体类型映射（key: 实体类全名，value: 实体定义）
 * @param errorCodes 错误码信息列表（可选）
 * @param version Dubbo 服务版本
 * @param group Dubbo 服务分组
 */
public record ServiceDefinition(
    String interfaceName,
    ApiGroupInfo apiGroup,
    String description,
    List<MethodDefinition> methods,
    Map<String, EntityDefinition> entities,
    List<ErrorCodeInfo> errorCodes,
    String version,
    String group
) {}
