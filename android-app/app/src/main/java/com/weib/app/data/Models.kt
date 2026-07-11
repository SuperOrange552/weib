package com.weib.app.data

import com.google.gson.JsonElement

data class ApiEnvelope<T>(val code: Int, val msg: String?, val data: T?)
data class LoginRequest(val username: String, val password: String, val captcha: String, val selectedRole: String)
data class MobileUser(val id: Long, val username: String, val nickname: String?, val avatar: String?,
                      val role: String, val activeRole: String? = null)
data class LoginData(val accessToken: String, val tokenType: String, val expiresIn: Long, val user: MobileUser)
data class CaptchaImage(val bytes: ByteArray, val expiresIn: Int)
data class ContentState(val title: String = "", val data: JsonElement? = null, val loading: Boolean = false, val error: String? = null)
