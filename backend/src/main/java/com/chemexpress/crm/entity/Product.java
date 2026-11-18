package com.chemexpress.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("products")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String tradeName;
    private String genericName;
    private String spec;
    private String dosageForm;
    private String insuranceType;
    private BigDecimal price;
    private String status;
    private String indication;
}
