package com.weib.controller.mobile;

import com.weib.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.*;

class MobileAccessPolicyTest {
    private final MobileAccessPolicy policy = new MobileAccessPolicy();

    @Test
    void onlyAllowsRequestedAppRole() {
        assertTrue(policy.hasRole(session(user("seeker"), "SEEKER"), "seeker"));
        assertTrue(policy.hasRole(session(user("seeker"), "BOSS"), "boss"));
        assertFalse(policy.hasRole(session(user("boss"), "SEEKER"), "boss"));
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

    private MockHttpSession session(User user, String activeRole) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        session.setAttribute("activeRole", activeRole);
        return session;
    }
}
