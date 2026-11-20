package com.proclipping.master.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.proclipping.master.R
import com.proclipping.master.data.model.DownloadStatus
import com.proclipping.master.ui.components.studioAmbientBackground
import com.proclipping.master.ui.screens.AuthScreen
import com.proclipping.master.ui.screens.ClipPreviewScreen
import com.proclipping.master.ui.screens.HelpSupportScreen
import com.proclipping.master.ui.screens.HistoryScreen
import com.proclipping.master.ui.screens.HomeScreen
import com.proclipping.master.ui.screens.MyClipsScreen
import com.proclipping.master.ui.screens.PrivacyPolicyScreen
import com.proclipping.master.ui.screens.ProcessingScreen
import com.proclipping.master.ui.screens.ProfileScreen
import com.proclipping.master.ui.screens.TermsConditionsScreen
import com.proclipping.master.ui.screens.UpgradePlanScreen
import com.proclipping.master.ui.theme.CharcoalCard
import com.proclipping.master.ui.theme.DeepCharcoal
import com.proclipping.master.ui.theme.ElectricPurple
import com.proclipping.master.ui.theme.InkMuted
import com.proclipping.master.ui.theme.InkPrimary
import com.proclipping.master.ui.theme.VividViolet
import com.proclipping.master.ui.viewmodel.StudioUiEvent
import com.proclipping.master.ui.viewmodel.StudioViewModel
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object MyClipsRoute
@Serializable data object HistoryRoute
@Serializable data object UpgradeRoute
@Serializable data object ProfileRoute

@Serializable data object AuthRoute
@Serializable data class ProcessingRoute(val jobId: String)
@Serializable data class ClipPreviewRoute(val jobId: String, val clipIndex: Int)
@Serializable data object HelpSupportRoute
@Serializable data object PrivacyPolicyRoute
@Serializable data object TermsConditionsRoute

private data class PublicNavDestination(
    val route: Any,
    val labelRes: Int,
    val icon: ImageVector,
    val tag: String
)

private val PUBLIC_TOP_DESTINATIONS = listOf(
    PublicNavDestination(HomeRoute, R.string.nav_home, Icons.Rounded.Home, "nav_home"),
    PublicNavDestination(MyClipsRoute, R.string.nav_my_clips, Icons.Rounded.VideoLibrary, "nav_my_clips"),
    PublicNavDestination(HistoryRoute, R.string.nav_history, Icons.Rounded.History, "nav_history"),
    PublicNavDestination(UpgradeRoute, R.string.nav_upgrade, Icons.Rounded.WorkspacePremium, "nav_upgrade"),
    PublicNavDestination(ProfileRoute, R.string.nav_profile, Icons.Rounded.Person, "nav_profile")
)

@Composable
fun ProClippingNavHost(
    viewModel: StudioViewModel,
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is StudioUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
                is StudioUiEvent.NavigateToProcessing -> {
                    navController.navigate(ProcessingRoute(event.jobId)) {
                        launchSingleTop = true
                    }
                }
                StudioUiEvent.NavigateToAuth -> {
                    navController.navigate(AuthRoute) {
                        launchSingleTop = true
                    }
                }
                StudioUiEvent.NavigateToUpgrade -> {
                    navController.navigate(UpgradeRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                StudioUiEvent.AuthCompleted -> {
                    navController.popBackStack(HomeRoute, inclusive = false)
                }
                is StudioUiEvent.OpenExternalUrl -> {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                        context.startActivity(intent)
                    }.onFailure {
                        snackbarHostState.showSnackbar("Unable to open checkout in browser.")
                    }
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isTopLevelRoute = PUBLIC_TOP_DESTINATIONS.any { dest ->
        currentDestination?.hasRoute(dest.route::class) == true
    }

    val navigateToTopLevel: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val useNavigationRail = widthSizeClass == WindowWidthSizeClass.Expanded

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = CharcoalCard,
                    contentColor = InkPrimary,
                    actionColor = VividViolet
                )
            }
        },
        bottomBar = {
            if (isTopLevelRoute && !useNavigationRail) {
                NavigationBar(
                    containerColor = DeepCharcoal.copy(alpha = 0.96f),
                    tonalElevation = 0.dp
                ) {
                    PUBLIC_TOP_DESTINATIONS.forEach { dest ->
                        val selected = currentDestination?.hasRoute(dest.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTopLevel(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = stringResource(dest.labelRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(dest.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = InkPrimary,
                                selectedTextColor = VividViolet,
                                indicatorColor = ElectricPurple.copy(alpha = 0.25f),
                                unselectedIconColor = InkMuted,
                                unselectedTextColor = InkMuted
                            ),
                            modifier = Modifier.testTag(dest.tag)
                        )
                    }
                }
            }
        },
        modifier = modifier
            .fillMaxSize()
            .studioAmbientBackground()
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isTopLevelRoute && useNavigationRail) {
                NavigationRail(
                    containerColor = DeepCharcoal.copy(alpha = 0.96f),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    PUBLIC_TOP_DESTINATIONS.forEach { dest ->
                        val selected = currentDestination?.hasRoute(dest.route::class) == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTopLevel(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = stringResource(dest.labelRes)
                                )
                            },
                            label = { Text(text = stringResource(dest.labelRes)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = InkPrimary,
                                selectedTextColor = VividViolet,
                                indicatorColor = ElectricPurple.copy(alpha = 0.25f),
                                unselectedIconColor = InkMuted,
                                unselectedTextColor = InkMuted
                            ),
                            modifier = Modifier.testTag(dest.tag)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = HomeRoute
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            uiState = uiState,
                            onSelectSourceType = viewModel::selectSourceType,
                            onYouTubeUrlChange = viewModel::updateYouTubeUrl,
                            onClearYouTubeUrl = viewModel::clearYouTubeUrl,
                            onLocalVideoPicked = { uri -> viewModel.onLocalVideoSelected(context, uri) },
                            onClearLocalVideo = viewModel::clearLocalVideo,
                            onSelectLanguage = viewModel::selectLanguage,
                            onSelectDuration = viewModel::selectDuration,
                            onTargetClipsChange = viewModel::updateTargetClips,
                            onToggleAutoSubtitles = viewModel::toggleAutoSubtitles,
                            onSelectSubtitlePreset = viewModel::selectSubtitlePreset,
                            onGenerateClips = viewModel::generateClips,
                            onRetryServiceCheck = viewModel::bootstrapApp,
                            onDismissSubmissionError = viewModel::clearSubmissionError,
                            onOpenAuth = { navController.navigate(AuthRoute) },
                            onOpenUpgrade = { navigateToTopLevel(UpgradeRoute) },
                            onViewAllClips = { navigateToTopLevel(MyClipsRoute) },
                            onPreviewClip = { clip ->
                                navController.navigate(ClipPreviewRoute(clip.jobId, clip.index))
                            },
                            onDownloadClip = viewModel::downloadClip
                        )
                    }

                    composable<MyClipsRoute> {
                        MyClipsScreen(
                            uiState = uiState,
                            onRefresh = viewModel::syncHistory,
                            onPreviewClip = { clip ->
                                navController.navigate(ClipPreviewRoute(clip.jobId, clip.index))
                            },
                            onDownloadClip = viewModel::downloadClip,
                            onGoToHome = { navigateToTopLevel(HomeRoute) }
                        )
                    }

                    composable<HistoryRoute> {
                        HistoryScreen(
                            uiState = uiState,
                            onSelectFilter = viewModel::setHistoryFilter,
                            onRefreshHistory = viewModel::syncHistory,
                            onOpenJob = { job ->
                                viewModel.openJob(job.id)
                                navController.navigate(ProcessingRoute(job.id))
                            },
                            onDeleteJob = viewModel::deleteHistoryJob,
                            onGoToHome = { navigateToTopLevel(HomeRoute) }
                        )
                    }

                    composable<UpgradeRoute> {
                        UpgradePlanScreen(
                            uiState = uiState,
                            onSelectPlan = viewModel::upgradeToPlan
                        )
                    }

                    composable<ProfileRoute> {
                        ProfileScreen(
                            uiState = uiState,
                            onOpenAuth = { navController.navigate(AuthRoute) },
                            onOpenMyClips = { navigateToTopLevel(MyClipsRoute) },
                            onOpenHistory = { navigateToTopLevel(HistoryRoute) },
                            onOpenUpgrade = { navigateToTopLevel(UpgradeRoute) },
                            onOpenHelpSupport = { navController.navigate(HelpSupportRoute) },
                            onOpenPrivacyPolicy = { navController.navigate(PrivacyPolicyRoute) },
                            onOpenTerms = { navController.navigate(TermsConditionsRoute) },
                            onLogOut = viewModel::logOut,
                            onDeleteAccount = viewModel::deleteAccount
                        )
                    }

                    composable<AuthRoute> {
                        AuthScreen(
                            uiState = uiState,
                            onNavigateBack = { navController.popBackStack() },
                            onSelectMode = viewModel::setAuthMode,
                            onNameChange = viewModel::updateAuthName,
                            onEmailChange = viewModel::updateAuthEmail,
                            onPasswordChange = viewModel::updateAuthPassword,
                            onSubmit = viewModel::submitAuth,
                            onOpenPrivacyPolicy = { navController.navigate(PrivacyPolicyRoute) },
                            onOpenTerms = { navController.navigate(TermsConditionsRoute) }
                        )
                    }

                    composable<ProcessingRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<ProcessingRoute>()
                        LaunchedEffect(route.jobId) {
                            viewModel.openJob(route.jobId)
                        }
                        val activeJob = uiState.activeJob?.takeIf { it.id == route.jobId }
                            ?: uiState.recentJobs.firstOrNull { it.id == route.jobId }
                        ProcessingScreen(
                            job = activeJob,
                            downloadStates = uiState.downloadStates,
                            onNavigateBack = { navController.popBackStack() },
                            onRefreshStatus = viewModel::refreshActiveJobOnce,
                            onRetryJob = viewModel::retryActiveJob,
                            onPreviewClip = { clip ->
                                navController.navigate(ClipPreviewRoute(clip.jobId, clip.index))
                            },
                            onDownloadClip = viewModel::downloadClip
                        )
                    }

                    composable<ClipPreviewRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<ClipPreviewRoute>()
                        val clip = viewModel.findClip(route.jobId, route.clipIndex)
                        val downloadStatus = clip?.let { uiState.downloadStates[it.id] } ?: DownloadStatus.IDLE
                        ClipPreviewScreen(
                            clip = clip,
                            downloadStatus = downloadStatus,
                            isReburningSubtitles = uiState.isReburningSubtitles,
                            onNavigateBack = { navController.popBackStack() },
                            onDownloadClip = viewModel::downloadClip,
                            onApplySubtitlePreset = viewModel::reburnSubtitles
                        )
                    }

                    composable<HelpSupportRoute> {
                        HelpSupportScreen(onNavigateBack = { navController.popBackStack() })
                    }

                    composable<PrivacyPolicyRoute> {
                        PrivacyPolicyScreen(onNavigateBack = { navController.popBackStack() })
                    }

                    composable<TermsConditionsRoute> {
                        TermsConditionsScreen(onNavigateBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
