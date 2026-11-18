package com.chemexpress.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("business_events")
public class BusinessEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;
    private Long bizId;
    private String action;
    private String detail;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createdAt;
}
