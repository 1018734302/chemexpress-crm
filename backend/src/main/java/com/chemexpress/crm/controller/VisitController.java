package com.chemexpress.crm.controller;

import com.chemexpress.crm.common.ApiResponse;
import com.chemexpress.crm.common.PageResult;
import com.chemexpress.crm.dto.Requests;
import com.chemexpress.crm.entity.Visit;
import com.chemexpress.crm.security.SecurityUtils;
import com.chemexpress.crm.service.VisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @GetMapping
    public ApiResponse<PageResult<Visit>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Long repId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(visitService.page(keyword, status, purpose, dateFrom, dateTo, repId, customerId,
                page, size, SecurityUtils.current()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Visit> detail(@PathVariable Long id) {
        return ApiResponse.ok(visitService.detail(id, SecurityUtils.current()));
    }

    @PostMapping
    public ApiResponse<Visit> create(@RequestBody @Valid Requests.VisitPlan request) {
        return ApiResponse.ok(visitService.createPlan(request, SecurityUtils.current()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Visit> update(@PathVariable Long id,
                                     @RequestBody @Valid Requests.VisitPlan request) {
        return ApiResponse.ok(visitService.updatePlan(id, request, SecurityUtils.current()));
    }

    @PostMapping("/{id}/checkin")
    public ApiResponse<Visit> checkin(@PathVariable Long id) {
        return ApiResponse.ok(visitService.checkin(id, SecurityUtils.current()));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<Visit> submit(@PathVariable Long id,
                                     @RequestBody @Valid Requests.VisitSubmit request) {
        return ApiResponse.ok(visitService.submit(id, request, SecurityUtils.current()));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Visit> approve(@PathVariable Long id) {
        return ApiResponse.ok(visitService.approve(id, SecurityUtils.current()));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Visit> reject(@PathVariable Long id,
                                     @RequestBody @Valid Requests.Reason request) {
        return ApiResponse.ok(visitService.reject(id, request.getReason(), SecurityUtils.current()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Visit> cancel(@PathVariable Long id,
                                     @RequestBody @Valid Requests.Reason request) {
        return ApiResponse.ok(visitService.cancel(id, request.getReason(), SecurityUtils.current()));
    }
}
