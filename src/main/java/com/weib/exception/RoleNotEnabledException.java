package com.weib.exception;

public class RoleNotEnabledException extends RuntimeException {
    private final String reason = "ROLE_NOT_ENABLED";

    public RoleNotEnabledException(String role) {
        super("账号未开通" + role + "身份");
    }

    public String getReason() {
        return reason;
    }
}
