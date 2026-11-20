package com.proclipping.master.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.proclipping.master.R
import com.proclipping.master.data.model.DownloadStatus
import com.proclipping.master.data.model.GeneratedClip
import com.proclipping.master.ui.components.EmptyStudioCard
import com.proclipping.master.ui.components.GeneratedClipCard
import com.proclipping.master.ui.theme.InkMuted
import com.proclipping.master.ui.theme.InkPrimary
import com.proclipping.master.ui.theme.NeonSky
import com.proclipping.master.ui.viewmodel.StudioUiState

@Composable
fun MyClipsScreen(
    uiState: StudioUiState,
    onRefresh: () -> Unit,
    onPreviewClip: (GeneratedClip) -> Unit,
    onDownloadClip: (GeneratedClip) -> Unit,
    onGoToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clips = uiState.allGeneratedClips

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.my_clips_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = InkPrimary
                    )
                    Text(
                        text = "${clips.size} generated 9:16 clip${if (clips.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !uiState.isSyncingHistory,
                    modifier = Modifier.testTag("my_clips_refresh_button")
                ) {
                    if (uiState.isSyncingHistory) {
                        CircularProgressIndicator(
                            color = NeonSky,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh),
                            tint = NeonSky
                        )
                    }
                }
            }

            if (clips.isEmpty()) {
                EmptyStudioCard(
                    title = stringResource(R.string.my_clips_empty_title),
                    body = stringResource(R.string.my_clips_empty_body),
                    icon = Icons.Rounded.VideoLibrary,
                    actionLabel = stringResource(R.string.my_clips_create_cta),
                    onAction = onGoToHome
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(clips, key = { it.id }) { clip ->
                        GeneratedClipCard(
                            clip = clip,
                            downloadStatus = uiState.downloadStates[clip.id] ?: DownloadStatus.IDLE,
                            onPreview = { onPreviewClip(clip) },
                            onDownload = { onDownloadClip(clip) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
