package com.storagepulse.cleaner.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepulse.cleaner.data.storage.StorageScannerRepository
import com.storagepulse.cleaner.data.storage.model.ScanState
import com.storagepulse.cleaner.data.storage.model.VolumeStats
import com.storagepulse.cleaner.data.media.LargeVideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val volume: VolumeStats,
    val scan: ScanState,
    /** ≥100 MB videos on every volume (incl. SD card): the same list the Swipe Deck opens. */
    val heavyVideos: Int? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val scanner: StorageScannerRepository,
    private val largeVideos: LargeVideoRepository,
) : ViewModel() {

    private val heavyVideos = MutableStateFlow<Int?>(null)

    // Total / used / free are shown instantly; categories fill in when the scan finishes.
    private val volume = MutableStateFlow(scanner.volumeStats())

    val uiState: StateFlow<DashboardUiState> = combine(volume, scanner.state, heavyVideos) { v, s, h ->
        DashboardUiState(volume = v, scan = s, heavyVideos = h)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(volume.value, scanner.state.value),
    )

    init {
        scanner.startScan()
        // Recount whenever a scan finishes (files may have been cleaned meanwhile).
        viewModelScope.launch {
            scanner.state.filter { it is ScanState.Done }.collect {
                heavyVideos.value = runCatching { largeVideos.largeVideos().size }.getOrNull()
            }
        }
    }

    fun rescan() {
        volume.value = scanner.volumeStats()
        scanner.startScan(force = true)
    }
}
