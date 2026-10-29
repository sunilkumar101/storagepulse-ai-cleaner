package com.storagepulse.cleaner.ui.screens.tools

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepulse.cleaner.data.apps.AppSpace
import com.storagepulse.cleaner.data.apps.AppSpaceRepository
import com.storagepulse.cleaner.data.apps.StaleInstaller
import com.storagepulse.cleaner.data.storage.StorageScannerRepository
import com.storagepulse.cleaner.data.trash.SafeTrashRepository
import com.storagepulse.cleaner.data.trash.TrashItemEntity
import com.storagepulse.cleaner.data.trash.TrashMoveResult
import com.storagepulse.cleaner.data.trash.TrashTotals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class ToolsTab { APPS, TRASH }
enum class AppFilter { ALL, UNUSED_30D }

sealed interface ToolsMessage {
    data class Restored(val n: Int) : ToolsMessage
    data class Emptied(val n: Int) : ToolsMessage
    data class Installers(val n: Int) : ToolsMessage
}

data class ToolsUiState(
    val tab: ToolsTab = ToolsTab.APPS,
    val filter: AppFilter = AppFilter.UNUSED_30D,
    val usageAccess: Boolean = false,
    val appsLoading: Boolean = true,
    val apps: List<AppSpace> = emptyList(),
    val installers: List<StaleInstaller> = emptyList(),
    val busy: Boolean = false,
) {
    val unused: List<AppSpace> get() = apps.filter { it.unusedDays() >= UNUSED_DAYS }
    val shownApps: List<AppSpace> get() = if (filter == AppFilter.UNUSED_30D) unused else apps
    val unusedBytes: Long get() = unused.sumOf { it.totalBytes }
    val installerBytes: Long get() = installers.sumOf { it.sizeBytes }

    companion object { const val UNUSED_DAYS = 30L }
}

@HiltViewModel
class SafeTrashAndAppsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val apps: AppSpaceRepository,
    private val trash: SafeTrashRepository,
    private val scanner: StorageScannerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ToolsUiState(tab = if (savedState.get<String>(ARG_TAB) == "trash") ToolsTab.TRASH else ToolsTab.APPS),
    )
    val state: StateFlow<ToolsUiState> = _state.asStateFlow()

    val trashItems: StateFlow<List<TrashItemEntity>> = trash.observeTrash().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val trashTotals: StateFlow<TrashTotals> = trash.observeTotals().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrashTotals(0, 0))

    private val _message = MutableStateFlow<ToolsMessage?>(null)
    val message: StateFlow<ToolsMessage?> = _message.asStateFlow()

    /** Called on every resume (returning from Usage Access settings or an uninstall). */
    fun refresh() {
        viewModelScope.launch {
            val access = apps.hasUsageAccess()
            _state.update { it.copy(usageAccess = access, appsLoading = access && it.apps.isEmpty()) }
            val installers = apps.staleInstallers()
            val list = if (access) apps.apps() else emptyList()
            _state.update { it.copy(apps = list, installers = installers, appsLoading = false) }
        }
    }

    fun setTab(t: ToolsTab) = _state.update { it.copy(tab = t) }
    fun setFilter(f: AppFilter) = _state.update { it.copy(filter = f) }

    fun usageAccessIntent() = apps.usageAccessIntent()
    fun appInfoIntent(pkg: String) = apps.appInfoIntent(pkg)
    fun uninstallIntent(pkg: String) = apps.uninstallIntent(pkg)

    fun trashInstallers() = work {
        val list = _state.value.installers
        val moved = trash.moveToSafeTrash(list.map { File(it.path) }).filterIsInstance<TrashMoveResult.Moved>()
        scanner.applyRemoval(moved.associate { it.item.originalPath to it.item.sizeBytes })
        val movedPaths = moved.map { it.item.originalPath }.toSet()
        _state.update { s -> s.copy(installers = s.installers.filterNot { it.path in movedPaths }) }
        _message.value = ToolsMessage.Installers(moved.size)
    }

    fun restore(id: Long) = work {
        val ok = trash.restoreFromTrash(id) is com.storagepulse.cleaner.data.trash.TrashRestoreResult.Restored
        if (ok) scanner.startScan(force = true)
        _message.value = ToolsMessage.Restored(if (ok) 1 else 0)
    }

    fun restoreAll() = work {
        val n = trash.restoreAll()
        if (n > 0) scanner.startScan(force = true)
        _message.value = ToolsMessage.Restored(n)
        refresh()
    }

    /** Permanent. The screen shows a red confirm dialog with count and size first. */
    fun emptyTrash() = work {
        val n = trash.emptyTrashPermanently()
        scanner.startScan(force = true)
        _message.value = ToolsMessage.Emptied(n)
    }

    fun consumeMessage() { _message.value = null }

    private fun work(block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { block() } finally { _state.update { it.copy(busy = false) } }
        }
    }

    companion object { const val ARG_TAB = "tab" }
}
