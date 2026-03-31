package com.pocrd.api_publish_service.api.constant;

import com.pocrd.api_publish_service.sdk.entity.AbstractReturnCode;

/**
 * 测试服务错误码定义
 *
 * <p>业务错误码区间：[2000, 2999]</p>
 */
public class TestServiceReturnCode extends AbstractReturnCode {

    // 测试服务业务错误码 2000-2999
    public static final int INVALID_PARAMETER_CODE = 2001;
    public static final TestServiceReturnCode INVALID_PARAMETER = new TestServiceReturnCode("参数无效", INVALID_PARAMETER_CODE);
    
    public static final int RESOURCE_NOT_FOUND_CODE = 2002;
    public static final TestServiceReturnCode RESOURCE_NOT_FOUND = new TestServiceReturnCode("资源未找到", RESOURCE_NOT_FOUND_CODE);
    
    public static final int OPERATION_FAILED_CODE = 2003;
    public static final TestServiceReturnCode OPERATION_FAILED = new TestServiceReturnCode("操作失败", OPERATION_FAILED_CODE);
    
    public static final int VALIDATION_ERROR_CODE = 2004;
    public static final TestServiceReturnCode VALIDATION_ERROR = new TestServiceReturnCode("验证错误", VALIDATION_ERROR_CODE);

    private TestServiceReturnCode(String desc, int code) {
        super(desc, code);
    }
}
