package com.storagepulse.cleaner.ui.screens.clean

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepulse.cleaner.data.dedup.AiDeduplicatorEngine
import com.storagepulse.cleaner.data.dedup.ClusterType
import com.storagepulse.cleaner.data.dedup.DedupState
import com.storagepulse.cleaner.data.dedup.MediaCluster
import com.storagepulse.cleaner.data.storage.StorageAccess
import com.storagepulse.cleaner.data.storage.StorageScannerRepository
import com.storagepulse.cleaner.data.trash.SafeTrashRepository
import com.storagepulse.cleaner.data.trash.TrashMoveResult
import com.storagepulse.cleaner.ads.AdMobManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class DedupFilter { ALL, EXACT, SIMILAR, BLURRY, SCREENSHOTS }

sealed interface CleanMessage {
    data class Moved(val files: Int, val bytes: Long, val trashIds: List<Long>) : CleanMessage
    data object KeepOne : CleanMessage
    data object NeedsFullAccess : CleanMessage
    data class Restored(val files: Int) : CleanMessage
    data object Failed : CleanMessage
}

data class AiCleanUiState(
    val engine: DedupState = DedupState.Idle,
    val filter: DedupFilter = DedupFilter.ALL,
    val smartKeeper: Boolean = true,
    val selected: Set<String> = emptySet(),
    val clusters: List<MediaCluster> = emptyList(),
    val counts: Map<DedupFilter, Int> = emptyMap(),
    val busy: Boolean = false,
) {
    private val all: List<MediaCluster> get() = (engine as? DedupState.Ready)?.result?.clusters.orEmpty()
    val selectedBytes: Long get() = all.flatMap { it.items }.filter { it.path in selected }.distinctBy { it.path }.sumOf { it.sizeBytes }
    val flaggedFiles: Int get() = all.sumOf { it.suggested.size }
    val flaggedClusters: Int get() = all.count { it.suggested.isNotEmpty() }
}

@HiltViewModel
class AiCleanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: AiDeduplicatorEngine,
    private val safeTrash: SafeTrashRepository,
    private val scanner: StorageScannerRepository,
    private val ads: AdMobManager,
) : ViewModel() {

    /** After a finished clean-up: maybe an interstitial (free users, max once per 3 min). */
    fun afterClean(activity: android.app.Activity) {
        ads.maybeShowInterstitial(activity)
    }

    private val filter = MutableStateFlow(DedupFilter.ALL)
    private val smartKeeper = MutableStateFlow(true)
    private val selected = MutableStateFlow<Set<String>>(emptySet())
    private val busy = MutableStateFlow(false)
    private val _message = MutableStateFlow<CleanMessage?>(null)
    val message: StateFlow<CleanMessage?> = _message

    /** Analysis run whose suggestions were last applied (so removals don't re-seed). */
    private var seededFor: Any? = null

    val uiState: StateFlow<AiCleanUiState> = combine(engine.state, filter, smartKeeper, selected, busy) { st, f, keeper, sel, b ->
        val all = (st as? DedupState.Ready)?.result?.clusters.orEmpty()
        AiCleanUiState(
            engine = st,
            filter = f,
            smartKeeper = keeper,
            selected = sel,
            clusters = all.filter { it.matches(f) },
            counts = DedupFilter.entries.associateWith { fl -> all.filter { it.matches(fl) }.sumOf { it.items.size } },
            busy = b,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiCleanUiState())

    init {
        engine.analyze()
        ads.preloadInterstitial()
        viewModelScope.launch {
            engine.state.collect { st ->
                if (st !is DedupState.Ready) return@collect
                val live = st.result.clusters.flatMap { c -> c.items.map { it.path } }.toSet()
                // A new analysis (not just a removal) re-applies Smart Keeper suggestions.
                val runKey = st.result.durationMs to st.result.analyzedImages
                if (seededFor != runKey) {
                    seededFor = runKey
                    selected.value = if (smartKeeper.value) st.result.clusters.flatMap { it.suggested }.toSet() else emptySet()
                } else {
                    selected.value = selected.value intersect live
                }
            }
        }
    }

    fun setFilter(f: DedupFilter) { filter.value = f }

    fun setSmartKeeper(on: Boolean) {
        smartKeeper.value = on
        val all = (engine.state.value as? DedupState.Ready)?.result?.clusters.orEmpty()
        selected.value = if (on) all.flatMap { it.suggested }.toSet() else emptySet()
    }

    fun toggle(path: String) {
        val current = selected.value
        if (path in current) {
            selected.value = current - path
            return
        }
        val cluster = clusterOf(path)
        if (cluster != null && cluster.needsSurvivor() && cluster.items.all { it.path == path || it.path in current }) {
            _message.value = CleanMessage.KeepOne
            return
        }
        selected.value = current + path
    }

    fun deselectCluster(cluster: MediaCluster) {
        selected.value = selected.value - cluster.items.map { it.path }.toSet()
    }

    fun selectSuggested(cluster: MediaCluster) {
        selected.value = selected.value + cluster.suggested
    }

    fun selectedIn(cluster: MediaCluster): List<String> =
        cluster.items.map { it.path }.filter { it in uiState.value.selected }

    /** Trash one file from its tile (still never the last copy of a group). */
    fun trashOne(path: String) {
        val cluster = clusterOf(path)
        if (cluster != null && cluster.needsSurvivor()) {
            val remaining = cluster.items.count { it.path != path }
            if (remaining < 1) {
                _message.value = CleanMessage.KeepOne
                return
            }
        }
        moveToTrash(listOf(path))
    }

    fun trashCluster(cluster: MediaCluster) = moveToTrash(selectedIn(cluster))

    fun trashSelected() = moveToTrash(uiState.value.selected.toList())

    private fun moveToTrash(paths: List<String>) {
        if (paths.isEmpty() || busy.value) return
        if (!StorageAccess.hasAllFilesAccess(context)) {
            _message.value = CleanMessage.NeedsFullAccess
            return
        }
        // Final guard: every exact/similar group keeps at least one photo.
        val all = (engine.state.value as? DedupState.Ready)?.result?.clusters.orEmpty()
        val wiped = all.any { c -> c.needsSurvivor() && c.items.all { it.path in paths } }
        if (wiped) {
            _message.value = CleanMessage.KeepOne
            return
        }
        busy.value = true
        viewModelScope.launch {
            val results = safeTrash.moveToSafeTrash(paths.map(::File))
            val moved = results.filterIsInstance<TrashMoveResult.Moved>()
            val movedPaths = moved.map { it.item.originalPath }.toSet()
            engine.remove(movedPaths)
            scanner.applyRemoval(moved.associate { it.item.originalPath to it.item.sizeBytes })
            selected.value = selected.value - movedPaths
            _message.value = if (moved.isEmpty()) {
                CleanMessage.Failed
            } else {
                CleanMessage.Moved(moved.size, moved.sumOf { it.item.sizeBytes }, moved.map { it.item.id })
            }
            busy.value = false
        }
    }

    /** Undo the last move: restore every file, then refresh analysis + storage map. */
    fun undo(trashIds: List<Long>) {
        viewModelScope.launch {
            busy.value = true
            var restored = 0
            trashIds.forEach { id ->
                if (safeTrash.restoreFromTrash(id) is com.storagepulse.cleaner.data.trash.TrashRestoreResult.Restored) restored++
            }
            busy.value = false
            _message.value = CleanMessage.Restored(restored)
            engine.analyze(force = true)
            scanner.startScan(force = true)
        }
    }

    fun reanalyze() = engine.analyze(force = true)

    fun consumeMessage() { _message.value = null }

    private fun clusterOf(path: String): MediaCluster? =
        (engine.state.value as? DedupState.Ready)?.result?.clusters?.firstOrNull { c -> c.items.any { it.path == path } }

    private fun MediaCluster.needsSurvivor() =
        type == ClusterType.EXACT_DUPLICATES || type == ClusterType.SIMILAR_BURST

    private fun MediaCluster.matches(f: DedupFilter) = when (f) {
        DedupFilter.ALL -> true
        DedupFilter.EXACT -> type == ClusterType.EXACT_DUPLICATES
        DedupFilter.SIMILAR -> type == ClusterType.SIMILAR_BURST
        DedupFilter.BLURRY -> type == ClusterType.BLURRY
        DedupFilter.SCREENSHOTS -> type == ClusterType.OLD_SCREENSHOTS
    }
}
