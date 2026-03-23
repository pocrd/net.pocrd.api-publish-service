package com.pocrd.api_publish_service.dao.mapper;

import com.pocrd.api_publish_service.dao.autogen.entity.UserDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * UserDTO 扩展 Mapper
 * 提供除 CRUD 外的常用功能
 */
public interface UserDTOExtMapper {

    /**
     * 批量插入用户
     */
    int batchInsert(@Param("list") List<UserDTO> list);

    /**
     * 根据用户名模糊查询
     */
    List<UserDTO> selectByUsernameLike(@Param("username") String username);

    /**
     * 根据状态查询用户列表
     */
    List<UserDTO> selectByStatus(@Param("status") Byte status);

    /**
     * 批量更新状态
     */
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Byte status);

    /**
     * 逻辑删除（更新 is_deleted 字段）
     */
    int logicDeleteById(@Param("id") Long id);

    /**
     * 批量逻辑删除
     */
    int batchLogicDelete(@Param("ids") List<Long> ids);
}
