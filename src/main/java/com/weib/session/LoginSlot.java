package com.weib.session;

public record LoginSlot(
        String sid,
        String activeRole,
        String clientType,
        long issuedAt,
        String deviceIdHash
) {
}
