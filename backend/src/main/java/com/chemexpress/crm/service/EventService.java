package com.chemexpress.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chemexpress.crm.entity.BusinessEvent;
import com.chemexpress.crm.mapper.BusinessEventMapper;
import com.chemexpress.crm.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final BusinessEventMapper eventMapper;

    public void record(String bizType, Long bizId, String action, String detail, CurrentUser user) {
        record(bizType, bizId, action, detail, user.id(), user.name());
    }

    public void record(String bizType, Long bizId, String action, String detail, Long operatorId, String operatorName) {
        BusinessEvent event = new BusinessEvent();
        event.setBizType(bizType);
        event.setBizId(bizId);
        event.setAction(action);
        event.setDetail(detail);
        event.setOperatorId(operatorId);
        event.setOperatorName(operatorName);
        event.setCreatedAt(LocalDateTime.now());
        eventMapper.insert(event);
    }

    public List<BusinessEvent> list(String bizType, Long bizId) {
        return eventMapper.selectList(new LambdaQueryWrapper<BusinessEvent>()
                .eq(BusinessEvent::getBizType, bizType)
                .eq(BusinessEvent::getBizId, bizId)
                .orderByDesc(BusinessEvent::getCreatedAt)
                .orderByDesc(BusinessEvent::getId));
    }

    public List<BusinessEvent> recent(int limit) {
        return eventMapper.selectList(new LambdaQueryWrapper<BusinessEvent>()
                .orderByDesc(BusinessEvent::getCreatedAt)
                .orderByDesc(BusinessEvent::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50))));
    }
}
