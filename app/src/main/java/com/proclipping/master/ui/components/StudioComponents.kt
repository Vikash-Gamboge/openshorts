package com.proclipping.master.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.proclipping.master.R
import com.proclipping.master.data.model.DownloadStatus
import com.proclipping.master.data.model.GeneratedClip
import com.proclipping.master.data.model.ProcessingJob
import com.proclipping.master.data.model.ProcessingStage
import com.proclipping.master.data.model.SourceType
import com.proclipping.master.ui.theme.CharcoalCard
import com.proclipping.master.ui.theme.CharcoalElevated
import com.proclipping.master.ui.theme.CharcoalInput
import com.proclipping.master.ui.theme.DeepBlueGlow
import com.proclipping.master.ui.theme.DeepPurpleGlow
import com.proclipping.master.ui.theme.ElectricBlue
import com.proclipping.master.ui.theme.ElectricPurple
import com.proclipping.master.ui.theme.HairlineBorder
import com.proclipping.master.ui.theme.HairlineHighlight
import com.proclipping.master.ui.theme.InkMuted
import com.proclipping.master.ui.theme.InkPrimary
import com.proclipping.master.ui.theme.InkSecondary
import com.proclipping.master.ui.theme.NeonSky
import com.proclipping.master.ui.theme.ObsidianBlack
import com.proclipping.master.ui.theme.PrimaryGradientBrush
import com.proclipping.master.ui.theme.StatusAmber
import com.proclipping.master.ui.theme.StatusAmberBg
import com.proclipping.master.ui.theme.StatusCrimson
import com.proclipping.master.ui.theme.StatusCrimsonBg
import com.proclipping.master.ui.theme.StatusEmerald
import com.proclipping.master.ui.theme.StatusEmeraldBg
import com.proclipping.master.ui.theme.SubtleBorderGradientBrush
import com.proclipping.master.ui.theme.VividViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Modifier.studioAmbientBackground(): Modifier = this.drawBehind {
    drawRect(color = ObsidianBlack)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                DeepPurpleGlow.copy(alpha = 0.28f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.14f, size.height * 0.08f),
            radius = size.width * 0.85f
        )
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                DeepBlueGlow.copy(alpha = 0.22f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.90f, size.height * 0.26f),
            radius = size.width * 0.78f
        )
    )
}

@Composable
fun StudioCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val borderBrush = if (highlighted) {
        SubtleBorderGradientBrush
    } else {
        Brush.linearGradient(listOf(HairlineBorder, HairlineBorder.copy(alpha = 0.7f)))
    }

    Surface(
        shape = shape,
        color = CharcoalCard,
        tonalElevation = 0.dp,
        shadowElevation = if (highlighted) 8.dp else 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderBrush, shape)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun GradientPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "primary_button_scale"
    )
    val shape = RoundedCornerShape(16.dp)
    val active = enabled && !isLoading

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .defaultMinSize(minHeight = 54.dp)
            .clip(shape)
            .background(
                brush = if (active) {
                    PrimaryGradientBrush
                } else {
                    Brush.horizontalGradient(listOf(CharcoalElevated, CharcoalElevated))
                }
            )
            .border(
                width = 1.dp,
                brush = if (active) {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(HairlineBorder, HairlineBorder))
                },
                shape = shape
            )
    ) {
        Button(
            onClick = onClick,
            enabled = active,
            interactionSource = interactionSource,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = InkPrimary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = InkMuted
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 54.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = InkPrimary,
                    strokeWidth = 2.2.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SecondaryStudioButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    accentColor: Color = VividViolet
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "secondary_button_scale"
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) accentColor.copy(alpha = 0.45f) else HairlineBorder
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CharcoalElevated.copy(alpha = 0.55f),
            contentColor = InkPrimary,
            disabledContainerColor = CharcoalInput,
            disabledContentColor = InkMuted
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 48.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) accentColor else InkMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PublicBrandHeader(
    creditsRemaining: Double,
    planDisplayName: String,
    onCreditsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, SubtleBorderGradientBrush, RoundedCornerShape(14.dp))
                    .background(CharcoalElevated)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.pro_clipping_icon_art),
                    contentDescription = stringResource(R.string.cd_app_logo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val formattedCredits = if (creditsRemaining >= 10.0) {
            creditsRemaining.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", creditsRemaining)
        }

        Surface(
            shape = CircleShape,
            color = CharcoalElevated,
            border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.45f)),
            modifier = Modifier
                .testTag("header_credits_pill")
                .clip(CircleShape)
                .clickable(onClick = onCreditsClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = VividViolet,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$formattedCredits cr · $planDisplayName",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = InkPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SectionEyebrow(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(Locale.US),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = VividViolet,
        modifier = modifier
    )
}

@Composable
fun SpecBadge(
    text: String,
    accent: Color = NeonSky,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accent,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyStudioCard(
    title: String,
    body: String,
    icon: ImageVector = Icons.Rounded.AutoAwesome,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    StudioCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = ElectricPurple.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.35f)),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = VividViolet,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                SecondaryStudioButton(
                    text = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ServiceNoticeCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = StatusAmberBg,
        border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = StatusAmber,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = InkPrimary,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .testTag("service_notice_retry_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = StatusAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.action_retry),
                    style = MaterialTheme.typography.labelMedium,
                    color = StatusAmber
                )
            }
        }
    }
}

@Composable
fun GeneratedClipCard(
    clip: GeneratedClip,
    downloadStatus: DownloadStatus,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    StudioCard(
        highlighted = false,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // 9:16 mini vertical preview badge
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(104.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ElectricPurple.copy(alpha = 0.35f),
                                ElectricBlue.copy(alpha = 0.22f),
                                CharcoalInput
                            )
                        )
                    )
                    .border(1.dp, HairlineHighlight, RoundedCornerShape(14.dp))
                    .clickable(onClick = onPreview),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = ObsidianBlack.copy(alpha = 0.65f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.processing_preview_clip),
                            tint = InkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = clip.formattedDuration,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpecBadge(text = clip.aspectRatio, accent = NeonSky)
                    SpecBadge(text = clip.language.nativeBadge, accent = VividViolet)
                    if (clip.viralScore != null) {
                        SpecBadge(text = "${clip.viralScore}% Viral", accent = StatusEmerald)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = clip.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = InkPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (clip.hookReason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = clip.hookReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecondaryStudioButton(
                text = stringResource(R.string.processing_preview_clip),
                onClick = onPreview,
                icon = Icons.Rounded.PlayArrow,
                accentColor = NeonSky,
                modifier = Modifier
                    .weight(1f)
                    .testTag("clip_preview_button_${clip.index}")
            )
            SecondaryStudioButton(
                text = when (downloadStatus) {
                    DownloadStatus.SAVED -> "Saved"
                    DownloadStatus.DOWNLOADING -> "Saving…"
                    else -> stringResource(R.string.processing_download_clip)
                },
                onClick = onDownload,
                enabled = downloadStatus != DownloadStatus.DOWNLOADING,
                icon = if (downloadStatus == DownloadStatus.SAVED) {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.Rounded.Download
                },
                accentColor = if (downloadStatus == DownloadStatus.SAVED) StatusEmerald else VividViolet,
                modifier = Modifier
                    .weight(1f)
                    .testTag("clip_download_button_${clip.index}")
            )
        }
    }
}

@Composable
fun ProjectHistoryCard(
    job: ProcessingJob,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (statusLabel, statusColor, statusBg, statusIcon) = when (job.stage) {
        ProcessingStage.COMPLETED -> Quad(
            "Ready",
            StatusEmerald,
            StatusEmeraldBg,
            Icons.Rounded.CheckCircle
        )
        ProcessingStage.FAILED -> Quad(
            "Interrupted",
            StatusCrimson,
            StatusCrimsonBg,
            Icons.Rounded.ErrorOutline
        )
        else -> Quad(
            "Processing",
            StatusAmber,
            StatusAmberBg,
            Icons.Rounded.Schedule
        )
    }

    val dateFormatter = remember { SimpleDateFormat("MMM d · h:mm a", Locale.US) }
    val dateLabel = remember(job.createdAtEpochMs) {
        dateFormatter.format(Date(job.createdAtEpochMs))
    }

    StudioCard(
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CharcoalElevated,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = VividViolet,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.sourceTitle.ifBlank { "AI Clipping Project" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append(dateLabel)
                        append(" · ")
                        append(if (job.sourceType == SourceType.YOUTUBE_URL) "YouTube" else "Upload")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 1
                )
            }

            Surface(
                shape = CircleShape,
                color = statusBg,
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = statusColor
                    )
                }
            }

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("history_delete_${job.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove project from list",
                        tint = InkMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val clipCount = job.clips.size.coerceAtLeast(job.readyClipsCount)
            SpecBadge(
                text = if (clipCount > 0) "$clipCount clip${if (clipCount == 1) "" else "s"}" else "${job.targetClips} target",
                accent = if (clipCount > 0) StatusEmerald else NeonSky
            )
            SpecBadge(text = job.durationOption.shortLabel, accent = VividViolet)
            SpecBadge(text = job.language.nativeBadge, accent = InkSecondary)
            if (job.autoSubtitles) {
                SpecBadge(text = "Captions", accent = ElectricBlue)
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
