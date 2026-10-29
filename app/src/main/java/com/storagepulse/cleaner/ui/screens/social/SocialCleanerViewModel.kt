package com.storagepulse.cleaner.ui.screens.social

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepulse.cleaner.data.social.SocialApp
import com.storagepulse.cleaner.data.social.SocialCategory
import com.storagepulse.cleaner.data.social.SocialChatCleanerRepository
import com.storagepulse.cleaner.data.social.SocialFile
import com.storagepulse.cleaner.data.social.SocialScan
import com.storagepulse.cleaner.data.storage.StorageAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class AgeFilter { ALL, OLDER_30D, OLDER_6M, OVER_50MB }

sealed interface SocialMessage {
    data class Cleaned(val files: Int, val bytes: Long, val trashIds: List<Long>) : SocialMessage
    data class Restored(val files: Int) : SocialMessage
    data object NeedsFullAccess : SocialMessage
}

data class SocialUiState(
    val loading: Boolean = true,
    val needsFullAccess: Boolean = false,
    val scans: Map<SocialApp, SocialScan> = emptyMap(),
    val app: SocialApp = SocialApp.WHATSAPP,
    val filter: AgeFilter = AgeFilter.ALL,
    val selected: Set<String> = emptySet(),
    val busy: Boolean = false,
) {
    val scan: SocialScan? get() = scans[app]

    /** Files of the current app that pass the age/size filter. */
    val visible: List<SocialFile>
        get() {
            val now = System.currentTimeMillis()
            return scan?.files.orEmpty().filter { f ->
                when (filter) {
                    AgeFilter.ALL -> true
                    AgeFilter.OLDER_30D -> now - f.lastModified > TimeUnit.DAYS.toMillis(30)
                    AgeFilter.OLDER_6M -> now - f.lastModified > TimeUnit.DAYS.toMillis(182)
                    AgeFilter.OVER_50MB -> f.sizeBytes > 50_000_000L
                }
            }
        }

    fun inCategory(c: SocialCategory) = visible.filter { it.category == c }
    val selectedVisible: List<SocialFile> get() = visible.filter { it.path in selected }
}

@HiltViewModel
class SocialCleanerViewModel @Inject constructor(
    private val repo: SocialChatCleanerRepository,
    private val context: Application,
) : ViewModel() {

    private val _state = MutableStateFlow(SocialUiState())
    val state: StateFlow<SocialUiState> = _state.asStateFlow()
    private val _message = MutableStateFlow<SocialMessage?>(null)
    val message: StateFlow<SocialMessage?> = _message.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (!StorageAccess.hasAllFilesAccess(context)) {
            _state.update { it.copy(loading = false, needsFullAccess = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, needsFullAccess = false) }
            val scans = repo.scanAll()
            _state.update { it.copy(loading = false, scans = scans, selected = defaultSelection(scans)) }
        }
    }

    /**
     * Pre-selects only what is safe to lose: sent copies, status/sticker cache, and voice notes
     * older than 90 days. Received videos are never pre-selected.
     */
    private fun defaultSelection(scans: Map<SocialApp, SocialScan>): Set<String> {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
        return scans.values.flatMap { it.files }.filter { f ->
            when (f.category) {
                SocialCategory.SENT, SocialCategory.STATUS -> true
                SocialCategory.VOICE -> f.lastModified < cutoff
                else -> false
            }
        }.map { it.path }.toSet()
    }

    fun setApp(app: SocialApp) = _state.update { it.copy(app = app) }
    fun setFilter(f: AgeFilter) = _state.update { it.copy(filter = f) }

    fun toggle(path: String) = _state.update {
        it.copy(selected = if (path in it.selected) it.selected - path else it.selected + path)
    }

    /** Select or clear every visible file of one category. */
    fun setCategory(c: SocialCategory, select: Boolean) = _state.update { s ->
        val paths = s.inCategory(c).map { it.path }.toSet()
        s.copy(selected = if (select) s.selected + paths else s.selected - paths)
    }

    fun cleanCategory(c: SocialCategory) = clean(_state.value.inCategory(c).filter { it.path in _state.value.selected })

    fun cleanSelected() = clean(_state.value.selectedVisible)

    private fun clean(files: List<SocialFile>) {
        if (files.isEmpty() || _state.value.busy) return
        if (!StorageAccess.hasAllFilesAccess(context)) {
            _message.value = SocialMessage.NeedsFullAccess
            return
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val r = repo.moveToSafeTrash(files)
            val moved = r.movedPaths
            _state.update { s ->
                s.copy(
                    busy = false,
                    selected = s.selected - moved,
                    scans = s.scans.mapValues { (_, scan) -> scan.copy(files = scan.files.filterNot { it.path in moved }) },
                )
            }
            _message.value = SocialMessage.Cleaned(r.movedFiles, r.movedBytes, r.trashIds)
        }
    }

    fun undo(trashIds: List<Long>) {
        viewModelScope.launch {
            val n = repo.restore(trashIds)
            _message.value = SocialMessage.Restored(n)
            val scans = repo.scanAll()
            _state.update { it.copy(scans = scans) }
        }
    }

    fun consumeMessage() { _message.value = null }
}
