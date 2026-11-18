package com.chemexpress.crm.controller;

import com.chemexpress.crm.common.ApiResponse;
import com.chemexpress.crm.dto.Requests;
import com.chemexpress.crm.entity.Contact;
import com.chemexpress.crm.security.SecurityUtils;
import com.chemexpress.crm.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public ApiResponse<List<Contact>> list(@PathVariable Long customerId) {
        return ApiResponse.ok(contactService.list(customerId, SecurityUtils.current()));
    }

    @PostMapping
    public ApiResponse<Contact> create(@PathVariable Long customerId,
                                       @RequestBody @Valid Requests.ContactSave request) {
        return ApiResponse.ok(contactService.create(customerId, request, SecurityUtils.current()));
    }

    @PutMapping("/{contactId}")
    public ApiResponse<Contact> update(@PathVariable Long customerId, @PathVariable Long contactId,
                                       @RequestBody @Valid Requests.ContactSave request) {
        return ApiResponse.ok(contactService.update(customerId, contactId, request, SecurityUtils.current()));
    }

    @DeleteMapping("/{contactId}")
    public ApiResponse<Void> delete(@PathVariable Long customerId, @PathVariable Long contactId) {
        contactService.delete(customerId, contactId, SecurityUtils.current());
        return ApiResponse.ok();
    }
}
