package com.chemexpress.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("payments")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private LocalDate payDate;
    private BigDecimal amount;
    private String method;
    private String referenceNo;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String createdByName;
    @TableField(exist = false)
    private String methodName;
}
