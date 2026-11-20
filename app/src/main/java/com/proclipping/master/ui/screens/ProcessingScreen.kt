package com.proclipping.master.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.proclipping.master.R
import com.proclipping.master.data.model.DownloadStatus
import com.proclipping.master.data.model.GeneratedClip
import com.proclipping.master.data.model.ProcessingJob
import com.proclipping.master.data.model.ProcessingStage
import com.proclipping.master.ui.components.EmptyStudioCard
import com.proclipping.master.ui.components.GeneratedClipCard
import com.proclipping.master.ui.components.GradientPrimaryButton
import com.proclipping.master.ui.components.SecondaryStudioButton
import com.proclipping.master.ui.components.SectionEyebrow
import com.proclipping.master.ui.components.SpecBadge
import com.proclipping.master.ui.components.StudioCard
import com.proclipping.master.ui.theme.CharcoalInput
import com.proclipping.master.ui.theme.ElectricBlue
import com.proclipping.master.ui.theme.ElectricPurple
import com.proclipping.master.ui.theme.InkMuted
import com.proclipping.master.ui.theme.InkPrimary
import com.proclipping.master.ui.theme.InkSecondary
import com.proclipping.master.ui.theme.NeonSky
import com.proclipping.master.ui.theme.StatusCrimson
import com.proclipping.master.ui.theme.StatusCrimsonBg
import com.proclipping.master.ui.theme.StatusEmerald
import com.proclipping.master.ui.theme.StatusEmeraldBg
import com.proclipping.master.ui.theme.VividViolet

private val VISIBLE_PIPELINE_STEPS = listOf(
    ProcessingStage.INGESTING,
    ProcessingStage.TRANSCRIBING,
    ProcessingStage.DETECTING_HIGHLIGHTS,
    ProcessingStage.REFRAMING_VERTICAL,
    ProcessingStage.BURNING_SUBTITLES
)

@Composable
fun ProcessingScreen(
    job: ProcessingJob?,
    downloadStates: Map<String, DownloadStatus>,
    onNavigateBack: () -> Unit,
    onRefreshStatus: () -> Unit,
    onRetryJob: () -> Unit,
    onPreviewClip: (GeneratedClip) -> Unit,
    onDownloadClip: (GeneratedClip) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    modifier = Modifier.testTag("processing_back_button")
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
                        text = stringResource(R.string.processing_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary
                    )
                    if (job != null) {
                        Text(
                            text = job.sourceTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (job != null && job.stage != ProcessingStage.COMPLETED) {
                    IconButton(
                        onClick = onRefreshStatus,
                        modifier = Modifier.testTag("processing_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh),
                            tint = NeonSky
                        )
                    }
                }
            }

            if (job == null) {
                EmptyStudioCard(
                    title = stringResource(R.string.processing_idle_title),
                    body = stringResource(R.string.processing_idle_body),
                    icon = Icons.Rounded.AutoAwesome,
                    actionLabel = stringResource(R.string.processing_go_home),
                    onAction = onNavigateBack
                )
            } else {
                val animatedProgress by animateFloatAsState(
                    targetValue = job.progressFraction.coerceIn(0f, 1f),
                    label = "processing_progress"
                )

                // Hero Progress / Completion / Error Card
                when (job.stage) {
                    ProcessingStage.COMPLETED -> {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = StatusEmeraldBg,
                            border = BorderStroke(1.dp, StatusEmerald.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusEmerald,
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = job.stage.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = InkPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${job.clips.size} vertical 9:16 clip${if (job.clips.size == 1) "" else "s"} ready to preview and download.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSecondary
                                    )
                                }
                            }
                        }
                    }

                    ProcessingStage.FAILED -> {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = StatusCrimsonBg,
                            border = BorderStroke(1.dp, StatusCrimson.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = StatusCrimson,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = job.stage.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = InkPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = job.errorMessage
                                        ?: "Processing could not be completed. Your credits are protected and you can try again.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    GradientPrimaryButton(
                                        text = stringResource(R.string.processing_retry),
                                        onClick = onRetryJob,
                                        icon = Icons.Rounded.Refresh,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("processing_retry_button")
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                SecondaryStudioButton(
                                    text = stringResource(R.string.processing_go_home),
                                    onClick = onNavigateBack,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    else -> {
                        StudioCard(highlighted = true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = VividViolet,
                                        strokeWidth = 2.6.dp,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = job.stage.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = InkPrimary
                                    )
                                }
                                SpecBadge(
                                    text = "${(animatedProgress * 100).toInt()}%",
                                    accent = NeonSky
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = job.stage.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                color = ElectricPurple,
                                trackColor = CharcoalInput,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(CircleShape)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            SectionEyebrow(text = "Studio Progress")
                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                VISIBLE_PIPELINE_STEPS.forEach { step ->
                                    val isDone = job.stage.stepIndex > step.stepIndex
                                    val isCurrent = job.stage.stepIndex == step.stepIndex
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                isDone -> Icons.Rounded.CheckCircle
                                                isCurrent -> Icons.Rounded.HourglassTop
                                                else -> Icons.Rounded.RadioButtonUnchecked
                                            },
                                            contentDescription = null,
                                            tint = when {
                                                isDone -> StatusEmerald
                                                isCurrent -> ElectricBlue
                                                else -> InkMuted
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = step.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                                                ),
                                                color = when {
                                                    isDone || isCurrent -> InkPrimary
                                                    else -> InkMuted
                                                }
                                            )
                                            if (isCurrent) {
                                                Text(
                                                    text = step.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = NeonSky
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Project Specs Strip
                StudioCard {
                    SectionEyebrow(text = "Project Details")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpecBadge(text = job.language.nativeBadge, accent = VividViolet)
                        SpecBadge(text = job.durationOption.shortLabel, accent = NeonSky)
                        SpecBadge(text = "${job.targetClips} clips", accent = ElectricBlue)
                        if (job.autoSubtitles) {
                            SpecBadge(text = job.subtitlePreset.displayName, accent = StatusEmerald)
                        }
                    }
                }

                // Generated Clips Section
                if (job.clips.isNotEmpty()) {
                    Text(
                        text = "${stringResource(R.string.processing_clips_ready)} (${job.clips.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = InkPrimary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        job.clips.forEach { clip ->
                            GeneratedClipCard(
                                clip = clip,
                                downloadStatus = downloadStates[clip.id] ?: DownloadStatus.IDLE,
                                onPreview = { onPreviewClip(clip) },
                                onDownload = { onDownloadClip(clip) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
