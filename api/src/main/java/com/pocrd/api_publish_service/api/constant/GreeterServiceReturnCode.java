package com.pocrd.api_publish_service.api.constant;

import com.pocrd.api_publish_service.sdk.entity.AbstractReturnCode;

/**
 * GreeterService 业务错误码定义
 *
 * <p>业务错误码区间：[1, Integer.MAX_VALUE]</p>
 * <p>通用错误码（保留区间 [-1000, 0]）定义在 {@link AbstractReturnCode}</p>
 */
public class GreeterServiceReturnCode extends AbstractReturnCode {

    // 问候服务业务错误码 1000-1999
    public static final int GREET_NAME_TOO_LONG_CODE = 1001;
    public static final GreeterServiceReturnCode GREET_NAME_TOO_LONG = new GreeterServiceReturnCode("问候名称过长", GREET_NAME_TOO_LONG_CODE);
    
    public static final int GREET_NAME_CONTAINS_INVALID_CHAR_CODE = 1002;
    public static final GreeterServiceReturnCode GREET_NAME_CONTAINS_INVALID_CHAR = new GreeterServiceReturnCode("问候名称包含非法字符", GREET_NAME_CONTAINS_INVALID_CHAR_CODE);
    
    public static final int BATCH_SIZE_EXCEEDED_CODE = 1003;
    public static final GreeterServiceReturnCode BATCH_SIZE_EXCEEDED = new GreeterServiceReturnCode("批量问候数量超过限制", BATCH_SIZE_EXCEEDED_CODE);

    private GreeterServiceReturnCode(String desc, int code) {
        super(desc, code);
    }
}
