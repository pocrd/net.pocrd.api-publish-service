package com.pocrd.api_publish_service.api;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;
import java.util.List;

/**
 * 泛型分页结果容器 - SDK不支持泛型容器类型
 */
@Description("泛型分页结果容器")
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @Description("当前页码") private int pageNum;
    @Description("每页大小") private int pageSize;
    @Description("总记录数") private long total;
    @Description("数据列表") private List<T> list;
    
    public PageResult() {
    }
    
    public int getPageNum() {
        return pageNum;
    }
    
    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public long getTotal() {
        return total;
    }
    
    public void setTotal(long total) {
        this.total = total;
    }
    
    public List<T> getList() {
        return list;
    }
    
    public void setList(List<T> list) {
        this.list = list;
    }
}
