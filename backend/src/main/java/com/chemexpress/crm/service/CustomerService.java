package com.chemexpress.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chemexpress.crm.common.BizException;
import com.chemexpress.crm.common.Constants;
import com.chemexpress.crm.common.PageResult;
import com.chemexpress.crm.dto.Requests;
import com.chemexpress.crm.entity.Contact;
import com.chemexpress.crm.entity.Customer;
import com.chemexpress.crm.entity.Order;
import com.chemexpress.crm.entity.User;
import com.chemexpress.crm.entity.Visit;
import com.chemexpress.crm.mapper.ContactMapper;
import com.chemexpress.crm.mapper.CustomerMapper;
import com.chemexpress.crm.mapper.OrderMapper;
import com.chemexpress.crm.mapper.UserMapper;
import com.chemexpress.crm.mapper.VisitMapper;
import com.chemexpress.crm.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final VisitMapper visitMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ScopeService scopeService;
    private final CodeService codeService;
    private final EventService eventService;

    public PageResult<Customer> page(String keyword, String type, String level, String region, String status,
                                     Long ownerId, long pageNo, long pageSize, CurrentUser user) {
        Page<Customer> page = new Page<>(clampPage(pageNo), clampSize(pageSize));
        customerMapper.selectPage(page, wrapper(keyword, type, level, region, status, ownerId, user)
                .orderByDesc(Customer::getUpdatedAt)
                .orderByDesc(Customer::getId));
        enrich(page.getRecords());
        return PageResult.of(page);
    }

    public Map<String, Object> stats(String keyword, String type, String level, String region, String status,
                                     Long ownerId, CurrentUser user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", customerMapper.selectCount(wrapper(keyword, type, level, region, status, ownerId, user)));
        result.put("active", customerMapper.selectCount(wrapper(keyword, type, level, region, status, ownerId, user)
                .eq(Customer::getStatus, Constants.CUSTOMER_STATUS_ACTIVE)));
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        result.put("newThisMonth", customerMapper.selectCount(wrapper(keyword, type, level, region, status, ownerId, user)
                .ge(Customer::getCreatedAt, monthStart)));
        result.put("levelA", customerMapper.selectCount(wrapper(keyword, type, level, region, status, ownerId, user)
                .eq(Customer::getLevel, "A")));
        return result;
    }

    public Customer detail(Long id, CurrentUser user) {
        Customer customer = scopeService.visibleCustomer(id, user);
        enrich(List.of(customer));
        return customer;
    }

    @Transactional
    public Customer create(Requests.CustomerSave request, CurrentUser user) {
        if (user.isAdmin() || user.isOps()) {
            throw BizException.forbidden("当前角色只能查看客户");
        }
        String region = resolveRegion(request, user);
        Long ownerId = resolveOwner(request.getOwnerId(), region, user);
        validateFields(request);
        ensureNameUnique(request.getName(), request.getType(), region, null);

        Customer customer = new Customer();
        copyFields(customer, request);
        customer.setRegion(region);
        customer.setOwnerId(ownerId);
        customer.setCode("TMP");
        customer.setVersion(0);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        customerMapper.insert(customer);
        customer.setCode(codeService.customerCode(customer.getId()));
        customerMapper.updateById(customer);
        eventService.record("CUSTOMER", customer.getId(), "CREATE", "创建客户 " + customer.getName(), user);
        enrich(List.of(customer));
        return customer;
    }

    @Transactional
    public Customer update(Long id, Requests.CustomerSave request, CurrentUser user) {
        Customer customer = scopeService.customer(id);
        scopeService.ensureCustomerWritable(customer, user);
        if (!customer.getType().equals(request.getType())) {
            throw new BizException("客户类型创建后不可修改");
        }
        validateFields(request);
        ensureNameUnique(request.getName(), request.getType(), customer.getRegion(), id);
        validateTransition(customer.getStatus(), request.getStatus(), request.getChurnReason(), user);

        String oldStatus = customer.getStatus();
        copyFields(customer, request);
        customer.setUpdatedAt(LocalDateTime.now());
        int updated = customerMapper.updateById(customer);
        if (updated == 0) {
            throw BizException.conflict("客户信息已被他人更新，请刷新后重试");
        }
        if (!oldStatus.equals(request.getStatus())) {
            eventService.record("CUSTOMER", id, "STATUS",
                    "状态从 " + Constants.label(Constants.CUSTOMER_STATUSES, oldStatus)
                            + " 变更为 " + Constants.label(Constants.CUSTOMER_STATUSES, request.getStatus()), user);
        } else {
            eventService.record("CUSTOMER", id, "UPDATE", "更新客户 " + customer.getName(), user);
        }
        enrich(List.of(customer));
        return customer;
    }

    @Transactional
    public void delete(Long id, CurrentUser user) {
        Customer customer = scopeService.customer(id);
        scopeService.ensureCustomerWritable(customer, user);
        long visits = visitMapper.selectCount(new LambdaQueryWrapper<Visit>().eq(Visit::getCustomerId, id));
        long orders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getCustomerId, id));
        if (visits > 0 || orders > 0) {
            throw BizException.conflict("该客户已有拜访或订单记录，不能删除；可改为暂停合作或已流失");
        }
        contactMapper.delete(new LambdaQueryWrapper<Contact>().eq(Contact::getCustomerId, id));
        customerMapper.deleteById(id);
        eventService.record("CUSTOMER", id, "DELETE", "删除客户 " + customer.getName(), user);
    }

    @Transactional
    public Customer transfer(Long id, Long ownerId, CurrentUser user) {
        Customer customer = scopeService.customer(id);
        if (!user.isAdmin() && !user.isDsm()) {
            throw BizException.forbidden("只有地区经理或管理员可以转交客户");
        }
        scopeService.ensureDsmRegion(customer.getRegion(), user);
        User owner = userMapper.selectById(ownerId);
        if (owner == null || !Constants.ROLE_MR.equals(owner.getRole()) || Boolean.FALSE.equals(owner.getEnabled())) {
            throw new BizException("请选择有效的医药代表");
        }
        if (!customer.getRegion().equals(owner.getRegion())) {
            throw new BizException("只能转交给同区域的医药代表");
        }
        Long oldOwner = customer.getOwnerId();
        customer.setOwnerId(ownerId);
        customer.setUpdatedAt(LocalDateTime.now());
        customerMapper.updateById(customer);
        eventService.record("CUSTOMER", id, "TRANSFER",
                "负责人从 " + userName(oldOwner) + " 转交给 " + owner.getName(), user);
        enrich(List.of(customer));
        return customer;
    }

    public List<Map<String, Object>> ownerOptions(String region) {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, Constants.ROLE_MR)
                        .eq(User::getEnabled, true)
                        .eq(StringUtils.hasText(region), User::getRegion, region)
                        .orderByAsc(User::getId))
                .stream()
                .map(u -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", u.getId());
                    item.put("name", u.getName());
                    item.put("region", u.getRegion());
                    return item;
                })
                .toList();
    }

    private LambdaQueryWrapper<Customer> wrapper(String keyword, String type, String level, String region,
                                                 String status, Long ownerId, CurrentUser user) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        scopeService.applyCustomerScope(wrapper, user);
        wrapper.and(StringUtils.hasText(keyword), w -> w
                .like(Customer::getName, keyword).or().like(Customer::getCode, keyword));
        wrapper.eq(StringUtils.hasText(type), Customer::getType, type);
        wrapper.eq(StringUtils.hasText(level), Customer::getLevel, level);
        wrapper.eq(StringUtils.hasText(region), Customer::getRegion, region);
        wrapper.eq(StringUtils.hasText(status), Customer::getStatus, status);
        wrapper.eq(ownerId != null, Customer::getOwnerId, ownerId);
        return wrapper;
    }

    private void validateFields(Requests.CustomerSave request) {
        if ("A".equals(request.getLevel()) && (request.getAnnualPotential() == null
                || request.getAnnualPotential().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BizException("A 级客户必须填写大于 0 的年采购潜力");
        }
        validateDict(Constants.CUSTOMER_TYPES, request.getType(), "客户类型");
        validateDict(Constants.CUSTOMER_LEVELS, request.getLevel(), "客户等级");
        validateDict(Constants.CUSTOMER_STATUSES, request.getStatus(), "客户状态");
        if (request.getAnnualPotential() != null && request.getAnnualPotential().scale() > 2) {
            throw new BizException("年采购潜力最多保留 2 位小数");
        }
    }

    private void validateTransition(String from, String to, String churnReason, CurrentUser user) {
        if (from.equals(to)) {
            return;
        }
        if (!isAllowedTransition(from, to)) {
            throw new BizException("客户状态不能从 " + Constants.label(Constants.CUSTOMER_STATUSES, from)
                    + " 变更为 " + Constants.label(Constants.CUSTOMER_STATUSES, to));
        }
        if (Constants.CUSTOMER_STATUS_CHURNED.equals(to)) {
            if (user.isMr()) {
                throw BizException.forbidden("代表不能直接将客户置为流失，请联系地区经理");
            }
            if (!StringUtils.hasText(churnReason) || churnReason.trim().length() < 5) {
                throw new BizException("置为流失时请填写至少 5 个字的流失原因");
            }
        }
    }

    private boolean isAllowedTransition(String from, String to) {
        return switch (from) {
            case Constants.CUSTOMER_STATUS_POTENTIAL ->
                    Constants.CUSTOMER_STATUS_ACTIVE.equals(to) || Constants.CUSTOMER_STATUS_CHURNED.equals(to);
            case Constants.CUSTOMER_STATUS_ACTIVE ->
                    Constants.CUSTOMER_STATUS_PAUSED.equals(to) || Constants.CUSTOMER_STATUS_CHURNED.equals(to);
            case Constants.CUSTOMER_STATUS_PAUSED ->
                    Constants.CUSTOMER_STATUS_ACTIVE.equals(to) || Constants.CUSTOMER_STATUS_CHURNED.equals(to);
            case Constants.CUSTOMER_STATUS_CHURNED -> Constants.CUSTOMER_STATUS_POTENTIAL.equals(to);
            default -> false;
        };
    }

    private void ensureNameUnique(String name, String type, String region, Long excludeId) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getName, name.trim())
                .eq(Customer::getType, type)
                .eq(Customer::getRegion, region);
        wrapper.ne(excludeId != null, Customer::getId, excludeId);
        if (customerMapper.selectCount(wrapper) > 0) {
            throw new BizException("同一区域内已存在同名同类型客户");
        }
    }

    private String resolveRegion(Requests.CustomerSave request, CurrentUser user) {
        if (user.isMr() || user.isDsm()) {
            return user.region();
        }
        return request.getRegion();
    }

    private Long resolveOwner(Long ownerId, String region, CurrentUser user) {
        if (user.isMr()) {
            return user.id();
        }
        if (ownerId == null) {
            throw new BizException("地区经理创建客户时必须选择负责人");
        }
        User owner = userMapper.selectById(ownerId);
        if (owner == null || !Constants.ROLE_MR.equals(owner.getRole()) || Boolean.FALSE.equals(owner.getEnabled())) {
            throw new BizException("请选择有效的医药代表");
        }
        if (!region.equals(owner.getRegion())) {
            throw new BizException("负责人必须与客户区域一致");
        }
        return ownerId;
    }

    private void copyFields(Customer customer, Requests.CustomerSave request) {
        customer.setName(request.getName().trim());
        customer.setType(request.getType());
        customer.setLevel(request.getLevel());
        customer.setStatus(request.getStatus());
        customer.setAddress(request.getAddress());
        customer.setAnnualPotential(request.getAnnualPotential());
        customer.setTags(request.getTags());
        customer.setSource(request.getSource());
        customer.setRemark(request.getRemark());
    }

    private void validateDict(Map<String, String> dict, String value, String label) {
        if (!dict.containsKey(value)) {
            throw new BizException(label + "不正确");
        }
    }

    private void enrich(List<Customer> customers) {
        if (customers.isEmpty()) {
            return;
        }
        Set<Long> ownerIds = customers.stream().map(Customer::getOwnerId).collect(Collectors.toSet());
        Map<Long, User> users = userMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        for (Customer customer : customers) {
            User owner = users.get(customer.getOwnerId());
            customer.setOwnerName(owner == null ? "—" : owner.getName());
            customer.setTypeName(Constants.label(Constants.CUSTOMER_TYPES, customer.getType()));
            customer.setStatusName(Constants.label(Constants.CUSTOMER_STATUSES, customer.getStatus()));
        }
    }

    private String userName(Long id) {
        User user = userMapper.selectById(id);
        return user == null ? "—" : user.getName();
    }

    private long clampPage(long page) {
        return Math.max(1, page);
    }

    private long clampSize(long size) {
        return Math.min(100, Math.max(1, size));
    }
}
