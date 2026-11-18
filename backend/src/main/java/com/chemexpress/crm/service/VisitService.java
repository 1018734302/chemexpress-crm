package com.chemexpress.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chemexpress.crm.common.BizException;
import com.chemexpress.crm.common.Constants;
import com.chemexpress.crm.common.PageResult;
import com.chemexpress.crm.dto.Requests;
import com.chemexpress.crm.entity.Contact;
import com.chemexpress.crm.entity.Customer;
import com.chemexpress.crm.entity.Product;
import com.chemexpress.crm.entity.User;
import com.chemexpress.crm.entity.Visit;
import com.chemexpress.crm.mapper.ContactMapper;
import com.chemexpress.crm.mapper.CustomerMapper;
import com.chemexpress.crm.mapper.ProductMapper;
import com.chemexpress.crm.mapper.UserMapper;
import com.chemexpress.crm.mapper.VisitMapper;
import com.chemexpress.crm.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisitService {

    private static final Set<String> ACTIVE_STATUSES = Set.of(
            Constants.VISIT_PLANNED, Constants.VISIT_CHECKED_IN, Constants.VISIT_SUBMITTED);

    private final VisitMapper visitMapper;
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final ScopeService scopeService;
    private final CodeService codeService;
    private final EventService eventService;

    public PageResult<Visit> page(String keyword, String status, String purpose, LocalDate dateFrom, LocalDate dateTo,
                                  Long repId, Long customerId, long pageNo, long pageSize, CurrentUser user) {
        LambdaQueryWrapper<Visit> wrapper = new LambdaQueryWrapper<>();
        scopeService.applyVisitScope(wrapper, user);
        wrapper.eq(StringUtils.hasText(status), Visit::getStatus, status);
        wrapper.eq(StringUtils.hasText(purpose), Visit::getPurpose, purpose);
        wrapper.ge(dateFrom != null, Visit::getVisitDate, dateFrom);
        wrapper.le(dateTo != null, Visit::getVisitDate, dateTo);
        wrapper.eq(repId != null, Visit::getRepId, repId);
        wrapper.eq(customerId != null, Visit::getCustomerId, customerId);
        if (StringUtils.hasText(keyword)) {
            List<Long> customerIds = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                            .and(w -> w.like(Customer::getName, keyword).or().like(Customer::getCode, keyword)))
                    .stream().map(Customer::getId).toList();
            if (customerIds.isEmpty()) {
                wrapper.like(Visit::getCode, keyword);
            } else {
                wrapper.and(w -> w.like(Visit::getCode, keyword).or().in(Visit::getCustomerId, customerIds));
            }
        }
        wrapper.orderByDesc(Visit::getVisitDate).orderByDesc(Visit::getId);
        Page<Visit> page = new Page<>(clampPage(pageNo), clampSize(pageSize));
        visitMapper.selectPage(page, wrapper);
        enrich(page.getRecords());
        return PageResult.of(page);
    }

    public Visit detail(Long id, CurrentUser user) {
        Visit visit = scopeService.visibleVisit(id, user);
        enrich(List.of(visit));
        List<Long> productIds = visitMapper.selectProductIds(id);
        visit.setProducts(productIds.isEmpty() ? List.of() : productMapper.selectBatchIds(productIds));
        visit.setEvents(eventService.list("VISIT", id));
        return visit;
    }

    @Transactional
    public Visit createPlan(Requests.VisitPlan request, CurrentUser user) {
        if (!user.isMr()) {
            throw BizException.forbidden("只有医药代表可以创建拜访计划");
        }
        Customer customer = scopeService.visibleCustomer(request.getCustomerId(), user);
        if (Constants.CUSTOMER_STATUS_CHURNED.equals(customer.getStatus())) {
            throw new BizException("已流失客户不能创建拜访");
        }
        validatePlan(request);
        ensureNoActiveDuplicate(customer.getId(), request.getVisitDate(), null);
        if (request.getContactId() != null) {
            ensureContact(customer.getId(), request.getContactId());
        }
        Visit visit = new Visit();
        visit.setCode(codeService.nextVisitCode(LocalDate.now()));
        visit.setCustomerId(customer.getId());
        visit.setContactId(request.getContactId());
        visit.setRepId(user.id());
        visit.setRegion(customer.getRegion());
        visit.setVisitDate(request.getVisitDate());
        visit.setPurpose(request.getPurpose());
        visit.setRemark(request.getRemark());
        visit.setStatus(Constants.VISIT_PLANNED);
        visit.setVersion(0);
        visit.setCreatedAt(LocalDateTime.now());
        visit.setUpdatedAt(LocalDateTime.now());
        visitMapper.insert(visit);
        eventService.record("VISIT", visit.getId(), "PLAN", "创建拜访计划 " + visit.getCode(), user);
        enrich(List.of(visit));
        return visit;
    }

    @Transactional
    public Visit updatePlan(Long id, Requests.VisitPlan request, CurrentUser user) {
        Visit visit = scopeService.visibleVisit(id, user);
        ensureOwner(visit, user);
        if (!Constants.VISIT_PLANNED.equals(visit.getStatus())) {
            throw BizException.conflict("只有计划中的拜访可以修改");
        }
        if (!visit.getCustomerId().equals(request.getCustomerId())) {
            throw new BizException("拜访客户不可修改");
        }
        validatePlan(request);
        ensureNoActiveDuplicate(visit.getCustomerId(), request.getVisitDate(), id);
        if (request.getContactId() != null) {
            ensureContact(visit.getCustomerId(), request.getContactId());
        }
        visit.setVisitDate(request.getVisitDate());
        visit.setContactId(request.getContactId());
        visit.setPurpose(request.getPurpose());
        visit.setRemark(request.getRemark());
        visit.setUpdatedAt(LocalDateTime.now());
        if (visitMapper.updateById(visit) == 0) {
            throw BizException.conflict("拜访已被他人更新，请刷新后重试");
        }
        eventService.record("VISIT", id, "UPDATE", "修改拜访计划 " + visit.getCode(), user);
        enrich(List.of(visit));
        return visit;
    }

    @Transactional
    public Visit checkin(Long id, CurrentUser user) {
        Visit visit = scopeService.visibleVisit(id, user);
        ensureOwner(visit, user);
        if (!Constants.VISIT_PLANNED.equals(visit.getStatus())) {
            throw BizException.conflict("当前状态不能签到");
        }
        if (!LocalDate.now().equals(visit.getVisitDate())) {
            throw new BizException("仅在拜访当日可以签到");
        }
        visit.setStatus(Constants.VISIT_CHECKED_IN);
        visit.setCheckinTime(LocalDateTime.now());
        visit.setCheckinDistance(60 + (int) (visit.getId() % 120));
        visit.setUpdatedAt(LocalDateTime.now());
        visitMapper.updateById(visit);
        eventService.record("VISIT", id, "CHECKIN", "完成签到（演示定位）", user);
        enrich(List.of(visit));
        return visit;
    }

    @Transactional
    public Visit submit(Long id, Requests.VisitSubmit request, CurrentUser user) {
        Visit visit = scopeService.visibleVisit(id, user);
        ensureOwner(visit, user);
        if (!Constants.VISIT_CHECKED_IN.equals(visit.getStatus())
                && !Constants.VISIT_REJECTED.equals(visit.getStatus())) {
            throw BizException.conflict("只有已签到或已驳回的拜访可以提交记录");
        }
        if (!Constants.VISIT_RESULTS.containsKey(request.getResult())) {
            throw new BizException("拜访结果不正确");
        }
        List<Product> products = productMapper.selectBatchIds(request.getProductIds());
        if (products.size() != request.getProductIds().stream().distinct().count()) {
            throw new BizException("关联产品不存在");
        }
        if (products.stream().anyMatch(p -> !Constants.PRODUCT_ACTIVE.equals(p.getStatus()))) {
            throw new BizException("已停售产品不能关联到拜访记录");
        }
        visit.setResult(request.getResult());
        visit.setFeedback(request.getFeedback());
        visit.setNextStep(request.getNextStep());
        visit.setStatus(Constants.VISIT_SUBMITTED);
        visit.setRejectReason(null);
        visit.setUpdatedAt(LocalDateTime.now());
        visitMapper.updateById(visit);
        visitMapper.deleteVisitProducts(id);
        request.getProductIds().stream().distinct().forEach(pid -> visitMapper.insertVisitProduct(id, pid));
        eventService.record("VISIT", id, "SUBMIT", "提交拜访记录 " + visit.getCode(), user);
        return detail(id, user);
    }

    @Transactional
    public Visit approve(Long id, CurrentUser user) {
        if (!user.isDsm()) {
            throw BizException.forbidden("只有地区经理可以审批拜访");
        }
        Visit visit = scopeService.visibleVisit(id, user);
        if (!Constants.VISIT_SUBMITTED.equals(visit.getStatus())) {
            throw BizException.conflict("只有待审批的拜访可以通过");
        }
        if (visit.getRepId().equals(user.id())) {
            throw BizException.forbidden("不能审批自己提交的拜访");
        }
        visit.setStatus(Constants.VISIT_APPROVED);
        visit.setApproverId(user.id());
        visit.setApprovedAt(LocalDateTime.now());
        visit.setUpdatedAt(LocalDateTime.now());
        visitMapper.updateById(visit);
        customerMapper.update(null, new LambdaUpdateWrapper<Customer>()
                .eq(Customer::getId, visit.getCustomerId())
                .set(Customer::getLastVisitDate, visit.getVisitDate()));
        eventService.record("VISIT", id, "APPROVE", "审批通过拜访 " + visit.getCode(), user);
        enrich(List.of(visit));
        return visit;
    }

    @Transactional
    public Visit reject(Long id, String reason, CurrentUser user) {
        if (!user.isDsm()) {
            throw BizException.forbidden("只有地区经理可以审批拜访");
        }
        Visit visit = scopeService.visibleVisit(id, user);
        if (!Constants.VISIT_SUBMITTED.equals(visit.getStatus())) {
            throw BizException.conflict("只有待审批的拜访可以驳回");
        }
        visit.setStatus(Constants.VISIT_REJECTED);
        visit.setRejectReason(reason);
        visit.setApproverId(user.id());
        visit.setApprovedAt(LocalDateTime.now());
        visit.setUpdatedAt(LocalDateTime.now());
        visitMapper.updateById(visit);
        eventService.record("VISIT", id, "REJECT", "驳回拜访：" + reason, user);
        enrich(List.of(visit));
        return visit;
    }

    @Transactional
    public Visit cancel(Long id, String reason, CurrentUser user) {
        Visit visit = scopeService.visibleVisit(id, user);
        ensureOwner(visit, user);
        if (!Constants.VISIT_PLANNED.equals(visit.getStatus())
                && !Constants.VISIT_CHECKED_IN.equals(visit.getStatus())) {
            throw BizException.conflict("当前状态不能取消拜访");
        }
        visit.setStatus(Constants.VISIT_CANCELLED);
        visit.setCancelReason(reason);
        visit.setUpdatedAt(LocalDateTime.now());
        visitMapper.updateById(visit);
        eventService.record("VISIT", id, "CANCEL", "取消拜访：" + reason, user);
        enrich(List.of(visit));
        return visit;
    }

    private void validatePlan(Requests.VisitPlan request) {
        if (!Constants.VISIT_PURPOSES.containsKey(request.getPurpose())) {
            throw new BizException("拜访目的不正确");
        }
        LocalDate today = LocalDate.now();
        if (request.getVisitDate().isBefore(today)) {
            throw new BizException("拜访日期不能早于今天");
        }
        if (request.getVisitDate().isAfter(today.plusDays(30))) {
            throw new BizException("拜访日期不能超过未来 30 天");
        }
    }

    private void ensureNoActiveDuplicate(Long customerId, LocalDate date, Long excludeId) {
        LambdaQueryWrapper<Visit> wrapper = new LambdaQueryWrapper<Visit>()
                .eq(Visit::getCustomerId, customerId)
                .eq(Visit::getVisitDate, date)
                .in(Visit::getStatus, ACTIVE_STATUSES);
        wrapper.ne(excludeId != null, Visit::getId, excludeId);
        Visit existing = visitMapper.selectOne(wrapper.last("LIMIT 1"));
        if (existing != null) {
            throw BizException.conflict("该客户当天已有未结束的拜访（" + existing.getCode() + "）");
        }
    }

    private void ensureContact(Long customerId, Long contactId) {
        Contact contact = contactMapper.selectById(contactId);
        if (contact == null || !customerId.equals(contact.getCustomerId())) {
            throw new BizException("联系人不属于当前客户");
        }
    }

    private void ensureOwner(Visit visit, CurrentUser user) {
        if (!user.isMr() || !visit.getRepId().equals(user.id())) {
            throw BizException.forbidden("只有拜访负责人可以执行该操作");
        }
    }

    private void enrich(List<Visit> visits) {
        if (visits.isEmpty()) {
            return;
        }
        Set<Long> customerIds = visits.stream().map(Visit::getCustomerId).collect(Collectors.toSet());
        Set<Long> repIds = visits.stream().map(Visit::getRepId).collect(Collectors.toSet());
        Set<Long> contactIds = visits.stream().map(Visit::getContactId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Customer> customers = customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<Long, User> users = userMapper.selectBatchIds(repIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Contact> contacts = contactIds.isEmpty() ? Map.of()
                : contactMapper.selectBatchIds(contactIds).stream().collect(Collectors.toMap(Contact::getId, Function.identity()));
        for (Visit visit : visits) {
            Customer customer = customers.get(visit.getCustomerId());
            User rep = users.get(visit.getRepId());
            Contact contact = visit.getContactId() == null ? null : contacts.get(visit.getContactId());
            visit.setCustomerName(customer == null ? "—" : customer.getName());
            visit.setRepName(rep == null ? "—" : rep.getName());
            visit.setContactName(contact == null ? null : contact.getName());
            visit.setPurposeName(Constants.label(Constants.VISIT_PURPOSES, visit.getPurpose()));
            visit.setStatusName(Constants.label(Constants.VISIT_STATUSES, visit.getStatus()));
        }
    }

    private long clampPage(long page) {
        return Math.max(1, page);
    }

    private long clampSize(long size) {
        return Math.min(100, Math.max(1, size));
    }
}
