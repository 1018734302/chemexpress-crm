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
import java.util.List;

@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private Long customerId;
    private Long repId;
    private String region;
    private String status;
    private BigDecimal totalAmount;
    private String receiverAddress;
    private LocalDate expectedDate;
    private String remark;
    private String rejectReason;
    private Long approverId;
    private LocalDateTime approvedAt;
    private String logisticsCompany;
    private String trackingNo;
    private LocalDateTime shippedAt;
    private String cancelReason;
    private LocalDateTime completedAt;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String repName;
    @TableField(exist = false)
    private String statusName;
    @TableField(exist = false)
    private BigDecimal paidAmount;
    @TableField(exist = false)
    private List<OrderItem> items;
    @TableField(exist = false)
    private List<Payment> payments;
    @TableField(exist = false)
    private List<BusinessEvent> events;
}
