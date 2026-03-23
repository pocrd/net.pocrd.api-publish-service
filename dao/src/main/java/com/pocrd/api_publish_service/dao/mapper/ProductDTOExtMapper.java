package com.pocrd.api_publish_service.dao.mapper;

import com.pocrd.api_publish_service.dao.autogen.entity.ProductDTO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * ProductDTO 扩展 Mapper
 * 提供除 CRUD 外的常用功能
 */
public interface ProductDTOExtMapper {

    /**
     * 批量插入商品
     */
    int batchInsert(@Param("list") List<ProductDTO> list);

    /**
     * 根据分类查询商品
     */
    List<ProductDTO> selectByCategory(@Param("category") String category);

    /**
     * 根据商品编码模糊查询
     */
    List<ProductDTO> selectByProductCodeLike(@Param("productCode") String productCode);

    /**
     * 根据价格范围查询商品
     */
    List<ProductDTO> selectByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                         @Param("maxPrice") BigDecimal maxPrice);

    /**
     * 扣减库存（乐观锁）
     * 返回影响行数，0 表示库存不足
     */
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * 增加库存
     */
    int addStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * 查询库存不足的商品
     */
    List<ProductDTO> selectLowStock(@Param("threshold") int threshold);

    /**
     * 逻辑删除商品
     */
    int logicDeleteById(@Param("id") Long id);
}
