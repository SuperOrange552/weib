package com.weib.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtUtilSessionClaimsTest {
    @Test
    void tokenCarriesSidAndClientType() {
        JwtUtil jwt = new JwtUtil("12345678901234567890123456789012", new MockEnvironment());

        Claims claims = jwt.validateToken(jwt.generateToken(7L, "seeker_ahua", "BOSS", "sid-1", "MOBILE"));

        assertEquals("sid-1", jwt.getSid(claims));
        assertEquals("MOBILE", jwt.getClientType(claims));
        assertEquals("BOSS", jwt.getRole(claims));
    }
}
