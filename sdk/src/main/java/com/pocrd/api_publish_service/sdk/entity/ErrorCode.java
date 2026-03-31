package com.pocrd.api_publish_service.sdk.entity;

/**
 * 通用错误码定义
 *
 * <p>错误码区间定义：</p>
 * <ul>
 *   <li>[-200, 0]：保留区间，用于存放业务无关的通用错误码（定义在本类中）</li>
 *   <li>[1, Integer.MAX_VALUE]：业务错误码区间，由各微服务自行定义</li>
 * </ul>
 */
public final class ErrorCode extends AbstractReturnCode {

    private ErrorCode(String desc, int code) {
        super(desc, code);
    }

    // ========== 成功 [0] ==========
    public static final ErrorCode SUCCESS = new ErrorCode("成功", 0);

    // ========== 通用错误 [-9, -1] ==========
    public static final ErrorCode UNKNOWN_ERROR = new ErrorCode("未知错误", -1);

    // ========== 参数错误 [-29, -10] ==========
    public static final ErrorCode PARAM_INVALID = new ErrorCode("参数无效", -10);
    public static final ErrorCode PARAM_EMPTY = new ErrorCode("参数为空", -11);
    public static final ErrorCode PARAM_FORMAT_ERROR = new ErrorCode("参数格式错误", -12);
    public static final ErrorCode PARAM_OUT_OF_RANGE = new ErrorCode("参数超出范围", -13);

    // ========== 系统错误 [-49, -30] ==========
    public static final ErrorCode SYSTEM_ERROR = new ErrorCode("系统错误", -30);
    public static final ErrorCode SERVICE_UNAVAILABLE = new ErrorCode("服务不可用", -31);
    public static final ErrorCode SERVICE_TIMEOUT = new ErrorCode("服务超时", -32);
    public static final ErrorCode SERVICE_RATE_LIMIT = new ErrorCode("服务限流", -33);

    // ========== 网络错误 [-69, -50] ==========
    public static final ErrorCode NETWORK_ERROR = new ErrorCode("网络错误", -50);
    public static final ErrorCode CONNECT_TIMEOUT = new ErrorCode("连接超时", -51);

    // ========== 数据错误 [-89, -70] ==========
    public static final ErrorCode DATA_NOT_FOUND = new ErrorCode("数据不存在", -70);
    public static final ErrorCode DATA_ALREADY_EXISTS = new ErrorCode("数据已存在", -71);
    public static final ErrorCode DATA_STATUS_ERROR = new ErrorCode("数据状态错误", -72);

    // ========== 权限错误 [-109, -90] ==========
    public static final ErrorCode NO_PERMISSION = new ErrorCode("无权限", -90);
    public static final ErrorCode NOT_LOGIN = new ErrorCode("未登录", -91);
    public static final ErrorCode LOGIN_EXPIRED = new ErrorCode("登录过期", -92);
}
