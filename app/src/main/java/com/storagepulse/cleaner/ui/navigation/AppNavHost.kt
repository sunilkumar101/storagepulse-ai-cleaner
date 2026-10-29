package com.storagepulse.cleaner.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import com.storagepulse.cleaner.ui.components.AdBanner
import com.storagepulse.cleaner.ui.screens.settings.WidgetsAndProSettingsScreen
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.storagepulse.cleaner.ui.screens.treemap.DiskTreemapScreen
import com.storagepulse.cleaner.ui.screens.gallery.FastGalleryAndVaultScreen
import com.storagepulse.cleaner.ui.screens.gallery.VaultScreen
import com.storagepulse.cleaner.ui.screens.tools.SafeTrashAndAppsScreen
import com.storagepulse.cleaner.ui.screens.tools.SafeTrashAndAppsViewModel
import com.storagepulse.cleaner.ui.screens.clean.AiDuplicateCleanerScreen
import com.storagepulse.cleaner.ui.screens.clean.SwipeNav
import com.storagepulse.cleaner.ui.screens.clean.SwipeToCleanScreen
import com.storagepulse.cleaner.ui.components.CleanerTab
import com.storagepulse.cleaner.ui.screens.compressor.CompressorNav
import com.storagepulse.cleaner.ui.screens.compressor.VideoCompressorScreen
import com.storagepulse.cleaner.ui.screens.social.SocialCleanerScreen
import com.storagepulse.cleaner.ui.screens.social.SocialNav
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.ui.components.AppBottomNavigationBar
import com.storagepulse.cleaner.ui.screens.dashboard.DashboardActions
import com.storagepulse.cleaner.ui.screens.dashboard.MainStorageCommandCenterScreen
import com.storagepulse.cleaner.ui.screens.onboarding.StorageDisclosureScreen
import com.storagepulse.cleaner.ui.theme.ObsidianCanvas

@Composable
fun AppNavHost(viewModel: AppStartViewModel = hiltViewModel()) {
    val startRoute by viewModel.startRoute.collectAsStateWithLifecycle()

    // Wait until we know whether storage access is already granted (one DataStore read).
    val start = startRoute ?: run {
        Box(Modifier.fillMaxSize().background(ObsidianCanvas))
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val adsEnabled by viewModel.adsEnabled.collectAsStateWithLifecycle()

    // Access revoked in Settings while the app was away: Android restores the old back stack,
    // so send the user back to the disclosure instead of showing empty screens.
    val accessScope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        val job = accessScope.launch {
            if (!viewModel.hasAccess() && navController.currentDestination?.route != Routes.ONBOARDING) {
                navController.navigate(Routes.ONBOARDING) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        onPauseOrDispose { job.cancel() }
    }

    // Widget / notification deep links (only once storage access is set up).
    val deepLink by DeepLinks.pending.collectAsStateWithLifecycle()
    LaunchedEffect(deepLink, start) {
        val route = deepLink ?: return@LaunchedEffect
        if (start == Routes.ONBOARDING) return@LaunchedEffect
        DeepLinks.consume()
        when {
            // Dashboard is the root: pop to it (navigateToTab would restore the saved stack on top).
            route == Routes.DASHBOARD -> if (!navController.popBackStack(Routes.DASHBOARD, inclusive = false)) navController.navigateToTab(route)
            route in Routes.tabRoutes -> navController.navigateToTab(route)
            else -> navController.navigate(route) { launchSingleTop = true }
        }
    }

    Scaffold(
        containerColor = ObsidianCanvas,
        // Each screen handles its own insets; the bottom bar pads for the nav bar itself.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentRoute in Routes.tabRoutes) {
                Column {
                    if (adsEnabled) AdBanner(viewModel.bannerUnitId, Modifier.background(ObsidianCanvas))
                    AppBottomNavigationBar(
                        currentRoute = currentRoute,
                        onTabSelected = { tab -> navController.navigateToTab(tab.route) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.ONBOARDING) {
                StorageDisclosureScreen(
                    onAccessReady = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.DASHBOARD) {
                MainStorageCommandCenterScreen(
                    actions = DashboardActions(
                        onReviewAndClean = { navController.navigateToTab(Routes.AI_CLEAN) },
                        onOpenTreemap = { navController.navigateToTab(Routes.TREEMAP) },
                        onOpenSwipeDeck = { navController.navigate(Routes.SWIPE_DECK) },
                        onOpenCompressor = { navController.navigate(Routes.COMPRESSOR) },
                        onOpenChatVaults = { navController.navigate(Routes.CHAT_CLEANER) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    ),
                )
            }
            composable(Routes.TREEMAP) {
                DiskTreemapScreen(
                    onCompressFolder = { folder -> navController.navigate(Routes.compressor(folder)) },
                )
            }
            composable(Routes.AI_CLEAN) { AiDuplicateCleanerScreen() }
            composable(Routes.GALLERY) {
                FastGalleryAndVaultScreen(
                    onOpenVault = { navController.navigate(Routes.VAULT) },
                    onCleanScreenshots = { navController.navigateToTab(Routes.AI_CLEAN) },
                )
            }
            composable(Routes.TOOLS) { SafeTrashAndAppsScreen() }
            composable(Routes.VAULT) { VaultScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS) { WidgetsAndProSettingsScreen(onBack = { navController.popBackStack() }) }

            composable(Routes.SWIPE_DECK) {
                SwipeToCleanScreen(
                    nav = SwipeNav(
                        onBack = { navController.popBackStack() },
                        onCompressor = { path ->
                            navController.navigate(if (path != null) Routes.compressor(path) else Routes.COMPRESSOR)
                        },
                        onSocial = { navController.navigate(Routes.CHAT_CLEANER) },
                        onSafeTrash = { navController.navigate(Routes.SAFE_TRASH) },
                    ),
                )
            }
            composable(
                route = Routes.SAFE_TRASH,
                arguments = listOf(navArgument(SafeTrashAndAppsViewModel.ARG_TAB) { type = NavType.StringType; defaultValue = "trash" }),
            ) { SafeTrashAndAppsScreen() }
            composable(
                route = Routes.COMPRESSOR_PATTERN,
                arguments = listOf(
                    navArgument(Routes.ARG_FOLDER) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                VideoCompressorScreen(
                    nav = CompressorNav(
                        onBack = { navController.popBackStack() },
                        onTab = { tab -> navController.navigateCleaner(tab) },
                    ),
                )
            }
            composable(Routes.CHAT_CLEANER) {
                SocialCleanerScreen(
                    nav = SocialNav(
                        onBack = { navController.popBackStack() },
                        onTab = { tab -> navController.navigateCleaner(tab) },
                    ),
                )
            }
        }
    }
}

/**
 * Cleaner sub-nav (Swipe Deck | Compressor | Social | Safe Trash): replaces the current cleaner
 * screen instead of stacking them, so Back always returns to where the user came from.
 */
private fun NavHostController.navigateCleaner(tab: CleanerTab) {
    val route = when (tab) {
        CleanerTab.SWIPE_DECK -> Routes.SWIPE_DECK
        CleanerTab.COMPRESSOR -> Routes.COMPRESSOR
        CleanerTab.SOCIAL -> Routes.CHAT_CLEANER
        CleanerTab.SAFE_TRASH -> Routes.SAFE_TRASH
    }
    val current = currentDestination?.route
    navigate(route) {
        if (current != null) popUpTo(current) { inclusive = true }
        launchSingleTop = true
    }
}

/** Switch tabs keeping each tab's state; Dashboard stays the root so Back returns there. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.DASHBOARD) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
