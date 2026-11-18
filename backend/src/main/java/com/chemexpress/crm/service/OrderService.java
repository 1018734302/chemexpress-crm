package com.chemexpress.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chemexpress.crm.common.BizException;
import com.chemexpress.crm.common.Constants;
import com.chemexpress.crm.common.PageResult;
import com.chemexpress.crm.dto.Requests;
import com.chemexpress.crm.entity.Customer;
import com.chemexpress.crm.entity.Order;
import com.chemexpress.crm.entity.OrderItem;
import com.chemexpress.crm.entity.Payment;
import com.chemexpress.crm.entity.Product;
import com.chemexpress.crm.entity.User;
import com.chemexpress.crm.mapper.CustomerMapper;
import com.chemexpress.crm.mapper.OrderItemMapper;
import com.chemexpress.crm.mapper.OrderMapper;
import com.chemexpress.crm.mapper.PaymentMapper;
import com.chemexpress.crm.mapper.ProductMapper;
import com.chemexpress.crm.mapper.UserMapper;
import com.chemexpress.crm.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final ProductMapper productMapper;
    private final CustomerMapper customerMapper;
    private final UserMapper userMapper;
    private final ScopeService scopeService;
    private final CodeService codeService;
    private final EventService eventService;

    public PageResult<Order> page(String keyword, String status, LocalDate dateFrom, LocalDate dateTo,
                                  long pageNo, long pageSize, CurrentUser user) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        scopeService.applyOrderScope(wrapper, user);
        wrapper.eq(StringUtils.hasText(status), Order::getStatus, status);
        wrapper.ge(dateFrom != null, Order::getCreatedAt, dateFrom == null ? null : dateFrom.atStartOfDay());
        wrapper.lt(dateTo != null, Order::getCreatedAt, dateTo == null ? null : dateTo.plusDays(1).atStartOfDay());
        if (StringUtils.hasText(keyword)) {
            List<Long> customerIds = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                            .and(w -> w.like(Customer::getName, keyword).or().like(Customer::getCode, keyword)))
                    .stream().map(Customer::getId).toList();
            if (customerIds.isEmpty()) {
                wrapper.like(Order::getCode, keyword);
            } else {
                wrapper.and(w -> w.like(Order::getCode, keyword).or().in(Order::getCustomerId, customerIds));
            }
        }
        wrapper.orderByDesc(Order::getCreatedAt).orderByDesc(Order::getId);
        Page<Order> page = new Page<>(Math.max(1, pageNo), Math.min(100, Math.max(1, pageSize)));
        orderMapper.selectPage(page, wrapper);
        enrich(page.getRecords());
        return PageResult.of(page);
    }

    public Order detail(Long id, CurrentUser user) {
        Order order = scopeService.visibleOrder(id, user);
        enrich(List.of(order));
        order.setItems(orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, id).orderByAsc(OrderItem::getId)));
        List<Payment> payments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, id).orderByAsc(Payment::getPayDate).orderByAsc(Payment::getId));
        Map<Long, User> users = payments.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(payments.stream().map(Payment::getCreatedBy).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        payments.forEach(p -> {
            User creator = users.get(p.getCreatedBy());
            p.setCreatedByName(creator == null ? "—" : creator.getName());
            p.setMethodName(Constants.label(Constants.PAYMENT_METHODS, p.getMethod()));
        });
        order.setPayments(payments);
        order.setEvents(eventService.list("ORDER", id));
        return order;
    }

    @Transactional
    public Order create(Requests.OrderSave request, CurrentUser user) {
        if (!user.isMr() && !user.isOps()) {
            throw BizException.forbidden("只有医药代表或商务运营可以创建订单");
        }
        Customer customer = scopeService.visibleCustomer(request.getCustomerId(), user);
        if (!Constants.CUSTOMER_STATUS_ACTIVE.equals(customer.getStatus())) {
            throw new BizException("只有合作中的客户可以下单");
        }
        validateExpectedDate(request.getExpectedDate());
        List<OrderItem> items = buildItems(request.getItems());
        BigDecimal total = total(items);

        Order order = new Order();
        order.setCode(codeService.nextOrderCode(LocalDate.now()));
        order.setCustomerId(customer.getId());
        order.setRepId(customer.getOwnerId());
        order.setRegion(customer.getRegion());
        order.setStatus(Boolean.TRUE.equals(request.getSubmit()) ? Constants.ORDER_PENDING : Constants.ORDER_DRAFT);
        order.setTotalAmount(total);
        order.setReceiverAddress(StringUtils.hasText(request.getReceiverAddress())
                ? request.getReceiverAddress() : customer.getAddress());
        order.setExpectedDate(request.getExpectedDate());
        order.setRemark(request.getRemark());
        order.setVersion(0);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);
        saveItems(order.getId(), items);
        eventService.record("ORDER", order.getId(), Boolean.TRUE.equals(request.getSubmit()) ? "SUBMIT" : "CREATE",
                "创建订单 " + order.getCode() + "，金额 " + total, user);
        return detail(order.getId(), user);
    }

    @Transactional
    public Order update(Long id, Requests.OrderSave request, CurrentUser user) {
        Order order = scopeService.visibleOrder(id, user);
        ensureEditor(order, user);
        if (!Constants.ORDER_DRAFT.equals(order.getStatus()) && !Constants.ORDER_REJECTED.equals(order.getStatus())) {
            throw BizException.conflict("只有草稿或已驳回订单可以修改");
        }
        if (!order.getCustomerId().equals(request.getCustomerId())) {
            throw new BizException("订单客户不可修改");
        }
        Customer customer = scopeService.customer(order.getCustomerId());
        if (Boolean.TRUE.equals(request.getSubmit()) && !Constants.CUSTOMER_STATUS_ACTIVE.equals(customer.getStatus())) {
            throw new BizException("客户当前不是合作中状态，不能提交订单");
        }
        validateExpectedDate(request.getExpectedDate());
        List<OrderItem> items = buildItems(request.getItems());
        BigDecimal total = total(items);
        order.setStatus(Boolean.TRUE.equals(request.getSubmit()) ? Constants.ORDER_PENDING : Constants.ORDER_DRAFT);
        order.setTotalAmount(total);
        order.setReceiverAddress(StringUtils.hasText(request.getReceiverAddress())
                ? request.getReceiverAddress() : customer.getAddress());
        order.setExpectedDate(request.getExpectedDate());
        order.setRemark(request.getRemark());
        order.setRejectReason(null);
        order.setUpdatedAt(LocalDateTime.now());
        if (orderMapper.updateById(order) == 0) {
            throw BizException.conflict("订单已被他人更新，请刷新后重试");
        }
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        saveItems(id, items);
        eventService.record("ORDER", id, Boolean.TRUE.equals(request.getSubmit()) ? "SUBMIT" : "UPDATE",
                "更新订单 " + order.getCode() + "，金额 " + total, user);
        return detail(id, user);
    }

    @Transactional
    public Order submit(Long id, CurrentUser user) {
        Order order = scopeService.visibleOrder(id, user);
        ensureEditor(order, user);
        if (!Constants.ORDER_DRAFT.equals(order.getStatus()) && !Constants.ORDER_REJECTED.equals(order.getStatus())) {
            throw BizException.conflict("当前订单状态不能提交");
        }
        Customer customer = scopeService.customer(order.getCustomerId());
        if (!Constants.CUSTOMER_STATUS_ACTIVE.equals(customer.getStatus())) {
            throw new BizException("客户当前不是合作中状态，不能提交订单");
        }
        long items = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        if (items == 0) {
            throw new BizException("订单至少需要一个产品");
        }
        order.setStatus(Constants.ORDER_PENDING);
        order.setRejectReason(null);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        eventService.record("ORDER", id, "SUBMIT", "提交订单 " + order.getCode(), user);
        return detail(id, user);
    }

    @Transactional
    public Order approve(Long id, CurrentUser user) {
        if (!user.isDsm()) {
            throw BizException.forbidden("只有地区经理可以审批订单");
        }
        Order order = scopeService.visibleOrder(id, user);
        if (!Constants.ORDER_PENDING.equals(order.getStatus())) {
            throw BizException.conflict("只有待审批订单可以通过");
        }
        if (order.getRepId().equals(user.id())) {
            throw BizException.forbidden("不能审批自己提交的订单");
        }
        order.setStatus(Constants.ORDER_APPROVED);
        order.setApproverId(user.id());
        order.setApprovedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        eventService.record("ORDER", id, "APPROVE", "审批通过订单 " + order.getCode(), user);
        return detail(id, user);
    }

    @Transactional
    public Order reject(Long id, String reason, CurrentUser user) {
        if (!user.isDsm()) {
            throw BizException.forbidden("只有地区经理可以审批订单");
        }
        Order order = scopeService.visibleOrder(id, user);
        if (!Constants.ORDER_PENDING.equals(order.getStatus())) {
            throw BizException.conflict("只有待审批订单可以驳回");
        }
        order.setStatus(Constants.ORDER_REJECTED);
        order.setRejectReason(reason);
        order.setApproverId(user.id());
        order.setApprovedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        eventService.record("ORDER", id, "REJECT", "驳回订单：" + reason, user);
        return detail(id, user);
    }

    @Transactional
    public Order ship(Long id, Requests.Ship request, CurrentUser user) {
        if (!user.isOps()) {
            throw BizException.forbidden("只有商务运营可以登记发货");
        }
        Order order = scopeService.visibleOrder(id, user);
        if (!Constants.ORDER_APPROVED.equals(order.getStatus())) {
            throw BizException.conflict("只有待发货订单可以发货");
        }
        if (!request.getTrackingNo().matches("[A-Za-z0-9-]{6,30}")) {
            throw new BizException("物流单号需为 6-30 位字母、数字或短横线");
        }
        order.setStatus(Constants.ORDER_SHIPPED);
        order.setLogisticsCompany(request.getLogisticsCompany());
        order.setTrackingNo(request.getTrackingNo());
        order.setShippedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        eventService.record("ORDER", id, "SHIP",
                "发货：" + request.getLogisticsCompany() + " / " + request.getTrackingNo(), user);
        return detail(id, user);
    }

    @Transactional
    public Order addPayment(Long id, Requests.PaymentSave request, CurrentUser user) {
        if (!user.isOps()) {
            throw BizException.forbidden("只有商务运营可以登记回款");
        }
        Order order = scopeService.visibleOrder(id, user);
        if (!Constants.ORDER_SHIPPED.equals(order.getStatus())) {
            throw BizException.conflict("只有已发货订单可以登记回款");
        }
        if (request.getPayDate().isAfter(LocalDate.now())) {
            throw new BizException("回款日期不能晚于今天");
        }
        BigDecimal paid = paidAmount(id);
        BigDecimal remain = order.getTotalAmount().subtract(paid);
        if (request.getAmount().compareTo(remain) > 0) {
            throw new BizException("回款金额不能超过未回款余额 " + remain.setScale(2, RoundingMode.HALF_UP));
        }
        if (!Constants.PAYMENT_METHODS.containsKey(request.getMethod())) {
            throw new BizException("回款方式不正确");
        }
        Payment payment = new Payment();
        payment.setOrderId(id);
        payment.setPayDate(request.getPayDate());
        payment.setAmount(request.getAmount());
        payment.setMethod(request.getMethod());
        payment.setReferenceNo(request.getReferenceNo());
        payment.setRemark(request.getRemark());
        payment.setCreatedBy(user.id());
        payment.setCreatedAt(LocalDateTime.now());
        paymentMapper.insert(payment);
        eventService.record("ORDER", id, "PAYMENT",
                "登记回款 " + request.getAmount().setScale(2, RoundingMode.HALF_UP) + " 元", user);
        BigDecimal newPaid = paid.add(request.getAmount());
        if (newPaid.compareTo(order.getTotalAmount()) == 0) {
            order.setStatus(Constants.ORDER_COMPLETED);
            order.setCompletedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            eventService.record("ORDER", id, "COMPLETE", "订单回款完成，自动结案", user);
        }
        return detail(id, user);
    }

    @Transactional
    public Order cancel(Long id, String reason, CurrentUser user) {
        Order order = scopeService.visibleOrder(id, user);
        if (!Constants.ORDER_DRAFT.equals(order.getStatus())
                && !Constants.ORDER_PENDING.equals(order.getStatus())
                && !Constants.ORDER_APPROVED.equals(order.getStatus())) {
            throw BizException.conflict("当前订单状态不能取消");
        }
        boolean allowed = user.isAdmin() || user.isOps()
                || (user.isMr() && order.getRepId().equals(user.id()))
                || (user.isDsm() && order.getRegion().equals(user.region()));
        if (!allowed) {
            throw BizException.forbidden("无权取消该订单");
        }
        if (paidAmount(id).compareTo(BigDecimal.ZERO) > 0) {
            throw BizException.conflict("订单已有回款，不能取消");
        }
        order.setStatus(Constants.ORDER_CANCELLED);
        order.setCancelReason(reason);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        eventService.record("ORDER", id, "CANCEL", "取消订单：" + reason, user);
        return detail(id, user);
    }

    @Transactional
    public void delete(Long id, CurrentUser user) {
        Order order = scopeService.visibleOrder(id, user);
        if (!Constants.ORDER_DRAFT.equals(order.getStatus())) {
            throw BizException.conflict("只有草稿订单可以删除");
        }
        boolean allowed = user.isAdmin() || user.isOps()
                || (user.isMr() && order.getRepId().equals(user.id()));
        if (!allowed) {
            throw BizException.forbidden("无权删除该订单");
        }
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        paymentMapper.delete(new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, id));
        orderMapper.deleteById(id);
        eventService.record("ORDER", id, "DELETE", "删除草稿订单 " + order.getCode(), user);
    }

    private List<OrderItem> buildItems(List<Requests.OrderItemSave> requests) {
        Set<Long> ids = new HashSet<>();
        for (Requests.OrderItemSave item : requests) {
            if (!ids.add(item.getProductId())) {
                throw new BizException("同一产品不能重复添加");
            }
            if (item.getDiscount().scale() > 2) {
                throw new BizException("折扣最多保留 2 位小数");
            }
        }
        Map<Long, Product> products = productMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        if (products.size() != ids.size()) {
            throw new BizException("部分产品不存在");
        }
        List<OrderItem> items = new ArrayList<>();
        for (Requests.OrderItemSave request : requests) {
            Product product = products.get(request.getProductId());
            if (!Constants.PRODUCT_ACTIVE.equals(product.getStatus())) {
                throw new BizException("产品 " + product.getTradeName() + " 已停售，不能下单");
            }
            BigDecimal unitPrice = request.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));
            BigDecimal discount = request.getDiscount().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal amount = gross.multiply(BigDecimal.ONE.subtract(discount)).setScale(2, RoundingMode.HALF_UP);
            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getTradeName());
            item.setProductSpec(product.getSpec());
            item.setUnitPrice(unitPrice);
            item.setQuantity(request.getQuantity());
            item.setDiscount(request.getDiscount());
            item.setAmount(amount);
            items.add(item);
        }
        return items;
    }

    private BigDecimal total(List<OrderItem> items) {
        return items.stream().map(OrderItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void saveItems(Long orderId, List<OrderItem> items) {
        for (OrderItem item : items) {
            item.setOrderId(orderId);
            orderItemMapper.insert(item);
        }
    }

    private BigDecimal paidAmount(Long orderId) {
        return paymentMapper.selectList(new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, orderId))
                .stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateExpectedDate(LocalDate expectedDate) {
        if (expectedDate == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (expectedDate.isBefore(today)) {
            throw new BizException("期望发货日期不能早于今天");
        }
        if (expectedDate.isAfter(today.plusDays(90))) {
            throw new BizException("期望发货日期不能超过未来 90 天");
        }
    }

    private void ensureEditor(Order order, CurrentUser user) {
        if (user.isOps()) {
            return;
        }
        if (user.isMr() && order.getRepId().equals(user.id())) {
            return;
        }
        throw BizException.forbidden("无权修改该订单");
    }

    private void enrich(List<Order> orders) {
        if (orders.isEmpty()) {
            return;
        }
        Set<Long> customerIds = orders.stream().map(Order::getCustomerId).collect(Collectors.toSet());
        Set<Long> repIds = orders.stream().map(Order::getRepId).collect(Collectors.toSet());
        Set<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toSet());
        Map<Long, Customer> customers = customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<Long, User> reps = userMapper.selectBatchIds(repIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, BigDecimal> paid = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                        .in(Payment::getOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(Payment::getOrderId,
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));
        for (Order order : orders) {
            Customer customer = customers.get(order.getCustomerId());
            User rep = reps.get(order.getRepId());
            order.setCustomerName(customer == null ? "—" : customer.getName());
            order.setRepName(rep == null ? "—" : rep.getName());
            order.setStatusName(Constants.label(Constants.ORDER_STATUSES, order.getStatus()));
            order.setPaidAmount(paid.getOrDefault(order.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        }
    }
}
