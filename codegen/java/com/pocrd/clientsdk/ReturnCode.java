package com.pocrd.clientsdk;

public class ErrorCode {

    private final int code;
    private final String message;

    private ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ErrorCode{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }

    // ========== 成功 [0] ==========
    public static final ErrorCode SUCCESS = new ErrorCode(0, "成功");

    // ========== 通用错误 [-9, -1] ==========
    public static final ErrorCode UNKNOWN_ERROR = new ErrorCode(-1, "未知错误");

    // ========== 参数错误 [-29, -10] ==========
    public static final ErrorCode PARAM_INVALID = new ErrorCode(-10, "参数无效");
    public static final ErrorCode PARAM_EMPTY = new ErrorCode(-11, "参数为空");
    public static final ErrorCode PARAM_FORMAT_ERROR = new ErrorCode(-12, "参数格式错误");
    public static final ErrorCode PARAM_OUT_OF_RANGE = new ErrorCode(-13, "参数超出范围");

    // ========== 系统错误 [-49, -30] ==========
    public static final ErrorCode SYSTEM_ERROR = new ErrorCode(-30, "系统错误");
    public static final ErrorCode SERVICE_UNAVAILABLE = new ErrorCode(-31, "服务不可用");
    public static final ErrorCode SERVICE_TIMEOUT = new ErrorCode(-32, "服务超时");
    public static final ErrorCode SERVICE_RATE_LIMIT = new ErrorCode(-33, "服务限流");

    // ========== 网络错误 [-69, -50] ==========
    public static final ErrorCode NETWORK_ERROR = new ErrorCode(-50, "网络错误");
    public static final ErrorCode CONNECT_TIMEOUT = new ErrorCode(-51, "连接超时");

    // ========== 数据错误 [-89, -70] ==========
    public static final ErrorCode DATA_NOT_FOUND = new ErrorCode(-70, "数据不存在");
    public static final ErrorCode DATA_ALREADY_EXISTS = new ErrorCode(-71, "数据已存在");
    public static final ErrorCode DATA_STATUS_ERROR = new ErrorCode(-72, "数据状态错误");

    // ========== 权限错误 [-109, -90] ==========
    public static final ErrorCode NO_PERMISSION = new ErrorCode(-90, "无权限");
    public static final ErrorCode NOT_LOGIN = new ErrorCode(-91, "未登录");
    public static final ErrorCode LOGIN_EXPIRED = new ErrorCode(-92, "登录过期");
}
