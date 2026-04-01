package com.pocrd.clientsdk;

public class ReturnCode {

    private final int code;
    private final String message;

    protected ReturnCode(int code, String message) {
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
        return "ReturnCode{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }

    // ========== 成功 [0] ==========
    public static final ReturnCode SUCCESS = new ReturnCode(0, "成功");

    // ========== 通用错误 [-9, -1] ==========
    public static final ReturnCode UNKNOWN_ERROR = new ReturnCode(-1, "未知错误");

    // ========== 参数错误 [-29, -10] ==========
    public static final ReturnCode PARAM_INVALID = new ReturnCode(-10, "参数无效");
    public static final ReturnCode PARAM_EMPTY = new ReturnCode(-11, "参数为空");
    public static final ReturnCode PARAM_FORMAT_ERROR = new ReturnCode(-12, "参数格式错误");
    public static final ReturnCode PARAM_OUT_OF_RANGE = new ReturnCode(-13, "参数超出范围");

    // ========== 系统错误 [-49, -30] ==========
    public static final ReturnCode SYSTEM_ERROR = new ReturnCode(-30, "系统错误");
    public static final ReturnCode SERVICE_UNAVAILABLE = new ReturnCode(-31, "服务不可用");
    public static final ReturnCode SERVICE_TIMEOUT = new ReturnCode(-32, "服务超时");
    public static final ReturnCode SERVICE_RATE_LIMIT = new ReturnCode(-33, "服务限流");

    // ========== 网络错误 [-69, -50] ==========
    public static final ReturnCode NETWORK_ERROR = new ReturnCode(-50, "网络错误");
    public static final ReturnCode CONNECT_TIMEOUT = new ReturnCode(-51, "连接超时");

    // ========== 数据错误 [-89, -70] ==========
    public static final ReturnCode DATA_NOT_FOUND = new ReturnCode(-70, "数据不存在");
    public static final ReturnCode DATA_ALREADY_EXISTS = new ReturnCode(-71, "数据已存在");
    public static final ReturnCode DATA_STATUS_ERROR = new ReturnCode(-72, "数据状态错误");

    // ========== 权限错误 [-109, -90] ==========
    public static final ReturnCode NO_PERMISSION = new ReturnCode(-90, "无权限");
    public static final ReturnCode NOT_LOGIN = new ReturnCode(-91, "未登录");
    public static final ReturnCode LOGIN_EXPIRED = new ReturnCode(-92, "登录过期");
}
