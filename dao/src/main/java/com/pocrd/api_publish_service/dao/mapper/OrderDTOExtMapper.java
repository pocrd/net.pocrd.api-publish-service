package com.pocrd.api_publish_service.dao.mapper;

import com.pocrd.api_publish_service.dao.autogen.entity.OrderDTO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * OrderDTO 扩展 Mapper
 * 提供除 CRUD 外的常用功能
 */
public interface OrderDTOExtMapper {

    /**
     * 批量插入订单
     */
    int batchInsert(@Param("list") List<OrderDTO> list);

    /**
     * 根据用户ID查询订单列表
     */
    List<OrderDTO> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据订单编号模糊查询
     */
    List<OrderDTO> selectByOrderNoLike(@Param("orderNo") String orderNo);

    /**
     * 查询金额范围内的订单
     */
    List<OrderDTO> selectByAmountRange(@Param("minAmount") BigDecimal minAmount, 
                                        @Param("maxAmount") BigDecimal maxAmount);

    /**
     * 批量更新订单状态
     */
    int batchUpdateStatus(@Param("orderNos") List<String> orderNos, @Param("status") Byte status);

    /**
     * 统计用户订单总金额
     */
    BigDecimal sumAmountByUserId(@Param("userId") Long userId);

    /**
     * 查询待支付且超过指定时间的订单（用于超时取消）
     */
    List<OrderDTO> selectUnpaidOrders(@Param("minutes") int minutes);
}
