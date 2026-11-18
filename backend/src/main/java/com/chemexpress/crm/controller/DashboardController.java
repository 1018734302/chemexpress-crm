package com.chemexpress.crm.controller;

import com.chemexpress.crm.common.ApiResponse;
import com.chemexpress.crm.security.SecurityUtils;
import com.chemexpress.crm.service.DashboardService;
import com.chemexpress.crm.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ApiResponse<DashboardVO> dashboard() {
        return ApiResponse.ok(dashboardService.build(SecurityUtils.current()));
    }
}
