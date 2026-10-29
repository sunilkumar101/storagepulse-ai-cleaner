package com.storagepulse.cleaner.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Rocket
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storagepulse.cleaner.BuildConfig
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.billing.ProEvent
import com.storagepulse.cleaner.data.settings.AccentColor
import com.storagepulse.cleaner.data.settings.SettingsRepository
import com.storagepulse.cleaner.notify.WeeklyClutterWorker
import com.storagepulse.cleaner.ui.components.AutoSizeText
import com.storagepulse.cleaner.ui.components.TelemetryBadge
import com.storagepulse.cleaner.ui.components.obsidianCard
import com.storagepulse.cleaner.ui.theme.AmberLight
import com.storagepulse.cleaner.ui.theme.Indigo
import com.storagepulse.cleaner.ui.theme.IndigoLight
import com.storagepulse.cleaner.ui.theme.ObsidianCanvas
import com.storagepulse.cleaner.ui.theme.SurfaceCard
import com.storagepulse.cleaner.ui.theme.SurfaceRaised
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.Telemetry
import com.storagepulse.cleaner.ui.theme.TextMuted
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import com.storagepulse.cleaner.ui.theme.accentOf
import kotlinx.coroutines.launch

/** Screen 10: home screen widgets, StoragePulse Pro and automation settings. */
@Composable
fun WidgetsAndProSettingsScreen(
    onBack: () -> Unit,
    viewModel: WidgetsAndProSettingsViewModel = hiltViewModel(),
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val purchased by viewModel.purchased.collectAsStateWithLifecycle()
    val debugPro by viewModel.debugOverride.collectAsStateWithLifecycle()
    val price by viewModel.price.collectAsStateWithLifecycle()
    val retention by viewModel.retentionDays.collectAsStateWithLifecycle()
    val weekly by viewModel.weekly.collectAsStateWithLifecycle()
    val accent by viewModel.accent.collectAsStateWithLifecycle()
    val adPrivacy by viewModel.adPrivacyRequired.collectAsStateWithLifecycle()

    val activity = LocalActivity.current
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.setWeekly(true)
        else scope.launch { snackbar.showSnackbar(resources.getString(R.string.st_notify_denied)) }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { m ->
            val text = when (m) {
                SettingsMessage.PinRequested -> R.string.st_pin_requested
                SettingsMessage.PinUnsupported -> R.string.st_pin_unsupported
                SettingsMessage.ProRequired -> R.string.st_pro_required
                is SettingsMessage.Pro -> when (m.event) {
                    ProEvent.Purchased -> R.string.st_pro_thanks
                    ProEvent.Pending -> R.string.st_pro_pending
                    ProEvent.Cancelled -> R.string.st_pro_cancelled
                    ProEvent.Restored -> R.string.st_pro_restored
                    ProEvent.NothingToRestore -> R.string.st_pro_nothing
                    ProEvent.Unavailable -> R.string.st_pro_unavailable
                    is ProEvent.Error -> R.string.st_pro_error
                }
            }
            if (m == SettingsMessage.ProRequired) listState.animateScrollToItem(PRO_ITEM_INDEX)
            snackbar.showSnackbar(resources.getString(text))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Header(isPro, onBack) }
            item { Hero() }
            item { SectionTitle(Icons.Outlined.Widgets, stringResource(R.string.st_gallery_title), stringResource(R.string.st_gallery_badge)) }
            item {
                WidgetSlot(stringResource(R.string.st_ff_4x2), stringResource(R.string.st_pin_4x2), locked = !isPro, onPin = { viewModel.pin(WidgetKind.BOOST_4X2) }) {
                    BoostWidgetPreview(data)
                }
            }
            item {
                WidgetSlot(stringResource(R.string.st_ff_2x2), stringResource(R.string.st_pin_2x2), locked = !isPro, onPin = { viewModel.pin(WidgetKind.CLUTTER_2X2) }) {
                    Row { Box(Modifier.weight(1f)) { ClutterWidgetPreview(data) }; Spacer(Modifier.weight(0.15f)) }
                }
            }
            item {
                WidgetSlot(stringResource(R.string.st_ff_4x1), stringResource(R.string.st_pin_4x1), locked = false, onPin = { viewModel.pin(WidgetKind.STRIP_4X1) }) {
                    StripWidgetPreview(data)
                }
            }
            item {
                ProCard(
                    isPro = isPro,
                    purchased = purchased,
                    price = price ?: stringResource(R.string.st_pro_default_price),
                    onBuy = { activity?.let(viewModel::buy) },
                    onRestore = viewModel::restore,
                )
            }
            item { SectionTitle(Icons.Outlined.Tune, stringResource(R.string.st_engine_title), null) }
            item {
                EngineCard(
                    weekly = weekly,
                    onWeekly = { on ->
                        val needsPermission = on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            activity != null && !WeeklyClutterWorker.canNotify(activity)
                        if (needsPermission) {
                            notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setWeekly(on)
                        }
                    },
                    retention = retention,
                    onRetention = viewModel::setRetention,
                    accent = accent,
                    onAccent = viewModel::setAccent,
                )
            }
            item {
                PrivacyCard(
                    showAdChoices = adPrivacy && !isPro,
                    onAdChoices = { activity?.let(viewModel::showAdPrivacy) },
                )
            }
            if (BuildConfig.DEBUG) item { DebugCard(debugPro, viewModel::setDebugPro) }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

/** Index of the Pro card in the list (scrolled to when a Pro widget is tapped). */
private const val PRO_ITEM_INDEX = 6

@Composable
private fun Header(isPro: Boolean, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.st_back), tint = TextPrimary) }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.app_short_name), style = MaterialTheme.typography.titleLarge, color = TextPrimary, maxLines = 1)
            Text(stringResource(R.string.st_header_sub), style = Telemetry.labelCaps, color = TextSecondary, maxLines = 1)
        }
        if (isPro) TelemetryBadge(stringResource(R.string.st_pro_badge), MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Hero() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.st_title), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Text(stringResource(R.string.st_subtitle), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String, badge: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
        if (badge != null) TelemetryBadge(badge, AmberLight)
    }
}

@Composable
private fun WidgetSlot(label: String, pinText: String, locked: Boolean, onPin: () -> Unit, preview: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().obsidianCard(20, SurfaceCard.copy(alpha = 0.6f)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AutoSizeText(label, Telemetry.labelCaps, TextSecondary, Modifier.weight(1f), minSize = 7.sp)
            Spacer(Modifier.width(8.dp))
            if (locked) TelemetryBadge(stringResource(R.string.st_pro_chip), IndigoLight) else TelemetryBadge(stringResource(R.string.st_live_chip), MaterialTheme.colorScheme.primary, showDot = true)
        }
        preview()
        OutlinedButton(
            onClick = onPin,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceStroke),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceRaised, contentColor = TextPrimary),
        ) {
            Icon(if (locked) Icons.Outlined.Lock else Icons.Outlined.AddToHomeScreen, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            AutoSizeText(pinText, MaterialTheme.typography.labelLarge, TextPrimary, minSize = 10.sp)
        }
    }
}

@Composable
private fun ProCard(isPro: Boolean, purchased: Boolean, price: String, onBuy: () -> Unit, onRestore: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SurfaceCard).border(1.dp, Indigo.copy(alpha = 0.5f), RoundedCornerShape(24.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(if (isPro) R.string.st_pro_active_title else R.string.st_pro_title), style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Text(stringResource(if (isPro) R.string.st_pro_active_sub else R.string.st_pro_sub, price), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(48.dp).background(Indigo, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Rocket, null, tint = TextPrimary)
            }
        }
        listOf(
            R.string.st_pro_b1_title to R.string.st_pro_b1,
            R.string.st_pro_b2_title to R.string.st_pro_b2,
            R.string.st_pro_b3_title to R.string.st_pro_b3,
        ).forEach { (t, b) ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.CheckCircle, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    androidx.compose.ui.text.buildAnnotatedString {
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = TextPrimary))
                        append(stringResource(t))
                        pop()
                        append(" ")
                        append(stringResource(b))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        if (!isPro) {
            Button(
                onClick = onBuy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = MaterialTheme.colorScheme.onPrimary),
            ) {
                Icon(Icons.Outlined.WorkspacePremium, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                AutoSizeText(stringResource(R.string.st_pro_buy, price), MaterialTheme.typography.titleSmall, MaterialTheme.colorScheme.onPrimary, minSize = 11.sp)
            }
        } else if (!purchased) {
            TelemetryBadge(stringResource(R.string.st_pro_debug_active), AmberLight)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onRestore, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.st_pro_restore), style = Telemetry.sm, color = TextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            AutoSizeText(stringResource(R.string.st_pro_secure), Telemetry.sm, TextMuted, Modifier.weight(1f), minSize = 8.sp)
        }
    }
}

@Composable
private fun EngineCard(
    weekly: Boolean,
    onWeekly: (Boolean) -> Unit,
    retention: Int,
    onRetention: (Int) -> Unit,
    accent: AccentColor,
    onAccent: (AccentColor) -> Unit,
) {
    Column(Modifier.fillMaxWidth().obsidianCard(20).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // Weekly notification
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingIcon(Icons.Outlined.NotificationsActive)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.st_weekly_title), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(stringResource(R.string.st_weekly_sub), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Switch(
                checked = weekly,
                onCheckedChange = onWeekly,
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary),
            )
        }
        // Safe Trash retention
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingIcon(Icons.Outlined.DeleteOutline)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.st_retention_title), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(stringResource(R.string.st_retention_sub), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                TelemetryBadge(androidx.compose.ui.res.pluralStringResource(R.plurals.st_days_caps, retention, retention), AmberLight)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsRepository.ALLOWED_RETENTION_DAYS.sorted().forEach { d ->
                    Chip(androidx.compose.ui.res.pluralStringResource(R.plurals.st_days, d, d), d == retention, null, Modifier.weight(1f)) { onRetention(d) }
                }
            }
        }
        // Accent
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingIcon(Icons.Outlined.Palette)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.st_accent_title), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(stringResource(R.string.st_accent_sub), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentColor.entries.forEach { a ->
                    val label = when (a) {
                        AccentColor.OBSIDIAN -> R.string.st_accent_obsidian
                        AccentColor.INDIGO -> R.string.st_accent_indigo
                        AccentColor.AMBER -> R.string.st_accent_amber
                    }
                    Chip(stringResource(label), a == accent, accentOf(a).base, Modifier.weight(1f)) { onAccent(a) }
                }
            }
        }
    }
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    Box(Modifier.size(40.dp).background(SurfaceRaised, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
    Spacer(Modifier.width(12.dp))
}

@Composable
private fun Chip(text: String, selected: Boolean, dot: Color?, modifier: Modifier, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent else SurfaceRaised)
            .border(1.dp, if (selected) accent else SurfaceStroke, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (dot != null) {
            Box(Modifier.size(10.dp).background(dot, CircleShape).border(1.dp, if (selected) MaterialTheme.colorScheme.onPrimary else Color.Transparent, CircleShape))
            Spacer(Modifier.width(6.dp))
        }
        AutoSizeText(text, Telemetry.sm, if (selected) MaterialTheme.colorScheme.onPrimary else TextPrimary, minSize = 8.sp)
    }
}

@Composable
private fun PrivacyCard(showAdChoices: Boolean, onAdChoices: () -> Unit) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val policyUrl = stringResource(R.string.privacy_policy_url)
    Row(Modifier.fillMaxWidth().obsidianCard(16, SurfaceRaised).padding(16.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.st_privacy_title), style = Telemetry.labelCaps, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.st_privacy_body), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { uriHandler.openUri(policyUrl) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.st_privacy_policy), color = MaterialTheme.colorScheme.primary)
                }
                if (showAdChoices) {
                    TextButton(onClick = onAdChoices, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.st_ad_choices), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

/** Debug builds only: fake Pro for testing widgets and ad hiding without a Play purchase. */
@Composable
private fun DebugCard(on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().obsidianCard(16).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.BugReport, null, tint = AmberLight, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.st_debug_title), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(stringResource(R.string.st_debug_sub), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Switch(checked = on, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = AmberLight))
    }
}
