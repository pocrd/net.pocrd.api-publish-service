package com.pocrd.api_publish_service.api.entity;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;
import java.util.List;

/**
 * 泛型分页结果容器 - SDK不支持泛型容器类型
 */
@Description("泛型分页结果容器")
public record PageResult<T>(
    @Description("当前页码") int pageNum,
    @Description("每页大小") int pageSize,
    @Description("总记录数") long total,
    @Description("数据列表") List<T> list
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
