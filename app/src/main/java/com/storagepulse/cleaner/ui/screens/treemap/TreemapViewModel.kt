package com.storagepulse.cleaner.ui.screens.treemap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepulse.cleaner.data.clean.CleanResult
import com.storagepulse.cleaner.data.clean.ClutterCleaner
import com.storagepulse.cleaner.data.storage.StorageScannerRepository
import com.storagepulse.cleaner.data.storage.StorageTree
import com.storagepulse.cleaner.data.storage.model.ClutterFile
import com.storagepulse.cleaner.data.storage.model.CleanupBreakdown
import com.storagepulse.cleaner.data.storage.model.DuplicateGroup
import com.storagepulse.cleaner.data.storage.model.ScanState
import com.storagepulse.cleaner.data.storage.model.StorageNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VisualMode { TREEMAP, SUNBURST }

sealed interface PurgeState {
    data object Idle : PurgeState
    data class Confirm(val items: List<ClutterFile>, val breakdown: CleanupBreakdown) : PurgeState
    data class Working(val done: Int, val total: Int) : PurgeState
    data class Finished(val result: CleanResult) : PurgeState
}

data class TreemapUiState(
    val scan: ScanState,
    val root: StorageNode? = null,
    /** Folder being shown (drill-down level). */
    val current: StorageNode? = null,
    /** Folder/file in the inspector card. */
    val selected: StorageNode? = null,
    val selectedClutter: List<ClutterFile> = emptyList(),
    val selectedDuplicates: List<DuplicateGroup> = emptyList(),
    val mode: VisualMode = VisualMode.TREEMAP,
    val purge: PurgeState = PurgeState.Idle,
) {
    val clutterBytes: Long get() = selectedClutter.sumOf { it.sizeBytes }
    val canZoomOut: Boolean get() = root != null && current != null && current.path != root.path
}

@HiltViewModel
class TreemapViewModel @Inject constructor(
    private val scanner: StorageScannerRepository,
    private val cleaner: ClutterCleaner,
) : ViewModel() {

    private val currentPath = MutableStateFlow<String?>(null)
    private val selectedPath = MutableStateFlow<String?>(null)
    private val mode = MutableStateFlow(VisualMode.TREEMAP)
    private val purge = MutableStateFlow<PurgeState>(PurgeState.Idle)

    val uiState: StateFlow<TreemapUiState> = combine(
        scanner.state, currentPath, selectedPath, mode, purge,
    ) { scan, curPath, selPath, m, p ->
        val snapshot = (scan as? ScanState.Done)?.snapshot
        val root = snapshot?.root
        if (snapshot == null || root == null) {
            return@combine TreemapUiState(scan = scan, mode = m, purge = p)
        }
        // Paths can vanish after a purge or rescan: fall back to the nearest valid level.
        val current = curPath?.let { StorageTree.find(root, it) }?.takeIf { it.isDirectory } ?: root
        val selected = selPath?.let { StorageTree.find(root, it) } ?: current
        val prefix = selected.path + "/"
        val clutter = snapshot.clutter.filter { it.path == selected.path || it.path.startsWith(prefix) }
        val duplicates = snapshot.duplicateGroups
            .filter { g -> g.extraCopies.any { it == selected.path || it.startsWith(prefix) } }
            .take(12)
        TreemapUiState(
            scan = scan,
            root = root,
            current = current,
            selected = selected,
            selectedClutter = clutter,
            selectedDuplicates = duplicates,
            mode = m,
            purge = p,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TreemapUiState(scanner.state.value))

    init {
        scanner.startScan()
    }

    fun setMode(m: VisualMode) { mode.value = m }

    fun select(node: StorageNode) { selectedPath.value = node.path }

    fun drillInto(node: StorageNode) {
        if (!node.isDirectory) return select(node)
        currentPath.value = node.path
        selectedPath.value = node.path
    }

    /** Breadcrumb / reset: jump to any ancestor folder. */
    fun goTo(path: String) {
        currentPath.value = path
        selectedPath.value = path
    }

    /** Back gesture / centre tap. Returns false at the root (let the system handle Back). */
    fun zoomOut(): Boolean {
        val state = uiState.value
        val root = state.root ?: return false
        val current = state.current ?: return false
        val parent = StorageTree.parentPath(root, current.path) ?: return false
        goTo(parent)
        return true
    }

    fun rescan() = scanner.startScan(force = true)

    // ------------------------------------------------------------------ purge

    fun requestPurge() {
        val items = uiState.value.selectedClutter
        if (items.isNotEmpty()) purge.value = PurgeState.Confirm(items, CleanupBreakdown.from(items))
    }

    fun dismissPurge() {
        if (purge.value !is PurgeState.Working) purge.value = PurgeState.Idle
    }

    fun confirmPurge() {
        val confirm = purge.value as? PurgeState.Confirm ?: return
        purge.value = PurgeState.Working(0, confirm.items.size)
        viewModelScope.launch {
            val result = cleaner.moveToSafeTrash(confirm.items) { done, total ->
                purge.update { if (it is PurgeState.Working) PurgeState.Working(done, total) else it }
            }
            purge.value = PurgeState.Finished(result)
        }
    }
}
