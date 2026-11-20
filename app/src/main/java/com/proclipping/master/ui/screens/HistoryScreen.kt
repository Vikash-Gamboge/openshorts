package com.proclipping.master.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.proclipping.master.R
import com.proclipping.master.data.model.ProcessingJob
import com.proclipping.master.ui.components.EmptyStudioCard
import com.proclipping.master.ui.components.ProjectHistoryCard
import com.proclipping.master.ui.components.ServiceNoticeCard
import com.proclipping.master.ui.theme.CharcoalInput
import com.proclipping.master.ui.theme.ElectricPurple
import com.proclipping.master.ui.theme.HairlineBorder
import com.proclipping.master.ui.theme.InkMuted
import com.proclipping.master.ui.theme.InkPrimary
import com.proclipping.master.ui.theme.NeonSky
import com.proclipping.master.ui.theme.VividViolet
import com.proclipping.master.ui.viewmodel.HistoryFilter
import com.proclipping.master.ui.viewmodel.StudioUiState

@Composable
fun HistoryScreen(
    uiState: StudioUiState,
    onSelectFilter: (HistoryFilter) -> Unit,
    onRefreshHistory: () -> Unit,
    onOpenJob: (ProcessingJob) -> Unit,
    onDeleteJob: (String) -> Unit,
    onGoToHome: () -> Unit,
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
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.history_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = InkPrimary
                    )
                    Text(
                        text = "${uiState.recentJobs.size} project${if (uiState.recentJobs.size == 1) "" else "s"} in your studio",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                }
                IconButton(
                    onClick = onRefreshHistory,
                    enabled = !uiState.isSyncingHistory,
                    modifier = Modifier.testTag("history_refresh_button")
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
                            contentDescription = stringResource(R.string.history_refresh_btn),
                            tint = NeonSky
                        )
                    }
                }
            }

            // Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilter.entries.forEach { filter ->
                    val selected = uiState.historyFilter == filter
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) ElectricPurple.copy(alpha = 0.22f) else CharcoalInput,
                        border = BorderStroke(1.dp, if (selected) VividViolet else HairlineBorder),
                        modifier = Modifier
                            .testTag("history_filter_${filter.name.lowercase()}")
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectFilter(filter) }
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selected) InkPrimary else InkMuted,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            if (uiState.historySyncError != null) {
                ServiceNoticeCard(
                    message = uiState.historySyncError,
                    onRetry = onRefreshHistory
                )
            }

            val items = uiState.filteredHistory
            if (items.isEmpty()) {
                EmptyStudioCard(
                    title = stringResource(R.string.history_empty_title),
                    body = stringResource(R.string.history_empty_body),
                    icon = Icons.Rounded.History,
                    actionLabel = stringResource(R.string.my_clips_create_cta),
                    onAction = onGoToHome
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items, key = { it.id }) { job ->
                        ProjectHistoryCard(
                            job = job,
                            onClick = { onOpenJob(job) },
                            onDelete = { onDeleteJob(job.id) }
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
