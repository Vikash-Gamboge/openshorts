package com.proclipping.master.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.proclipping.master.data.model.ClipDurationOption
import com.proclipping.master.data.model.ClipLanguage
import com.proclipping.master.data.model.ProcessingJob
import com.proclipping.master.data.model.SubtitlePreset
import com.proclipping.master.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pro_clipping_master_prefs"
)

internal const val PUBLIC_API_BASE_URL = "https://openshorts-3rks.onrender.com"

data class StudioPreferences(
    val authToken: String = "",
    val userProfile: UserProfile = UserProfile(),
    val language: ClipLanguage = ClipLanguage.ENGLISH,
    val durationOption: ClipDurationOption = ClipDurationOption.SEC_60,
    val targetClips: Int = 5,
    val autoSubtitles: Boolean = true,
    val subtitlePreset: SubtitlePreset = SubtitlePreset.HORMOZI
)

class SettingsDataStore(
    private val context: Context,
    private val json: Json
) {
    private object Keys {
        val AUTH_TOKEN_ENC = stringPreferencesKey("auth_session_token_enc")
        val USER_ID = stringPreferencesKey("auth_user_id")
        val USER_EMAIL = stringPreferencesKey("auth_user_email")
        val USER_DISPLAY_NAME = stringPreferencesKey("auth_user_display_name")
        val USER_PLAN = stringPreferencesKey("auth_user_plan")
        val CREDITS_REMAINING = doublePreferencesKey("credits_remaining")
        val CREDITS_ALLOWANCE = doublePreferencesKey("credits_allowance")
        val CREDITS_USED = doublePreferencesKey("credits_used")
        val STORAGE_PROVIDER = stringPreferencesKey("storage_provider")
        val ORIGINAL_RETENTION_HOURS = intPreferencesKey("original_retention_hours")
        val LANGUAGE = stringPreferencesKey("clip_language")
        val DURATION_SECONDS = intPreferencesKey("clip_duration_seconds")
        val TARGET_CLIPS = intPreferencesKey("target_clips")
        val AUTO_SUBTITLES = booleanPreferencesKey("auto_subtitles")
        val SUBTITLE_PRESET = stringPreferencesKey("subtitle_preset")
        val RECENT_JOBS_JSON = stringPreferencesKey("recent_jobs_json")
        // Legacy keys cleaned up automatically on sign-out
        val LEGACY_GEMINI_KEY_ENC = stringPreferencesKey("gemini_api_key_enc")
        val LEGACY_SESSION_TOKEN = stringPreferencesKey("backend_session_token")
        val LEGACY_BACKEND_URL = stringPreferencesKey("backend_base_url")
    }

    val preferencesFlow: Flow<StudioPreferences> = context.studioDataStore.data.map { prefs ->
        val langName = prefs[Keys.LANGUAGE]
        val language = ClipLanguage.entries.firstOrNull { it.name == langName } ?: ClipLanguage.ENGLISH

        val durSec = prefs[Keys.DURATION_SECONDS] ?: 60
        val durationOption = ClipDurationOption.entries.firstOrNull { it.seconds == durSec }
            ?: ClipDurationOption.SEC_60

        val presetName = prefs[Keys.SUBTITLE_PRESET]
        val subtitlePreset = SubtitlePreset.entries.firstOrNull { it.name == presetName }
            ?: SubtitlePreset.HORMOZI

        val token = SecureKeyCipher.decrypt(prefs[Keys.AUTH_TOKEN_ENC])
        val profile = UserProfile(
            id = prefs[Keys.USER_ID].orEmpty(),
            email = prefs[Keys.USER_EMAIL].orEmpty(),
            displayName = prefs[Keys.USER_DISPLAY_NAME].orEmpty(),
            plan = prefs[Keys.USER_PLAN] ?: "free",
            creditsRemaining = prefs[Keys.CREDITS_REMAINING] ?: 20.0,
            creditsAllowance = prefs[Keys.CREDITS_ALLOWANCE] ?: 20.0,
            creditsUsed = prefs[Keys.CREDITS_USED] ?: 0.0,
            storageProvider = prefs[Keys.STORAGE_PROVIDER] ?: "cloud",
            originalRetentionHours = prefs[Keys.ORIGINAL_RETENTION_HOURS] ?: 6
        )

        StudioPreferences(
            authToken = token,
            userProfile = profile,
            language = language,
            durationOption = durationOption,
            targetClips = (prefs[Keys.TARGET_CLIPS] ?: 5).coerceIn(1, 15),
            autoSubtitles = prefs[Keys.AUTO_SUBTITLES] ?: true,
            subtitlePreset = subtitlePreset
        )
    }

    val recentJobsFlow: Flow<List<ProcessingJob>> = context.studioDataStore.data.map { prefs ->
        val raw = prefs[Keys.RECENT_JOBS_JSON].orEmpty()
        if (raw.isBlank()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<ProcessingJob>>(raw)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    suspend fun saveAuthSession(token: String, profile: UserProfile) {
        val encryptedToken = SecureKeyCipher.encrypt(token.trim())
        context.studioDataStore.edit { prefs ->
            // Remove any legacy developer/BYOK keys from previous builds
            prefs.remove(Keys.LEGACY_GEMINI_KEY_ENC)
            prefs.remove(Keys.LEGACY_SESSION_TOKEN)
            prefs.remove(Keys.LEGACY_BACKEND_URL)

            if (encryptedToken.isNotBlank()) {
                prefs[Keys.AUTH_TOKEN_ENC] = encryptedToken
            }
            prefs[Keys.USER_ID] = profile.id
            prefs[Keys.USER_EMAIL] = profile.email
            prefs[Keys.USER_DISPLAY_NAME] = profile.displayName
            prefs[Keys.USER_PLAN] = profile.plan
            prefs[Keys.CREDITS_REMAINING] = profile.creditsRemaining
            prefs[Keys.CREDITS_ALLOWANCE] = profile.creditsAllowance
            prefs[Keys.CREDITS_USED] = profile.creditsUsed
            prefs[Keys.STORAGE_PROVIDER] = profile.storageProvider
            prefs[Keys.ORIGINAL_RETENTION_HOURS] = profile.originalRetentionHours
        }
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        context.studioDataStore.edit { prefs ->
            prefs[Keys.USER_ID] = profile.id
            prefs[Keys.USER_EMAIL] = profile.email
            prefs[Keys.USER_DISPLAY_NAME] = profile.displayName
            prefs[Keys.USER_PLAN] = profile.plan
            prefs[Keys.CREDITS_REMAINING] = profile.creditsRemaining
            prefs[Keys.CREDITS_ALLOWANCE] = profile.creditsAllowance
            prefs[Keys.CREDITS_USED] = profile.creditsUsed
            prefs[Keys.STORAGE_PROVIDER] = profile.storageProvider
            prefs[Keys.ORIGINAL_RETENTION_HOURS] = profile.originalRetentionHours
        }
    }

    suspend fun clearAuthSessionAndLocalHistory() {
        context.studioDataStore.edit { prefs ->
            prefs.remove(Keys.AUTH_TOKEN_ENC)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.USER_DISPLAY_NAME)
            prefs.remove(Keys.USER_PLAN)
            prefs.remove(Keys.CREDITS_REMAINING)
            prefs.remove(Keys.CREDITS_ALLOWANCE)
            prefs.remove(Keys.CREDITS_USED)
            prefs.remove(Keys.STORAGE_PROVIDER)
            prefs.remove(Keys.ORIGINAL_RETENTION_HOURS)
            prefs.remove(Keys.RECENT_JOBS_JSON)
            prefs.remove(Keys.LEGACY_GEMINI_KEY_ENC)
            prefs.remove(Keys.LEGACY_SESSION_TOKEN)
            prefs.remove(Keys.LEGACY_BACKEND_URL)
        }
    }

    suspend fun purgeLegacySecrets() {
        context.studioDataStore.edit { prefs ->
            prefs.remove(Keys.LEGACY_GEMINI_KEY_ENC)
            prefs.remove(Keys.LEGACY_SESSION_TOKEN)
            prefs.remove(Keys.LEGACY_BACKEND_URL)
        }
    }

    suspend fun saveLanguage(language: ClipLanguage) {
        context.studioDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun saveDuration(durationOption: ClipDurationOption) {
        context.studioDataStore.edit { it[Keys.DURATION_SECONDS] = durationOption.seconds }
    }

    suspend fun saveTargetClips(count: Int) {
        context.studioDataStore.edit { it[Keys.TARGET_CLIPS] = count.coerceIn(1, 15) }
    }

    suspend fun saveAutoSubtitles(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.AUTO_SUBTITLES] = enabled }
    }

    suspend fun saveSubtitlePreset(preset: SubtitlePreset) {
        context.studioDataStore.edit { it[Keys.SUBTITLE_PRESET] = preset.name }
    }

    suspend fun upsertJob(job: ProcessingJob) {
        context.studioDataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_JOBS_JSON]?.let { raw ->
                try {
                    json.decodeFromString<List<ProcessingJob>>(raw)
                } catch (_: Exception) {
                    emptyList()
                }
            } ?: emptyList()

            val updated = buildList {
                add(job)
                addAll(current.filterNot { it.id == job.id })
            }.take(40)

            prefs[Keys.RECENT_JOBS_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun removeJob(jobId: String) {
        context.studioDataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_JOBS_JSON]?.let { raw ->
                try {
                    json.decodeFromString<List<ProcessingJob>>(raw)
                } catch (_: Exception) {
                    emptyList()
                }
            } ?: emptyList()
            val updated = current.filterNot { it.id == jobId }
            prefs[Keys.RECENT_JOBS_JSON] = json.encodeToString(updated)
        }
    }
}
