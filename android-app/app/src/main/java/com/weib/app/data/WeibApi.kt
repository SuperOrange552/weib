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
import retrofit2.http.PUT
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface WeibApi {
    @GET("captcha") suspend fun captcha(@Query("_") nonce: Long): Response<ResponseBody>
    @POST("api/mobile/auth/login") suspend fun login(@Body body: LoginRequest): ApiEnvelope<LoginData>
    @GET("api/mobile/auth/me") suspend fun me(): ApiEnvelope<MobileUser>
    @POST("api/mobile/auth/logout") suspend fun logout(): ApiEnvelope<JsonElement>

    @GET("api/seeker/jobs") suspend fun jobs(
        @Query("page") page: Int, @Query("size") size: Int = 20,
        @Query("keyword") keyword: String = "", @Query("city") city: String = ""
    ): ApiEnvelope<JsonElement>
    @GET("api/mobile/boss/talents") suspend fun talents(
        @Query("page") page: Int, @Query("size") size: Int = 20, @Query("q") query: String = ""
    ): ApiEnvelope<JsonElement>
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
    @GET("api/chat/messages/{conversationId}") suspend fun chatMessages(
        @Path("conversationId") conversationId: String, @Query("sinceId") sinceId: Long = 0
    ): ApiEnvelope<JsonElement>
    @POST("api/chat/send") suspend fun sendMessage(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiEnvelope<JsonElement>
    @GET("api/mobile/resume-access/requests") suspend fun resumeAccessRequests(): ApiEnvelope<JsonElement>
    @POST("api/mobile/resume-access/requests") suspend fun requestResumeAccess(@Body body: Map<String, String>): ApiEnvelope<JsonElement>
    @POST("api/mobile/resume-access/requests/{id}/decision") suspend fun decideResumeAccess(@Path("id") id: Long, @Body body: Map<String, Boolean>): ApiEnvelope<JsonElement>
    @GET("api/mobile/resume-access/requests/{id}/resume") suspend fun authorizedResume(@Path("id") id: Long): ApiEnvelope<JsonElement>

    @GET("api/forum/posts") suspend fun forumPosts(@Query("page") page: Int = 0, @Query("size") size: Int = 20,
        @Query("q") query: String = "", @Query("sectionId") sectionId: Long? = null): ApiEnvelope<JsonElement>
    @GET("api/forum/sections") suspend fun forumSections(): ApiEnvelope<JsonElement>
    @POST("api/forum/posts") suspend fun createForumPost(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiEnvelope<JsonElement>
    @GET("api/forum/posts/{id}/comments") suspend fun forumComments(@Path("id") id: Long): ApiEnvelope<JsonElement>
    @POST("api/forum/posts/{id}/comments") suspend fun createForumComment(@Path("id") id: Long, @Body body: Map<String, String>): ApiEnvelope<JsonElement>
    @POST("api/forum/posts/{id}/like") suspend fun likeForumPost(@Path("id") id: Long): ApiEnvelope<JsonElement>
    @POST("api/forum/posts/{id}/favorite") suspend fun favoriteForumPost(@Path("id") id: Long): ApiEnvelope<JsonElement>
    @GET("api/complaints/mine") suspend fun myComplaints(): ApiEnvelope<JsonElement>
    @POST("api/complaints") suspend fun createComplaint(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiEnvelope<JsonElement>
    @GET("api/appeals/mine") suspend fun myAppeals(): ApiEnvelope<JsonElement>
    @POST("api/appeals") suspend fun createAppeal(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiEnvelope<JsonElement>
    @Multipart @POST("api/forum/media") suspend fun uploadForumImage(@Part file: MultipartBody.Part): ApiEnvelope<JsonElement>
    @Multipart @POST("api/appeals/evidence") suspend fun uploadEvidenceImage(@Part file: MultipartBody.Part): ApiEnvelope<JsonElement>
    @GET("api/mobile/notifications") suspend fun notificationEvents(
        @Query("afterEventId") afterEventId: Long, @Query("limit") limit: Int = 100
    ): ApiEnvelope<JsonElement>

    @GET("api/seeker/job/{id}") suspend fun jobDetail(@Path("id") id: String): ApiEnvelope<JsonElement>
    @GET("api/seeker/company/{id}") suspend fun companyDetail(@Path("id") id: String): ApiEnvelope<JsonElement>
    @POST("api/mobile/seeker/jobs/{id}/apply") suspend fun apply(@Path("id") id: String): ApiEnvelope<JsonElement>
    @POST("api/mobile/seeker/jobs/{id}/favorite") suspend fun toggleFavorite(@Path("id") id: String): ApiEnvelope<JsonElement>
    @POST("api/mobile/seeker/applications/{id}/withdraw") suspend fun withdraw(@Path("id") id: String): ApiEnvelope<JsonElement>
    @POST("api/seeker/resume") suspend fun saveResume(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiEnvelope<JsonElement>
    @Multipart @POST("api/seeker/resume/media") suspend fun uploadResumeMedia(
        @Part file: MultipartBody.Part, @Part("kind") kind: RequestBody
    ): ApiEnvelope<JsonElement>
    @POST("api/mobile/boss/applications/{id}/status") suspend fun updateApplicationStatus(
        @Path("id") id: String, @Body body: Map<String, String>): ApiEnvelope<JsonElement>
    @POST("api/mobile/boss/jobs/{id}/close") suspend fun closeJob(@Path("id") id: String): ApiEnvelope<JsonElement>
    @POST("api/mobile/boss/jobs/{id}/reopen") suspend fun reopenJob(@Path("id") id: String): ApiEnvelope<JsonElement>
    @POST("api/mobile/boss/jobs") suspend fun createJob(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiEnvelope<JsonElement>
    @PUT("api/mobile/boss/jobs/{id}") suspend fun updateJob(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any?>): ApiEnvelope<JsonElement>
}
