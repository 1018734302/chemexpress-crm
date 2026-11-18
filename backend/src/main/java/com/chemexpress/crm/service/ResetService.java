package com.chemexpress.crm.service;

import com.chemexpress.crm.common.BizException;
import com.chemexpress.crm.common.Constants;
import com.chemexpress.crm.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResetService {

    private final JdbcTemplate jdbcTemplate;
    private final SeedService seedService;
    private final AppMetaService appMetaService;

    @Transactional
    public Map<String, Object> reset(String confirm, CurrentUser user) {
        if (!user.isAdmin()) {
            throw BizException.forbidden("只有系统管理员可以重置演示数据");
        }
        if (confirm == null || !"RESET".equals(confirm.trim())) {
            throw new BizException("请输入 RESET 以确认重置");
        }
        log.warn("Demo data reset requested by {}", user.username());
        jdbcTemplate.execute("DELETE FROM business_events");
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM order_items");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM visit_products");
        jdbcTemplate.execute("DELETE FROM visits");
        jdbcTemplate.execute("DELETE FROM contacts");
        jdbcTemplate.execute("DELETE FROM customers");
        jdbcTemplate.execute("DELETE FROM products");
        jdbcTemplate.execute("DELETE FROM users");

        seedService.seedAll();
        long dataVersion = appMetaService.bumpDataVersion();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataVersion", dataVersion);
        result.put("message", "演示数据已重置，请重新登录");
        result.put("users", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class));
        result.put("customers", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers", Long.class));
        result.put("visits", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visits", Long.class));
        result.put("orders", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class));
        return result;
    }
}
