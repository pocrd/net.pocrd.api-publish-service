package com.pocrd.api_publish_service.sdk.annotation;

import java.lang.annotation.*;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumDef {
    /**
     * 枚举类型定义
     */
    Class<? extends Enum> value();
}
