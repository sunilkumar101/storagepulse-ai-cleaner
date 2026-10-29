package com.storagepulse.cleaner.ui.screens.gallery

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.vault.VaultItemEntity
import com.storagepulse.cleaner.data.vault.VaultRepository
import com.storagepulse.cleaner.data.vault.VaultSession
import com.storagepulse.cleaner.ui.components.TelemetryBadge
import com.storagepulse.cleaner.ui.components.obsidianCard
import com.storagepulse.cleaner.ui.theme.AmberLight
import com.storagepulse.cleaner.ui.theme.EmeraldLight
import com.storagepulse.cleaner.ui.theme.Indigo
import com.storagepulse.cleaner.ui.theme.IndigoLight
import com.storagepulse.cleaner.ui.theme.ObsidianCanvas
import com.storagepulse.cleaner.ui.theme.OnEmeraldLight
import com.storagepulse.cleaner.ui.theme.SurfaceRaised
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.Telemetry
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import com.storagepulse.cleaner.ui.util.BiometricAuth
import com.storagepulse.cleaner.ui.util.formatBytes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vault: VaultRepository,
    private val session: VaultSession,
) : ViewModel() {
    val items: StateFlow<List<VaultItemEntity>> = vault.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _unlocked = MutableStateFlow(session.isUnlocked)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()
    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()
    private val _restored = MutableStateFlow<Int?>(null)
    val restored: StateFlow<Int?> = _restored.asStateFlow()

    fun onAuthenticated() { session.unlock(); _unlocked.value = true }
    /** Leaving the screen locks the vault again (thumbnails are only ever in memory). */
    fun lockNow() { session.lock(); _unlocked.value = false; _selected.value = emptySet() }
    fun toggle(id: Long) = _selected.update { if (id in it) it - id else it + id }

    fun restoreSelected() {
        val ids = _selected.value.toList()
        if (ids.isEmpty() || !session.isUnlocked) return
        viewModelScope.launch {
            val n = ids.count { vault.restore(it) != null }
            _selected.value = emptySet()
            _restored.value = n
        }
    }

    fun consumeRestored() { _restored.value = null }

    suspend fun thumbnail(item: VaultItemEntity): ImageBitmap? = withContext(Dispatchers.IO) {
        vault.thumbnail(item)?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
}

/** Encrypted private vault (requires biometric / screen-lock unlock). */
@Composable
fun VaultScreen(onBack: () -> Unit, viewModel: VaultViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val restored by viewModel.restored.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.lockNow() }
    val restoredText = restored?.let { androidx.compose.ui.res.pluralStringResource(R.plurals.vault_restore_done, it, it) }
    LaunchedEffect(restored) {
        if (restoredText != null) {
            snackbar.showSnackbar(restoredText)
            viewModel.consumeRestored()
        }
    }

    fun unlock() {
        val reason = BiometricAuth.unavailableReason(context)
        if (reason != null) {
            scope.launch { snackbar.showSnackbar(resources.getString(reason)) }
            return
        }
        BiometricAuth.authenticate(
            context,
            resources.getString(R.string.vault_prompt_title),
            resources.getString(R.string.vault_prompt_sub),
            onSuccess = viewModel::onAuthenticated,
            onError = { e -> scope.launch { snackbar.showSnackbar(resources.getString(R.string.vault_auth_failed, e)) } },
        )
    }

    Box(Modifier.fillMaxSize().background(ObsidianCanvas).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.disclosure_back), tint = TextPrimary) }
                Text(stringResource(R.string.vault_screen_title), style = MaterialTheme.typography.headlineSmall, color = TextPrimary, modifier = Modifier.weight(1f))
                if (unlocked) {
                    IconButton(onClick = viewModel::lockNow) { Icon(Icons.Outlined.Lock, stringResource(R.string.vault_lock_now), tint = TextPrimary) }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = SurfaceStroke)
            if (!unlocked) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp).obsidianCard().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(64.dp).background(Indigo.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Lock, null, tint = TextPrimary)
                    }
                    Text(stringResource(R.string.vault_locked_state), color = TextPrimary)
                    Button(onClick = ::unlock, colors = ButtonDefaults.buttonColors(containerColor = Indigo, contentColor = TextPrimary)) {
                        Icon(Icons.Outlined.Fingerprint, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.vault_unlock))
                    }
                }
                LaunchedEffect(Unit) { unlock() }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            Modifier.fillMaxWidth().background(AmberLight.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).border(1.dp, AmberLight.copy(alpha = 0.35f), RoundedCornerShape(12.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Warning, null, tint = AmberLight)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.vault_warning), style = MaterialTheme.typography.bodySmall, color = AmberLight)
                        }
                    }
                    if (items.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.vault_empty), color = TextSecondary) }
                    }
                    items(items, key = { it.id }) { item ->
                        VaultTile(item, item.id in selected, viewModel) { viewModel.toggle(item.id) }
                    }
                }
                if (selected.isNotEmpty()) {
                    Button(
                        onClick = viewModel::restoreSelected,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight, contentColor = OnEmeraldLight),
                    ) {
                        Icon(Icons.Outlined.Restore, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.vault_restore, selected.size))
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun VaultTile(item: VaultItemEntity, selected: Boolean, vm: VaultViewModel, onClick: () -> Unit) {
    val thumb by produceState<ImageBitmap?>(null, item.id) { value = vm.thumbnail(item) }
    Column(Modifier.clip(RoundedCornerShape(10.dp)).background(SurfaceRaised).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            thumb?.let { androidx.compose.foundation.Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            if (item.isVideo) {
                TelemetryBadge(stringResource(R.string.vault_video), IndigoLight, modifier = Modifier.align(Alignment.TopStart).padding(4.dp).background(ObsidianCanvas.copy(alpha = 0.7f), RoundedCornerShape(50)))
            }
            if (selected) Icon(Icons.Outlined.CheckCircle, null, tint = EmeraldLight, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(ObsidianCanvas, CircleShape))
        }
        Text(item.fileName, style = Telemetry.sm, color = TextPrimary, maxLines = 1, overflow = TextOverflow.MiddleEllipsis, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
        Text(formatBytes(item.sizeBytes), style = Telemetry.sm, color = TextSecondary, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
    }
}
