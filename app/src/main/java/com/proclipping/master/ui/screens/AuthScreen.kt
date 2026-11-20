package com.proclipping.master.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.proclipping.master.R
import com.proclipping.master.ui.components.GradientPrimaryButton
import com.proclipping.master.ui.components.SecondaryStudioButton
import com.proclipping.master.ui.components.StudioCard
import com.proclipping.master.ui.theme.CharcoalElevated
import com.proclipping.master.ui.theme.CharcoalInput
import com.proclipping.master.ui.theme.HairlineBorder
import com.proclipping.master.ui.theme.InkMuted
import com.proclipping.master.ui.theme.InkPrimary
import com.proclipping.master.ui.theme.InkSecondary
import com.proclipping.master.ui.theme.StatusCrimson
import com.proclipping.master.ui.theme.StatusCrimsonBg
import com.proclipping.master.ui.theme.SubtleBorderGradientBrush
import com.proclipping.master.ui.theme.VividViolet
import com.proclipping.master.ui.viewmodel.AuthMode
import com.proclipping.master.ui.viewmodel.StudioUiState

@Composable
fun AuthScreen(
    uiState: StudioUiState,
    onNavigateBack: () -> Unit,
    onSelectMode: (AuthMode) -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("auth_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = InkPrimary
                    )
                }
            }

            // Brand emblem
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.5.dp, SubtleBorderGradientBrush, RoundedCornerShape(22.dp))
                    .background(CharcoalElevated)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.pro_clipping_icon_art),
                    contentDescription = stringResource(R.string.cd_app_logo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.auth_welcome_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = InkPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.auth_welcome_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    textAlign = TextAlign.Center
                )
            }

            StudioCard(highlighted = true) {
                // Login / Sign Up Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CharcoalInput)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AuthModeTab(
                        label = stringResource(R.string.auth_login_tab),
                        selected = uiState.authMode == AuthMode.LOGIN,
                        onClick = { onSelectMode(AuthMode.LOGIN) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_tab_login")
                    )
                    AuthModeTab(
                        label = stringResource(R.string.auth_signup_tab),
                        selected = uiState.authMode == AuthMode.SIGNUP,
                        onClick = { onSelectMode(AuthMode.SIGNUP) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_tab_signup")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = uiState.authMode == AuthMode.SIGNUP) {
                    Column {
                        Text(
                            text = stringResource(R.string.auth_name_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = InkSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.authNameInput,
                            onValueChange = onNameChange,
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.auth_name_placeholder),
                                    color = InkMuted
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = VividViolet
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = authTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_name_input")
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                Text(
                    text = stringResource(R.string.auth_email_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = InkSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = uiState.authEmailInput,
                    onValueChange = onEmailChange,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.auth_email_placeholder),
                            color = InkMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Email,
                            contentDescription = null,
                            tint = VividViolet
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = authTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.auth_password_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = InkSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = uiState.authPasswordInput,
                    onValueChange = onPasswordChange,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.auth_password_placeholder),
                            color = InkMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = VividViolet
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.testTag("auth_toggle_password")
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Rounded.VisibilityOff
                                } else {
                                    Icons.Rounded.Visibility
                                },
                                contentDescription = stringResource(R.string.cd_toggle_password),
                                tint = InkMuted
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = authTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input")
                )

                AnimatedVisibility(visible = uiState.authError != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StatusCrimsonBg,
                        border = BorderStroke(1.dp, StatusCrimson.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = StatusCrimson,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.authError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = InkPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                GradientPrimaryButton(
                    text = if (uiState.authMode == AuthMode.SIGNUP) {
                        stringResource(R.string.auth_signup_cta)
                    } else {
                        stringResource(R.string.auth_login_cta)
                    },
                    onClick = onSubmit,
                    enabled = !uiState.isAuthSubmitting,
                    isLoading = uiState.isAuthSubmitting,
                    modifier = Modifier.testTag("auth_submit_button")
                )

                if (!uiState.authRequiredByService) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SecondaryStudioButton(
                        text = stringResource(R.string.auth_continue_guest),
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_continue_guest_button")
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onOpenPrivacyPolicy) {
                    Text(
                        text = stringResource(R.string.profile_privacy_policy),
                        style = MaterialTheme.typography.labelMedium,
                        color = InkMuted
                    )
                }
                Text(text = "·", color = InkMuted)
                TextButton(onClick = onOpenTerms) {
                    Text(
                        text = stringResource(R.string.profile_terms_conditions),
                        style = MaterialTheme.typography.labelMedium,
                        color = InkMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthModeTab(
    label: String,
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
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) InkPrimary else InkMuted
            )
        }
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VividViolet,
    unfocusedBorderColor = HairlineBorder,
    focusedContainerColor = CharcoalInput,
    unfocusedContainerColor = CharcoalInput,
    focusedTextColor = InkPrimary,
    unfocusedTextColor = InkPrimary
)
