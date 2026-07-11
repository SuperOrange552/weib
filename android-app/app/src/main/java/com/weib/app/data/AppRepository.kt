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
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    val session = SessionStore(context)
    private val api: WeibApi
    private lateinit var httpClient: OkHttpClient
    private lateinit var realtime: com.weib.app.data.realtime.RealtimeClient
    private val _securityEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val securityEvents = _securityEvents.asSharedFlow()

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
                val response = chain.proceed(request)
                if (response.code == 401) {
                    val body = runCatching { response.peekBody(4096).string() }.getOrDefault("")
                    if (body.contains("KICKED")) _securityEvents.tryEmit("KICKED")
                }
                response
            }
        if (BuildConfig.DEBUG) builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        if (BuildConfig.DEBUG && BuildConfig.TRUST_LOCAL_CERT) trustLocalCertificate(builder)
        httpClient = builder.build()
        realtime = com.weib.app.data.realtime.RealtimeClient(httpClient, BuildConfig.API_BASE_URL,
            { reason -> _securityEvents.tryEmit(reason) },
            { id, type, title -> com.weib.app.data.notification.SystemNotificationFactory.show(appContext, id, type, title) })
        api = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(httpClient)
            .addConverterFactory(GsonConverterFactory.create()).build().create(WeibApi::class.java)
    }

    suspend fun captcha(): CaptchaImage = withContext(Dispatchers.IO) {
        val response = api.captcha()
        if (!response.isSuccessful) error("验证码刷新过于频繁，请稍后再试")
        CaptchaImage(response.body()?.bytes() ?: error("验证码加载失败"),
            response.headers()["X-Captcha-Expires-In"]?.toIntOrNull() ?: 120)
    }

    suspend fun login(username: String, password: String, captcha: String, selectedRole: String): LoginData {
        val result = api.login(LoginRequest(username, password, captcha, selectedRole))
        if (result.code != 200 || result.data == null) error(result.msg ?: "登录失败")
        session.save(result.data.accessToken, result.data.user.role)
        realtime.connect()
        return result.data
    }

    suspend fun load(role: String, route: String): ApiEnvelope<JsonElement> = when (route) {
        "jobs" -> api.jobs()
        "applications" -> api.applications()
        "dashboard" -> api.bossDashboard()
        "boss_jobs" -> api.bossJobs()
        "talent" -> api.bossApplications()
        "messages" -> if (role == "seeker") api.seekerConversations() else api.commonNotifications()
        "forum" -> api.forumPosts()
        "profile" -> if (role == "seeker") api.resume() else api.bossCompany()
        else -> ApiEnvelope(400, "页面不存在", null)
    }

    suspend fun logout() { realtime.disconnect(); runCatching { api.logout() }; session.clear() }

    fun connectRealtime() = realtime.connect()
    suspend fun notificationEvents(afterEventId: Long) = api.notificationEvents(afterEventId)
    suspend fun apply(jobId: String) = api.apply(jobId)
    suspend fun toggleFavorite(jobId: String) = api.toggleFavorite(jobId)
    suspend fun withdraw(applicationId: String) = api.withdraw(applicationId)
    suspend fun saveResume(fields: Map<String, Any?>) = api.saveResume(fields)
    suspend fun uploadResumeMedia(uri: Uri, kind: String): String = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val name = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        } ?: "upload"
        val temp = kotlin.io.path.createTempFile(appContext.cacheDir.toPath(), "resume-", "-" + name).toFile()
        resolver.openInputStream(uri)?.use { input -> temp.outputStream().use(input::copyTo) } ?: error("无法读取文件")
        try {
            val part = MultipartBody.Part.createFormData("file", name, temp.asRequestBody(mime.toMediaTypeOrNull()))
            val result = api.uploadResumeMedia(part, kind.toRequestBody("text/plain".toMediaTypeOrNull()))
            if (result.code != 200) error(result.msg ?: "上传失败")
            val url = result.data?.asJsonObject?.get("url")?.asString ?: error("上传结果无效")
            saveResume(mapOf((if (kind == "avatar") "avatar" else "attachmentPath") to url))
            url
        } finally { temp.delete() }
    }

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
