package com.pocrd.api_publish_service.api.entity;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;

/**
 * 非record类型的实体类 - 用于测试SDK的record类型强制检查
 *
 * <p>此类故意使用传统class定义而非record，用于验证SDK能够正确检测并报告错误。</p>
 */
@Description("非record类型实体 - 应该被SDK检测为错误")
public class NonRecordEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Description("字段1")
    private String field1;

    @Description("字段2")
    private int field2;

    public NonRecordEntity() {
    }

    public NonRecordEntity(String field1, int field2) {
        this.field1 = field1;
        this.field2 = field2;
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
