package com.pocrd.api_publish_service.api.entity;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;
import java.util.List;

/**
 * 循环依赖测试实体A
 *
 * <p>与CircularEntityB形成循环依赖关系，用于测试SDK的循环依赖检测能力。</p>
 */
@Description("循环依赖测试实体A")
public record CircularEntityA(
    @Description("实体A的ID") long id,
    @Description("实体A的名称") String name,
    @Description("关联的实体B列表") List<CircularEntityB> entityBList
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
