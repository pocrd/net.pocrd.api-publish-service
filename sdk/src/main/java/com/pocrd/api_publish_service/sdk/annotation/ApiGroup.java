package com.pocrd.api_publish_service.sdk.annotation;

import java.lang.annotation.*;

import com.pocrd.api_publish_service.sdk.entity.AbstractReturnCode;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiGroup {
    /**
     * 错误码下限
     */
    int minCode();

    /**
     * 错误码上限
     */
    int maxCode();

    /**
     * ApiGroup名称
     */
    String name();

    /**
     * 错误码定义
     */
    Class<? extends AbstractReturnCode> codeDefine();
}