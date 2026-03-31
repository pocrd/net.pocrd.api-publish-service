package com.pocrd.api_publish_service.api;

import com.pocrd.api_publish_service.api.constant.GreeterServiceReturnCode;
import static com.pocrd.api_publish_service.api.constant.GreeterServiceReturnCode.GREET_NAME_TOO_LONG_CODE;
import static com.pocrd.api_publish_service.api.constant.GreeterServiceReturnCode.GREET_NAME_CONTAINS_INVALID_CHAR_CODE;
import com.pocrd.api_publish_service.sdk.annotation.ApiGroup;
import com.pocrd.api_publish_service.sdk.annotation.ApiParameter;
import com.pocrd.api_publish_service.sdk.annotation.Description;
import com.pocrd.api_publish_service.sdk.annotation.DesignedErrorCode;
import org.apache.dubbo.common.stream.StreamObserver;

/**
 * Greeter service interface definition - HTTP Export via Higress Gateway
 *
 * 此接口定义的方法将通过 Higress 网关暴露给公网 HTTP 访问
 * 使用 Dubbo Triple 协议（兼容 gRPC/HTTP2）
 */
@ApiGroup(
    name = "GreeterService",
    minCode = 1000,
    maxCode = 1999,
    codeDefine = GreeterServiceReturnCode.class
)
@Description("问候服务 - 通过 Higress 网关暴露的 HTTP 接口")
public interface GreeterServiceHttpExport {
    /**
     * Unary call - simple greeting
     * @param name the person's name
     * @return greeting message
     */
    @Description("简单问候接口")
    @DesignedErrorCode({GREET_NAME_TOO_LONG_CODE, GREET_NAME_CONTAINS_INVALID_CHAR_CODE})
    String greet(
        @ApiParameter(
            name = "name",
            required = true,
            desc = "问候对象名称，必须包含三个连续字母（如 abc, xyz）",
            verifyRegex = "(?i).*(abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz).*",
            verifyMsg = "名称必须包含三个连续字母（如 abc, xyz）",
            exampleValue = "AxyzB"
        ) String name
    );

    @Description("双重问候接口")
    @DesignedErrorCode({GREET_NAME_TOO_LONG_CODE, GREET_NAME_CONTAINS_INVALID_CHAR_CODE})
    String greet2(
        @ApiParameter(
            name = "name1",
            required = true,
            desc = "第一个问候对象",
            exampleValue = "Hello"
        ) String name1,
        @ApiParameter(
            name = "name2",
            required = true,
            desc = "第二个问候对象",
            exampleValue = "World"
        ) String name2
    );

    /**
     * Server streaming - greet multiple times
     * @param name the person's name
     * @param observer the stream observer to send multiple greetings
     */
    @Description("流式问候接口")
    @DesignedErrorCode({GREET_NAME_TOO_LONG_CODE, GREET_NAME_CONTAINS_INVALID_CHAR_CODE})
    void greetStream(
        @ApiParameter(
            name = "name",
            required = true,
            desc = "问候对象名称",
            exampleValue = "StreamUser"
        ) String name,
        @ApiParameter(
            name = "observer",
            required = true,
            desc = "流式响应观察者"
        ) StreamObserver<String> observer
    );

}
