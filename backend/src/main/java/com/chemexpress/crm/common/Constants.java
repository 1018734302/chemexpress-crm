package com.chemexpress.crm.common;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Constants {

    private Constants() {
    }

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_DSM = "DSM";
    public static final String ROLE_MR = "MR";
    public static final String ROLE_OPS = "OPS";

    public static final String REGION_EAST = "华东";
    public static final String REGION_NORTH = "华北";
    public static final String REGION_HQ = "总部";

    public static final String CUSTOMER_TYPE_HOSPITAL = "HOSPITAL";
    public static final String CUSTOMER_TYPE_CHAIN = "CHAIN";
    public static final String CUSTOMER_TYPE_PHARMACY = "PHARMACY";
    public static final String CUSTOMER_TYPE_DISTRIBUTOR = "DISTRIBUTOR";

    public static final String CUSTOMER_STATUS_POTENTIAL = "POTENTIAL";
    public static final String CUSTOMER_STATUS_ACTIVE = "ACTIVE";
    public static final String CUSTOMER_STATUS_PAUSED = "PAUSED";
    public static final String CUSTOMER_STATUS_CHURNED = "CHURNED";

    public static final String VISIT_PLANNED = "PLANNED";
    public static final String VISIT_CHECKED_IN = "CHECKED_IN";
    public static final String VISIT_SUBMITTED = "SUBMITTED";
    public static final String VISIT_APPROVED = "APPROVED";
    public static final String VISIT_REJECTED = "REJECTED";
    public static final String VISIT_CANCELLED = "CANCELLED";

    public static final String ORDER_DRAFT = "DRAFT";
    public static final String ORDER_PENDING = "PENDING";
    public static final String ORDER_APPROVED = "APPROVED";
    public static final String ORDER_SHIPPED = "SHIPPED";
    public static final String ORDER_COMPLETED = "COMPLETED";
    public static final String ORDER_REJECTED = "REJECTED";
    public static final String ORDER_CANCELLED = "CANCELLED";

    public static final String PRODUCT_ACTIVE = "ACTIVE";
    public static final String PRODUCT_INACTIVE = "INACTIVE";

    public static final Map<String, String> ROLES = map(
            ROLE_ADMIN, "系统管理员",
            ROLE_DSM, "地区经理",
            ROLE_MR, "医药代表",
            ROLE_OPS, "商务运营"
    );

    public static final Map<String, String> REGIONS = map(
            REGION_EAST, "华东",
            REGION_NORTH, "华北",
            REGION_HQ, "总部"
    );

    public static final Map<String, String> CUSTOMER_TYPES = map(
            CUSTOMER_TYPE_HOSPITAL, "医院",
            CUSTOMER_TYPE_CHAIN, "连锁药店",
            CUSTOMER_TYPE_PHARMACY, "单体药店",
            CUSTOMER_TYPE_DISTRIBUTOR, "商业公司"
    );

    public static final Map<String, String> CUSTOMER_LEVELS = map(
            "A", "A 级",
            "B", "B 级",
            "C", "C 级"
    );

    public static final Map<String, String> CUSTOMER_STATUSES = map(
            CUSTOMER_STATUS_POTENTIAL, "潜在客户",
            CUSTOMER_STATUS_ACTIVE, "合作中",
            CUSTOMER_STATUS_PAUSED, "暂停合作",
            CUSTOMER_STATUS_CHURNED, "已流失"
    );

    public static final Map<String, String> VISIT_PURPOSES = map(
            "DEVELOPMENT", "客户开发",
            "MAINTAIN", "日常维护",
            "GROWTH", "上量沟通",
            "ACADEMIC", "学术推广",
            "RELATION", "客情维护"
    );

    public static final Map<String, String> VISIT_STATUSES = map(
            VISIT_PLANNED, "计划中",
            VISIT_CHECKED_IN, "已签到",
            VISIT_SUBMITTED, "待审批",
            VISIT_APPROVED, "已通过",
            VISIT_REJECTED, "已驳回",
            VISIT_CANCELLED, "已取消"
    );

    public static final Map<String, String> VISIT_RESULTS = map(
            "SUCCESS", "成功",
            "PARTIAL", "部分达成",
            "FAILED", "未达成"
    );

    public static final Map<String, String> ORDER_STATUSES = map(
            ORDER_DRAFT, "草稿",
            ORDER_PENDING, "待审批",
            ORDER_APPROVED, "待发货",
            ORDER_SHIPPED, "已发货",
            ORDER_COMPLETED, "已完成",
            ORDER_REJECTED, "已驳回",
            ORDER_CANCELLED, "已取消"
    );

    public static final Map<String, String> PAYMENT_METHODS = map(
            "TRANSFER", "电汇",
            "ACCEPTANCE", "承兑",
            "CASH", "现金",
            "OTHER", "其他"
    );

    public static final Map<String, String> CONTACT_ROLES = map(
            "DECISION", "决策人",
            "PURCHASE", "采购",
            "PHARMACY", "药剂科",
            "STORE", "店长",
            "OTHER", "其他"
    );

    public static String label(Map<String, String> map, String code) {
        if (code == null) {
            return null;
        }
        return map.getOrDefault(code, code);
    }

    private static Map<String, String> map(String... values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(values[i], values[i + 1]);
        }
        return result;
    }
}
