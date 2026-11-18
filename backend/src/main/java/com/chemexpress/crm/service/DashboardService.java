package com.chemexpress.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chemexpress.crm.common.Constants;
import com.chemexpress.crm.entity.Customer;
import com.chemexpress.crm.entity.Order;
import com.chemexpress.crm.entity.Payment;
import com.chemexpress.crm.entity.Visit;
import com.chemexpress.crm.mapper.CustomerMapper;
import com.chemexpress.crm.mapper.OrderMapper;
import com.chemexpress.crm.mapper.PaymentMapper;
import com.chemexpress.crm.mapper.VisitMapper;
import com.chemexpress.crm.security.CurrentUser;
import com.chemexpress.crm.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CustomerMapper customerMapper;
    private final VisitMapper visitMapper;
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final ScopeService scopeService;
    private final VisitService visitService;
    private final OrderService orderService;
    private final EventService eventService;

    public DashboardVO build(CurrentUser user) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime start = monthStart.atStartOfDay();
        LocalDateTime end = monthStart.plusMonths(1).atStartOfDay();

        DashboardVO vo = new DashboardVO();
        vo.setKpis(buildKpis(user, monthStart, start, end));
        List<String> months = new ArrayList<>();
        List<BigDecimal> trend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate m = monthStart.minusMonths(i);
            months.add(m.format(MONTH));
            trend.add(monthSales(user, m.atStartOfDay(), m.plusMonths(1).atStartOfDay()));
        }
        vo.setMonths(months);
        vo.setSalesTrend(trend);
        vo.setCustomerTypes(typeDistribution(user));
        if (user.isMr() || user.isDsm()) {
            vo.setTodayVisits(visitService.page(null, null, null, today, today, null, null, 1, 8, user).records());
            vo.setRecentEvents(List.of());
        } else {
            vo.setRecentOrders(orderService.page(null, null, null, null, 1, 6, user).records());
            vo.setRecentEvents(eventService.recent(8));
        }
        return vo;
    }

    private List<DashboardVO.Kpi> buildKpis(CurrentUser user, LocalDate monthStart,
                                             LocalDateTime start, LocalDateTime end) {
        List<DashboardVO.Kpi> kpis = new ArrayList<>();
        if (user.isMr()) {
            long pending = pendingVisitRejected(user) + pendingDraftOrders(user);
            kpis.add(new DashboardVO.Kpi("sales", "本月销售额", money(monthSales(user, start, end)), "已发货与已完成订单"));
            kpis.add(new DashboardVO.Kpi("customers", "活跃客户", countActiveCustomers(user) + " 家", "合作中客户"));
            kpis.add(new DashboardVO.Kpi("visits", "本月拜访", monthApprovedVisits(user, monthStart) + " 次", "已通过拜访"));
            kpis.add(new DashboardVO.Kpi("pending", "待我处理", pending + " 项", "被驳回拜访与草稿订单"));
        } else if (user.isDsm()) {
            long visitPending = countVisits(user, Constants.VISIT_SUBMITTED);
            long orderPending = countOrders(user, Constants.ORDER_PENDING);
            kpis.add(new DashboardVO.Kpi("sales", "区域本月销售额", money(monthSales(user, start, end)), "本区域已发货与已完成"));
            kpis.add(new DashboardVO.Kpi("customers", "活跃客户", countActiveCustomers(user) + " 家", "本区域合作中客户"));
            kpis.add(new DashboardVO.Kpi("visits", "团队本月拜访", monthApprovedVisits(user, monthStart) + " 次", "已通过拜访"));
            kpis.add(new DashboardVO.Kpi("pending", "待我审批", (visitPending + orderPending) + " 项",
                    "拜访 " + visitPending + " / 订单 " + orderPending));
        } else if (user.isOps()) {
            BigDecimal shipped = monthShippedAmount(start, end);
            BigDecimal receivable = receivableAmount();
            BigDecimal received = monthReceived(start, end);
            kpis.add(new DashboardVO.Kpi("sales", "本月发货金额", money(shipped), "已发货订单金额"));
            kpis.add(new DashboardVO.Kpi("ship", "待发货订单", countOrders(user, Constants.ORDER_APPROVED) + " 单", "审批通过待发货"));
            kpis.add(new DashboardVO.Kpi("receivable", "待回款金额", money(receivable), "已发货订单未回款"));
            kpis.add(new DashboardVO.Kpi("received", "本月回款", money(received), "回款登记合计"));
        } else {
            kpis.add(new DashboardVO.Kpi("customers", "客户总数",
                    customerMapper.selectCount(new LambdaQueryWrapper<>()) + " 家", "全量客户"));
            kpis.add(new DashboardVO.Kpi("sales", "本月发货金额", money(monthShippedAmount(start, end)), "全量订单"));
            kpis.add(new DashboardVO.Kpi("visits", "本月拜访", monthApprovedVisits(user, monthStart) + " 次", "已通过拜访"));
            kpis.add(new DashboardVO.Kpi("pending", "待处理",
                    (countVisits(user, Constants.VISIT_SUBMITTED) + countOrders(user, Constants.ORDER_PENDING)) + " 项",
                    "待审批拜访与订单"));
        }
        return kpis;
    }

    private BigDecimal monthSales(CurrentUser user, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        scopeService.applyOrderScope(wrapper, user);
        wrapper.in(Order::getStatus, List.of(Constants.ORDER_SHIPPED, Constants.ORDER_COMPLETED))
                .ge(Order::getShippedAt, start)
                .lt(Order::getShippedAt, end);
        return sum(orderMapper.selectList(wrapper));
    }

    private BigDecimal monthShippedAmount(LocalDateTime start, LocalDateTime end) {
        return sum(orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, List.of(Constants.ORDER_SHIPPED, Constants.ORDER_COMPLETED))
                .ge(Order::getShippedAt, start)
                .lt(Order::getShippedAt, end)));
    }

    private BigDecimal monthReceived(LocalDateTime start, LocalDateTime end) {
        return paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                        .ge(Payment::getPayDate, start.toLocalDate())
                        .lt(Payment::getPayDate, end.toLocalDate()))
                .stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal receivableAmount() {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, List.of(Constants.ORDER_SHIPPED, Constants.ORDER_COMPLETED)));
        if (orders.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = sum(orders);
        BigDecimal paid = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                        .in(Payment::getOrderId, orders.stream().map(Order::getId).toList()))
                .stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.subtract(paid);
    }

    private long countActiveCustomers(CurrentUser user) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        scopeService.applyCustomerScope(wrapper, user);
        return customerMapper.selectCount(wrapper.eq(Customer::getStatus, Constants.CUSTOMER_STATUS_ACTIVE));
    }

    private long monthApprovedVisits(CurrentUser user, LocalDate monthStart) {
        LambdaQueryWrapper<Visit> wrapper = new LambdaQueryWrapper<>();
        scopeService.applyVisitScope(wrapper, user);
        return visitMapper.selectCount(wrapper.eq(Visit::getStatus, Constants.VISIT_APPROVED)
                .ge(Visit::getVisitDate, monthStart)
                .lt(Visit::getVisitDate, monthStart.plusMonths(1)));
    }

    private long countVisits(CurrentUser user, String status) {
        LambdaQueryWrapper<Visit> wrapper = new LambdaQueryWrapper<>();
        scopeService.applyVisitScope(wrapper, user);
        return visitMapper.selectCount(wrapper.eq(Visit::getStatus, status));
    }

    private long countOrders(CurrentUser user, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        scopeService.applyOrderScope(wrapper, user);
        return orderMapper.selectCount(wrapper.eq(Order::getStatus, status));
    }

    private long pendingVisitRejected(CurrentUser user) {
        return countVisits(user, Constants.VISIT_REJECTED);
    }

    private long pendingDraftOrders(CurrentUser user) {
        return countOrders(user, Constants.ORDER_DRAFT);
    }

    private List<DashboardVO.NameValue> typeDistribution(CurrentUser user) {
        List<DashboardVO.NameValue> result = new ArrayList<>();
        for (String type : List.of(Constants.CUSTOMER_TYPE_HOSPITAL, Constants.CUSTOMER_TYPE_CHAIN,
                Constants.CUSTOMER_TYPE_PHARMACY, Constants.CUSTOMER_TYPE_DISTRIBUTOR)) {
            LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
            scopeService.applyCustomerScope(wrapper, user);
            result.add(new DashboardVO.NameValue(Constants.label(Constants.CUSTOMER_TYPES, type),
                    customerMapper.selectCount(wrapper.eq(Customer::getType, type))));
        }
        return result;
    }

    private BigDecimal sum(List<Order> orders) {
        return orders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal value) {
        return "¥ " + String.format("%,.2f", value == null ? BigDecimal.ZERO : value);
    }
}
