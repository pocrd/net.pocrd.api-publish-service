package com.pocrd.api_publish_service.api.entity.conflict;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;

/**
 * 命名冲突测试实体 - 与 api.entity.AddressInfo 同名但不同包
 *
 * <p>用于测试SDK的实体类名冲突检测能力。</p>
 */
@Description("命名冲突测试实体 - 与主包AddressInfo同名")
public record AddressInfo(
    @Description("冲突实体ID") long conflictId,
    @Description("冲突实体名称") String conflictName
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
