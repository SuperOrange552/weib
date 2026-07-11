package com.weib.identity;

import com.weib.entity.RoleProfile;
import com.weib.entity.User;
import com.weib.service.IdentityService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveIdentityResolver {
    private final IdentityService identityService;

    public ActiveIdentity require(HttpSession session, String expectedRole) {
        ActiveIdentity identity = current(session);
        if (!expectedRole.equalsIgnoreCase(identity.role())) {
            throw new AccessDeniedException("ROLE_MISMATCH");
        }
        return identity;
    }

    public ActiveIdentity current(HttpSession session) {
        if (session == null || !(session.getAttribute("user") instanceof User user)) {
            throw new AccessDeniedException("UNAUTHENTICATED");
        }
        String selectedRole = (String) session.getAttribute("activeRole");
        if (selectedRole == null) throw new AccessDeniedException("ROLE_MISMATCH");
        String activeRole = identityService.requireEnabledRole(user.getId(), selectedRole);
        RoleProfile profile = identityService.profile(user.getId(), activeRole).orElse(null);
        return new ActiveIdentity(user.getId(), activeRole,
                profile != null && profile.getNickname() != null ? profile.getNickname() : user.getNickname(),
                profile != null && profile.getAvatar() != null ? profile.getAvatar() : user.getAvatar());
    }

    public boolean hasRole(HttpSession session, String expectedRole) {
        try {
            require(session, expectedRole);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
