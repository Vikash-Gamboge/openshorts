package com.proclipping.master.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.proclipping.master.R
import com.proclipping.master.data.model.ClipDurationOption
import com.proclipping.master.data.model.ClipLanguage
import com.proclipping.master.data.model.DownloadStatus
import com.proclipping.master.data.model.GeneratedClip
import com.proclipping.master.data.model.ServiceAvailability
import com.proclipping.master.data.model.SourceType
import com.proclipping.master.data.model.SubtitlePreset
import com.proclipping.master.ui.components.EmptyStudioCard
import com.proclipping.master.ui.components.GeneratedClipCard
import com.proclipping.master.ui.components.GradientPrimaryButton
import com.proclipping.master.ui.components.PublicBrandHeader
import com.proclipping.master.ui.components.SecondaryStudioButton
import com.proclipping.master.ui.components.SectionEyebrow
import com.proclipping.master.ui.components.ServiceNoticeCard
import com.proclipping.master.ui.components.SpecBadge
import com.proclipping.master.ui.components.StudioCard
import com.proclipping.master.ui.theme.CharcoalCard
import com.proclipping.master.ui.theme.CharcoalElevated
import com.proclipping.master.ui.theme.CharcoalInput
import com.proclipping.master.ui.theme.ElectricBlue
import com.proclipping.master.ui.theme.ElectricPurple
import com.proclipping.master.ui.theme.HairlineBorder
import com.proclipping.master.ui.theme.HairlineHighlight
import com.proclipping.master.ui.theme.InkMuted
import com.proclipping.master.ui.theme.InkPrimary
import com.proclipping.master.ui.theme.InkSecondary
import com.proclipping.master.ui.theme.NeonSky
import com.proclipping.master.ui.theme.StatusCrimson
import com.proclipping.master.ui.theme.StatusCrimsonBg
import com.proclipping.master.ui.theme.StatusEmerald
import com.proclipping.master.ui.theme.VividViolet
import com.proclipping.master.ui.viewmodel.StudioUiState
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: StudioUiState,
    onSelectSourceType: (SourceType) -> Unit,
    onYouTubeUrlChange: (String) -> Unit,
    onClearYouTubeUrl: () -> Unit,
    onLocalVideoPicked: (android.net.Uri) -> Unit,
    onClearLocalVideo: () -> Unit,
    onSelectLanguage: (ClipLanguage) -> Unit,
    onSelectDuration: (ClipDurationOption) -> Unit,
    onTargetClipsChange: (Int) -> Unit,
    onToggleAutoSubtitles: (Boolean) -> Unit,
    onSelectSubtitlePreset: (SubtitlePreset) -> Unit,
    onGenerateClips: () -> Unit,
    onRetryServiceCheck: () -> Unit,
    onDismissSubmissionError: () -> Unit,
    onOpenAuth: () -> Unit,
    onOpenUpgrade: () -> Unit,
    onViewAllClips: () -> Unit,
    onPreviewClip: (GeneratedClip) -> Unit,
    onDownloadClip: (GeneratedClip) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Zero-permission Android Photo Picker for MP4 selection (Google Play compliant)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onLocalVideoPicked(uri)
        }
    }

    val launchVideoPicker = {
        videoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        )
    }

    val youtubeAllowed = (uiState.serviceAvailability as? ServiceAvailability.Ready)?.youtubeUrlEnabled ?: true

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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Clean Public Brand Header
            PublicBrandHeader(
                creditsRemaining = uiState.userProfile.creditsRemaining,
                planDisplayName = uiState.userProfile.planDisplayName,
                onCreditsClick = onOpenUpgrade
            )

            // 2. Welcome Message + Available Free Credits Summary
            StudioCard(highlighted = true, contentPadding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isSignedIn) {
                                stringResource(R.string.home_welcome_user, uiState.userProfile.greetingName)
                            } else {
                                stringResource(R.string.home_welcome_guest)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_credits_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted
                        )
                    }
                    if (!uiState.isSignedIn) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ElectricPurple.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, VividViolet.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .testTag("home_sign_in_chip")
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenAuth)
                        ) {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = InkPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val remaining = uiState.userProfile.creditsRemaining.coerceAtLeast(0.0)
                val allowance = uiState.userProfile.creditsAllowance.coerceAtLeast(1.0)
                val ratio = (remaining / allowance).toFloat().coerceIn(0f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = VividViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.home_credits_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = InkSecondary
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.0f / %.0f min", remaining, allowance),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = InkPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { ratio },
                    color = ElectricPurple,
                    trackColor = CharcoalInput,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )
            }

            // Friendly service warmup notice (never exposes URLs or developer errors)
            val availability = uiState.serviceAvailability
            if (availability is ServiceAvailability.TemporarilyUnavailable) {
                ServiceNoticeCard(
                    message = availability.message,
                    onRetry = onRetryServiceCheck
                )
            }

            // 3. Large Interactive Upload Video / YouTube Card
            StudioCard(highlighted = true) {
                SectionEyebrow(text = "Source Video")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.home_upload_card_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = InkPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_upload_card_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Segmented source switch
                if (youtubeAllowed) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CharcoalInput)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SourceSwitchChip(
                            label = stringResource(R.string.home_upload_tab),
                            icon = Icons.Rounded.CloudUpload,
                            selected = uiState.sourceType == SourceType.LOCAL_MP4,
                            onClick = { onSelectSourceType(SourceType.LOCAL_MP4) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("source_tab_upload")
                        )
                        SourceSwitchChip(
                            label = stringResource(R.string.home_youtube_tab),
                            icon = Icons.Rounded.Link,
                            selected = uiState.sourceType == SourceType.YOUTUBE_URL,
                            onClick = { onSelectSourceType(SourceType.YOUTUBE_URL) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("source_tab_youtube")
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (uiState.sourceType == SourceType.LOCAL_MP4 || !youtubeAllowed) {
                    val picked = uiState.selectedLocalVideo
                    if (picked == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            ElectricPurple.copy(alpha = 0.14f),
                                            ElectricBlue.copy(alpha = 0.08f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = VividViolet.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable(onClick = launchVideoPicker)
                                .padding(horizontal = 18.dp, vertical = 24.dp)
                                .testTag("home_upload_dropzone"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = ElectricPurple.copy(alpha = 0.22f),
                                    border = BorderStroke(1.dp, VividViolet.copy(alpha = 0.55f)),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.CloudUpload,
                                            contentDescription = null,
                                            tint = InkPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.home_upload_video_btn),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = InkPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.home_upload_video_sub),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkMuted
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CharcoalInput,
                            border = BorderStroke(1.dp, StatusEmerald.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = ElectricPurple.copy(alpha = 0.2f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.VideoFile,
                                                contentDescription = null,
                                                tint = VividViolet,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = picked.displayName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = InkPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            SpecBadge(text = picked.formattedDuration, accent = NeonSky)
                                            SpecBadge(text = picked.formattedSize, accent = StatusEmerald)
                                        }
                                    }
                                    IconButton(
                                        onClick = onClearLocalVideo,
                                        modifier = Modifier.testTag("home_clear_video_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.cd_clear),
                                            tint = InkMuted
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                SecondaryStudioButton(
                                    text = stringResource(R.string.home_change_video_btn),
                                    onClick = launchVideoPicker,
                                    icon = Icons.Rounded.CloudUpload,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("home_change_video_button")
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = uiState.youtubeUrl,
                        onValueChange = onYouTubeUrlChange,
                        singleLine = true,
                        isError = uiState.youtubeUrlError != null,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.home_youtube_placeholder),
                                color = InkMuted
                            )
                        },
                        supportingText = uiState.youtubeUrlError?.let { err ->
                            { Text(text = err, color = StatusCrimson) }
                        },
                        trailingIcon = {
                            if (uiState.youtubeUrl.isNotBlank()) {
                                IconButton(
                                    onClick = onClearYouTubeUrl,
                                    modifier = Modifier.testTag("home_clear_url_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.cd_clear),
                                        tint = InkMuted
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        val pasted = clipboardManager.getText()?.text.orEmpty().trim()
                                        if (pasted.isNotBlank()) onYouTubeUrlChange(pasted)
                                    },
                                    modifier = Modifier.testTag("home_paste_url_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentPaste,
                                        contentDescription = stringResource(R.string.cd_paste),
                                        tint = NeonSky
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VividViolet,
                            unfocusedBorderColor = HairlineBorder,
                            focusedContainerColor = CharcoalInput,
                            unfocusedContainerColor = CharcoalInput,
                            focusedTextColor = InkPrimary,
                            unfocusedTextColor = InkPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_youtube_url_input")
                    )
                }

                // Live Upload Progress Indicator
                AnimatedVisibility(visible = uiState.uploadProgress != null) {
                    val progress = (uiState.uploadProgress ?: 0f).coerceIn(0f, 1f)
                    Column(modifier = Modifier.padding(top = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Uploading video securely…",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkSecondary
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = NeonSky
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            color = ElectricBlue,
                            trackColor = CharcoalInput,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }

            // 4. Compact Clip Preferences Card
            StudioCard {
                SectionEyebrow(text = stringResource(R.string.home_settings_title))
                Spacer(modifier = Modifier.height(14.dp))

                // Language Selection
                Text(
                    text = stringResource(R.string.home_language_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = InkSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ClipLanguage.entries.forEach { lang ->
                        val selected = uiState.language == lang
                        SelectablePill(
                            title = lang.displayName,
                            subtitle = lang.nativeBadge,
                            selected = selected,
                            onClick = { onSelectLanguage(lang) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_lang_${lang.code}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Duration Selection
                Text(
                    text = stringResource(R.string.home_duration_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = InkSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClipDurationOption.entries.forEach { opt ->
                        val selected = uiState.durationOption == opt
                        SelectablePill(
                            title = opt.shortLabel,
                            subtitle = opt.descriptor,
                            selected = selected,
                            onClick = { onSelectDuration(opt) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_duration_${opt.seconds}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Clips per video stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.home_clips_count_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = InkSecondary
                        )
                        Text(
                            text = "AI picks top ${uiState.targetClips} viral moments",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CharcoalElevated,
                            border = BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier.size(40.dp)
                        ) {
                            IconButton(
                                onClick = { onTargetClipsChange(uiState.targetClips - 1) },
                                enabled = uiState.targetClips > 1,
                                modifier = Modifier.testTag("home_clips_minus")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = "Decrease clip count",
                                    tint = InkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = uiState.targetClips.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = InkPrimary
                        )
                        Surface(
                            shape = CircleShape,
                            color = CharcoalElevated,
                            border = BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier.size(40.dp)
                        ) {
                            IconButton(
                                onClick = { onTargetClipsChange(uiState.targetClips + 1) },
                                enabled = uiState.targetClips < 15,
                                modifier = Modifier.testTag("home_clips_plus")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Increase clip count",
                                    tint = InkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto Subtitles Toggle + Style Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_subtitles_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = InkPrimary
                        )
                        Text(
                            text = stringResource(R.string.home_subtitles_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted
                        )
                    }
                    Switch(
                        checked = uiState.autoSubtitles,
                        onCheckedChange = onToggleAutoSubtitles,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = InkPrimary,
                            checkedTrackColor = ElectricPurple,
                            uncheckedThumbColor = InkMuted,
                            uncheckedTrackColor = CharcoalInput
                        ),
                        modifier = Modifier.testTag("home_subtitles_switch")
                    )
                }

                AnimatedVisibility(visible = uiState.autoSubtitles) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SubtitlePreset.entries.forEach { preset ->
                            val selected = uiState.subtitlePreset == preset
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) ElectricPurple.copy(alpha = 0.22f) else CharcoalInput,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) VividViolet else HairlineBorder
                                ),
                                modifier = Modifier
                                    .testTag("home_preset_${preset.apiKey}")
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectSubtitlePreset(preset) }
                            ) {
                                Text(
                                    text = preset.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (selected) InkPrimary else InkMuted,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Friendly error banner if submission fails
            AnimatedVisibility(visible = uiState.submissionError != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StatusCrimsonBg,
                    border = BorderStroke(1.dp, StatusCrimson.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = StatusCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.submissionError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = InkPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissSubmissionError) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_clear),
                                tint = InkMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 5. Primary Generate AI Clips CTA (duplicate-guarded via isSubmittingJob)
            GradientPrimaryButton(
                text = if (uiState.isSubmittingJob) {
                    if (uiState.uploadProgress != null) "Uploading Video…" else "Starting AI Studio…"
                } else {
                    stringResource(R.string.home_generate_clips_btn)
                },
                onClick = onGenerateClips,
                enabled = !uiState.isSubmittingJob,
                isLoading = uiState.isSubmittingJob,
                icon = Icons.Rounded.AutoAwesome,
                modifier = Modifier.testTag("home_generate_clips_button")
            )

            // 6. Recent Generated Clips Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.home_recent_clips),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = InkPrimary
                )
                if (uiState.allGeneratedClips.isNotEmpty()) {
                    TextButton(
                        onClick = onViewAllClips,
                        modifier = Modifier.testTag("home_view_all_clips_button")
                    ) {
                        Text(
                            text = stringResource(R.string.home_view_all),
                            color = VividViolet,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            val recentClips = uiState.allGeneratedClips.take(4)
            if (recentClips.isEmpty()) {
                EmptyStudioCard(
                    title = stringResource(R.string.home_recent_empty_title),
                    body = stringResource(R.string.home_recent_empty_body)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    recentClips.forEach { clip ->
                        GeneratedClipCard(
                            clip = clip,
                            downloadStatus = uiState.downloadStates[clip.id] ?: DownloadStatus.IDLE,
                            onPreview = { onPreviewClip(clip) },
                            onDownload = { onDownloadClip(clip) }
                        )
                    }
                }
            }

            // 7. Upgrade Plan Promotional Card
            StudioCard(highlighted = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ElectricPurple.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, VividViolet.copy(alpha = 0.5f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.WorkspacePremium,
                                contentDescription = null,
                                tint = VividViolet,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_upgrade_card_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.home_upgrade_card_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                SecondaryStudioButton(
                    text = stringResource(R.string.home_upgrade_cta),
                    onClick = onOpenUpgrade,
                    icon = Icons.Rounded.WorkspacePremium,
                    accentColor = VividViolet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_upgrade_cta_button")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SourceSwitchChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = if (selected) CharcoalElevated else CharcoalInput,
        border = if (selected) BorderStroke(1.dp, VividViolet.copy(alpha = 0.55f)) else null,
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) VividViolet else InkMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) InkPrimary else InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SelectablePill(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ElectricPurple.copy(alpha = 0.2f) else CharcoalInput,
        border = BorderStroke(
            width = if (selected) 1.4.dp else 1.dp,
            color = if (selected) VividViolet else HairlineBorder
        ),
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (selected) InkPrimary else InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) VividViolet else InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
