package com.storagepulse.cleaner.ui.screens.onboarding

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.settings.AccessMode
import com.storagepulse.cleaner.data.storage.StorageAccess
import com.storagepulse.cleaner.ui.components.TelemetryBadge
import com.storagepulse.cleaner.ui.components.obsidianCard
import com.storagepulse.cleaner.ui.theme.AmberLight
import com.storagepulse.cleaner.ui.theme.Emerald
import com.storagepulse.cleaner.ui.theme.EmeraldLight
import com.storagepulse.cleaner.ui.theme.IndigoLight
import com.storagepulse.cleaner.ui.theme.JetBrainsMono
import com.storagepulse.cleaner.ui.theme.ObsidianCanvas
import com.storagepulse.cleaner.ui.theme.OnEmeraldLight
import com.storagepulse.cleaner.ui.theme.StoragePulseTheme
import com.storagepulse.cleaner.ui.theme.SurfaceCard
import com.storagepulse.cleaner.ui.theme.SurfaceRaised
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.SurfaceSunken
import com.storagepulse.cleaner.ui.theme.Telemetry
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Screen 1 — Storage Access & Offline Privacy Disclosure.
 * Design: design_screenshots/stitch_storagepulse_ai_cleaner_app/storage_access_offline_privacy_disclosure/
 */
@Composable
fun StorageDisclosureScreen(
    onAccessReady: () -> Unit,
    viewModel: StorageDisclosureViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var awaitingAllFilesAccess by rememberSaveable { mutableStateOf(false) }
    var awaitingAppSettings by rememberSaveable { mutableStateOf(false) }
    var mediaBlocked by rememberSaveable { mutableStateOf(false) }
    var notice by rememberSaveable { mutableStateOf<Int?>(null) }
    var showPolicy by rememberSaveable { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    fun complete(mode: AccessMode) {
        if (finished) return
        finished = true
        scope.launch {
            viewModel.saveAccessMode(mode)
            onAccessReady()
        }
    }

    // After a denial, Android stops showing the dialog once rationale is no longer offered
    // ("Don't ask again"). From then on the Scoped button opens this app's Settings page.
    fun isMediaPermanentlyDenied(): Boolean {
        val activity = context as? Activity ?: return false
        return StorageAccess.mediaPermissions.none { activity.shouldShowRequestPermissionRationale(it) }
    }

    val mediaLauncher = rememberLauncherForActivityResult(RequestMultiplePermissions()) {
        when {
            StorageAccess.hasMediaAccess(context) -> complete(AccessMode.SCOPED)
            isMediaPermanentlyDenied() -> {
                mediaBlocked = true
                notice = R.string.notice_media_blocked
            }
            else -> notice = R.string.notice_media_denied
        }
    }
    val legacyLauncher = rememberLauncherForActivityResult(RequestMultiplePermissions()) {
        if (StorageAccess.hasAllFilesAccess(context)) {
            complete(AccessMode.FULL)
        } else {
            notice = R.string.notice_full_denied
        }
    }

    fun requestFullAccess() {
        notice = null
        if (StorageAccess.hasAllFilesAccess(context)) {
            complete(AccessMode.FULL)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val opened = StorageAccess.allFilesAccessIntents(context).any { intent ->
                runCatching { context.startActivity(intent) }.isSuccess
            }
            if (opened) {
                awaitingAllFilesAccess = true
            } else {
                // No settings screen on this ROM: fall back to media permissions.
                mediaLauncher.launch(StorageAccess.mediaPermissions)
            }
        } else {
            legacyLauncher.launch(StorageAccess.legacyPermissions)
        }
    }

    fun requestScopedAccess() {
        notice = null
        when {
            StorageAccess.hasMediaAccess(context) -> complete(AccessMode.SCOPED)
            mediaBlocked -> {
                val opened = runCatching {
                    context.startActivity(StorageAccess.appDetailsSettingsIntent(context))
                }.isSuccess
                if (opened) awaitingAppSettings = true
            }
            else -> mediaLauncher.launch(StorageAccess.mediaPermissions)
        }
    }

    // Coming back from a Settings page: continue if access was granted there.
    LifecycleResumeEffect(Unit) {
        when {
            StorageAccess.hasAllFilesAccess(context) -> complete(AccessMode.FULL)
            awaitingAppSettings -> {
                awaitingAppSettings = false
                if (StorageAccess.hasMediaAccess(context)) {
                    complete(AccessMode.SCOPED)
                } else {
                    notice = R.string.notice_media_blocked
                }
            }
            awaitingAllFilesAccess -> {
                // All Files Access not turned on: fall back to runtime media permissions.
                awaitingAllFilesAccess = false
                notice = R.string.notice_full_denied
                mediaLauncher.launch(StorageAccess.mediaPermissions)
            }
        }
        onPauseOrDispose { }
    }

    StorageDisclosureContent(
        notice = notice,
        onBack = { (context as? Activity)?.finish() },
        onInfo = { showPolicy = true },
        onGrantFullAccess = ::requestFullAccess,
        onScopedMode = ::requestScopedAccess,
    )

    if (showPolicy) {
        AlertDialog(
            onDismissRequest = { showPolicy = false },
            containerColor = SurfaceCard,
            title = { Text(stringResource(R.string.policy_dialog_title)) },
            text = { Text(stringResource(R.string.policy_dialog_body), color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showPolicy = false }) {
                    Text(stringResource(R.string.policy_dialog_ok), color = EmeraldLight)
                }
            },
        )
    }
}

@Composable
private fun StorageDisclosureContent(
    @StringRes notice: Int?,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    onGrantFullAccess: () -> Unit,
    onScopedMode: () -> Unit,
) {
    Scaffold(
        containerColor = ObsidianCanvas,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { DisclosureTopBar(onBack = onBack, onInfo = onInfo) },
        // Sticky CTA (as in the design): always visible, the disclosure scrolls above it.
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(ObsidianCanvas)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HorizontalDivider(color = SurfaceStroke)
                // Inline (not a snackbar) so it never covers the retry buttons.
                if (notice != null) PermissionNotice(stringResource(notice))
                Button(
                    onClick = onGrantFullAccess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldLight,
                        contentColor = OnEmeraldLight,
                    ),
                ) {
                    // Shrinks (16sp → 12sp) so the full label fits on 360dp phones.
                    BasicText(
                        text = stringResource(R.string.cta_grant),
                        style = MaterialTheme.typography.labelLarge.copy(color = OnEmeraldLight),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 16.sp),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.size(8.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                }
                TextButton(
                    onClick = onScopedMode,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.cta_scoped),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp) // readable column on tablets / foldables
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BrandCapsule()
                HeroPanel()
                HeadlineBlock()
                InfoCard(
                    icon = Icons.Outlined.AccountTree,
                    iconTint = EmeraldLight,
                    title = stringResource(R.string.card_treemap_title),
                    subtitle = stringResource(R.string.card_treemap_sub),
                    badge = stringResource(R.string.card_treemap_badge),
                    badgeColor = IndigoLight,
                ) {
                    val prefix = stringResource(R.string.card_treemap_body_prefix)
                    val code = stringResource(R.string.card_treemap_body_code)
                    val suffix = stringResource(R.string.card_treemap_body_suffix)
                    Text(
                        text = buildAnnotatedString {
                            append(prefix)
                            withStyle(
                                SpanStyle(
                                    fontFamily = JetBrainsMono,
                                    color = EmeraldLight,
                                    background = SurfaceSunken,
                                ),
                            ) { append(code) }
                            append(suffix)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                InfoCard(
                    icon = Icons.Outlined.Shield,
                    iconTint = IndigoLight,
                    title = stringResource(R.string.card_cloud_title),
                    subtitle = stringResource(R.string.card_cloud_sub),
                    badge = stringResource(R.string.card_cloud_badge),
                    badgeColor = EmeraldLight,
                ) {
                    BodyText(stringResource(R.string.card_cloud_body))
                }
                InfoCard(
                    icon = Icons.Outlined.DeleteSweep,
                    iconTint = AmberLight,
                    title = stringResource(R.string.card_trash_title),
                    subtitle = stringResource(R.string.card_trash_sub),
                    badge = stringResource(R.string.card_trash_badge),
                    badgeColor = AmberLight,
                ) {
                    BodyText(stringResource(R.string.card_trash_body))
                }
                SandboxGuarantee()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DisclosureTopBar(onBack: () -> Unit, onInfo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianCanvas)
            .windowInsetsPadding(
                WindowInsets.statusBars.only(WindowInsetsSides.Top),
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
            )
            .height(64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.disclosure_back),
                tint = TextPrimary,
            )
        }
        // Shrinks (22sp → 15sp) instead of clipping on narrow 360dp phones.
        BasicText(
            text = stringResource(R.string.disclosure_title),
            style = MaterialTheme.typography.headlineSmall.copy(color = TextPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 15.sp, maxFontSize = 22.sp),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onInfo) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = stringResource(R.string.disclosure_info),
                tint = TextPrimary,
            )
        }
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(40.dp)
                .background(EmeraldLight, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = OnEmeraldLight,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun BrandCapsule() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .obsidianCard(radius = 14)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(Emerald, CircleShape))
        Spacer(Modifier.size(10.dp))
        Text(
            text = stringResource(R.string.disclosure_brand),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TelemetryBadge(
            text = stringResource(R.string.disclosure_offline_badge),
            color = EmeraldLight,
            showDot = true,
        )
    }
}

@Composable
private fun HeroPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Emerald.copy(alpha = 0.16f), SurfaceSunken, ObsidianCanvas),
                ),
            )
            .border(1.dp, SurfaceStroke, RoundedCornerShape(20.dp))
            .padding(vertical = 20.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.hero_secure_storage),
            contentDescription = stringResource(R.string.disclosure_hero_desc),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Row(
            modifier = Modifier
                .background(SurfaceRaised, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Outlined.VerifiedUser,
                contentDescription = null,
                tint = EmeraldLight,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.disclosure_enclave),
                style = Telemetry.sm,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HeadlineBlock() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.disclosure_headline),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )
        Text(
            text = stringResource(R.string.disclosure_subhead),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    body: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .obsidianCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SurfaceRaised, RoundedCornerShape(10.dp))
                    .border(1.dp, SurfaceStroke, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 2, // wraps beside the badge on narrow phones instead of clipping
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = Telemetry.sm,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(8.dp))
            TelemetryBadge(text = badge, color = badgeColor)
        }
        body()
    }
}

@Composable
private fun BodyText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
}

@Composable
private fun SandboxGuarantee() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .obsidianCard(fill = SurfaceSunken)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Emerald.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Verified,
                contentDescription = null,
                tint = EmeraldLight,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.sandbox_title),
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            Text(
                text = stringResource(R.string.sandbox_body),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun PermissionNotice(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmberLight.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .border(1.dp, AmberLight.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = AmberLight,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = AmberLight)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF090D16, heightDp = 1600)
@Composable
private fun StorageDisclosurePreview() {
    StoragePulseTheme {
        StorageDisclosureContent(
            notice = R.string.notice_media_denied,
            onBack = {},
            onInfo = {},
            onGrantFullAccess = {},
            onScopedMode = {},
        )
    }
}
