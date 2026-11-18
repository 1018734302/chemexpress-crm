package com.chemexpress.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("customers")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String type;
    private String level;
    private String region;
    private Long ownerId;
    private String status;
    private String address;
    private BigDecimal annualPotential;
    private String tags;
    private String source;
    private String remark;
    private LocalDate lastVisitDate;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String ownerName;

    @TableField(exist = false)
    private String typeName;

    @TableField(exist = false)
    private String statusName;
}
