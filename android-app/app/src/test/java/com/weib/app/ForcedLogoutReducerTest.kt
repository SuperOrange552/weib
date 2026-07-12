package com.weib.app

import com.weib.app.data.realtime.*
import org.junit.Assert.*
import org.junit.Test

class ForcedLogoutReducerTest {
    @Test fun kickedClearsSessionAndShowsSecurityMessage() {
        val next = ForcedLogoutReducer.reduce(SecurityState("token", null), SecurityEvent("KICKED"))
        assertNull(next.token)
        assertEquals("你的账号已在其他设备登录，请检查密码是否泄露；如有风险，请立即修改密码。", next.dialog)
    }

    @Test fun expiredServerSessionClearsLocalLoginAndReturnsToLogin() {
        val next = ForcedLogoutReducer.reduce(SecurityState("stale-token", null), SecurityEvent("SESSION_EXPIRED"))
        assertNull(next.token)
        assertNotNull(next.dialog)
    }
}
