package com.proclipping.master.data.repository

import android.content.Context
import android.net.Uri
import com.proclipping.master.data.local.PUBLIC_API_BASE_URL
import com.proclipping.master.data.local.SettingsDataStore
import com.proclipping.master.data.local.StudioPreferences
import com.proclipping.master.data.model.AuthLoginRequest
import com.proclipping.master.data.model.AuthSignupRequest
import com.proclipping.master.data.model.BillingPlanDto
import com.proclipping.master.data.model.CheckoutRequestDto
import com.proclipping.master.data.model.ClipDurationOption
import com.proclipping.master.data.model.ClipLanguage
import com.proclipping.master.data.model.CreateUploadRequest
import com.proclipping.master.data.model.DEFAULT_PUBLIC_PLANS
import com.proclipping.master.data.model.DeleteAccountRequestDto
import com.proclipping.master.data.model.GeneratedClip
import com.proclipping.master.data.model.HistorySyncResult
import com.proclipping.master.data.model.JobStatusResponse
import com.proclipping.master.data.model.LocalVideoSelection
import com.proclipping.master.data.model.ProcessVideoJsonRequest
import com.proclipping.master.data.model.ProcessingJob
import com.proclipping.master.data.model.ProcessingStage
import com.proclipping.master.data.model.ServiceAvailability
import com.proclipping.master.data.model.SourceType
import com.proclipping.master.data.model.SubscriptionPlanTier
import com.proclipping.master.data.model.SubtitlePreset
import com.proclipping.master.data.model.SubtitleRequestDto
import com.proclipping.master.data.model.UserProfile
import com.proclipping.master.data.remote.ContentUriProgressRequestBody
import com.proclipping.master.data.remote.OpenShortsApiClient
import com.proclipping.master.data.remote.OpenShortsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException

/**
 * Central public repository for Pro Clipping Master.
 * Connects over HTTPS to the deployed FastAPI backend with server-side Gemini AI,
 * per-user authentication, credits metering, and safe user-facing error handling.
 */
class ClippingRepository(
    private val context: Context,
    private val apiClient: OpenShortsApiClient,
    private val settingsDataStore: SettingsDataStore
) {
    val preferencesFlow: Flow<StudioPreferences> = settingsDataStore.preferencesFlow
    val recentJobsFlow: Flow<List<ProcessingJob>> = settingsDataStore.recentJobsFlow

    suspend fun purgeLegacySecrets() = settingsDataStore.purgeLegacySecrets()
    suspend fun saveLanguage(language: ClipLanguage) = settingsDataStore.saveLanguage(language)
    suspend fun saveDuration(duration: ClipDurationOption) = settingsDataStore.saveDuration(duration)
    suspend fun saveTargetClips(count: Int) = settingsDataStore.saveTargetClips(count)
    suspend fun saveAutoSubtitles(enabled: Boolean) = settingsDataStore.saveAutoSubtitles(enabled)
    suspend fun saveSubtitlePreset(preset: SubtitlePreset) = settingsDataStore.saveSubtitlePreset(preset)
    suspend fun removeLocalJob(jobId: String) = settingsDataStore.removeJob(jobId)

    suspend fun checkServiceAvailability(): ServiceAvailability = withContext(Dispatchers.IO) {
        val prefs = preferencesFlow.first()
        var lastError = "Service is warming up. Please try again shortly."

        for (attempt in 0..1) {
            try {
                val service = apiClient.getService(PUBLIC_API_BASE_URL, prefs.authToken)
                val healthResponse = service.checkHealth()
                if (!healthResponse.isSuccessful) {
                    val code = healthResponse.code()
                    val err = apiClient.parseError(healthResponse)
                    lastError = err.message
                    if (attempt == 0 && code in setOf(502, 503, 504)) {
                        delay(2000L)
                        continue
                    }
                    return@withContext ServiceAvailability.TemporarilyUnavailable(lastError)
                }

                val cfg = runCatching {
                    val res = service.getConfig()
                    if (res.isSuccessful) res.body() else null
                }.getOrNull()

                return@withContext ServiceAvailability.Ready(
                    authEnabled = cfg?.authEnabled ?: cfg?.billingEnabled ?: false,
                    youtubeUrlEnabled = cfg?.youtubeUrlEnabled ?: true,
                    aiConfigured = cfg?.aiConfigured ?: true,
                    r2Configured = cfg?.r2Configured ?: false,
                    paymentsConfigured = cfg?.paymentsConfigured ?: false,
                    originalRetentionHours = cfg?.originalRetentionHours ?: 6
                )
            } catch (t: Throwable) {
                val mapped = apiClient.mapNetworkFailure(t)
                lastError = mapped.message
                if (attempt == 0 && t is SocketTimeoutException) {
                    delay(1500L)
                    continue
                }
                break
            }
        }
        ServiceAvailability.TemporarilyUnavailable(lastError)
    }

    // --- Authentication & Account Management ---

    suspend fun signUp(
        displayName: String,
        email: String,
        password: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val service = apiClient.getService(PUBLIC_API_BASE_URL)
            val response = service.signUp(
                AuthSignupRequest(
                    email = email.trim(),
                    password = password,
                    displayName = displayName.trim().takeIf { it.isNotEmpty() }
                )
            )
            if (response.code() == 404) {
                return@withContext Result.failure(
                    OpenShortsException.ServerError(
                        503,
                        "Account registration is temporarily unavailable while the server finishes updating. Please try again shortly."
                    )
                )
            }
            if (!response.isSuccessful) {
                return@withContext Result.failure(apiClient.parseError(response))
            }
            val body = response.body()
                ?: return@withContext Result.failure(
                    OpenShortsException.ServerError(500, "Could not create your account right now.")
                )
            val profile = UserProfile(
                id = body.user.id,
                email = body.user.email,
                displayName = body.user.displayName.orEmpty(),
                plan = body.user.plan ?: "free",
                creditsRemaining = body.minutes?.remaining ?: 20.0,
                creditsAllowance = body.minutes?.planAllowance ?: 20.0,
                creditsUsed = body.minutes?.planUsed ?: 0.0
            )
            // Clear any previous user's local jobs on sign-up for strict user isolation
            settingsDataStore.clearAuthSessionAndLocalHistory()
            settingsDataStore.saveAuthSession(body.token, profile)
            Result.success(profile)
        } catch (t: Throwable) {
            Result.failure(apiClient.mapNetworkFailure(t))
        }
    }

    suspend fun logIn(
        email: String,
        password: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val service = apiClient.getService(PUBLIC_API_BASE_URL)
            val response = service.logIn(
                AuthLoginRequest(
                    email = email.trim(),
                    password = password
                )
            )
            if (response.code() == 404) {
                return@withContext Result.failure(
                    OpenShortsException.ServerError(
                        503,
                        "Account sign-in is temporarily unavailable while the server finishes updating. Please try again shortly."
                    )
                )
            }
            if (!response.isSuccessful) {
                return@withContext Result.failure(apiClient.parseError(response))
            }
            val body = response.body()
                ?: return@withContext Result.failure(
                    OpenShortsException.ServerError(500, "Could not sign you in right now.")
                )
            val profile = UserProfile(
                id = body.user.id,
                email = body.user.email,
                displayName = body.user.displayName.orEmpty(),
                plan = body.user.plan ?: "free",
                creditsRemaining = body.minutes?.remaining ?: 20.0,
                creditsAllowance = body.minutes?.planAllowance ?: 20.0,
                creditsUsed = body.minutes?.planUsed ?: 0.0
            )
            settingsDataStore.clearAuthSessionAndLocalHistory()
            settingsDataStore.saveAuthSession(body.token, profile)
            Result.success(profile)
        } catch (t: Throwable) {
            Result.failure(apiClient.mapNetworkFailure(t))
        }
    }

    suspend fun logOut(): Result<Unit> = withContext(Dispatchers.IO) {
        val prefs = preferencesFlow.first()
        if (prefs.authToken.isNotBlank()) {
            runCatching {
                apiClient.getService(PUBLIC_API_BASE_URL, prefs.authToken).logOut()
            }
        }
        settingsDataStore.clearAuthSessionAndLocalHistory()
        Result.success(Unit)
    }

    suspend fun refreshAccountProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        val prefs = preferencesFlow.first()
        if (prefs.authToken.isBlank()) {
            return@withContext Result.success(UserProfile())
        }
        try {
            val service = apiClient.getService(PUBLIC_API_BASE_URL, prefs.authToken)
            val response = service.getAccountProfile()
            if (response.code() == 401) {
                settingsDataStore.clearAuthSessionAndLocalHistory()
                return@withContext Result.failure(OpenShortsException.AuthenticationRequired("Your session expired. Please sign in again."))
            }
            if (!response.isSuccessful) {
                return@withContext Result.failure(apiClient.parseError(response))
            }
            val body = response.body()
                ?: return@withContext Result.success(prefs.userProfile)
            val updated = UserProfile(
                id = body.user.id,
                email = body.user.email,
                displayName = body.user.displayName ?: prefs.userProfile.displayName,
                plan = body.plan ?: "free",
                creditsRemaining = body.minutes?.remaining ?: 20.0,
                creditsAllowance = body.minutes?.planAllowance ?: 20.0,
                creditsUsed = body.minutes?.planUsed ?: 0.0,
                storageProvider = body.storageProvider ?: "cloud",
                originalRetentionHours = body.originalRetentionHours ?: 6
            )
            settingsDataStore.updateUserProfile(updated)
            Result.success(updated)
        } catch (t: Throwable) {
            Result.failure(apiClient.mapNetworkFailure(t))
        }
    }

    suspend fun deleteAccount(confirmEmail: String): Result<Unit> = withContext(Dispatchers.IO) {
        val prefs = preferencesFlow.first()
        if (prefs.authToken.isBlank()) {
            return@withContext Result.failure(OpenShortsException.AuthenticationRequired())
        }
        try {
            val service = apiClient.getService(PUBLIC_API_BASE_URL, prefs.authToken)
            val response = service.deleteAccount(
                DeleteAccountRequestDto(confirm = confirmEmail.trim())
            )
            if (!response.isSuccessful) {
                return@withContext Result.failure(apiClient.parseError(response))
            }
            settingsDataStore.clearAuthSessionAndLocalHistory()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(apiClient.mapNetworkFailure(t))
        }
    }

    // --- Billing & Subscription Plans ---

    suspend fun fetchBillingPlans(): Pair<List<SubscriptionPlanTier>, Boolean> = withContext(Dispatchers.IO) {
        val prefs = preferencesFlow.first()
        try {
            val service = apiClient.getService(PUBLIC_API_BASE_URL, prefs.authToken)
            val response = service.getBillingPlans()
            if (!response.isSuccessful) {
                return@withContext Pair(DEFAULT_PUBLIC_PLANS, false)
            }
            val body = response.body() ?: return@withContext Pair(DEFAULT_PUBLIC_PLANS, false)
            val monthlyRemote = body.plans
                .filter { it.interval.equals("month", ignoreCase = true) }
                .associateBy { it.plan.lowercase() }

            val merged = DEFAULT_PUBLIC_PLANS.map { tier ->
                val remote: BillingPlanDto? = monthlyRemote[tier.id]
                if (remote != null) {
                    val priceDollars = if (remote.amount > 0) "$${remote.amount / 100}" else tier.priceDisplay
                    tier.copy(
                        priceDisplay = priceDollars,
                        creditsPerMonth = if (remote.minutes > 0) remote.minutes else tier.creditsPerMonth,
                        priceId = remote.priceId,
                        checkoutAvailable = body.paymentsConfigured && remote.checkoutAvailable && remote.priceId.isNotBlank()
                    )
                } else {
                    tier
                }
            }
            Pair(merged, body.paymentsConfigured)
        } catch (_: Throwable) {
            Pair(DEFAULT_PUBLIC_PLANS, false)
        }
    }

    suspend fun startCheckout(priceId: String): Result<String> = withContext(Dispatchers.IO) {
        val prefs = preferencesFlow.first()
        if (prefs.authToken.isBlank()) {
            return@withContext Result.failure(OpenShortsException.AuthenticationRequired("Please sign in to upgrade your plan."))
        }
        try {
            val service = apiClient.getService(PUBLIC_API_BASE_URL, prefs.authToken)
            val response = service.createCheckout(CheckoutRequestDto(priceId = priceId))
            if (!response.isSuccessful) {
                return@withContext Result.failure(apiClient.parseError(response))
            }
            val checkoutUrl = response.body()?.url.orEmpty()
            if (checkoutUrl.isBlank()) {
                Result.failure(
                    OpenShortsException.ServerError(
                        503,
                        "Online plan checkout is currently being finalized. Please try again soon."
                    )
                )
            } else {
                Result.success(checkoutUrl)
            }
        } catch (t: Throwable) {
            Result.failure(apiClient.mapNetworkFailure(t))
        }
    }

    // --- Video Upload, AI Clip Generation & Polling ---

    suspend fun submitYouTubeJob(
        youtubeUrl: String,
        language: ClipLanguage,
        durationOption: ClipDurationOption,
        targetClips: Int,
        autoSubtitles: Boolean,
        subtitlePreset: SubtitlePreset
    ): Result<ProcessingJob> = withContext(Dispatchers.IO) {
        val prefs = preferencesFlow.first()
        val cleanUrl = youtubeUrl.trim()
        try {
            val service = apiClient.getService(PUBLIC_API_BASE_URL, prefs.authToken)
            val baseRequest = ProcessVideoJsonRequest(
                url = cleanUrl,
                outputFormat = "vertical",
                clipMinSeconds = durationOption.minSeconds,
                clipMaxSeconds = durationOption.maxSeconds,
                targetClips = targetClips,
                captions = if (autoSubtitles) "true" else "false",
                language = language.code,
                autoHook = false,
                forceLowQuality = false,
                acknowledged = true
            )

            var response = service.processVideo(request = baseRequest)
            if (!response.isSuccessful && response.code() in setOf(502, 503, 504)) {
                delay(2000L)
                response = service.processVideo(request = baseRequest)
            }
            if (!response.isSuccessful) {
                return@withContext Result.failure(apiClient.parseError(response))
            }

            var body = response.body()
            // If YouTube only provides <720p on this server, automatically confirm and proceed
            // instead of crashing on missing job_id.
            if (body?.needsConfirmation == true && body.jobId.isNullOrBlank()) {
                val forcedResponse = service.processVideo(
                    request = baseRequest.copy(forceLowQuality = true)
                )
                if (!forcedResponse.isSuccessful) {
                    return@withContext Result.failure(apiClient.parseError(forcedResponse))
                }
                body = forcedResponse.body()
            }

            val resolvedJobId = body?.jobId?.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(
                    OpenShortsException.ServerError(
                        500,
                        body?.message?.takeIf { it.isNotBlank() }
                            ?: "Could not start YouTube video processing. Please try again."
                    )
                )

            val job = ProcessingJob(
                id = resolvedJobId,
                sourceTitle = extractYouTubeLabel(cleanUrl),
                sourceValue = cleanUrl,
                sourceType = SourceType.YOUTUBE_URL,
                language = language,
                durationOption = durationOption,
                targetClips = targetClips,
                autoSubtitles = autoSubtitles,
                subtitlePreset = subtitlePreset,
                stage = ProcessingStage.QUEUED,
                progressFraction = 0.08f,
                statusMessage = "Your YouTube video is queued for AI highlight detection",
                createdAtEpochMs = System.currentTimeMillis()
            )
            settingsDataStore.upsertJob(job)
            Result.success(job)
        } catch (t: Throwable) {
            Result.failure(apiClient.mapNetworkFailure(t))
        }
    }

    suspend fun submitLocalVideoJob(
        video: LocalVideoSelection,
        language: ClipLanguage,
        durationOption: ClipDurationOption,
        targetClips: Int,
        autoSubtitles: Boolean,
        subtitlePreset: SubtitlePreset,
        onUploadProgress: (Float) -> Unit
    ): Result<ProcessingJob> = withContext(Dispatchers.IO) {
        val prefs = preferencesFlow.first()
        val videoUri = Uri.parse(video.uriString)

        // Pre-flight local validation before opening network upload
        val maxAllowedBytes = 2048L * 1024L * 1024L
        if (video.sizeBytes > maxAllowedBytes) {
            return@withContext Result.failure(
                OpenShortsException.ServerError(
                    413,
                    "This video exceeds the 2 GB upload limit. Please select a smaller video."
                )
            )
        }
        if (video.durationMs in 1L until 45_000L) {
            val secs = (video.durationMs / 1000L).coerceAtLeast(1L)
            return@withContext Result.failure(
                OpenShortsException.ServerError(
                    400,
                    "This video is only ${secs}s long. Clip generation needs at least 45 seconds of footage."
                )
            )
        }
        val canReadStream = runCatching {
            context.contentResolver.openInputStream(videoUri)?.use { true } ?: false
        }.getOrDefault(false)
        if (!canReadStream) {
            return@withContext Result.failure(
                OpenShortsException.ServerError(
                    400,
                    "Unable to read the selected video file. Please pick the video again from your gallery."
                )
            )
        }

        val resolvedMime = context.contentResolver.getType(videoUri)
            ?.takeIf { it.startsWith("video/") }
            ?: "video/mp4"
        val resolvedFilename = video.displayName.trim().ifBlank { "video.mp4" }

        var reservedUploadId: String? = null
        try {
            val service = apiClient.getService(PUBLIC_API_BASE_URL, prefs.authToken)

            // Step 1: Reserve an upload slot (with single cold-start retry)
            var slotResponse = try {
                service.createUploadSlot(CreateUploadRequest(filename = resolvedFilename))
            } catch (e: SocketTimeoutException) {
                delay(1500L)
                service.createUploadSlot(CreateUploadRequest(filename = resolvedFilename))
            }
            if (!slotResponse.isSuccessful && slotResponse.code() in setOf(502, 503, 504)) {
                delay(2000L)
                slotResponse = service.createUploadSlot(CreateUploadRequest(filename = resolvedFilename))
            }
            if (!slotResponse.isSuccessful) {
                return@withContext Result.failure(apiClient.parseError(slotResponse))
            }
            val slot = slotResponse.body()
                ?: return@withContext Result.failure(
                    OpenShortsException.ServerError(500, "Could not prepare video upload. Please try again.")
                )
            reservedUploadId = slot.uploadId

            val serverMaxMb = slot.maxMb ?: 2048
            if (video.sizeBytes > 0L && video.sizeBytes > serverMaxMb.toLong() * 1024L * 1024L) {
                runCatching { service.deleteUploadSlot(slot.uploadId) }
                return@withContext Result.failure(
                    OpenShortsException.ServerError(
                        413,
                        "This video exceeds the ${serverMaxMb} MB server upload limit."
                    )
                )
            }

            // Step 2: Stream local video bytes with live upload progress
            val body = ContentUriProgressRequestBody(
                contentResolver = context.contentResolver,
                uri = videoUri,
                totalBytes = video.sizeBytes,
                mimeType = resolvedMime,
                onProgress = onUploadProgress
            )
            val putResponse = service.uploadVideoBytes(slot.uploadId, body)
            if (!putResponse.isSuccessful) {
                runCatching { service.deleteUploadSlot(slot.uploadId) }
                val uploadErr = apiClient.parseError(putResponse)
                return@withContext Result.failure(
                    OpenShortsException.ServerError(
                        uploadErr.httpCode ?: putResponse.code(),
                        "Upload failed: ${uploadErr.message}"
                    )
                )
            }

            // Step 3: Start server-side AI highlight detection & 9:16 rendering
            val processResponse = service.processVideo(
                request = ProcessVideoJsonRequest(
                    uploadId = slot.uploadId,
                    outputFormat = "vertical",
                    clipMinSeconds = durationOption.minSeconds,
                    clipMaxSeconds = durationOption.maxSeconds,
                    targetClips = targetClips,
                    captions = if (autoSubtitles) "true" else "false",
                    language = language.code,
                    autoHook = false,
                    acknowledged = true
                )
            )
            if (!processResponse.isSuccessful) {
                runCatching { service.deleteUploadSlot(slot.uploadId) }
                val procErr = apiClient.parseError(processResponse)
                return@withContext Result.failure(
                    OpenShortsException.ServerError(
                        procErr.httpCode ?: processResponse.code(),
                        "Video uploaded, but AI processing could not start: ${procErr.message}"
                    )
                )
            }
            val jobRes = processResponse.body()
            val resolvedJobId = jobRes?.jobId?.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(
                    OpenShortsException.ServerError(500, "Video uploaded, but AI clipping could not start. Please try again.")
                )

            val job = ProcessingJob(
                id = resolvedJobId,
                sourceTitle = resolvedFilename,
                sourceValue = video.uriString,
                sourceType = SourceType.LOCAL_MP4,
                language = language,
                durationOption = durationOption,
                targetClips = targetClips,
                autoSubtitles = autoSubtitles,
                subtitlePreset = subtitlePreset,
                stage = ProcessingStage.QUEUED,
                progressFraction = 0.22f,
                uploadProgressFraction = 1f,
                statusMessage = "Upload complete · Finding the best moments in your video",
                createdAtEpochMs = System.currentTimeMillis()
            )
            settingsDataStore.upsertJob(job)
            Result.success(job)
        } catch (t: Throwable) {
            reservedUploadId?.let { id ->
                runCatching {
                    val svc = apiClient.getService(PUBLIC_API_BASE_URL, prefs.authToken)
                    svc.deleteUploadSlot(id)
                }
            }
            Result.failure(apiClient.mapNetworkFailure(t))
        }
    }

    suspend fun pollJobStatus(currentJob: ProcessingJob): Result<ProcessingJob> =
        withContext(Dispatchers.IO) {
            val prefs = preferencesFlow.first()
            try {
                val service = apiClient.getService(PUBLIC_API_BASE_URL, prefs.authToken)
                val response = service.getJobStatus(currentJob.id)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(apiClient.parseError(response))
                }
                val dto = response.body()
                    ?: return@withContext Result.failure(
                        OpenShortsException.ServerError(500, "Could not read clip progress.")
                    )

                val mapped = mapStatusResponse(currentJob, dto)
                settingsDataStore.upsertJob(mapped)
                Result.success(mapped)
            } catch (t: Throwable) {
                Result.failure(apiClient.mapNetworkFailure(t))
            }
        }

    suspend fun applySubtitlesToClip(
        jobId: String,
        clipIndex: Int,
        preset: SubtitlePreset
    ): Result<String> = withContext(Dispatchers.IO) {
        val prefs = preferencesFlow.first()
        try {
            val service = apiClient.getService(PUBLIC_API_BASE_URL, prefs.authToken)
            val response = service.applySubtitles(
                SubtitleRequestDto(
                    jobId = jobId,
                    clipIndex = clipIndex,
                    preset = preset.apiKey
                )
            )
            if (!response.isSuccessful) {
                return@withContext Result.failure(apiClient.parseError(response))
            }
            val body = response.body()
            val updatedUrl = apiClient.resolveMediaUrl(
                PUBLIC_API_BASE_URL,
                body?.resolvedVideoUrl()
            )

            if (updatedUrl.isNotBlank()) {
                val storedJob = recentJobsFlow.first().firstOrNull { it.id == jobId }
                if (storedJob != null) {
                    val updatedJob = storedJob.copy(
                        clips = storedJob.clips.map { clip ->
                            if (clip.index == clipIndex) {
                                clip.copy(
                                    videoUrl = updatedUrl,
                                    downloadUrl = updatedUrl,
                                    hasSubtitles = true
                                )
                            } else clip
                        }
                    )
                    settingsDataStore.upsertJob(updatedJob)
                }
            }

            Result.success(updatedUrl)
        } catch (t: Throwable) {
            Result.failure(apiClient.mapNetworkFailure(t))
        }
    }

    suspend fun syncHistoryFromCloud(): Result<HistorySyncResult> = withContext(Dispatchers.IO) {
        val prefs = preferencesFlow.first()
        try {
            val service = apiClient.getService(PUBLIC_API_BASE_URL, prefs.authToken)
            val cfg = runCatching {
                val res = service.getConfig()
                if (res.isSuccessful) res.body() else null
            }.getOrNull()

            if (cfg != null && !cfg.billingEnabled) {
                val refreshed = refreshLocalJobsFromStatusEndpoint()
                return@withContext Result.success(HistorySyncResult.LocalRefreshed(refreshed))
            }

            if (prefs.authToken.isBlank()) {
                val refreshed = refreshLocalJobsFromStatusEndpoint()
                return@withContext Result.success(HistorySyncResult.LocalRefreshed(refreshed))
            }

            val historyRes = service.getHistory()
            if (historyRes.code() == 404) {
                val refreshed = refreshLocalJobsFromStatusEndpoint()
                return@withContext Result.success(HistorySyncResult.LocalRefreshed(refreshed))
            }
            if (!historyRes.isSuccessful) {
                return@withContext Result.failure(apiClient.parseError(historyRes))
            }

            val projectsRes = runCatching { service.getProjects() }.getOrNull()
            val videos = historyRes.body()?.videos.orEmpty()
            val projectMap = projectsRes?.body()?.projects?.associateBy { it.jobId }.orEmpty()
            val grouped = videos.groupBy { it.jobId ?: it.id }

            val syncedJobs = grouped.map { (jobId, vids) ->
                val project = projectMap[jobId]
                val clips = vids.mapIndexed { idx, v ->
                    val viewUrl = apiClient.resolveMediaUrl(PUBLIC_API_BASE_URL, v.viewUrl)
                    val dlUrl = apiClient.resolveMediaUrl(PUBLIC_API_BASE_URL, v.downloadUrl ?: v.viewUrl)
                    GeneratedClip(
                        id = v.id,
                        index = idx,
                        jobId = jobId,
                        title = v.title ?: "AI Short #${idx + 1}",
                        hookReason = "Saved in your cloud clip library",
                        viralScore = null,
                        startSeconds = 0.0,
                        endSeconds = v.duration ?: 45.0,
                        videoUrl = viewUrl,
                        downloadUrl = dlUrl,
                        hasSubtitles = true,
                        language = prefs.language
                    )
                }
                ProcessingJob(
                    id = jobId,
                    sourceTitle = project?.title ?: vids.firstOrNull()?.title ?: "AI Clip Project",
                    sourceValue = "",
                    sourceType = SourceType.LOCAL_MP4,
                    language = prefs.language,
                    durationOption = prefs.durationOption,
                    targetClips = clips.size.coerceAtLeast(1),
                    autoSubtitles = true,
                    subtitlePreset = prefs.subtitlePreset,
                    stage = ProcessingStage.COMPLETED,
                    progressFraction = 1f,
                    statusMessage = "${clips.size} clip(s) saved in your library",
                    totalClipsFound = clips.size,
                    readyClipsCount = clips.size,
                    clips = clips,
                    createdAtEpochMs = System.currentTimeMillis()
                )
            }

            syncedJobs.forEach { settingsDataStore.upsertJob(it) }
            Result.success(HistorySyncResult.CloudSynced(syncedJobs))
        } catch (t: Throwable) {
            Result.failure(apiClient.mapNetworkFailure(t))
        }
    }

    private suspend fun refreshLocalJobsFromStatusEndpoint(): Int {
        val localJobs = recentJobsFlow.first()
        var refreshedCount = 0
        for (job in localJobs.take(10)) {
            if (pollJobStatus(job).isSuccess) {
                refreshedCount++
            }
        }
        return refreshedCount
    }

    private fun mapStatusResponse(
        current: ProcessingJob,
        dto: JobStatusResponse
    ): ProcessingJob {
        val statusLower = dto.status.lowercase()
        val logs = dto.logs

        // Interpret pipeline progress markers internally without exposing raw logs to public users
        val foundRegex = Regex("Found (\\d+) (?:viral )?clips", RegexOption.IGNORE_CASE)
        val readyRegex = Regex("Clip (\\d+) ready", RegexOption.IGNORE_CASE)
        var totalFound = current.totalClipsFound
        val readyIndices = mutableSetOf<String>()
        for (line in logs) {
            foundRegex.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { totalFound = it }
            readyRegex.find(line)?.groupValues?.getOrNull(1)?.let { readyIndices.add(it) }
        }
        val readyCount = if (totalFound > 0) readyIndices.size.coerceAtMost(totalFound) else readyIndices.size

        val clips = dto.result?.resolvedClips().orEmpty().mapIndexed { index, clip ->
            val videoUrl = apiClient.resolveMediaUrl(PUBLIC_API_BASE_URL, clip.videoUrl)
            val downloadUrl = apiClient.resolveMediaUrl(PUBLIC_API_BASE_URL, clip.downloadUrl ?: clip.videoUrl)
            val resolvedLanguage = when (clip.language?.lowercase()) {
                "hi", "hindi" -> ClipLanguage.HINDI
                "en", "english" -> ClipLanguage.ENGLISH
                else -> current.language
            }
            GeneratedClip(
                id = "${current.id}_clip_$index",
                index = index,
                jobId = current.id,
                title = clip.resolvedTitle(index),
                hookReason = clip.resolvedHookReason(),
                viralScore = clip.resolvedViralScore(),
                startSeconds = clip.start ?: 0.0,
                endSeconds = clip.end ?: ((clip.start ?: 0.0) + current.durationOption.seconds),
                videoUrl = videoUrl,
                downloadUrl = downloadUrl,
                hasSubtitles = clip.resolvedHasSubtitles(current.autoSubtitles),
                language = resolvedLanguage
            )
        }

        val combinedLog = logs.joinToString("\n").lowercase()
        val stage = when {
            statusLower in setOf("completed", "done", "finished", "success") -> ProcessingStage.COMPLETED
            statusLower in setOf("failed", "error", "cancelled") -> ProcessingStage.FAILED
            statusLower == "queued" -> ProcessingStage.QUEUED
            "subtitle" in combinedLog || "caption" in combinedLog -> ProcessingStage.BURNING_SUBTITLES
            "refram" in combinedLog || "vertical" in combinedLog || "crop" in combinedLog || readyCount > 0 ->
                ProcessingStage.REFRAMING_VERTICAL
            "viral" in combinedLog || "highlight" in combinedLog || "gemini" in combinedLog || totalFound > 0 ->
                ProcessingStage.DETECTING_HIGHLIGHTS
            "transcrib" in combinedLog || "whisper" in combinedLog -> ProcessingStage.TRANSCRIBING
            else -> ProcessingStage.INGESTING
        }

        val progress = when (stage) {
            ProcessingStage.QUEUED -> 0.10f
            ProcessingStage.INGESTING -> 0.24f
            ProcessingStage.TRANSCRIBING -> 0.42f
            ProcessingStage.DETECTING_HIGHLIGHTS -> 0.58f
            ProcessingStage.REFRAMING_VERTICAL -> {
                if (totalFound > 0) {
                    0.62f + (0.28f * (readyCount.toFloat() / totalFound.toFloat()))
                } else {
                    0.74f
                }
            }
            ProcessingStage.BURNING_SUBTITLES -> 0.90f
            ProcessingStage.COMPLETED -> 1.0f
            ProcessingStage.FAILED -> current.progressFraction
        }

        val queuePos = dto.queue?.position
        val queueEta = dto.queue?.resolvedEtaSeconds()
        val statusMsg = when {
            stage == ProcessingStage.FAILED ->
                "We couldn't finish processing this video. Please try again or choose another video."
            stage == ProcessingStage.COMPLETED ->
                "${clips.size} vertical 9:16 AI clip(s) ready to preview and download"
            stage == ProcessingStage.QUEUED && queuePos != null -> {
                if (queueEta != null && queueEta > 0) {
                    "Position #$queuePos in queue · Est. wait ~${queueEta}s"
                } else {
                    "Position #$queuePos in AI processing queue"
                }
            }
            totalFound > 0 -> "Creating clip $readyCount of $totalFound…"
            else -> stage.subtitle
        }

        val resolvedSourceTitle = dto.result?.title?.takeIf { it.isNotBlank() }
            ?: clips.firstOrNull()?.title?.takeIf { current.sourceTitle == "YouTube Video" }
            ?: current.sourceTitle

        return current.copy(
            sourceTitle = resolvedSourceTitle,
            stage = stage,
            progressFraction = progress,
            statusMessage = statusMsg,
            totalClipsFound = maxOf(totalFound, clips.size),
            readyClipsCount = if (stage == ProcessingStage.COMPLETED) clips.size else readyCount,
            clips = if (clips.isNotEmpty()) clips else current.clips,
            errorMessage = if (stage == ProcessingStage.FAILED) statusMsg else null
        )
    }

    private fun extractYouTubeLabel(url: String): String {
        val clean = url.trim()
        val idMatch = Regex("(?:v=|youtu\\.be/|shorts/)([A-Za-z0-9_-]{6,})").find(clean)
        val videoId = idMatch?.groupValues?.getOrNull(1)
        return if (videoId != null) "YouTube · $videoId" else "YouTube Video"
    }
}
