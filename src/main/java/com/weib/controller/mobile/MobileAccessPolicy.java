package com.weib.controller.mobile;

import com.weib.entity.User;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MobileAccessPolicy {
    public boolean hasRole(User user, String role) {
        return user != null && role.equals(user.getRole());
    }

    public boolean ownsCompanyResource(Long ownerCompanyId, Long resourceCompanyId) {
        return ownerCompanyId != null && Objects.equals(ownerCompanyId, resourceCompanyId);
    }
}
