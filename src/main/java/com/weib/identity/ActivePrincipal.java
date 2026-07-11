package com.weib.identity;

import java.security.Principal;

public record ActivePrincipal(Long userId, String activeRole) implements Principal {
    @Override public String getName() { return userId.toString(); }
}
