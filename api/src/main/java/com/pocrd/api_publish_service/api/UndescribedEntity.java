package com.pocrd.api_publish_service.api;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;

/**
 * 缺少 @Description 注解的实体类
 */
// @Description("此实体缺少Description注解")  // 故意缺少
public class UndescribedEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @Description("字段1") private String field1;
    @Description("字段2") private int field2;
    
    public UndescribedEntity() {
    }
    
    public String getField1() {
        return field1;
    }
    
    public void setField1(String field1) {
        this.field1 = field1;
    }
    
    public int getField2() {
        return field2;
    }
    
    public void setField2(int field2) {
        this.field2 = field2;
    }
}
