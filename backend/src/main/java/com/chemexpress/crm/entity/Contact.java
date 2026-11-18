package com.chemexpress.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contacts")
public class Contact {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String name;
    private String title;
    private String role;
    private String phone;
    private String email;
    private Boolean primaryContact;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String customerName;

    @TableField(exist = false)
    private String roleName;
}
