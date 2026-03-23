package com.pocrd.api_publish_service.sdk.annotation;

import java.lang.annotation.*;

import com.pocrd.api_publish_service.sdk.entity.EnumNull;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiParameter {
    /**
     * 是否为必要参数
     */
    boolean required();

    /**
     * 参数名称, 默认使用字段名
     */
    String name() default "";

    /**
     * 默认值
     */
    String defaultValue() default "";

    /**
     * 验证参数是否合法的
     */
    String verifyRegex() default "";

    /**
     * 参数验证失败的提示信息
     */
    String verifyMsg() default "";

    /**
     * 由于安全原因需要在日志系统中忽略的参数
     */
    boolean ignoreForSecurity() default false;

    /**
     * 枚举类型定义, 用于描述当前字符串的取值范围而又不引入接口二进制兼容问题
     */
    Class<? extends Enum> enumDef() default EnumNull.class;

    /**
     * 参数注释
     */
    String desc();

    /**
     * <h3>参数的样例值，用于提升文档的可读性</h3>
     * <p>如果参数类型为实体类型，则忽略此字段，转而在实体的各个字段上使用 @ExampleValue 声明该参数的各字段样例值。</p>
     * <br>
     * <p>值的格式应与所修饰字段的类型的JSON序列化后格式一致。即所提供值应能够被反序列化为参数的声明类型</p>
     * @return 参数的样例值
     */
    String exampleValue() default "";
}