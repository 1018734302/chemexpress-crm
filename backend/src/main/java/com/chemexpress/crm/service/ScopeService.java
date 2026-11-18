package com.chemexpress.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chemexpress.crm.common.BizException;
import com.chemexpress.crm.entity.Customer;
import com.chemexpress.crm.entity.Order;
import com.chemexpress.crm.entity.Visit;
import com.chemexpress.crm.mapper.CustomerMapper;
import com.chemexpress.crm.mapper.OrderMapper;
import com.chemexpress.crm.mapper.VisitMapper;
import com.chemexpress.crm.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScopeService {

    private final CustomerMapper customerMapper;
    private final VisitMapper visitMapper;
    private final OrderMapper orderMapper;

    public void applyCustomerScope(LambdaQueryWrapper<Customer> wrapper, CurrentUser user) {
        if (user.isMr()) {
            wrapper.eq(Customer::getOwnerId, user.id());
        } else if (user.isDsm()) {
            wrapper.eq(Customer::getRegion, user.region());
        }
    }

    public void applyVisitScope(LambdaQueryWrapper<Visit> wrapper, CurrentUser user) {
        if (user.isMr()) {
            wrapper.eq(Visit::getRepId, user.id());
        } else if (user.isDsm()) {
            wrapper.eq(Visit::getRegion, user.region());
        }
    }

    public void applyOrderScope(LambdaQueryWrapper<Order> wrapper, CurrentUser user) {
        if (user.isMr()) {
            wrapper.eq(Order::getRepId, user.id());
        } else if (user.isDsm()) {
            wrapper.eq(Order::getRegion, user.region());
        }
    }

    public Customer customer(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw BizException.notFound("客户不存在");
        }
        return customer;
    }

    public Customer visibleCustomer(Long id, CurrentUser user) {
        Customer customer = customer(id);
        ensureCustomerVisible(customer, user);
        return customer;
    }

    public void ensureCustomerVisible(Customer customer, CurrentUser user) {
        if (user.isMr() && !customer.getOwnerId().equals(user.id())) {
            throw BizException.forbidden("无权访问该客户");
        }
        if (user.isDsm() && !customer.getRegion().equals(user.region())) {
            throw BizException.forbidden("无权访问该客户");
        }
    }

    public void ensureCustomerWritable(Customer customer, CurrentUser user) {
        if (user.isAdmin() || user.isOps()) {
            throw BizException.forbidden("当前角色只能查看客户");
        }
        ensureCustomerVisible(customer, user);
    }

    public Visit visibleVisit(Long id, CurrentUser user) {
        Visit visit = visitMapper.selectById(id);
        if (visit == null) {
            throw BizException.notFound("拜访记录不存在");
        }
        if (user.isMr() && !visit.getRepId().equals(user.id())) {
            throw BizException.forbidden("无权访问该拜访");
        }
        if (user.isDsm() && !visit.getRegion().equals(user.region())) {
            throw BizException.forbidden("无权访问该拜访");
        }
        return visit;
    }

    public Order visibleOrder(Long id, CurrentUser user) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        if (user.isMr() && !order.getRepId().equals(user.id())) {
            throw BizException.forbidden("无权访问该订单");
        }
        if (user.isDsm() && !order.getRegion().equals(user.region())) {
            throw BizException.forbidden("无权访问该订单");
        }
        return order;
    }

    public void ensureDsmRegion(String region, CurrentUser user) {
        if (user.isDsm() && !region.equals(user.region())) {
            throw BizException.forbidden("只能操作本区域数据");
        }
    }
}
