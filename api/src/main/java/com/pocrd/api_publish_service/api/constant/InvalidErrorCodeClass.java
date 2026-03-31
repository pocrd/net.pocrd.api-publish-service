package com.pocrd.api_publish_service.api.constant;

/**
 * 无效的错误码定义类 - 不继承AbstractReturnCode
 *
 * <p>用于测试SDK对codeDefine类型的检查能力。</p>
 */
public class InvalidErrorCodeClass {

    public static final int INVALID_CODE = 9999;
    public static final String INVALID_MESSAGE = "无效错误码";

    private final String desc;
    private final int code;

    public InvalidErrorCodeClass(String desc, int code) {
        this.desc = desc;
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public int getCode() {
        return code;
    }
}
