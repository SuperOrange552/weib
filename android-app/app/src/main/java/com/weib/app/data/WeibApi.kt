package com.weib.app.data

import com.google.gson.JsonElement
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.http.DELETE

interface WeibApi {
    @GET("captcha") suspend fun captcha(): Response<ResponseBody>
    @POST("api/mobile/auth/login") suspend fun login(@Body body: LoginRequest): ApiEnvelope<LoginData>
    @GET("api/mobile/auth/me") suspend fun me(): ApiEnvelope<MobileUser>
    @POST("api/mobile/auth/logout") suspend fun logout(): ApiEnvelope<JsonElement>

    @GET("api/seeker/jobs?size=50") suspend fun jobs(): ApiEnvelope<JsonElement>
    @GET("api/seeker/applications") suspend fun applications(): ApiEnvelope<JsonElement>
    @GET("api/seeker/conversations") suspend fun seekerConversations(): ApiEnvelope<JsonElement>
    @GET("api/seeker/resume") suspend fun resume(): ApiEnvelope<JsonElement>
    @GET("api/seeker/favorites") suspend fun favorites(): ApiEnvelope<JsonElement>
    @GET("api/seeker/notifications") suspend fun notifications(): ApiEnvelope<JsonElement>
    @GET("api/mobile/common/notifications") suspend fun commonNotifications(): ApiEnvelope<JsonElement>

    @GET("api/mobile/boss/dashboard") suspend fun bossDashboard(): ApiEnvelope<JsonElement>
    @GET("api/mobile/boss/jobs") suspend fun bossJobs(): ApiEnvelope<JsonElement>
    @GET("api/mobile/boss/applications") suspend fun bossApplications(): ApiEnvelope<JsonElement>
    @GET("api/mobile/boss/company") suspend fun bossCompany(): ApiEnvelope<JsonElement>

    @GET("api/forum/posts") suspend fun forumPosts(): ApiEnvelope<JsonElement>
    @GET("api/mobile/notifications") suspend fun notificationEvents(
        @Query("afterEventId") afterEventId: Long, @Query("limit") limit: Int = 100
    ): ApiEnvelope<JsonElement>

    @GET("api/seeker/job/{id}") suspend fun jobDetail(@Path("id") id: String): ApiEnvelope<JsonElement>
    @GET("api/seeker/company/{id}") suspend fun companyDetail(@Path("id") id: String): ApiEnvelope<JsonElement>
    @POST("api/mobile/seeker/jobs/{id}/apply") suspend fun apply(@Path("id") id: String): ApiEnvelope<JsonElement>
    @POST("api/mobile/seeker/jobs/{id}/favorite") suspend fun toggleFavorite(@Path("id") id: String): ApiEnvelope<JsonElement>
    @POST("api/mobile/seeker/applications/{id}/withdraw") suspend fun withdraw(@Path("id") id: String): ApiEnvelope<JsonElement>
    @POST("api/seeker/resume") suspend fun saveResume(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiEnvelope<JsonElement>
}
