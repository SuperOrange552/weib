package com.weib.app.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class AppCookieJar : CookieJar {
    private val cookies = ConcurrentHashMap<String, Cookie>()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { cookie -> this.cookies["${cookie.domain}:${cookie.name}"] = cookie }
    }
    override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies.values
        .filter { it.expiresAt > System.currentTimeMillis() && it.matches(url) }
}
