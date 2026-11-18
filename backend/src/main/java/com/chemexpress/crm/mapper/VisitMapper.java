package com.chemexpress.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chemexpress.crm.entity.Visit;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VisitMapper extends BaseMapper<Visit> {

    @Delete("DELETE FROM visit_products WHERE visit_id = #{visitId}")
    void deleteVisitProducts(@Param("visitId") Long visitId);

    @Insert("INSERT INTO visit_products(visit_id, product_id) VALUES(#{visitId}, #{productId})")
    void insertVisitProduct(@Param("visitId") Long visitId, @Param("productId") Long productId);

    @Select("SELECT product_id FROM visit_products WHERE visit_id = #{visitId}")
    List<Long> selectProductIds(@Param("visitId") Long visitId);
}
