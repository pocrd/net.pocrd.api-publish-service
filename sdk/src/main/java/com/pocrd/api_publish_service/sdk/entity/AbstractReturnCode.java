package com.pocrd.api_publish_service.sdk.entity;

import java.io.Serializable;

/**
 * 错误码抽象基类
 *
 * <p>错误码区间定义：</p>
 * <ul>
 *   <li>[-1000, 0]：保留区间，用于存放业务无关的通用错误码（定义在 {@link ErrorCode} 中）</li>
 *   <li>[1, Integer.MAX_VALUE]：业务错误码区间，由各微服务自行定义</li>
 * </ul>
 *
 * @see ErrorCode
 */
public abstract class AbstractReturnCode implements Serializable {

    private       String name;
    private final String desc;
    private final int    code;

    private       String             service;
    private final AbstractReturnCode display;

    /**
     * 初始化一个对外暴露的ReturnCode(用于客户端异常处理)
     */
    public AbstractReturnCode(String desc, int code) {
        this.desc = desc;
        this.code = code;
        this.display = this;
    }

    /**
     * 初始化一个不对外暴露的ReturnCode(仅用于服务端数据分析)
     */
    public AbstractReturnCode(String desc, int code, AbstractReturnCode displayAs) {
        this.desc = desc;
        this.code = code;
        this.display = displayAs;
    }

    public String getDesc() {
        return desc;
    }
    public int getCode() {
        return code;
    }
    public AbstractReturnCode getDisplay() {
        return display;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getService() {
        return service;
    }
    public void setService(String service) {
        this.service = service;
    }

    @Override
    public String toString() {
        return String.format("%d[%s]", code, name);
    }
}