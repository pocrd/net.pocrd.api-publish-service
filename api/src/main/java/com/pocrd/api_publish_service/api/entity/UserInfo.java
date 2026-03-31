package com.pocrd.api_publish_service.api.entity;

import com.pocrd.api_publish_service.sdk.annotation.Description;

import java.io.Serializable;
import java.util.List;

/**
 * 用户信息实体
 */
@Description("用户信息实体，包含基本资料和关联信息")
public record UserInfo(
    @Description("用户ID") long userId,
    @Description("用户名") String username,
    @Description("用户邮箱") String email,
    @Description("用户年龄") int age,
    @Description("是否激活") boolean active,
    @Description("用户地址") AddressInfo address,
    @Description("用户标签列表") List<String> tags,
    @Description("用户角色列表") String[] roles
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
