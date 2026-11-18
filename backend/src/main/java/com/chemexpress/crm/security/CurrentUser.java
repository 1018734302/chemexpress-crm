package com.chemexpress.crm.security;

import com.chemexpress.crm.common.Constants;

public record CurrentUser(Long id, String username, String name, String role, String region, long dataVersion) {

    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(role);
    }

    public boolean isDsm() {
        return Constants.ROLE_DSM.equals(role);
    }

    public boolean isMr() {
        return Constants.ROLE_MR.equals(role);
    }

    public boolean isOps() {
        return Constants.ROLE_OPS.equals(role);
    }
}
