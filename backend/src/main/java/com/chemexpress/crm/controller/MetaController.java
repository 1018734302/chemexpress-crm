package com.chemexpress.crm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chemexpress.crm.common.ApiResponse;
import com.chemexpress.crm.common.Constants;
import com.chemexpress.crm.entity.User;
import com.chemexpress.crm.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/meta")
@RequiredArgsConstructor
public class MetaController {

    private final UserMapper userMapper;

    @GetMapping
    public ApiResponse<Map<String, Object>> options() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roles", Constants.ROLES);
        data.put("regions", Constants.REGIONS);
        data.put("customerTypes", Constants.CUSTOMER_TYPES);
        data.put("customerLevels", Constants.CUSTOMER_LEVELS);
        data.put("customerStatuses", Constants.CUSTOMER_STATUSES);
        data.put("visitPurposes", Constants.VISIT_PURPOSES);
        data.put("visitStatuses", Constants.VISIT_STATUSES);
        data.put("visitResults", Constants.VISIT_RESULTS);
        data.put("orderStatuses", Constants.ORDER_STATUSES);
        data.put("paymentMethods", Constants.PAYMENT_METHODS);
        data.put("contactRoles", Constants.CONTACT_ROLES);
        List<Map<String, Object>> owners = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, Constants.ROLE_MR)
                        .eq(User::getEnabled, true)
                        .orderByAsc(User::getId))
                .stream()
                .map(user -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", user.getId());
                    item.put("name", user.getName());
                    item.put("region", user.getRegion());
                    return item;
                })
                .toList();
        data.put("owners", owners);
        return ApiResponse.ok(data);
    }
}
