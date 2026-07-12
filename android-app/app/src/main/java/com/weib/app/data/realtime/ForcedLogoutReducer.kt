package com.weib.app.data.realtime

data class SecurityState(val token: String?, val dialog: String?)
data class SecurityEvent(val reason: String)
object ForcedLogoutReducer {
    fun reduce(state: SecurityState, event: SecurityEvent): SecurityState = when (event.reason) {
        "KICKED" -> SecurityState(null, "你的账号已在其他设备登录，请检查密码是否泄露；如有风险，请立即修改密码。")
        "PASSWORD_CHANGED" -> SecurityState(null, "密码已修改，请重新登录。")
        "ACCOUNT_BANNED" -> SecurityState(null, "账号已被封禁。")
        "SESSION_EXPIRED" -> SecurityState(null, "登录状态已失效，请重新登录。")
        else -> state
    }
}
