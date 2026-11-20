package com.proclipping.master.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ClipLanguage(
    val code: String,
    val displayName: String,
    val nativeBadge: String
) {
    ENGLISH("en", "English", "EN"),
    HINDI("hi", "हिन्दी · Hindi", "HI")
}

@Serializable
enum class ClipDurationOption(
    val seconds: Int,
    val minSeconds: Int,
    val maxSeconds: Int,
    val shortLabel: String,
    val descriptor: String
) {
    SEC_30(30, 20, 35, "30s", "Snappy Hook"),
    SEC_45(45, 35, 50, "45s", "Balanced"),
    SEC_60(60, 45, 65, "60s", "Standard Short"),
    SEC_90(90, 65, 95, "90s", "Deep Story")
}

@Serializable
enum class SubtitlePreset(
    val apiKey: String,
    val displayName: String,
    val styleHint: String
) {
    HORMOZI("hormozi", "Bold Viral", "High-contrast pop"),
    KARAOKE_POP("lime", "Karaoke Pop", "Active word glow"),
    PILL("pill", "Neon Pill", "Rounded caption pill"),
    ONE_WORD("oneword", "Single Word", "Rapid punch-in"),
    CLEAN("clean", "Minimal Clean", "Crisp broadcast")
}

@Serializable
enum class SourceType {
    LOCAL_MP4,
    YOUTUBE_URL
}

@Serializable
enum class ProcessingStage(
    val stepIndex: Int,
    val title: String,
    val subtitle: String
) {
    QUEUED(0, "Queued for Processing", "Preparing your video in the AI studio queue"),
    INGESTING(1, "Preparing Video", "Uploading and reading your source video"),
    TRANSCRIBING(2, "Analyzing Speech", "Generating accurate word-level timestamps"),
    DETECTING_HIGHLIGHTS(3, "Finding Best Moments", "Detecting viral hooks and strong story arcs"),
    REFRAMING_VERTICAL(4, "Framing 9:16 Vertical", "Auto-tracking subjects into vertical HD format"),
    BURNING_SUBTITLES(5, "Styling Subtitles", "Rendering crisp captions into your clips"),
    COMPLETED(6, "Your AI Clips Are Ready!", "All vertical 9:16 clips are ready to preview and download"),
    FAILED(-1, "Processing Interrupted", "Something went wrong — you can try again safely")
}

sealed interface ServiceAvailability {
    data object Checking : ServiceAvailability
    data class Ready(
        val authEnabled: Boolean = false,
        val youtubeUrlEnabled: Boolean = true,
        val aiConfigured: Boolean = true,
        val r2Configured: Boolean = false,
        val paymentsConfigured: Boolean = false,
        val originalRetentionHours: Int = 6
    ) : ServiceAvailability

    data class TemporarilyUnavailable(
        val message: String
    ) : ServiceAvailability
}

data class UserProfile(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val plan: String = "free",
    val creditsRemaining: Double = 20.0,
    val creditsAllowance: Double = 20.0,
    val creditsUsed: Double = 0.0,
    val storageProvider: String = "cloud",
    val originalRetentionHours: Int = 6
) {
    val isSignedIn: Boolean
        get() = id.isNotBlank() && email.isNotBlank()

    val greetingName: String
        get() = displayName.trim().ifBlank {
            email.substringBefore("@").replaceFirstChar { it.uppercase() }
        }

    val planDisplayName: String
        get() = when (plan.lowercase()) {
            "starter" -> "Starter"
            "creator" -> "Creator"
            "pro" -> "Pro"
            else -> "Free"
        }
}

data class SubscriptionPlanTier(
    val id: String,
    val name: String,
    val priceDisplay: String,
    val periodLabel: String,
    val creditsPerMonth: Int,
    val highlightTag: String? = null,
    val features: List<String>,
    val priceId: String = "",
    val checkoutAvailable: Boolean = false
)

sealed interface HistorySyncResult {
    data class CloudSynced(val jobs: List<ProcessingJob>) : HistorySyncResult
    data class LocalRefreshed(val refreshedJobsCount: Int) : HistorySyncResult
}

data class LocalVideoSelection(
    val uriString: String,
    val displayName: String,
    val sizeBytes: Long,
    val durationMs: Long
) {
    val formattedSize: String
        get() {
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) String.format("%.1f MB", mb) else "${(sizeBytes / 1024).coerceAtLeast(1)} KB"
        }

    val formattedDuration: String
        get() {
            if (durationMs <= 0L) return "MP4"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    SAVED,
    FAILED
}

@Serializable
data class GeneratedClip(
    val id: String,
    val index: Int,
    val jobId: String,
    val title: String,
    val hookReason: String,
    val viralScore: Int?,
    val startSeconds: Double,
    val endSeconds: Double,
    val videoUrl: String,
    val downloadUrl: String,
    val hasSubtitles: Boolean,
    val aspectRatio: String = "9:16",
    val language: ClipLanguage = ClipLanguage.ENGLISH
) {
    val durationSeconds: Int
        get() = (endSeconds - startSeconds).toInt().coerceAtLeast(1)

    val formattedDuration: String
        get() {
            val secs = durationSeconds
            return String.format("%02d:%02d", secs / 60, secs % 60)
        }
}

@Serializable
data class ProcessingJob(
    val id: String,
    val sourceTitle: String,
    val sourceValue: String,
    val sourceType: SourceType,
    val language: ClipLanguage,
    val durationOption: ClipDurationOption,
    val targetClips: Int,
    val autoSubtitles: Boolean,
    val subtitlePreset: SubtitlePreset,
    val stage: ProcessingStage,
    val progressFraction: Float,
    val uploadProgressFraction: Float? = null,
    val statusMessage: String,
    val totalClipsFound: Int = 0,
    val readyClipsCount: Int = 0,
    val clips: List<GeneratedClip> = emptyList(),
    val createdAtEpochMs: Long,
    val errorMessage: String? = null
)

val DEFAULT_PUBLIC_PLANS = listOf(
    SubscriptionPlanTier(
        id = "free",
        name = "Free",
        priceDisplay = "$0",
        periodLabel = "/month",
        creditsPerMonth = 20,
        features = listOf(
            "20 free AI processing credits monthly",
            "Automatic viral highlight detection",
            "9:16 vertical HD output & auto subtitles",
            "Original uploads auto-deleted after 6 hours"
        ),
        checkoutAvailable = false
    ),
    SubscriptionPlanTier(
        id = "starter",
        name = "Starter",
        priceDisplay = "$19",
        periodLabel = "/month",
        creditsPerMonth = 100,
        features = listOf(
            "100 AI processing credits monthly",
            "Watermark-free 9:16 HD downloads",
            "Durable cloud clip library",
            "Priority processing queue"
        ),
        checkoutAvailable = false
    ),
    SubscriptionPlanTier(
        id = "creator",
        name = "Creator",
        priceDisplay = "$39",
        periodLabel = "/month",
        creditsPerMonth = 300,
        highlightTag = "Most Popular",
        features = listOf(
            "300 AI processing credits monthly",
            "Watermark-free 9:16 HD exports",
            "All caption presets & Hindi/English AI",
            "Faster queue & cloud storage backup"
        ),
        checkoutAvailable = false
    ),
    SubscriptionPlanTier(
        id = "pro",
        name = "Pro",
        priceDisplay = "$79",
        periodLabel = "/month",
        creditsPerMonth = 750,
        features = listOf(
            "750 AI processing credits monthly",
            "Highest priority rendering queue",
            "Full cloud clip library & downloads",
            "Built for agencies & daily creators"
        ),
        checkoutAvailable = false
    )
)
