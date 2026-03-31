package com.pocrd.api_publish_service.api;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部分字段缺少 @Description 注解的实体类
 */
@Description("部分字段缺少Description的实体")
public class PartialDescribedEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @Description("有描述的字段") private String describedField;
    private String undescribedField;  // 缺少 @Description
    @Description("另一个有描述的字段") private int anotherDescribedField;
    private LocalDateTime unsupportedField;  // 缺少 @Description 且不支持的类型
    
    public PartialDescribedEntity() {
    }
    
    public String getDescribedField() {
        return describedField;
    }
    
    public void setDescribedField(String describedField) {
        this.describedField = describedField;
    }
    
    public String getUndescribedField() {
        return undescribedField;
    }
    
    public void setUndescribedField(String undescribedField) {
        this.undescribedField = undescribedField;
    }
    
    public int getAnotherDescribedField() {
        return anotherDescribedField;
    }
    
    public void setAnotherDescribedField(int anotherDescribedField) {
        this.anotherDescribedField = anotherDescribedField;
    }
    
    public LocalDateTime getUnsupportedField() {
        return unsupportedField;
    }
    
    public void setUnsupportedField(LocalDateTime unsupportedField) {
        this.unsupportedField = unsupportedField;
    }
}
