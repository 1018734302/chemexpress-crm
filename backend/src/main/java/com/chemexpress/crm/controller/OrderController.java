package com.chemexpress.crm.controller;

import com.chemexpress.crm.common.ApiResponse;
import com.chemexpress.crm.common.PageResult;
import com.chemexpress.crm.dto.Requests;
import com.chemexpress.crm.entity.Order;
import com.chemexpress.crm.security.SecurityUtils;
import com.chemexpress.crm.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<PageResult<Order>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(orderService.page(keyword, status, dateFrom, dateTo, page, size,
                SecurityUtils.current()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Order> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.detail(id, SecurityUtils.current()));
    }

    @PostMapping
    public ApiResponse<Order> create(@RequestBody @Valid Requests.OrderSave request) {
        return ApiResponse.ok(orderService.create(request, SecurityUtils.current()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Order> update(@PathVariable Long id,
                                     @RequestBody @Valid Requests.OrderSave request) {
        return ApiResponse.ok(orderService.update(id, request, SecurityUtils.current()));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<Order> submit(@PathVariable Long id) {
        return ApiResponse.ok(orderService.submit(id, SecurityUtils.current()));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Order> approve(@PathVariable Long id) {
        return ApiResponse.ok(orderService.approve(id, SecurityUtils.current()));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Order> reject(@PathVariable Long id,
                                     @RequestBody @Valid Requests.Reason request) {
        return ApiResponse.ok(orderService.reject(id, request.getReason(), SecurityUtils.current()));
    }

    @PostMapping("/{id}/ship")
    public ApiResponse<Order> ship(@PathVariable Long id,
                                   @RequestBody @Valid Requests.Ship request) {
        return ApiResponse.ok(orderService.ship(id, request, SecurityUtils.current()));
    }

    @PostMapping("/{id}/payments")
    public ApiResponse<Order> addPayment(@PathVariable Long id,
                                         @RequestBody @Valid Requests.PaymentSave request) {
        return ApiResponse.ok(orderService.addPayment(id, request, SecurityUtils.current()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Order> cancel(@PathVariable Long id,
                                     @RequestBody @Valid Requests.Reason request) {
        return ApiResponse.ok(orderService.cancel(id, request.getReason(), SecurityUtils.current()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        orderService.delete(id, SecurityUtils.current());
        return ApiResponse.ok();
    }
}
