package com.proclipping.master.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.proclipping.master.R
import com.proclipping.master.data.model.SubscriptionPlanTier
import com.proclipping.master.ui.components.GradientPrimaryButton
import com.proclipping.master.ui.components.SecondaryStudioButton
import com.proclipping.master.ui.components.SectionEyebrow
import com.proclipping.master.ui.components.SpecBadge
import com.proclipping.master.ui.components.StudioCard
import com.proclipping.master.ui.theme.ElectricBlue
import com.proclipping.master.ui.theme.InkMuted
import com.proclipping.master.ui.theme.InkPrimary
import com.proclipping.master.ui.theme.InkSecondary
import com.proclipping.master.ui.theme.NeonSky
import com.proclipping.master.ui.theme.StatusEmerald
import com.proclipping.master.ui.theme.VividViolet
import com.proclipping.master.ui.viewmodel.StudioUiState
import java.util.Locale

@Composable
fun UpgradePlanScreen(
    uiState: StudioUiState,
    onSelectPlan: (SubscriptionPlanTier) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePlanId = uiState.userProfile.plan.lowercase(Locale.US)

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
            // Header
            Column {
                Text(
                    text = stringResource(R.string.upgrade_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = InkPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.upgrade_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted
                )
            }

            // Current Balance Summary
            StudioCard(highlighted = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        SectionEyebrow(text = "Active Plan")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${uiState.userProfile.planDisplayName} Plan",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = InkPrimary
                        )
                    }
                    SpecBadge(
                        text = String.format(
                            Locale.US,
                            "%.0f / %.0f min left",
                            uiState.userProfile.creditsRemaining.coerceAtLeast(0.0),
                            uiState.userProfile.creditsAllowance.coerceAtLeast(1.0)
                        ),
                        accent = StatusEmerald
                    )
                }
            }

            // Honest notice when Stripe checkout is not yet enabled (ZERO fake payments)
            if (!uiState.paymentsConfigured) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ElectricBlue.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, NeonSky.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = NeonSky,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.upgrade_payment_pending_title),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = InkPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = stringResource(R.string.upgrade_payment_pending_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                    }
                }
            }

            // Plan Cards
            uiState.plans.forEach { tier ->
                val isCurrent = tier.id.equals(activePlanId, ignoreCase = true)
                val isPopular = !tier.highlightTag.isNullOrBlank()

                StudioCard(highlighted = isPopular || isCurrent) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = tier.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = InkPrimary
                            )
                            if (isPopular) {
                                SpecBadge(text = tier.highlightTag!!, accent = VividViolet)
                            } else if (isCurrent) {
                                SpecBadge(text = "Active", accent = StatusEmerald)
                            }
                        }

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = tier.priceDisplay,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = InkPrimary
                            )
                            Text(
                                text = tier.periodLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted,
                                modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = NeonSky,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${tier.creditsPerMonth} AI processing minutes / month",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonSky
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tier.features.forEach { feature ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusEmerald,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = feature,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        isCurrent -> {
                            SecondaryStudioButton(
                                text = stringResource(R.string.upgrade_current_plan),
                                onClick = {},
                                enabled = false,
                                accentColor = StatusEmerald,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("plan_current_${tier.id}")
                            )
                        }
                        tier.id.equals("free", ignoreCase = true) -> {
                            // Free tier doesn't require a checkout button
                        }
                        tier.checkoutAvailable -> {
                            GradientPrimaryButton(
                                text = "${stringResource(R.string.upgrade_select_plan)} · ${tier.name}",
                                onClick = { onSelectPlan(tier) },
                                enabled = !uiState.isStartingCheckout,
                                isLoading = uiState.isStartingCheckout,
                                icon = Icons.Rounded.WorkspacePremium,
                                modifier = Modifier.testTag("plan_upgrade_${tier.id}")
                            )
                        }
                        else -> {
                            SecondaryStudioButton(
                                text = "Checkout Coming Soon",
                                onClick = { onSelectPlan(tier) },
                                icon = Icons.Rounded.WorkspacePremium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("plan_coming_soon_${tier.id}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
