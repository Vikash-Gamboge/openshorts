package com.proclipping.master.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Kotlinx Serialization DTOs matching the Pro Clipping Master / OpenShorts FastAPI backend.
 */
@Serializable
data class HealthResponse(
    val status: String = "ok",
    val version: String? = null,
    val gpu: Boolean? = null
)

@Serializable
data class BackendConfigResponse(
    val youtubeUrlEnabled: Boolean = true,
    val billingEnabled: Boolean = false,
    val authEnabled: Boolean = false,
    val googleAuthEnabled: Boolean = false,
    val aiConfigured: Boolean = true,
    val r2Configured: Boolean = false,
    val paymentsConfigured: Boolean = false,
    val originalRetentionHours: Int = 6,
    val jobRetentionSeconds: Int? = null,
    val localLlm: JsonElement? = null,
    @SerialName("max_file_size_mb") val maxFileSizeMb: Int? = null,
    @SerialName("max_clips_per_video") val maxClipsPerVideo: Int? = null
)

// --- Authentication & Account DTOs ---

@Serializable
data class AuthSignupRequest(
    val email: String,
    val password: String,
    @SerialName("display_name") val displayName: String? = null
)

@Serializable
data class AuthLoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthUserDto(
    val id: String,
    val email: String,
    @SerialName("display_name") val displayName: String? = null,
    val entitled: Boolean = true,
    val plan: String? = null
)

@Serializable
data class MinutesBalanceDto(
    @SerialName("plan_allowance") val planAllowance: Double = 20.0,
    @SerialName("plan_used") val planUsed: Double = 0.0,
    @SerialName("plan_remaining") val planRemaining: Double = 20.0,
    @SerialName("topup_remaining") val topupRemaining: Double = 0.0,
    val remaining: Double = 20.0
)

@Serializable
data class AuthResponseDto(
    val token: String,
    val user: AuthUserDto,
    val minutes: MinutesBalanceDto? = null
)

@Serializable
data class MeResponseDto(
    val user: AuthUserDto,
    val entitled: Boolean = true,
    val plan: String? = "free",
    val status: String? = null,
    val interval: String? = null,
    @SerialName("period_end") val periodEnd: String? = null,
    val minutes: MinutesBalanceDto? = null,
    @SerialName("storage_provider") val storageProvider: String? = null,
    @SerialName("original_retention_hours") val originalRetentionHours: Int? = null
)

@Serializable
data class LogoutResponseDto(
    val ok: Boolean = true
)

@Serializable
data class DeleteAccountRequestDto(
    val confirm: String,
    val reason: String? = "other"
)

@Serializable
data class DeleteAccountResponseDto(
    val deleted: Boolean = true
)

// --- Billing & Plans DTOs ---

@Serializable
data class BillingPlanDto(
    val kind: String = "subscription",
    @SerialName("price_id") val priceId: String = "",
    @SerialName("lookup_key") val lookupKey: String? = null,
    val plan: String,
    val interval: String = "month",
    val minutes: Int = 0,
    val amount: Int = 0,
    val currency: String = "usd",
    @SerialName("checkout_available") val checkoutAvailable: Boolean = false
)

@Serializable
data class BillingPlansResponseDto(
    val plans: List<BillingPlanDto> = emptyList(),
    @SerialName("payments_configured") val paymentsConfigured: Boolean = false
)

@Serializable
data class CheckoutRequestDto(
    @SerialName("price_id") val priceId: String,
    val source: String = "android_app"
)

@Serializable
data class CheckoutResponseDto(
    val url: String? = null
)

// --- Upload, Processing & Clip DTOs ---

@Serializable
data class CreateUploadRequest(
    val filename: String
)

@Serializable
data class CreateUploadResponse(
    @SerialName("upload_id") val uploadId: String,
    @SerialName("upload_url") val uploadUrl: String? = null,
    val method: String = "PUT",
    @SerialName("max_mb") val maxMb: Int? = null,
    @SerialName("expires_in") val expiresIn: Int? = null,
    val hint: String? = null
)

@Serializable
data class PutUploadResponse(
    @SerialName("upload_id") val uploadId: String? = null,
    val bytes: Long? = null,
    @SerialName("duration_seconds") val durationSeconds: Double? = null,
    val duration: Double? = null,
    val hint: String? = null
)

@Serializable
data class ProcessVideoJsonRequest(
    val url: String? = null,
    @SerialName("upload_id") val uploadId: String? = null,
    @SerialName("output_format") val outputFormat: String = "vertical",
    @SerialName("clip_min_seconds") val clipMinSeconds: Int? = null,
    @SerialName("clip_max_seconds") val clipMaxSeconds: Int? = null,
    @SerialName("target_clips") val targetClips: Int? = null,
    val captions: String? = null,
    val language: String? = null,
    @SerialName("auto_hook") val autoHook: Boolean = false,
    @SerialName("force_low_quality") val forceLowQuality: Boolean = false,
    val acknowledged: Boolean = true
)

@Serializable
data class ProcessJobResponse(
    @SerialName("job_id") val jobId: String? = null,
    val status: String = "queued",
    @SerialName("needs_confirmation") val needsConfirmation: Boolean = false,
    @SerialName("quality_check") val qualityCheck: JsonElement? = null,
    val partial: JsonElement? = null,
    @SerialName("first_video") val firstVideo: Boolean? = null,
    val message: String? = null
)

@Serializable
data class QueueInfoDto(
    val position: Int? = null,
    val ahead: Int? = null,
    @SerialName("eta_seconds") val etaSeconds: Int? = null,
    @SerialName("eta_is_minimum") val etaIsMinimum: Boolean? = null,
    @SerialName("estimated_wait_seconds") val estimatedWaitSeconds: Int? = null
) {
    fun resolvedEtaSeconds(): Int? = etaSeconds ?: estimatedWaitSeconds
}

@Serializable
data class JobStatusResponse(
    val status: String,
    val logs: List<String> = emptyList(),
    @SerialName("log_times") val logTimes: List<Double?>? = null,
    val result: JobResultDto? = null,
    val queue: QueueInfoDto? = null,
    val partial: JsonElement? = null,
    val error: String? = null
)

@Serializable
data class JobResultDto(
    val clips: List<ClipItemDto> = emptyList(),
    val shorts: List<ClipItemDto> = emptyList(),
    val title: String? = null,
    @SerialName("source_video") val sourceVideo: String? = null,
    @SerialName("cost_analysis") val costAnalysis: JsonElement? = null
) {
    fun resolvedClips(): List<ClipItemDto> = if (clips.isNotEmpty()) clips else shorts
}

@Serializable
data class ClipItemDto(
    val title: String? = null,
    @SerialName("video_title_for_youtube_short") val youtubeTitle: String? = null,
    @SerialName("viral_hook_text") val viralHookText: String? = null,
    @SerialName("video_description_for_tiktok") val tiktokDescription: String? = null,
    @SerialName("video_description_for_instagram") val instagramDescription: String? = null,
    val why: String? = null,
    @SerialName("predicted_score") val predictedScore: Int? = null,
    @SerialName("viral_score") val viralScore: Int? = null,
    val start: Double? = null,
    val end: Double? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    @SerialName("caption_style") val captionStyle: JsonElement? = null,
    val subtitled: Boolean? = null,
    @SerialName("captions_burned") val captionsBurned: Boolean? = null,
    val layout: String? = null,
    val language: String? = null
) {
    fun resolvedTitle(index: Int): String {
        return youtubeTitle?.takeIf { it.isNotBlank() }
            ?: title?.takeIf { it.isNotBlank() }
            ?: viralHookText?.takeIf { it.isNotBlank() }
            ?: "Highlight Clip #${index + 1}"
    }

    fun resolvedViralScore(): Int? = predictedScore ?: viralScore

    fun resolvedHookReason(): String {
        return why?.takeIf { it.isNotBlank() }
            ?: viralHookText?.takeIf { it.isNotBlank() }
            ?: tiktokDescription?.takeIf { it.isNotBlank() }
            ?: "Selected by AI highlight detection"
    }

    fun resolvedHasSubtitles(defaultAutoSubtitles: Boolean): Boolean {
        return captionsBurned
            ?: subtitled
            ?: (captionStyle != null)
            ?: defaultAutoSubtitles
    }
}

@Serializable
data class SubtitleRequestDto(
    @SerialName("job_id") val jobId: String,
    @SerialName("clip_index") val clipIndex: Int,
    val position: String? = null,
    val style: String? = null,
    val preset: String? = null,
    @SerialName("font_size") val fontSize: Int? = null,
    @SerialName("highlight_color") val highlightColor: String? = null
)

@Serializable
data class SubtitleResponseDto(
    val success: Boolean? = null,
    @SerialName("new_video_url") val newVideoUrl: String? = null,
    val status: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    val message: String? = null
) {
    fun resolvedVideoUrl(): String? {
        return newVideoUrl?.takeIf { it.isNotBlank() }
            ?: videoUrl?.takeIf { it.isNotBlank() }
            ?: downloadUrl?.takeIf { it.isNotBlank() }
    }
}

@Serializable
data class HistoryResponseDto(
    val videos: List<HistoryVideoDto> = emptyList()
)

@Serializable
data class HistoryVideoDto(
    val id: String,
    @SerialName("job_id") val jobId: String? = null,
    val title: String? = null,
    @SerialName("view_url") val viewUrl: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val duration: Double? = null
)

@Serializable
data class ProjectsResponseDto(
    val projects: List<ProjectSummaryDto> = emptyList()
)

@Serializable
data class ProjectSummaryDto(
    @SerialName("job_id") val jobId: String,
    val title: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("clip_count") val clipCount: Int? = null,
    val status: String? = null
)

@Serializable
data class FastApiErrorDto(
    val detail: JsonElement? = null,
    val error: String? = null,
    val message: String? = null
)
