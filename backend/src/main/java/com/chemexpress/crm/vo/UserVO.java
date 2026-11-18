package com.chemexpress.crm.vo;

import com.chemexpress.crm.common.Constants;
import com.chemexpress.crm.entity.User;

import java.time.LocalDateTime;

public record UserVO(
        Long id,
        String username,
        String name,
        String role,
        String roleName,
        String region,
        String phone,
        Boolean enabled,
        LocalDateTime createdAt
) {

    public static UserVO from(User user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getRole(),
                Constants.label(Constants.ROLES, user.getRole()),
                user.getRegion(),
                user.getPhone(),
                !Boolean.FALSE.equals(user.getEnabled()),
                user.getCreatedAt()
        );
    }
}
