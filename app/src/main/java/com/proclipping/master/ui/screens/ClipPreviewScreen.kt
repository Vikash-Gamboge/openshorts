package com.proclipping.master.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.proclipping.master.R
import com.proclipping.master.data.model.DownloadStatus
import com.proclipping.master.data.model.GeneratedClip
import com.proclipping.master.data.model.SubtitlePreset
import com.proclipping.master.ui.components.GradientPrimaryButton
import com.proclipping.master.ui.components.SectionEyebrow
import com.proclipping.master.ui.components.SpecBadge
import com.proclipping.master.ui.components.StudioCard
import com.proclipping.master.ui.theme.CharcoalCard
import com.proclipping.master.ui.theme.CharcoalInput
import com.proclipping.master.ui.theme.ElectricBlue
import com.proclipping.master.ui.theme.ElectricPurple
import com.proclipping.master.ui.theme.HairlineBorder
import com.proclipping.master.ui.theme.InkMuted
import com.proclipping.master.ui.theme.InkPrimary
import com.proclipping.master.ui.theme.InkSecondary
import com.proclipping.master.ui.theme.NeonSky
import com.proclipping.master.ui.theme.ObsidianBlack
import com.proclipping.master.ui.theme.StatusEmerald
import com.proclipping.master.ui.theme.SubtleBorderGradientBrush
import com.proclipping.master.ui.theme.VividViolet

@Composable
fun ClipPreviewScreen(
    clip: GeneratedClip?,
    downloadStatus: DownloadStatus,
    isReburningSubtitles: Boolean,
    onNavigateBack: () -> Unit,
    onDownloadClip: (GeneratedClip) -> Unit,
    onApplySubtitlePreset: (jobId: String, clipIndex: Int, preset: SubtitlePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPreset by remember { mutableStateOf(SubtitlePreset.HORMOZI) }
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    val videoUrl = clip?.videoUrl.orEmpty()
    val exoPlayer = remember(videoUrl) {
        if (videoUrl.isNotBlank()) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUrl))
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
                prepare()
            }
        } else {
            null
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("preview_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = InkPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.preview_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary
                    )
                    Text(
                        text = clip?.title ?: "Clip unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                        maxLines = 1
                    )
                }
                if (clip != null) {
                    SpecBadge(text = "9:16 · ${clip.formattedDuration}", accent = NeonSky)
                }
            }

            if (clip == null) {
                StudioCard {
                    Text(
                        text = stringResource(R.string.preview_no_video),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
            } else {
                // 9:16 Vertical Video Player Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 310.dp)
                            .fillMaxWidth()
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(ObsidianBlack)
                            .border(1.5.dp, SubtleBorderGradientBrush, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (exoPlayer != null) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = false
                                    }
                                },
                                update = { it.player = exoPlayer },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Playback controls overlay
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = CharcoalCard.copy(alpha = 0.8f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            isPlaying = !isPlaying
                                            if (isPlaying) exoPlayer.play() else exoPlayer.pause()
                                        },
                                        modifier = Modifier.testTag("preview_play_pause_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = stringResource(R.string.cd_play_pause),
                                            tint = InkPrimary
                                        )
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = CharcoalCard.copy(alpha = 0.8f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            isMuted = !isMuted
                                            exoPlayer.volume = if (isMuted) 0f else 1f
                                        },
                                        modifier = Modifier.testTag("preview_mute_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                            contentDescription = stringResource(R.string.cd_mute_toggle),
                                            tint = InkPrimary
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.preview_no_video),
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                }

                // Clip Metadata & AI Highlight Explanation
                StudioCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpecBadge(text = clip.language.nativeBadge, accent = VividViolet)
                        if (clip.viralScore != null) {
                            SpecBadge(text = "VIRAL ${clip.viralScore}/100", accent = StatusEmerald)
                        }
                        if (clip.hasSubtitles) {
                            SpecBadge(text = "SUBTITLED", accent = NeonSky)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = clip.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = clip.hookReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                // Subtitle Style Re-burn Control (POST /api/subtitle)
                StudioCard {
                    SectionEyebrow(text = "Caption Style Preset")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SubtitlePreset.entries.forEach { preset ->
                            val selected = preset == selectedPreset
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) ElectricPurple.copy(alpha = 0.22f) else CharcoalInput,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) VividViolet else HairlineBorder
                                ),
                                modifier = Modifier
                                    .testTag("preview_preset_${preset.apiKey}")
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedPreset = preset }
                            ) {
                                Text(
                                    text = preset.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) InkPrimary else InkMuted,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onApplySubtitlePreset(clip.jobId, clip.index, selectedPreset) },
                        enabled = !isReburningSubtitles,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("preview_reburn_subtitles_button")
                    ) {
                        if (isReburningSubtitles) {
                            CircularProgressIndicator(
                                color = VividViolet,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.ClosedCaption,
                                contentDescription = null,
                                tint = VividViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = stringResource(R.string.preview_apply_subtitles_cta),
                            color = InkPrimary
                        )
                    }
                }

                // Download & Share Actions
                GradientPrimaryButton(
                    text = when (downloadStatus) {
                        DownloadStatus.SAVED -> "Saved to Movies/ProClippingMaster"
                        DownloadStatus.DOWNLOADING -> "Starting Download…"
                        else -> stringResource(R.string.preview_download_cta)
                    },
                    onClick = { onDownloadClip(clip) },
                    icon = Icons.Rounded.Download,
                    modifier = Modifier.testTag("preview_download_button")
                )

                if (clip.downloadUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, clip.title)
                                putExtra(Intent.EXTRA_TEXT, clip.downloadUrl)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share 9:16 Clip"))
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("preview_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = NeonSky,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.preview_share_cta),
                            color = InkPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
