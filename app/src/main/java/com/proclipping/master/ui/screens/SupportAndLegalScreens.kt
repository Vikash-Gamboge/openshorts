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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.proclipping.master.ui.components.SectionEyebrow
import com.proclipping.master.ui.components.StudioCard
import com.proclipping.master.ui.theme.InkMuted
import com.proclipping.master.ui.theme.InkPrimary
import com.proclipping.master.ui.theme.InkSecondary

@Composable
fun HelpSupportScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DocumentScaffold(
        title = stringResource(R.string.help_title),
        subtitle = "Answers to common creator questions",
        onNavigateBack = onNavigateBack,
        modifier = modifier
    ) {
        InfoSectionCard(
            eyebrow = "Getting Started",
            title = "How does Pro Clipping Master work?",
            body = "Choose a local MP4 video from your device or paste a YouTube link on the Home screen, pick your language (English or Hindi), target length (30s–90s), and caption style, then tap Generate AI Clips. Our AI detects the strongest hooks, reframes the speaker into 9:16 vertical HD, and adds crisp captions automatically."
        )
        InfoSectionCard(
            eyebrow = "Original Videos",
            title = "How long are my original uploaded videos kept?",
            body = "After your clips finish generating and are verified as safely stored, original uploaded source videos are automatically deleted after 6 hours to protect your privacy and keep processing fast. Your generated 9:16 clips remain available in My Clips so you can preview and download them anytime."
        )
        InfoSectionCard(
            eyebrow = "Credits & Plans",
            title = "How do AI processing credits work?",
            body = "Credits are measured in minutes of source video processed. Every account starts with a free monthly allowance. If processing is interrupted before completion, your reserved credits are automatically refunded."
        )
        InfoSectionCard(
            eyebrow = "Downloads & Subtitles",
            title = "Where are downloaded clips saved?",
            body = "Downloaded 9:16 clips are saved directly to your device's Movies/ProClippingMaster folder and appear in your Gallery. You can also switch caption presets on any clip directly from the Clip Preview screen."
        )
    }
}

@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DocumentScaffold(
        title = stringResource(R.string.privacy_title),
        subtitle = "How Pro Clipping Master protects your videos and account",
        onNavigateBack = onNavigateBack,
        modifier = modifier
    ) {
        InfoSectionCard(
            eyebrow = "1. Data We Collect",
            title = "Account & Video Processing Data",
            body = "When you create an account, we store your display name, email address, securely hashed password (salted scrypt), and monthly credit usage. When you upload a video or submit a link, our servers process the audio and visual frames strictly to generate your requested 9:16 vertical clips and captions."
        )
        InfoSectionCard(
            eyebrow = "2. AI Processing",
            title = "Automated Highlight Detection & Captions",
            body = "Your video transcript and keyframes are analyzed securely on our server infrastructure to identify engaging moments and synchronize captions. All communication between the Android app and our servers is encrypted over HTTPS (TLS)."
        )
        InfoSectionCard(
            eyebrow = "3. Cloud Storage & 6-Hour Source Cleanup",
            title = "Verified Retention Policy",
            body = "Generated clips are stored privately for your account in cloud object storage (Cloudflare R2 when enabled on the server) and isolated per user so other users cannot access your content. Original uploaded videos are retained for up to 6 hours after processing completes and are permanently deleted only after every generated clip has been verified as safely stored. Generated clips are not automatically deleted unless you remove them or delete your account."
        )
        InfoSectionCard(
            eyebrow = "4. Your Rights & Account Deletion",
            title = "Full Control Over Your Data",
            body = "You can sign out at any time from Profile, which revokes your active session token on the server and clears local session data. You may also permanently delete your account from Profile → Delete Account, which removes your account record, project history, and stored cloud clips."
        )
    }
}

@Composable
fun TermsConditionsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DocumentScaffold(
        title = stringResource(R.string.terms_title),
        subtitle = "Creator usage guidelines & service terms",
        onNavigateBack = onNavigateBack,
        modifier = modifier
    ) {
        InfoSectionCard(
            eyebrow = "1. Content Ownership",
            title = "You Own Your Videos & Generated Clips",
            body = "You retain full ownership of the videos you upload and the 9:16 clips generated by Pro Clipping Master. By submitting a video or link, you confirm that you have the necessary rights and permissions to process and redistribute that content."
        )
        InfoSectionCard(
            eyebrow = "2. Acceptable Use",
            title = "Responsible Creator Usage",
            body = "You agree not to upload unlawful, harmful, or copyright-infringing material, and not to abuse rate limits or attempt to bypass per-account credit allowances. Automated scraping or unauthorized access attempts may result in account suspension."
        )
        InfoSectionCard(
            eyebrow = "3. Service Availability & Credits",
            title = "Processing Allowances & Subscriptions",
            body = "Free accounts include a monthly processing allowance. Optional paid plans, when enabled, provide expanded monthly allowances and priority rendering. Unused monthly credits reset on your billing cycle. Failed processing jobs do not consume credits."
        )
        InfoSectionCard(
            eyebrow = "4. Storage & Retention",
            title = "Source Cleanup & Downloads",
            body = "Original uploaded videos are deleted automatically 6 hours after verified clip generation. We recommend downloading your favorite final clips to your device for long-term personal archiving."
        )
    }
}

@Composable
private fun DocumentScaffold(
    title: String,
    subtitle: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("document_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = InkPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = InkPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                }
            }

            content()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoSectionCard(
    eyebrow: String,
    title: String,
    body: String
) {
    StudioCard {
        SectionEyebrow(text = eyebrow)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = InkPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary
        )
    }
}
