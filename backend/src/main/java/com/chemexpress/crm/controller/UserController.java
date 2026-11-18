package com.chemexpress.crm.controller;

import com.chemexpress.crm.common.ApiResponse;
import com.chemexpress.crm.common.PageResult;
import com.chemexpress.crm.dto.Requests;
import com.chemexpress.crm.security.SecurityUtils;
import com.chemexpress.crm.service.UserService;
import com.chemexpress.crm.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<PageResult<UserVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(userService.page(keyword, role, region, enabled, page, size,
                SecurityUtils.current()));
    }

    @PostMapping
    public ApiResponse<UserVO> create(@RequestBody @Valid Requests.UserSave request) {
        return ApiResponse.ok(userService.create(request, SecurityUtils.current()));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserVO> update(@PathVariable Long id,
                                      @RequestBody @Valid Requests.UserSave request) {
        return ApiResponse.ok(userService.update(id, request, SecurityUtils.current()));
    }

    @PostMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @RequestBody @Valid Requests.PasswordReset request) {
        userService.resetPassword(id, request, SecurityUtils.current());
        return ApiResponse.ok();
    }
}
