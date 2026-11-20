package com.proclipping.master.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.proclipping.master.R
import com.proclipping.master.ui.components.GradientPrimaryButton
import com.proclipping.master.ui.components.SecondaryStudioButton
import com.proclipping.master.ui.components.SectionEyebrow
import com.proclipping.master.ui.components.SpecBadge
import com.proclipping.master.ui.components.StudioCard
import com.proclipping.master.ui.theme.CharcoalCard
import com.proclipping.master.ui.theme.CharcoalInput
import com.proclipping.master.ui.theme.ElectricPurple
import com.proclipping.master.ui.theme.HairlineBorder
import com.proclipping.master.ui.theme.InkMuted
import com.proclipping.master.ui.theme.InkPrimary
import com.proclipping.master.ui.theme.InkSecondary
import com.proclipping.master.ui.theme.NeonSky
import com.proclipping.master.ui.theme.StatusCrimson
import com.proclipping.master.ui.theme.StatusEmerald
import com.proclipping.master.ui.theme.VividViolet
import com.proclipping.master.ui.viewmodel.StudioUiState
import java.util.Locale

@Composable
fun ProfileScreen(
    uiState: StudioUiState,
    onOpenAuth: () -> Unit,
    onOpenMyClips: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenUpgrade: () -> Unit,
    onOpenHelpSupport: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    onLogOut: () -> Unit,
    onDeleteAccount: (confirmEmail: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmEmail by remember { mutableStateOf("") }

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
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = InkPrimary
            )

            // Account Header Card
            StudioCard(highlighted = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ElectricPurple.copy(alpha = 0.22f),
                        border = BorderStroke(1.2.dp, VividViolet.copy(alpha = 0.6f)),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (uiState.isSignedIn) {
                                Text(
                                    text = uiState.userProfile.greetingName.take(1).uppercase(Locale.US),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = InkPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = VividViolet,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isSignedIn) {
                                uiState.userProfile.greetingName
                            } else {
                                "Creator Account"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (uiState.isSignedIn) {
                                uiState.userProfile.email
                            } else {
                                "Sign in to sync your clips across devices"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpecBadge(
                        text = "${uiState.userProfile.planDisplayName} Plan",
                        accent = VividViolet
                    )
                    SpecBadge(
                        text = String.format(
                            Locale.US,
                            "%.0f min credits left",
                            uiState.userProfile.creditsRemaining.coerceAtLeast(0.0)
                        ),
                        accent = StatusEmerald
                    )
                    SpecBadge(
                        text = "${uiState.allGeneratedClips.size} clips",
                        accent = NeonSky
                    )
                }

                if (!uiState.isSignedIn) {
                    Spacer(modifier = Modifier.height(16.dp))
                    GradientPrimaryButton(
                        text = stringResource(R.string.profile_sign_in_cta),
                        onClick = onOpenAuth,
                        modifier = Modifier.testTag("profile_sign_in_button")
                    )
                }
            }

            // Library & Plan Menu
            StudioCard {
                SectionEyebrow(text = "Studio")
                Spacer(modifier = Modifier.height(8.dp))
                ProfileMenuRow(
                    icon = Icons.Rounded.VideoLibrary,
                    title = stringResource(R.string.nav_my_clips),
                    subtitle = "${uiState.allGeneratedClips.size} saved vertical shorts",
                    onClick = onOpenMyClips,
                    tag = "profile_row_my_clips"
                )
                HorizontalDivider(color = HairlineBorder.copy(alpha = 0.6f))
                ProfileMenuRow(
                    icon = Icons.Rounded.History,
                    title = stringResource(R.string.history_title),
                    subtitle = "${uiState.recentJobs.size} clipping projects",
                    onClick = onOpenHistory,
                    tag = "profile_row_history"
                )
                HorizontalDivider(color = HairlineBorder.copy(alpha = 0.6f))
                ProfileMenuRow(
                    icon = Icons.Rounded.WorkspacePremium,
                    title = stringResource(R.string.upgrade_title),
                    subtitle = "View credit allowances & creator plans",
                    onClick = onOpenUpgrade,
                    tag = "profile_row_upgrade"
                )
            }

            // Support & Legal Menu
            StudioCard {
                SectionEyebrow(text = "Support & Legal")
                Spacer(modifier = Modifier.height(8.dp))
                ProfileMenuRow(
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    title = stringResource(R.string.profile_help_support),
                    subtitle = "FAQs, clip retention & tips",
                    onClick = onOpenHelpSupport,
                    tag = "profile_row_help"
                )
                HorizontalDivider(color = HairlineBorder.copy(alpha = 0.6f))
                ProfileMenuRow(
                    icon = Icons.Rounded.PrivacyTip,
                    title = stringResource(R.string.profile_privacy_policy),
                    subtitle = "6-hour original video cleanup & data rights",
                    onClick = onOpenPrivacyPolicy,
                    tag = "profile_row_privacy"
                )
                HorizontalDivider(color = HairlineBorder.copy(alpha = 0.6f))
                ProfileMenuRow(
                    icon = Icons.Rounded.Description,
                    title = stringResource(R.string.profile_terms_conditions),
                    subtitle = "Usage terms and creator rights",
                    onClick = onOpenTerms,
                    tag = "profile_row_terms"
                )
            }

            // Account Security Actions (only when signed in)
            if (uiState.isSignedIn) {
                StudioCard {
                    SectionEyebrow(text = "Account")
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryStudioButton(
                        text = stringResource(R.string.profile_logout_cta),
                        onClick = onLogOut,
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        accentColor = NeonSky,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_logout_button")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SecondaryStudioButton(
                        text = stringResource(R.string.profile_delete_account_cta),
                        onClick = {
                            deleteConfirmEmail = ""
                            showDeleteDialog = true
                        },
                        enabled = !uiState.isDeletingAccount,
                        icon = Icons.Rounded.DeleteForever,
                        accentColor = StatusCrimson,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_delete_account_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = CharcoalCard,
            titleContentColor = InkPrimary,
            textContentColor = InkSecondary,
            title = {
                Text(
                    text = "Delete Account Permanently?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This permanently deletes your account, project history, and all stored cloud clips. Type your email (${uiState.userProfile.email}) to confirm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                    OutlinedTextField(
                        value = deleteConfirmEmail,
                        onValueChange = { deleteConfirmEmail = it },
                        singleLine = true,
                        placeholder = {
                            Text(text = uiState.userProfile.email, color = InkMuted)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StatusCrimson,
                            unfocusedBorderColor = HairlineBorder,
                            focusedContainerColor = CharcoalInput,
                            unfocusedContainerColor = CharcoalInput,
                            focusedTextColor = InkPrimary,
                            unfocusedTextColor = InkPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_confirm_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccount(deleteConfirmEmail)
                    },
                    enabled = deleteConfirmEmail.trim().equals(uiState.userProfile.email.trim(), ignoreCase = true),
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusCrimson),
                    modifier = Modifier.testTag("delete_account_confirm_button")
                ) {
                    Text(stringResource(R.string.action_confirm_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = InkMuted)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String,
    iconTint: Color = VividViolet
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = InkPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = InkMuted
        )
    }
}
