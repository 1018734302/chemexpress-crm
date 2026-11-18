package com.chemexpress.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chemexpress.crm.entity.Order;
import com.chemexpress.crm.entity.Visit;
import com.chemexpress.crm.mapper.OrderMapper;
import com.chemexpress.crm.mapper.VisitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class CodeService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final VisitMapper visitMapper;
    private final OrderMapper orderMapper;

    public String customerCode(Long id) {
        return "CUS-%04d".formatted(id);
    }

    public synchronized String nextVisitCode(LocalDate date) {
        String prefix = "VIS-" + date.format(MONTH) + "-";
        Long count = visitMapper.selectCount(new LambdaQueryWrapper<Visit>().likeRight(Visit::getCode, prefix));
        return prefix + "%04d".formatted((count == null ? 0 : count) + 1);
    }

    public synchronized String nextOrderCode(LocalDate date) {
        String prefix = "SO-" + date.format(MONTH) + "-";
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<Order>().likeRight(Order::getCode, prefix));
        return prefix + "%04d".formatted((count == null ? 0 : count) + 1);
    }
}
