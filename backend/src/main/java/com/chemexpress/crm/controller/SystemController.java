package com.chemexpress.crm.controller;

import com.chemexpress.crm.common.ApiResponse;
import com.chemexpress.crm.dto.Requests;
import com.chemexpress.crm.security.SecurityUtils;
import com.chemexpress.crm.service.ResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final ResetService resetService;

    @PostMapping("/reset")
    public ApiResponse<Map<String, Object>> reset(@RequestBody @Valid Requests.Reset request) {
        return ApiResponse.ok(resetService.reset(request.getConfirm(), SecurityUtils.current()));
    }
}
