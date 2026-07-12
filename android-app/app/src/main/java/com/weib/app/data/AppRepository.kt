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
                if (response.code == 401 && session.token() != null) {
                    val body = runCatching { response.peekBody(4096).string() }.getOrDefault("")
                    _securityEvents.tryEmit(if (body.contains("KICKED")) "KICKED" else "SESSION_EXPIRED")
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
        "jobs" -> api.jobs(0)
        "applications" -> api.applications()
        "dashboard" -> api.bossDashboard()
        "boss_jobs" -> api.bossJobs()
        "boss_applications" -> api.bossApplications()
        "talent" -> api.bossApplications()
        "messages" -> if (role == "seeker") api.seekerConversations() else api.bossApplications()
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

    suspend fun jobs(page: Int, keyword: String, city: String) = api.jobs(page, 20, keyword, city)
    suspend fun talents(page: Int, query: String) = api.talents(page, 20, query)
    suspend fun updateApplicationStatus(id: String, status: String) = api.updateApplicationStatus(id, mapOf("status" to status))
    suspend fun closeJob(id: String) = api.closeJob(id)
    suspend fun reopenJob(id: String) = api.reopenJob(id)
    suspend fun createJob(fields: Map<String, Any?>) = api.createJob(fields)
    suspend fun updateJob(id: String, fields: Map<String, Any?>) = api.updateJob(id, fields)
    suspend fun chatMessages(conversationId: String) = api.chatMessages(conversationId)
    suspend fun sendMessage(conversationId: String, content: String) = api.sendMessage(mapOf(
        "conversationId" to conversationId, "content" to content, "messageType" to "text",
        "clientMessageId" to UUID.randomUUID().toString()
    ))
    suspend fun resumeAccessRequests() = api.resumeAccessRequests()
    suspend fun requestResumeAccess(seekerId: String) = api.requestResumeAccess(mapOf("seekerId" to seekerId))
    suspend fun decideResumeAccess(id: Long, approved: Boolean) = api.decideResumeAccess(id, mapOf("approved" to approved))
    suspend fun authorizedResume(id: Long) = api.authorizedResume(id)
    suspend fun forumPosts(page: Int, query: String, sectionId: Long?) = api.forumPosts(page, 20, query, sectionId)
    suspend fun forumSections() = api.forumSections()
    suspend fun createForumPost(fields: Map<String, Any?>) = api.createForumPost(fields)
    suspend fun forumComments(id: Long) = api.forumComments(id)
    suspend fun createForumComment(id: Long, content: String) = api.createForumComment(id, mapOf("content" to content))
    suspend fun likeForumPost(id: Long) = api.likeForumPost(id)
    suspend fun favoriteForumPost(id: Long) = api.favoriteForumPost(id)
    suspend fun myComplaints() = api.myComplaints()
    suspend fun createComplaint(fields: Map<String, Any?>) = api.createComplaint(fields)
    suspend fun myAppeals() = api.myAppeals()
    suspend fun createAppeal(fields: Map<String, Any?>) = api.createAppeal(fields)

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
