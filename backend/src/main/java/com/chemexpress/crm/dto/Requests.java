package com.chemexpress.crm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Requests {

    private Requests() {
    }

    @Data
    public static class Login {
        @NotBlank(message = "请输入账号")
        private String username;
        @NotBlank(message = "请输入密码")
        private String password;
    }

    @Data
    public static class CustomerSave {
        @NotBlank(message = "请输入客户名称")
        @Size(min = 2, max = 60, message = "客户名称需为 2-60 个字符")
        private String name;
        @NotBlank(message = "请选择客户类型")
        private String type;
        @NotBlank(message = "请选择客户等级")
        private String level;
        @NotBlank(message = "请选择区域")
        private String region;
        private Long ownerId;
        @NotBlank(message = "请选择客户状态")
        private String status;
        @NotBlank(message = "请输入客户地址")
        @Size(max = 120, message = "地址不能超过 120 个字符")
        private String address;
        @DecimalMin(value = "0", message = "年潜力不能为负数")
        @DecimalMax(value = "999999999", message = "年潜力超出范围")
        private BigDecimal annualPotential;
        @Size(max = 200, message = "标签不能超过 200 个字符")
        private String tags;
        private String source;
        @Size(max = 500, message = "备注不能超过 500 个字符")
        private String remark;
        private String churnReason;
    }

    @Data
    public static class Transfer {
        @NotNull(message = "请选择新的负责人")
        private Long ownerId;
    }

    @Data
    public static class ContactSave {
        @NotBlank(message = "请输入联系人姓名")
        @Size(min = 1, max = 40, message = "姓名长度不正确")
        private String name;
        @Size(max = 40, message = "职务不能超过 40 个字符")
        private String title;
        @NotBlank(message = "请选择联系人角色")
        private String role;
        @Size(max = 20, message = "手机号长度不正确")
        private String phone;
        @Size(max = 80, message = "邮箱长度不正确")
        private String email;
        private Boolean primaryContact;
        private String status;
        @Size(max = 300, message = "备注不能超过 300 个字符")
        private String remark;
    }

    @Data
    public static class VisitPlan {
        @NotNull(message = "请选择客户")
        private Long customerId;
        private Long contactId;
        @NotNull(message = "请选择拜访日期")
        private LocalDate visitDate;
        @NotBlank(message = "请选择拜访目的")
        private String purpose;
        @Size(max = 300, message = "备注不能超过 300 个字符")
        private String remark;
    }

    @Data
    public static class VisitSubmit {
        @NotBlank(message = "请选择拜访结果")
        private String result;
        @NotBlank(message = "请填写客户反馈")
        @Size(min = 10, max = 1000, message = "客户反馈需为 10-1000 个字符")
        private String feedback;
        @NotBlank(message = "请填写下一步计划")
        @Size(min = 5, max = 500, message = "下一步计划需为 5-500 个字符")
        private String nextStep;
        @NotEmpty(message = "请至少选择一个关联产品")
        private List<Long> productIds;
    }

    @Data
    public static class Reason {
        @NotBlank(message = "请填写原因")
        @Size(min = 5, max = 300, message = "原因需为 5-300 个字符")
        private String reason;
    }

    @Data
    public static class OrderItemSave {
        @NotNull(message = "请选择产品")
        private Long productId;
        @NotNull(message = "请输入数量")
        @Min(value = 1, message = "数量至少为 1")
        @Max(value = 99999, message = "数量超出范围")
        private Integer quantity;
        @NotNull(message = "请输入单价")
        @DecimalMin(value = "0.01", message = "单价至少为 0.01")
        private BigDecimal unitPrice;
        @NotNull(message = "请输入折扣")
        @DecimalMin(value = "0", message = "折扣不能小于 0")
        @DecimalMax(value = "100", message = "折扣不能超过 100")
        private BigDecimal discount;
    }

    @Data
    public static class OrderSave {
        @NotNull(message = "请选择客户")
        private Long customerId;
        @NotEmpty(message = "请至少添加一个产品")
        @Size(max = 20, message = "最多添加 20 个产品")
        @Valid
        private List<OrderItemSave> items;
        @Size(max = 120, message = "收货地址不能超过 120 个字符")
        private String receiverAddress;
        private LocalDate expectedDate;
        @Size(max = 500, message = "备注不能超过 500 个字符")
        private String remark;
        private Boolean submit;
    }

    @Data
    public static class Ship {
        @NotBlank(message = "请填写物流公司")
        @Size(max = 60, message = "物流公司名称过长")
        private String logisticsCompany;
        @NotBlank(message = "请填写物流单号")
        @Size(min = 6, max = 30, message = "物流单号需为 6-30 位")
        private String trackingNo;
    }

    @Data
    public static class PaymentSave {
        @NotNull(message = "请选择回款日期")
        private LocalDate payDate;
        @NotNull(message = "请输入回款金额")
        @DecimalMin(value = "0.01", message = "回款金额至少为 0.01")
        private BigDecimal amount;
        @NotBlank(message = "请选择回款方式")
        private String method;
        @Size(max = 60, message = "流水号过长")
        private String referenceNo;
        @Size(max = 300, message = "备注不能超过 300 个字符")
        private String remark;
    }

    @Data
    public static class UserSave {
        @Size(min = 3, max = 30, message = "账号需为 3-30 个字符")
        @Pattern(regexp = "^[a-zA-Z0-9._-]*$", message = "账号只能包含字母、数字、点、下划线或短横线")
        private String username;
        @NotBlank(message = "请输入姓名")
        @Size(min = 2, max = 20, message = "姓名需为 2-20 个字符")
        private String name;
        @NotBlank(message = "请选择角色")
        private String role;
        @NotBlank(message = "请选择区域")
        private String region;
        @Size(max = 20, message = "手机号不能超过 20 个字符")
        private String phone;
        @Size(min = 6, max = 32, message = "密码需为 6-32 个字符")
        private String password;
        private Boolean enabled;
    }

    @Data
    public static class PasswordReset {
        @NotBlank(message = "请输入新密码")
        @Size(min = 6, max = 32, message = "密码需为 6-32 个字符")
        private String password;
    }

    @Data
    public static class Reset {
        @NotBlank(message = "请输入 RESET 以确认重置")
        private String confirm;
    }
}
