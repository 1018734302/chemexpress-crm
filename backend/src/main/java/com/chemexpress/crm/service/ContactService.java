package com.chemexpress.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.chemexpress.crm.common.BizException;
import com.chemexpress.crm.common.Constants;
import com.chemexpress.crm.dto.Requests;
import com.chemexpress.crm.entity.Contact;
import com.chemexpress.crm.entity.Customer;
import com.chemexpress.crm.entity.Visit;
import com.chemexpress.crm.mapper.ContactMapper;
import com.chemexpress.crm.mapper.VisitMapper;
import com.chemexpress.crm.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactMapper contactMapper;
    private final VisitMapper visitMapper;
    private final ScopeService scopeService;
    private final EventService eventService;

    public List<Contact> list(Long customerId, CurrentUser user) {
        Customer customer = scopeService.visibleCustomer(customerId, user);
        List<Contact> contacts = contactMapper.selectList(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, customerId)
                .orderByDesc(Contact::getPrimaryContact)
                .orderByAsc(Contact::getId));
        contacts.forEach(c -> c.setRoleName(Constants.label(Constants.CONTACT_ROLES, c.getRole())));
        return contacts;
    }

    @Transactional
    public Contact create(Long customerId, Requests.ContactSave request, CurrentUser user) {
        Customer customer = scopeService.customer(customerId);
        scopeService.ensureCustomerWritable(customer, user);
        validate(request);
        Long count = contactMapper.selectCount(new LambdaQueryWrapper<Contact>().eq(Contact::getCustomerId, customerId));
        boolean primary = Boolean.TRUE.equals(request.getPrimaryContact()) || count == 0;
        if (primary) {
            clearPrimary(customerId);
        }
        Contact contact = new Contact();
        contact.setCustomerId(customerId);
        copy(contact, request);
        contact.setPrimaryContact(primary);
        contact.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "ACTIVE");
        contact.setCreatedAt(LocalDateTime.now());
        contact.setUpdatedAt(LocalDateTime.now());
        contactMapper.insert(contact);
        eventService.record("CUSTOMER", customerId, "CONTACT", "新增联系人 " + contact.getName(), user);
        contact.setRoleName(Constants.label(Constants.CONTACT_ROLES, contact.getRole()));
        return contact;
    }

    @Transactional
    public Contact update(Long customerId, Long contactId, Requests.ContactSave request, CurrentUser user) {
        Customer customer = scopeService.customer(customerId);
        scopeService.ensureCustomerWritable(customer, user);
        Contact contact = require(customerId, contactId);
        validate(request);
        boolean primary = Boolean.TRUE.equals(request.getPrimaryContact());
        if (primary) {
            clearPrimary(customerId);
        } else if (Boolean.TRUE.equals(contact.getPrimaryContact())) {
            Long others = contactMapper.selectCount(new LambdaQueryWrapper<Contact>()
                    .eq(Contact::getCustomerId, customerId)
                    .ne(Contact::getId, contactId));
            if (others == 0) {
                primary = true;
            }
        }
        copy(contact, request);
        contact.setPrimaryContact(primary);
        contact.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : contact.getStatus());
        contact.setUpdatedAt(LocalDateTime.now());
        contactMapper.updateById(contact);
        eventService.record("CUSTOMER", customerId, "CONTACT", "更新联系人 " + contact.getName(), user);
        contact.setRoleName(Constants.label(Constants.CONTACT_ROLES, contact.getRole()));
        return contact;
    }

    @Transactional
    public void delete(Long customerId, Long contactId, CurrentUser user) {
        Customer customer = scopeService.customer(customerId);
        scopeService.ensureCustomerWritable(customer, user);
        Contact contact = require(customerId, contactId);
        long referenced = visitMapper.selectCount(new LambdaQueryWrapper<Visit>()
                .eq(Visit::getCustomerId, customerId)
                .eq(Visit::getContactId, contactId));
        if (referenced > 0) {
            throw BizException.conflict("该联系人已被拜访记录引用，不能删除；可将其状态改为停用");
        }
        contactMapper.deleteById(contactId);
        if (Boolean.TRUE.equals(contact.getPrimaryContact())) {
            Contact next = contactMapper.selectOne(new LambdaQueryWrapper<Contact>()
                    .eq(Contact::getCustomerId, customerId)
                    .orderByAsc(Contact::getId)
                    .last("LIMIT 1"));
            if (next != null) {
                next.setPrimaryContact(true);
                contactMapper.updateById(next);
            }
        }
        eventService.record("CUSTOMER", customerId, "CONTACT", "删除联系人 " + contact.getName(), user);
    }

    private void clearPrimary(Long customerId) {
        contactMapper.update(null, new LambdaUpdateWrapper<Contact>()
                .eq(Contact::getCustomerId, customerId)
                .set(Contact::getPrimaryContact, false));
    }

    private Contact require(Long customerId, Long contactId) {
        Contact contact = contactMapper.selectById(contactId);
        if (contact == null || !customerId.equals(contact.getCustomerId())) {
            throw BizException.notFound("联系人不存在");
        }
        return contact;
    }

    private void validate(Requests.ContactSave request) {
        if (!Constants.CONTACT_ROLES.containsKey(request.getRole())) {
            throw new BizException("联系人角色不正确");
        }
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().matches("\\d{11}")) {
            throw new BizException("手机号需为 11 位数字");
        }
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BizException("邮箱格式不正确");
        }
    }

    private void copy(Contact contact, Requests.ContactSave request) {
        contact.setName(request.getName().trim());
        contact.setTitle(request.getTitle());
        contact.setRole(request.getRole());
        contact.setPhone(request.getPhone());
        contact.setEmail(request.getEmail());
        contact.setRemark(request.getRemark());
    }
}
