package com.proclipping.master.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.proclipping.master.data.model.GeneratedClip

/**
 * Downloads individual 9:16 vertical MP4 clips using Android's system DownloadManager
 * into Movies/ProClippingMaster without requiring any broad storage permissions.
 */
class ClipDownloadManager(private val context: Context) {

    fun enqueueDownload(
        clip: GeneratedClip,
        bearerToken: String = ""
    ): Result<Long> = runCatching {
        val targetUrl = clip.downloadUrl.ifBlank { clip.videoUrl }
        require(targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
            "Clip download URL is not available from backend."
        }

        val sanitizedTitle = clip.title
            .replace(Regex("[^A-Za-z0-9._ -]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
            .take(42)
            .ifBlank { "short_clip_${clip.index + 1}" }

        val fileName = "${sanitizedTitle}_9x16.mp4"

        val request = DownloadManager.Request(Uri.parse(targetUrl))
            .setTitle(clip.title.ifBlank { "Pro Clipping Master Short" })
            .setDescription("Downloading 9:16 vertical short (${clip.formattedDuration})")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_MOVIES,
                "ProClippingMaster/$fileName"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        if (bearerToken.isNotBlank()) {
            request.addRequestHeader("Authorization", "Bearer ${bearerToken.trim()}")
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
    }
}
