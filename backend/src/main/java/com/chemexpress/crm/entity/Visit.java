package com.chemexpress.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("visits")
public class Visit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private Long customerId;
    private Long contactId;
    private Long repId;
    private String region;
    private LocalDate visitDate;
    private String purpose;
    private String status;
    private LocalDateTime checkinTime;
    private Integer checkinDistance;
    private String result;
    private String feedback;
    private String nextStep;
    private String rejectReason;
    private Long approverId;
    private LocalDateTime approvedAt;
    private String cancelReason;
    private String remark;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String contactName;
    @TableField(exist = false)
    private String repName;
    @TableField(exist = false)
    private String purposeName;
    @TableField(exist = false)
    private String statusName;
    @TableField(exist = false)
    private List<Product> products;
    @TableField(exist = false)
    private List<BusinessEvent> events;
}
