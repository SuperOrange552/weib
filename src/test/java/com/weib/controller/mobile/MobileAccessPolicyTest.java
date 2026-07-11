package com.weib.controller.mobile;

import com.weib.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobileAccessPolicyTest {
    private final MobileAccessPolicy policy = new MobileAccessPolicy();

    @Test
    void onlyAllowsRequestedAppRole() {
        assertTrue(policy.hasRole(user("seeker"), "seeker"));
        assertTrue(policy.hasRole(user("boss"), "boss"));
        assertFalse(policy.hasRole(user("admin"), "boss"));
        assertFalse(policy.hasRole(null, "seeker"));
    }

    @Test
    void verifiesCompanyResourceOwnership() {
        assertTrue(policy.ownsCompanyResource(11L, 11L));
        assertFalse(policy.ownsCompanyResource(11L, 12L));
        assertFalse(policy.ownsCompanyResource(null, 12L));
    }

    private User user(String role) {
        User user = new User();
        user.setRole(role);
        return user;
    }
}