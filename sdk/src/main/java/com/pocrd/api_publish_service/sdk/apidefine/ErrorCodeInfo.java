package com.pocrd.api_publish_service.sdk.apidefine;

import java.util.List;

/**
 * 错误码信息定义
 * 
 * <p>用于描述所有错误码的完整信息，包括：</p>
 * <ul>
 *   <li>所有错误码实例的详细信息</li>
 * </ul>
 * 
 * @param codes 错误码列表（已按 code 排序）
 */
public record ErrorCodeInfo(
    List<ErrorCodeItem> codes
) {
    
    /**
     * 错误码项
     * 
     * @param name 错误码名称（字段名）
     * @param code 错误码值
     * @param desc 错误码描述
     */
    public record ErrorCodeItem(
        String name,
        int code,
        String desc
    ) {}
}
