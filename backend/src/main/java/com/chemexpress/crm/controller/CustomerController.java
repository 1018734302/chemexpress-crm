package com.chemexpress.crm.controller;

import com.chemexpress.crm.common.ApiResponse;
import com.chemexpress.crm.common.PageResult;
import com.chemexpress.crm.dto.Requests;
import com.chemexpress.crm.entity.Customer;
import com.chemexpress.crm.security.SecurityUtils;
import com.chemexpress.crm.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ApiResponse<PageResult<Customer>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(customerService.page(keyword, type, level, region, status, ownerId,
                page, size, SecurityUtils.current()));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long ownerId) {
        return ApiResponse.ok(customerService.stats(keyword, type, level, region, status, ownerId,
                SecurityUtils.current()));
    }

    @GetMapping("/owners")
    public ApiResponse<List<Map<String, Object>>> owners(@RequestParam(required = false) String region) {
        return ApiResponse.ok(customerService.ownerOptions(region));
    }

    @GetMapping("/{id}")
    public ApiResponse<Customer> detail(@PathVariable Long id) {
        return ApiResponse.ok(customerService.detail(id, SecurityUtils.current()));
    }

    @PostMapping
    public ApiResponse<Customer> create(@RequestBody @Valid Requests.CustomerSave request) {
        return ApiResponse.ok(customerService.create(request, SecurityUtils.current()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Customer> update(@PathVariable Long id,
                                        @RequestBody @Valid Requests.CustomerSave request) {
        return ApiResponse.ok(customerService.update(id, request, SecurityUtils.current()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id, SecurityUtils.current());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/transfer")
    public ApiResponse<Customer> transfer(@PathVariable Long id,
                                          @RequestBody @Valid Requests.Transfer request) {
        return ApiResponse.ok(customerService.transfer(id, request.getOwnerId(), SecurityUtils.current()));
    }
}
