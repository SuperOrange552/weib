package com.weib.controller.mobile;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MobileAccessPolicy {
    public boolean hasRole(HttpSession session, String role) {
        if (session == null || session.getAttribute("user") == null) return false;
        Object activeRole = session.getAttribute("activeRole");
        return activeRole != null && role.equalsIgnoreCase(activeRole.toString());
    }

    public boolean ownsCompanyResource(Long ownerCompanyId, Long resourceCompanyId) {
        return ownerCompanyId != null && Objects.equals(ownerCompanyId, resourceCompanyId);
    }
}
