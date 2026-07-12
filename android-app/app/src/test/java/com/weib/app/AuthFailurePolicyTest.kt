package com.weib.app

import com.weib.app.data.realtime.AuthFailurePolicy
import org.junit.Assert.*
import org.junit.Test

class AuthFailurePolicyTest {
    @Test fun anyProtectedApi401ExpiresSessionEvenWhenLocalTokenWasNotRestored() {
        assertTrue(AuthFailurePolicy.isUnauthorizedApiResult(401))
        assertEquals("SESSION_EXPIRED", AuthFailurePolicy.securityEvent(401, "/api/seeker/applications", "unauthorized", true))
    }

    @Test fun login401StaysOnLoginAndShowsLoginError() {
        assertNull(AuthFailurePolicy.securityEvent(401, "/api/mobile/auth/login", "bad credentials", false))
    }
}
