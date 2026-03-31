package com.pocrd.api_publish_service.api;

import com.pocrd.api_publish_service.api.constant.GreeterServiceReturnCode;
import static com.pocrd.api_publish_service.api.constant.GreeterServiceReturnCode.GREET_NAME_TOO_LONG_CODE;
import static com.pocrd.api_publish_service.api.constant.GreeterServiceReturnCode.BATCH_SIZE_EXCEEDED_CODE;
import com.pocrd.api_publish_service.api.entity.ServiceInfo;
import com.pocrd.api_publish_service.sdk.annotation.ApiGroup;
import com.pocrd.api_publish_service.sdk.annotation.ApiParameter;
import com.pocrd.api_publish_service.sdk.annotation.Description;
import com.pocrd.api_publish_service.sdk.annotation.DesignedErrorCode;

/**
 * Greeter service interface definition - Internal Dubbo RPC Only
 *
 * 此接口定义的方法仅在内网供其他 Dubbo 服务调用
 * 不会通过 Higress 网关暴露给公网
 * 使用 Dubbo 原生 RPC 协议进行高效内部通信
 */
@ApiGroup(
    name = "GreeterServiceInternal",
    minCode = 1000,
    maxCode = 1999,
    codeDefine = GreeterServiceReturnCode.class
)
@Description("内部问候服务 - 仅内网 Dubbo RPC 调用")
public interface GreeterServiceInternal {
    /**
     * 内部服务间问候 - 简单问候
     * @param name 问候对象名称
     * @return 问候消息
     */
    @Description("内部简单问候接口")
    @DesignedErrorCode({GREET_NAME_TOO_LONG_CODE})
    String greetInternal(
        @ApiParameter(
            name = "name",
            required = true,
            desc = "问候对象名称",
            exampleValue = "InternalUser"
        ) String name
    );

    /**
     * 内部服务间批量问候
     * @param names 问候对象名称列表
     * @return 问候消息列表
     */
    @Description("内部批量问候接口")
    @DesignedErrorCode({BATCH_SIZE_EXCEEDED_CODE})
    java.util.List<String> greetBatch(
        @ApiParameter(
            name = "names",
            required = true,
            desc = "问候对象名称列表",
            exampleValue = "[\"User1\", \"User2\", \"User3\"]"
        ) java.util.List<String> names
    );

    /**
     * 内部健康检查 - 用于服务监控
     * @return 服务健康状态
     */
    @Description("内部健康检查接口")
    boolean healthCheck();

    /**
     * 内部服务信息获取
     * @return 服务详细信息
     */
    @Description("内部服务信息获取接口")
    ServiceInfo getServiceInfo();
}
