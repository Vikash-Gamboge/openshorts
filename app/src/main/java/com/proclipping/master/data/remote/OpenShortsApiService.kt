package com.proclipping.master.data.remote

import com.proclipping.master.data.model.AuthLoginRequest
import com.proclipping.master.data.model.AuthResponseDto
import com.proclipping.master.data.model.AuthSignupRequest
import com.proclipping.master.data.model.BackendConfigResponse
import com.proclipping.master.data.model.BillingPlansResponseDto
import com.proclipping.master.data.model.CheckoutRequestDto
import com.proclipping.master.data.model.CheckoutResponseDto
import com.proclipping.master.data.model.CreateUploadRequest
import com.proclipping.master.data.model.CreateUploadResponse
import com.proclipping.master.data.model.DeleteAccountRequestDto
import com.proclipping.master.data.model.DeleteAccountResponseDto
import com.proclipping.master.data.model.HealthResponse
import com.proclipping.master.data.model.HistoryResponseDto
import com.proclipping.master.data.model.JobStatusResponse
import com.proclipping.master.data.model.LogoutResponseDto
import com.proclipping.master.data.model.MeResponseDto
import com.proclipping.master.data.model.ProcessJobResponse
import com.proclipping.master.data.model.ProcessVideoJsonRequest
import com.proclipping.master.data.model.ProjectsResponseDto
import com.proclipping.master.data.model.PutUploadResponse
import com.proclipping.master.data.model.SubtitleRequestDto
import com.proclipping.master.data.model.SubtitleResponseDto
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit contract for Pro Clipping Master's FastAPI backend.
 * Gemini AI authentication is handled strictly server-side via GEMINI_API_KEY;
 * no AI API keys are ever stored in or transmitted from the public Android client.
 */
interface OpenShortsApiService {

    @GET("health")
    suspend fun checkHealth(): Response<HealthResponse>

    @GET("api/config")
    suspend fun getConfig(): Response<BackendConfigResponse>

    // --- Authentication & Account ---

    @POST("api/auth/signup")
    suspend fun signUp(
        @Body request: AuthSignupRequest
    ): Response<AuthResponseDto>

    @POST("api/auth/login")
    suspend fun logIn(
        @Body request: AuthLoginRequest
    ): Response<AuthResponseDto>

    @POST("api/auth/logout")
    suspend fun logOut(): Response<LogoutResponseDto>

    @GET("api/me")
    suspend fun getAccountProfile(): Response<MeResponseDto>

    @HTTP(method = "DELETE", path = "api/account", hasBody = true)
    suspend fun deleteAccount(
        @Body request: DeleteAccountRequestDto
    ): Response<DeleteAccountResponseDto>

    // --- Billing & Plans ---

    @GET("api/billing/plans")
    suspend fun getBillingPlans(): Response<BillingPlansResponseDto>

    @POST("api/billing/checkout")
    suspend fun createCheckout(
        @Body request: CheckoutRequestDto
    ): Response<CheckoutResponseDto>

    // --- Video Upload, AI Processing & Clips ---

    @POST("api/uploads")
    suspend fun createUploadSlot(
        @Body request: CreateUploadRequest
    ): Response<CreateUploadResponse>

    @PUT("api/uploads/{uploadId}")
    suspend fun uploadVideoBytes(
        @Path("uploadId") uploadId: String,
        @Body videoBody: RequestBody
    ): Response<PutUploadResponse>

    @DELETE("api/uploads/{uploadId}")
    suspend fun deleteUploadSlot(
        @Path("uploadId") uploadId: String
    ): Response<Unit>

    @POST("api/process")
    suspend fun processVideo(
        @Body request: ProcessVideoJsonRequest
    ): Response<ProcessJobResponse>

    @GET("api/status/{jobId}")
    suspend fun getJobStatus(
        @Path("jobId") jobId: String
    ): Response<JobStatusResponse>

    @POST("api/subtitle")
    suspend fun applySubtitles(
        @Body request: SubtitleRequestDto
    ): Response<SubtitleResponseDto>

    @GET("api/history")
    suspend fun getHistory(): Response<HistoryResponseDto>

    @GET("api/projects")
    suspend fun getProjects(): Response<ProjectsResponseDto>
}
