package com.proclipping.master.data.remote

import android.content.ContentResolver
import android.net.Uri
import com.proclipping.master.data.model.FastApiErrorDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

sealed class OpenShortsException(
    override val message: String,
    val isRetryable: Boolean = true,
    val httpCode: Int? = null
) : Exception(message) {

    class AuthenticationRequired(
        detail: String = "Please sign in to continue."
    ) : OpenShortsException(
        message = detail,
        isRetryable = false,
        httpCode = 401
    )

    class CreditsExhausted(
        detail: String = "You have used all your available credits. Upgrade your plan to continue."
    ) : OpenShortsException(
        message = detail,
        isRetryable = false,
        httpCode = 402
    )

    class NetworkUnreachable(detail: String) : OpenShortsException(
        message = detail,
        isRetryable = true
    )

    class ServerError(code: Int, detail: String) : OpenShortsException(
        message = detail,
        isRetryable = code >= 500 || code == 429 || code == 408,
        httpCode = code
    )
}

/**
 * HTTPS Retrofit client for Pro Clipping Master.
 * Strictly sanitizes all network and backend errors into safe, user-friendly copy
 * so internal URLs, headers, API keys, or raw technical diagnostics never appear in the UI.
 */
class OpenShortsApiClient {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var cachedService: OpenShortsApiService? = null

    fun normalizeBaseUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }

    fun resolveMediaUrl(baseUrl: String, relativeOrAbsolute: String?): String {
        if (relativeOrAbsolute.isNullOrBlank()) return ""
        val trimmed = relativeOrAbsolute.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        val cleanBase = normalizeBaseUrl(baseUrl).trimEnd('/')
        if (cleanBase.isEmpty()) return trimmed
        val cleanPath = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return "$cleanBase$cleanPath"
    }

    @Synchronized
    fun getService(rawBaseUrl: String, bearerToken: String = ""): OpenShortsApiService {
        val normalized = normalizeBaseUrl(rawBaseUrl)
        val cleanToken = bearerToken.trim()
        val existing = cachedService
        if (existing != null && cachedBaseUrl == normalized && cachedToken == cleanToken) {
            return existing
        }

        val authInterceptor = Interceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("User-Agent", "ProClippingMaster-Android/2.0")
            if (cleanToken.isNotEmpty()) {
                builder.header("Authorization", "Bearer $cleanToken")
            }
            chain.proceed(builder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
            redactHeader("X-Gemini-Key")
        }

        val okHttp = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionSpecs(
                listOf(
                    ConnectionSpec.MODERN_TLS,
                    ConnectionSpec.COMPATIBLE_TLS
                )
            )
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        val service = retrofit.create(OpenShortsApiService::class.java)
        cachedBaseUrl = normalized
        cachedToken = cleanToken
        cachedService = service
        return service
    }

    fun parseError(response: Response<*>): OpenShortsException {
        val code = response.code()
        val rawBody = try {
            response.errorBody()?.string().orEmpty()
        } catch (_: Exception) {
            ""
        }

        val parsedDetail = try {
            if (rawBody.isNotBlank()) {
                val dto = json.decodeFromString<FastApiErrorDto>(rawBody)
                val detailText = when (val d = dto.detail) {
                    is JsonPrimitive -> d.contentOrNull
                    is JsonArray -> d.mapNotNull { item ->
                        (item as? JsonObject)?.get("msg")?.let { (it as? JsonPrimitive)?.contentOrNull }
                            ?: (item as? JsonPrimitive)?.contentOrNull
                    }.joinToString("; ").takeIf { it.isNotBlank() }
                    is JsonObject -> (d["message"] as? JsonPrimitive)?.contentOrNull
                        ?: (d["error"] as? JsonPrimitive)?.contentOrNull
                    else -> null
                }
                detailText ?: dto.message ?: dto.error
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

        val safeDetail = sanitizePublicMessage(parsedDetail)

        return when {
            code == 401 -> OpenShortsException.AuthenticationRequired(
                safeDetail ?: "Please sign in to continue."
            )
            code == 402 -> OpenShortsException.CreditsExhausted(
                safeDetail ?: "Your available credits are used up. Upgrade your plan to keep creating clips."
            )
            code == 400 && isTechnicalOrKeyError(parsedDetail) -> OpenShortsException.ServerError(
                code = 503,
                detail = "AI video processing is temporarily unavailable. Please try again in a few moments."
            )
            code == 409 && !safeDetail.isNullOrBlank() -> OpenShortsException.ServerError(code, safeDetail)
            code == 413 -> OpenShortsException.ServerError(
                code,
                "This video file is too large. Please upload a smaller MP4 video."
            )
            code == 429 -> OpenShortsException.ServerError(
                code,
                safeDetail ?: "Too many requests right now. Please wait a moment and try again."
            )
            code in setOf(502, 503, 504) -> OpenShortsException.ServerError(
                code,
                safeDetail ?: "Our AI processing engine is warming up. Please try again in a few seconds."
            )
            code >= 500 -> OpenShortsException.ServerError(
                code,
                "Something went wrong while processing your request. Please try again shortly."
            )
            !safeDetail.isNullOrBlank() -> OpenShortsException.ServerError(code, safeDetail)
            else -> OpenShortsException.ServerError(
                code,
                "We couldn't complete this request right now. Please try again."
            )
        }
    }

    fun mapNetworkFailure(throwable: Throwable): OpenShortsException {
        if (throwable is OpenShortsException) return throwable
        return when (throwable) {
            is UnknownHostException, is ConnectException -> OpenShortsException.NetworkUnreachable(
                "Unable to connect right now. Check your internet connection and try again."
            )
            is SSLHandshakeException, is SSLException -> OpenShortsException.NetworkUnreachable(
                "Secure connection could not be verified. Check your network and try again."
            )
            is SocketTimeoutException -> OpenShortsException.NetworkUnreachable(
                "The AI service is taking a moment to respond. Please wait a few seconds and tap Retry."
            )
            is IOException -> OpenShortsException.NetworkUnreachable(
                "Network connection interrupted. Please check your internet and try again."
            )
            else -> OpenShortsException.NetworkUnreachable(
                "Something went wrong. Please try again in a moment."
            )
        }
    }

    private fun isTechnicalOrKeyError(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val lower = raw.lowercase()
        return listOf(
            "x-gemini-key",
            "gemini_api_key",
            "api key",
            "traceback",
            "exception",
            "sqlalchemy",
            "postgres",
            "uvicorn",
            "fastapi",
            "http://",
            "https://",
            "openshorts-3rks",
            "render.com",
            "header"
        ).any { it in lower }
    }

    private fun sanitizePublicMessage(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if (isTechnicalOrKeyError(raw)) return null
        val cleaned = raw.trim()
        if (cleaned.length > 220) return null
        return cleaned
    }
}

/**
 * Streams a local video Content URI directly to OkHttp without buffering the full video in memory,
 * emitting real byte-level upload progress (0f..1f).
 *
 * Uses AssetFileDescriptor length verification and falls back to chunked streaming (-1L) whenever
 * the ContentResolver cannot guarantee an exact byte count (e.g. cloud-backed Photo Picker URIs),
 * preventing OkHttp ProtocolException mismatches during upload.
 */
class ContentUriProgressRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val totalBytes: Long,
    private val mimeType: String = "video/mp4",
    private val onProgress: (Float) -> Unit
) : RequestBody() {

    private val verifiedLength: Long by lazy {
        val afdLen = runCatching {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        }.getOrNull() ?: -1L
        when {
            afdLen > 0L -> afdLen
            totalBytes > 0L -> totalBytes
            else -> -1L
        }
    }

    override fun contentType(): MediaType = mimeType.ifBlank { "video/mp4" }.toMediaType()

    // Always use chunked transfer (-1L) when length is uncertain so OkHttp never aborts with
    // ProtocolException("expected X bytes but received Y") on virtual/cloud MediaStore streams.
    override fun contentLength(): Long = if (verifiedLength > 0L) verifiedLength else -1L

    override fun isOneShot(): Boolean = false

    override fun writeTo(sink: BufferedSink) {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to read the selected video file from your device.")

        val denominator = if (verifiedLength > 0L) verifiedLength else totalBytes
        inputStream.use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
            var uploaded = 0L
            var read = input.read(buffer)
            while (read != -1) {
                sink.write(buffer, 0, read)
                uploaded += read
                if (denominator > 0L) {
                    onProgress((uploaded.toFloat() / denominator.toFloat()).coerceIn(0f, 0.99f))
                }
                read = input.read(buffer)
            }
            sink.flush()
            onProgress(1f)
        }
    }
}
