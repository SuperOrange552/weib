package com.weib.session;

public enum SessionInvalidationReason {
    KICKED,
    EXPIRED,
    PASSWORD_CHANGED,
    ACCOUNT_BANNED,
    ADMIN_FORCED
}
