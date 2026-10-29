package com.storagepulse.cleaner.ui.screens.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepulse.cleaner.data.gallery.GalleryIndex
import com.storagepulse.cleaner.data.gallery.GalleryRepository
import com.storagepulse.cleaner.data.gallery.MediaEntry
import com.storagepulse.cleaner.data.gallery.MediaFolder
import com.storagepulse.cleaner.data.vault.VaultRepository
import com.storagepulse.cleaner.data.vault.VaultSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class GalleryMode { DCIM, ALBUMS }
enum class GallerySort { SIZE, DATE, NAME }

sealed interface GalleryMessage {
    data class GpsStripped(val stripped: Int, val skipped: Int) : GalleryMessage
    data class Locked(val locked: Int, val failed: Int) : GalleryMessage
    data object CoverSet : GalleryMessage
    data class Cropped(val name: String) : GalleryMessage
    data object CropFailed : GalleryMessage
}

data class GalleryUiState(
    val loading: Boolean = true,
    val index: GalleryIndex? = null,
    val mode: GalleryMode = GalleryMode.ALBUMS,
    val query: String = "",
    val sort: GallerySort = GallerySort.SIZE,
    val columns: Int = 3,
    val openFolder: String? = null,
    val selected: Set<String> = emptySet(),
    val busy: Boolean = false,
) {
    val folders: List<MediaFolder>
        get() {
            val all = index?.folders.orEmpty().filter { query.isBlank() || it.name.contains(query, true) || it.path.contains(query, true) }
            return when (sort) {
                GallerySort.SIZE -> all.sortedByDescending { it.bytes }
                GallerySort.DATE -> all.sortedByDescending { it.latest }
                GallerySort.NAME -> all.sortedBy { it.name.lowercase() }
            }
        }

    /** Media grid: the open folder, or the camera roll in DCIM mode. */
    val media: List<MediaEntry>
        get() {
            val src = index?.entries.orEmpty()
            val scoped = when {
                openFolder != null -> src.filter { it.folder == openFolder }
                mode == GalleryMode.DCIM -> src.filter { it.path.contains("/DCIM/", ignoreCase = true) }
                else -> emptyList()
            }.filter { query.isBlank() || it.name.contains(query, true) }
            return when (sort) {
                GallerySort.SIZE -> scoped.sortedByDescending { it.sizeBytes }
                GallerySort.DATE -> scoped.sortedByDescending { it.modified }
                GallerySort.NAME -> scoped.sortedBy { it.name.lowercase() }
            }
        }

    val showsMedia: Boolean get() = openFolder != null || mode == GalleryMode.DCIM
    val selectedEntries: List<MediaEntry> get() = index?.entries.orEmpty().filter { it.path in selected }
}

data class VaultSummary(val photos: Int = 0, val videos: Int = 0, val hardwareBacked: Boolean = false)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repo: GalleryRepository,
    private val vault: VaultRepository,
    private val session: VaultSession,
) : ViewModel() {

    private val _state = MutableStateFlow(GalleryUiState())
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()
    private val _message = MutableStateFlow<GalleryMessage?>(null)
    val message: StateFlow<GalleryMessage?> = _message.asStateFlow()

    val vaultSummary: StateFlow<VaultSummary> = combine(vault.photoCount(), vault.videoCount()) { p, v ->
        VaultSummary(p, v, vault.isHardwareBacked)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaultSummary())

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            val index = repo.load()
            _state.update { s -> s.copy(loading = false, index = index, selected = s.selected.filter { p -> index.entries.any { it.path == p } }.toSet()) }
        }
    }

    fun setMode(m: GalleryMode) = _state.update { it.copy(mode = m, openFolder = null, selected = emptySet()) }
    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun cycleSort() = _state.update { it.copy(sort = GallerySort.entries[(it.sort.ordinal + 1) % GallerySort.entries.size]) }
    fun setColumns(n: Int) = _state.update { it.copy(columns = n) }
    fun openFolder(path: String) = _state.update { it.copy(openFolder = path, selected = emptySet()) }
    fun closeFolder() = _state.update { it.copy(openFolder = null, selected = emptySet()) }
    fun toggle(path: String) = _state.update { it.copy(selected = if (path in it.selected) it.selected - path else it.selected + path) }
    fun clearSelection() = _state.update { it.copy(selected = emptySet()) }

    fun stripGps() = work {
        val r = repo.stripGps(_state.value.selected.toList())
        _message.value = GalleryMessage.GpsStripped(r.stripped, r.skipped)
    }

    fun lockSelected() = work {
        val r = vault.lock(_state.value.selected.toList())
        _state.update { it.copy(selected = it.selected - r.lockedPaths) }
        _message.value = GalleryMessage.Locked(r.locked, r.failed)
        reload()
    }

    fun setCover() = work {
        val s = _state.value
        val path = s.selected.singleOrNull() ?: return@work
        repo.setCover(File(path).parent ?: return@work, path)
        _message.value = GalleryMessage.CoverSet
        _state.update { it.copy(selected = emptySet()) }
        reload()
    }

    fun saveCrop(path: String, l: Float, t: Float, r: Float, b: Float) = work {
        val out = repo.saveCrop(path, l, t, r, b)
        _message.value = out?.let { GalleryMessage.Cropped(it.name) } ?: GalleryMessage.CropFailed
        reload()
    }

    fun unlockVault() = session.unlock()

    fun consumeMessage() { _message.value = null }

    private fun work(block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { block() } finally { _state.update { it.copy(busy = false) } }
        }
    }
}
