package com.proclipping.master.ui.viewmodel

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proclipping.master.data.model.ClipDurationOption
import com.proclipping.master.data.model.ClipLanguage
import com.proclipping.master.data.model.DEFAULT_PUBLIC_PLANS
import com.proclipping.master.data.model.DownloadStatus
import com.proclipping.master.data.model.GeneratedClip
import com.proclipping.master.data.model.HistorySyncResult
import com.proclipping.master.data.model.LocalVideoSelection
import com.proclipping.master.data.model.ProcessingJob
import com.proclipping.master.data.model.ProcessingStage
import com.proclipping.master.data.model.ServiceAvailability
import com.proclipping.master.data.model.SourceType
import com.proclipping.master.data.model.SubscriptionPlanTier
import com.proclipping.master.data.model.SubtitlePreset
import com.proclipping.master.data.model.UserProfile
import com.proclipping.master.data.remote.OpenShortsException
import com.proclipping.master.data.repository.ClipDownloadManager
import com.proclipping.master.data.repository.ClippingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class HistoryFilter(val label: String) {
    ALL("All"),
    COMPLETED("Ready"),
    ACTIVE("Active"),
    FAILED("Failed")
}

enum class AuthMode {
    LOGIN,
    SIGNUP
}

data class StudioUiState(
    // Service & Auth
    val serviceAvailability: ServiceAvailability = ServiceAvailability.Checking,
    val userProfile: UserProfile = UserProfile(),
    val authTokenPresent: Boolean = false,
    val authMode: AuthMode = AuthMode.LOGIN,
    val authNameInput: String = "",
    val authEmailInput: String = "",
    val authPasswordInput: String = "",
    val isAuthSubmitting: Boolean = false,
    val authError: String? = null,
    val isDeletingAccount: Boolean = false,

    // Video Source & Clipping Preferences
    val sourceType: SourceType = SourceType.LOCAL_MP4,
    val youtubeUrl: String = "",
    val youtubeUrlError: String? = null,
    val selectedLocalVideo: LocalVideoSelection? = null,
    val language: ClipLanguage = ClipLanguage.ENGLISH,
    val durationOption: ClipDurationOption = ClipDurationOption.SEC_60,
    val targetClips: Int = 5,
    val autoSubtitles: Boolean = true,
    val subtitlePreset: SubtitlePreset = SubtitlePreset.HORMOZI,

    // Processing & Library
    val isSubmittingJob: Boolean = false,
    val uploadProgress: Float? = null,
    val activeJob: ProcessingJob? = null,
    val recentJobs: List<ProcessingJob> = emptyList(),
    val historyFilter: HistoryFilter = HistoryFilter.ALL,
    val isSyncingHistory: Boolean = false,
    val historySyncError: String? = null,
    val submissionError: String? = null,
    val downloadStates: Map<String, DownloadStatus> = emptyMap(),
    val isReburningSubtitles: Boolean = false,

    // Plans & Subscriptions
    val plans: List<SubscriptionPlanTier> = DEFAULT_PUBLIC_PLANS,
    val paymentsConfigured: Boolean = false,
    val isStartingCheckout: Boolean = false
) {
    val isSignedIn: Boolean
        get() = authTokenPresent && userProfile.isSignedIn

    val authRequiredByService: Boolean
        get() = (serviceAvailability as? ServiceAvailability.Ready)?.authEnabled == true

    val allGeneratedClips: List<GeneratedClip>
        get() = recentJobs
            .flatMap { it.clips }
            .distinctBy { it.id }

    val filteredHistory: List<ProcessingJob>
        get() = when (historyFilter) {
            HistoryFilter.ALL -> recentJobs
            HistoryFilter.COMPLETED -> recentJobs.filter { it.stage == ProcessingStage.COMPLETED }
            HistoryFilter.ACTIVE -> recentJobs.filter {
                it.stage != ProcessingStage.COMPLETED && it.stage != ProcessingStage.FAILED
            }
            HistoryFilter.FAILED -> recentJobs.filter { it.stage == ProcessingStage.FAILED }
        }
}

sealed interface StudioUiEvent {
    data class ShowMessage(val text: String) : StudioUiEvent
    data class NavigateToProcessing(val jobId: String) : StudioUiEvent
    data object NavigateToAuth : StudioUiEvent
    data object NavigateToUpgrade : StudioUiEvent
    data object AuthCompleted : StudioUiEvent
    data class OpenExternalUrl(val url: String) : StudioUiEvent
}

class StudioViewModel(
    private val repository: ClippingRepository,
    private val downloadManager: ClipDownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StudioUiEvent>()
    val events: SharedFlow<StudioUiEvent> = _events.asSharedFlow()

    private var pollingJob: Job? = null
    private var currentAuthToken: String = ""
    private var bootstrapped = false

    init {
        viewModelScope.launch { repository.purgeLegacySecrets() }
        observePreferences()
        observeRecentJobs()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            repository.preferencesFlow.collectLatest { prefs ->
                currentAuthToken = prefs.authToken
                _uiState.update { state ->
                    state.copy(
                        authTokenPresent = prefs.authToken.isNotBlank(),
                        userProfile = prefs.userProfile,
                        language = prefs.language,
                        durationOption = prefs.durationOption,
                        targetClips = prefs.targetClips,
                        autoSubtitles = prefs.autoSubtitles,
                        subtitlePreset = prefs.subtitlePreset
                    )
                }

                if (!bootstrapped) {
                    bootstrapped = true
                    bootstrapApp()
                }
            }
        }
    }

    private fun observeRecentJobs() {
        viewModelScope.launch {
            repository.recentJobsFlow.collectLatest { jobs ->
                _uiState.update { state ->
                    val updatedActive = state.activeJob?.let { active ->
                        jobs.firstOrNull { it.id == active.id } ?: active
                    }
                    state.copy(
                        recentJobs = jobs,
                        activeJob = updatedActive
                    )
                }
            }
        }
    }

    fun bootstrapApp() {
        viewModelScope.launch {
            _uiState.update { it.copy(serviceAvailability = ServiceAvailability.Checking) }
            val availability = repository.checkServiceAvailability()
            _uiState.update { it.copy(serviceAvailability = availability) }

            val (plans, paymentsReady) = repository.fetchBillingPlans()
            _uiState.update {
                it.copy(
                    plans = plans,
                    paymentsConfigured = paymentsReady
                )
            }

            if (currentAuthToken.isNotBlank()) {
                repository.refreshAccountProfile().onSuccess { profile ->
                    _uiState.update { it.copy(userProfile = profile) }
                }
                repository.syncHistoryFromCloud()
            }
        }
    }

    // --- Authentication Actions ---

    fun setAuthMode(mode: AuthMode) {
        _uiState.update { it.copy(authMode = mode, authError = null) }
    }

    fun updateAuthName(value: String) {
        _uiState.update { it.copy(authNameInput = value, authError = null) }
    }

    fun updateAuthEmail(value: String) {
        _uiState.update { it.copy(authEmailInput = value, authError = null) }
    }

    fun updateAuthPassword(value: String) {
        _uiState.update { it.copy(authPasswordInput = value, authError = null) }
    }

    fun submitAuth() {
        val state = _uiState.value
        if (state.isAuthSubmitting) return

        val email = state.authEmailInput.trim()
        val password = state.authPasswordInput
        val name = state.authNameInput.trim()

        if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
            _uiState.update { it.copy(authError = "Please enter a valid email address.") }
            return
        }
        if (password.length < 8) {
            _uiState.update { it.copy(authError = "Password must be at least 8 characters long.") }
            return
        }
        if (state.authMode == AuthMode.SIGNUP && name.isBlank()) {
            _uiState.update { it.copy(authError = "Please enter your name to create an account.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAuthSubmitting = true, authError = null) }
            val result = when (state.authMode) {
                AuthMode.SIGNUP -> repository.signUp(name, email, password)
                AuthMode.LOGIN -> repository.logIn(email, password)
            }
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isAuthSubmitting = false,
                            authPasswordInput = "",
                            authError = null,
                            userProfile = profile,
                            authTokenPresent = true
                        )
                    }
                    repository.syncHistoryFromCloud()
                    _events.emit(StudioUiEvent.ShowMessage("Welcome, ${profile.greetingName}!"))
                    _events.emit(StudioUiEvent.AuthCompleted)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isAuthSubmitting = false,
                            authError = err.message ?: "Authentication failed. Please try again."
                        )
                    }
                }
            )
        }
    }

    fun logOut() {
        pollingJob?.cancel()
        viewModelScope.launch {
            repository.logOut()
            _uiState.update {
                it.copy(
                    authTokenPresent = false,
                    userProfile = UserProfile(),
                    activeJob = null,
                    recentJobs = emptyList(),
                    authPasswordInput = "",
                    submissionError = null
                )
            }
            _events.emit(StudioUiEvent.ShowMessage("Signed out safely"))
        }
    }

    fun deleteAccount(confirmEmail: String) {
        val state = _uiState.value
        if (state.isDeletingAccount) return
        if (!confirmEmail.trim().equals(state.userProfile.email.trim(), ignoreCase = true)) {
            viewModelScope.launch {
                _events.emit(StudioUiEvent.ShowMessage("Please type your exact account email to confirm deletion."))
            }
            return
        }

        pollingJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true) }
            val result = repository.deleteAccount(confirmEmail)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            authTokenPresent = false,
                            userProfile = UserProfile(),
                            activeJob = null,
                            recentJobs = emptyList()
                        )
                    }
                    _events.emit(StudioUiEvent.ShowMessage("Your account and clips have been permanently deleted."))
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isDeletingAccount = false) }
                    _events.emit(StudioUiEvent.ShowMessage(err.message ?: "Could not delete account right now."))
                }
            )
        }
    }

    fun upgradeToPlan(tier: SubscriptionPlanTier) {
        val state = _uiState.value
        if (state.isStartingCheckout) return
        if (!tier.checkoutAvailable || tier.priceId.isBlank()) {
            viewModelScope.launch {
                _events.emit(
                    StudioUiEvent.ShowMessage("Online checkout is being finalized. Enjoy your free credits in the meantime!")
                )
            }
            return
        }
        if (!state.isSignedIn) {
            viewModelScope.launch {
                _events.emit(StudioUiEvent.ShowMessage("Sign in to upgrade your plan."))
                _events.emit(StudioUiEvent.NavigateToAuth)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isStartingCheckout = true) }
            val result = repository.startCheckout(tier.priceId)
            _uiState.update { it.copy(isStartingCheckout = false) }
            result.fold(
                onSuccess = { url -> _events.emit(StudioUiEvent.OpenExternalUrl(url)) },
                onFailure = { err ->
                    _events.emit(StudioUiEvent.ShowMessage(err.message ?: "Could not open checkout right now."))
                }
            )
        }
    }

    // --- Video Source & Clipping Preferences ---

    fun selectSourceType(type: SourceType) {
        _uiState.update {
            it.copy(
                sourceType = type,
                youtubeUrlError = null,
                submissionError = null
            )
        }
    }

    fun updateYouTubeUrl(url: String) {
        _uiState.update {
            it.copy(
                youtubeUrl = url,
                youtubeUrlError = null,
                submissionError = null
            )
        }
    }

    fun clearYouTubeUrl() {
        _uiState.update { it.copy(youtubeUrl = "", youtubeUrlError = null) }
    }

    fun onLocalVideoSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            val selection = withContext(Dispatchers.IO) {
                var displayName = "video.mp4"
                var sizeBytes = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex >= 0) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                        if (sizeIndex >= 0) {
                            sizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                }

                var durationMs = 0L
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    durationMs = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                } catch (_: Exception) {
                    durationMs = 0L
                } finally {
                    runCatching { retriever.release() }
                }

                LocalVideoSelection(
                    uriString = uri.toString(),
                    displayName = displayName,
                    sizeBytes = sizeBytes,
                    durationMs = durationMs
                )
            }

            _uiState.update {
                it.copy(
                    sourceType = SourceType.LOCAL_MP4,
                    selectedLocalVideo = selection,
                    submissionError = null
                )
            }
        }
    }

    fun clearLocalVideo() {
        _uiState.update { it.copy(selectedLocalVideo = null) }
    }

    fun selectLanguage(language: ClipLanguage) {
        _uiState.update { it.copy(language = language) }
        viewModelScope.launch { repository.saveLanguage(language) }
    }

    fun selectDuration(duration: ClipDurationOption) {
        _uiState.update { it.copy(durationOption = duration) }
        viewModelScope.launch { repository.saveDuration(duration) }
    }

    fun updateTargetClips(count: Int) {
        val clamped = count.coerceIn(1, 15)
        _uiState.update { it.copy(targetClips = clamped) }
        viewModelScope.launch { repository.saveTargetClips(clamped) }
    }

    fun toggleAutoSubtitles(enabled: Boolean) {
        _uiState.update { it.copy(autoSubtitles = enabled) }
        viewModelScope.launch { repository.saveAutoSubtitles(enabled) }
    }

    fun selectSubtitlePreset(preset: SubtitlePreset) {
        _uiState.update { it.copy(subtitlePreset = preset) }
        viewModelScope.launch { repository.saveSubtitlePreset(preset) }
    }

    fun clearSubmissionError() {
        _uiState.update { it.copy(submissionError = null) }
    }

    fun generateClips() {
        val state = _uiState.value
        // Prevent duplicate submissions while a request is already in flight
        if (state.isSubmittingJob) return

        // Validate input source
        when (state.sourceType) {
            SourceType.YOUTUBE_URL -> {
                val url = state.youtubeUrl.trim()
                if (url.isEmpty()) {
                    _uiState.update {
                        it.copy(youtubeUrlError = "Paste a YouTube video link to generate clips.")
                    }
                    return
                }
                if (!isLikelyYouTubeUrl(url)) {
                    _uiState.update {
                        it.copy(youtubeUrlError = "Please enter a valid youtube.com or youtu.be video link.")
                    }
                    return
                }
            }
            SourceType.LOCAL_MP4 -> {
                if (state.selectedLocalVideo == null) {
                    _uiState.update {
                        it.copy(submissionError = "Please select a video from your device first.")
                    }
                    return
                }
            }
        }

        // If cloud auth is enabled on the backend, ensure the user is signed in
        if (state.authRequiredByService && !state.isSignedIn) {
            viewModelScope.launch {
                _events.emit(StudioUiEvent.ShowMessage("Please sign in to generate AI clips."))
                _events.emit(StudioUiEvent.NavigateToAuth)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmittingJob = true,
                    uploadProgress = if (state.sourceType == SourceType.LOCAL_MP4) 0f else null,
                    submissionError = null
                )
            }

            // Warm up / verify service if not yet marked Ready
            if (state.serviceAvailability !is ServiceAvailability.Ready) {
                val recheck = repository.checkServiceAvailability()
                _uiState.update { it.copy(serviceAvailability = recheck) }
                if (recheck is ServiceAvailability.TemporarilyUnavailable) {
                    _uiState.update {
                        it.copy(
                            isSubmittingJob = false,
                            uploadProgress = null,
                            submissionError = recheck.message
                        )
                    }
                    return@launch
                }
            }

            val result = when (state.sourceType) {
                SourceType.YOUTUBE_URL -> repository.submitYouTubeJob(
                    youtubeUrl = state.youtubeUrl,
                    language = state.language,
                    durationOption = state.durationOption,
                    targetClips = state.targetClips,
                    autoSubtitles = state.autoSubtitles,
                    subtitlePreset = state.subtitlePreset
                )
                SourceType.LOCAL_MP4 -> repository.submitLocalVideoJob(
                    video = state.selectedLocalVideo!!,
                    language = state.language,
                    durationOption = state.durationOption,
                    targetClips = state.targetClips,
                    autoSubtitles = state.autoSubtitles,
                    subtitlePreset = state.subtitlePreset,
                    onUploadProgress = { fraction ->
                        _uiState.update { it.copy(uploadProgress = fraction) }
                    }
                )
            }

            result.fold(
                onSuccess = { job ->
                    _uiState.update {
                        it.copy(
                            isSubmittingJob = false,
                            uploadProgress = null,
                            activeJob = job,
                            submissionError = null
                        )
                    }
                    if (currentAuthToken.isNotBlank()) {
                        repository.refreshAccountProfile().onSuccess { profile ->
                            _uiState.update { it.copy(userProfile = profile) }
                        }
                    }
                    startPollingJob(job)
                    _events.emit(StudioUiEvent.NavigateToProcessing(job.id))
                },
                onFailure = { error ->
                    val message = error.message ?: "We couldn't start processing right now. Please try again."
                    _uiState.update {
                        it.copy(
                            isSubmittingJob = false,
                            uploadProgress = null,
                            submissionError = message
                        )
                    }
                    when (error) {
                        is OpenShortsException.AuthenticationRequired -> {
                            _events.emit(StudioUiEvent.ShowMessage(message))
                            _events.emit(StudioUiEvent.NavigateToAuth)
                        }
                        is OpenShortsException.CreditsExhausted -> {
                            _events.emit(StudioUiEvent.ShowMessage(message))
                            _events.emit(StudioUiEvent.NavigateToUpgrade)
                        }
                        else -> Unit
                    }
                }
            )
        }
    }

    fun openJob(jobId: String) {
        val job = _uiState.value.recentJobs.firstOrNull { it.id == jobId }
            ?: _uiState.value.activeJob?.takeIf { it.id == jobId }
            ?: return

        _uiState.update { it.copy(activeJob = job) }
        if (job.stage != ProcessingStage.COMPLETED && job.stage != ProcessingStage.FAILED) {
            startPollingJob(job)
        }
    }

    fun retryActiveJob() {
        val job = _uiState.value.activeJob ?: return
        if (job.sourceType == SourceType.YOUTUBE_URL && job.sourceValue.isNotBlank()) {
            _uiState.update {
                it.copy(
                    sourceType = SourceType.YOUTUBE_URL,
                    youtubeUrl = job.sourceValue,
                    language = job.language,
                    durationOption = job.durationOption,
                    targetClips = job.targetClips,
                    autoSubtitles = job.autoSubtitles,
                    subtitlePreset = job.subtitlePreset
                )
            }
            generateClips()
        } else {
            startPollingJob(job.copy(stage = ProcessingStage.QUEUED, errorMessage = null))
        }
    }

    fun refreshActiveJobOnce() {
        val job = _uiState.value.activeJob ?: return
        viewModelScope.launch {
            repository.pollJobStatus(job).fold(
                onSuccess = { updated ->
                    _uiState.update { it.copy(activeJob = updated) }
                },
                onFailure = { err ->
                    _events.emit(StudioUiEvent.ShowMessage(err.message ?: "Could not refresh clip status."))
                }
            )
        }
    }

    private fun startPollingJob(initialJob: ProcessingJob) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var current = initialJob
            var consecutiveFailures = 0

            while (isActive) {
                val res = repository.pollJobStatus(current)
                res.fold(
                    onSuccess = { updated ->
                        consecutiveFailures = 0
                        current = updated
                        _uiState.update { it.copy(activeJob = updated) }
                        if (updated.stage == ProcessingStage.COMPLETED) {
                            if (currentAuthToken.isNotBlank()) {
                                repository.refreshAccountProfile().onSuccess { profile ->
                                    _uiState.update { it.copy(userProfile = profile) }
                                }
                            }
                            _events.emit(
                                StudioUiEvent.ShowMessage(
                                    "${updated.clips.size} AI clip(s) are ready to preview & download!"
                                )
                            )
                            return@launch
                        }
                        if (updated.stage == ProcessingStage.FAILED) {
                            if (currentAuthToken.isNotBlank()) {
                                repository.refreshAccountProfile().onSuccess { profile ->
                                    _uiState.update { it.copy(userProfile = profile) }
                                }
                            }
                            return@launch
                        }
                    },
                    onFailure = { err ->
                        consecutiveFailures++
                        if (consecutiveFailures >= 5) {
                            val failed = current.copy(
                                stage = ProcessingStage.FAILED,
                                statusMessage = "Connection interrupted while tracking progress",
                                errorMessage = err.message ?: "Please check your internet connection and tap Try Again."
                            )
                            _uiState.update { it.copy(activeJob = failed) }
                            return@launch
                        }
                    }
                )
                delay(if (consecutiveFailures > 0) 3500L else 2500L)
            }
        }
    }

    fun downloadClip(clip: GeneratedClip) {
        _uiState.update {
            it.copy(downloadStates = it.downloadStates + (clip.id to DownloadStatus.DOWNLOADING))
        }
        val result = downloadManager.enqueueDownload(clip, currentAuthToken)
        viewModelScope.launch {
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(downloadStates = it.downloadStates + (clip.id to DownloadStatus.SAVED))
                    }
                    _events.emit(StudioUiEvent.ShowMessage("Downloading ${clip.title} to your gallery"))
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(downloadStates = it.downloadStates + (clip.id to DownloadStatus.FAILED))
                    }
                    _events.emit(StudioUiEvent.ShowMessage(err.message ?: "Download could not be started."))
                }
            )
        }
    }

    fun reburnSubtitles(jobId: String, clipIndex: Int, preset: SubtitlePreset) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReburningSubtitles = true) }
            val result = repository.applySubtitlesToClip(jobId, clipIndex, preset)
            _uiState.update { it.copy(isReburningSubtitles = false) }
            result.fold(
                onSuccess = { updatedUrl ->
                    if (updatedUrl.isNotBlank()) {
                        _uiState.update { state ->
                            val updatedActive = state.activeJob?.let { job ->
                                if (job.id == jobId) {
                                    job.copy(
                                        clips = job.clips.map { clip ->
                                            if (clip.index == clipIndex) {
                                                clip.copy(
                                                    videoUrl = updatedUrl,
                                                    downloadUrl = updatedUrl,
                                                    hasSubtitles = true
                                                )
                                            } else clip
                                        }
                                    )
                                } else job
                            }
                            state.copy(activeJob = updatedActive)
                        }
                    }
                    _events.emit(StudioUiEvent.ShowMessage("Caption style updated (${preset.displayName})"))
                },
                onFailure = { err ->
                    _events.emit(StudioUiEvent.ShowMessage(err.message ?: "Could not update captions right now."))
                }
            )
        }
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _uiState.update { it.copy(historyFilter = filter) }
    }

    fun syncHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingHistory = true, historySyncError = null) }
            val result = repository.syncHistoryFromCloud()
            result.fold(
                onSuccess = { outcome ->
                    _uiState.update { it.copy(isSyncingHistory = false, historySyncError = null) }
                    when (outcome) {
                        is HistorySyncResult.CloudSynced -> {
                            _events.emit(
                                StudioUiEvent.ShowMessage(
                                    "Library refreshed (${outcome.jobs.size} project${if (outcome.jobs.size == 1) "" else "s"})"
                                )
                            )
                        }
                        is HistorySyncResult.LocalRefreshed -> {
                            _events.emit(StudioUiEvent.ShowMessage("Clip history is up to date"))
                        }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSyncingHistory = false,
                            historySyncError = err.message ?: "Unable to refresh clip history right now."
                        )
                    }
                }
            )
        }
    }

    fun deleteHistoryJob(jobId: String) {
        viewModelScope.launch {
            repository.removeLocalJob(jobId)
            if (_uiState.value.activeJob?.id == jobId) {
                _uiState.update { it.copy(activeJob = null) }
            }
        }
    }

    fun findClip(jobId: String, clipIndex: Int): GeneratedClip? {
        val fromActive = _uiState.value.activeJob
            ?.takeIf { it.id == jobId }
            ?.clips
            ?.firstOrNull { it.index == clipIndex }
        if (fromActive != null) return fromActive

        return _uiState.value.recentJobs
            .firstOrNull { it.id == jobId }
            ?.clips
            ?.firstOrNull { it.index == clipIndex }
    }

    private fun isLikelyYouTubeUrl(url: String): Boolean {
        val lower = url.lowercase()
        return (lower.startsWith("http://") || lower.startsWith("https://")) &&
            ("youtube.com/" in lower || "youtu.be/" in lower || "m.youtube.com/" in lower)
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }

    class Factory(
        private val repository: ClippingRepository,
        private val downloadManager: ClipDownloadManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudioViewModel(repository, downloadManager) as T
        }
    }
}
