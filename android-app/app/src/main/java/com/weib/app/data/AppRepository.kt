package com.weib.app.data

import android.content.Context
import com.google.gson.JsonElement
import com.weib.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class AppRepository(context: Context) {
    val session = SessionStore(context)
    private val api: WeibApi

    init {
        val builder = OkHttpClient.Builder()
            .cookieJar(AppCookieJar())
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    session.token()?.let { header("Authorization", "Bearer $it") }
                    if (chain.request().method != "GET" && chain.request().header("Idempotency-Key") == null) {
                        header("Idempotency-Key", UUID.randomUUID().toString())
                    }
                }.build()
                chain.proceed(request)
            }
        if (BuildConfig.DEBUG) builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        if (BuildConfig.DEBUG && BuildConfig.TRUST_LOCAL_CERT) trustLocalCertificate(builder)
        api = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(builder.build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(WeibApi::class.java)
    }

    suspend fun captcha(): CaptchaImage = withContext(Dispatchers.IO) {
        val response = api.captcha()
        if (!response.isSuccessful) error("验证码刷新过于频繁，请稍后再试")
        CaptchaImage(response.body()?.bytes() ?: error("验证码加载失败"),
            response.headers()["X-Captcha-Expires-In"]?.toIntOrNull() ?: 120)
    }

    suspend fun login(username: String, password: String, captcha: String): LoginData {
        val result = api.login(LoginRequest(username, password, captcha))
        if (result.code != 200 || result.data == null) error(result.msg ?: "登录失败")
        session.save(result.data.accessToken, result.data.user.role)
        return result.data
    }

    suspend fun load(role: String, route: String): ApiEnvelope<JsonElement> = when (route) {
        "jobs" -> api.jobs()
        "applications" -> api.applications()
        "dashboard" -> api.bossDashboard()
        "boss_jobs" -> api.bossJobs()
        "talent" -> api.bossApplications()
        "messages" -> if (role == "seeker") api.seekerConversations() else api.notifications()
        "forum" -> api.forumPosts()
        "profile" -> if (role == "seeker") api.resume() else api.bossCompany()
        else -> ApiEnvelope(400, "页面不存在", null)
    }

    suspend fun logout() { runCatching { api.logout() }; session.clear() }

    private fun trustLocalCertificate(builder: OkHttpClient.Builder) {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        builder.sslSocketFactory(context.socketFactory, trustManager).hostnameVerifier { _, _ -> true }
    }
}
