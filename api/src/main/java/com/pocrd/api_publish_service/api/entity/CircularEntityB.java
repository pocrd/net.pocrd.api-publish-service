package com.pocrd.api_publish_service.api.entity;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;

/**
 * 循环依赖测试实体B
 *
 * <p>与CircularEntityA形成循环依赖关系，用于测试SDK的循环依赖检测能力。</p>
 */
@Description("循环依赖测试实体B")
public record CircularEntityB(
    @Description("实体B的ID") long id,
    @Description("实体B的名称") String name,
    @Description("关联的实体A") CircularEntityA entityA
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
