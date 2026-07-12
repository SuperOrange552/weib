package com.weib.app.data.realtime

object AuthFailurePolicy {
    fun securityEvent(httpCode: Int, requestPath: String, responseBody: String, hasLocalSession: Boolean): String? {
        if (!hasLocalSession || httpCode != 401 || requestPath.endsWith("/api/mobile/auth/login")) return null
        return if (responseBody.contains("KICKED")) "KICKED" else "SESSION_EXPIRED"
    }
    fun isUnauthorizedApiResult(code: Int): Boolean = code == 401
}
