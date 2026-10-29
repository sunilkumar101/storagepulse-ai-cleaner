package com.storagepulse.cleaner.ui.screens.clean

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepulse.cleaner.data.media.LargeVideoRepository
import com.storagepulse.cleaner.data.media.VideoDetails
import com.storagepulse.cleaner.data.media.VideoItem
import com.storagepulse.cleaner.data.storage.StorageScannerRepository
import com.storagepulse.cleaner.data.trash.SafeTrashRepository
import com.storagepulse.cleaner.data.trash.TrashMoveResult
import com.storagepulse.cleaner.data.trash.TrashRestoreResult
import com.storagepulse.cleaner.ads.AdMobManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** One decision, so Undo can reverse it. */
private sealed interface SwipeAction {
    val index: Int
    data class Trashed(override val index: Int, val item: VideoItem, val trashId: Long) : SwipeAction
    data class Kept(override val index: Int) : SwipeAction
}

sealed interface SwipeMessage {
    data class Trashed(val name: String) : SwipeMessage
    data class Failed(val reason: String) : SwipeMessage
    data class Undone(val name: String) : SwipeMessage
}

data class SwipeUiState(
    val loading: Boolean = true,
    val items: List<VideoItem> = emptyList(),
    val index: Int = 0,
    val freedBytes: Long = 0,
    val details: VideoDetails? = null,
    val canUndo: Boolean = false,
    val busy: Boolean = false,
    val reviewed: Int = 0,
) {
    val current: VideoItem? get() = items.getOrNull(index)
    val finished: Boolean get() = !loading && items.isNotEmpty() && index >= items.size
}

@HiltViewModel
class SwipeDeckViewModel @Inject constructor(
    private val videos: LargeVideoRepository,
    private val safeTrash: SafeTrashRepository,
    private val scanner: StorageScannerRepository,
    private val ads: AdMobManager,
) : ViewModel() {

    private var sessionAdShown = false

    /**
     * End of a swipe session (deck finished, or the user leaves): after at least
     * [SESSION_AD_MIN_REVIEWED] reviews, free users may get one capped interstitial.
     */
    fun endSession(activity: android.app.Activity) {
        if (sessionAdShown || _state.value.reviewed < SESSION_AD_MIN_REVIEWED) return
        sessionAdShown = ads.maybeShowInterstitial(activity)
    }

    private val _state = MutableStateFlow(SwipeUiState())
    val state: StateFlow<SwipeUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<SwipeMessage?>(null)
    val message: StateFlow<SwipeMessage?> = _message.asStateFlow()

    private val history = ArrayDeque<SwipeAction>()

    init {
        load()
        ads.preloadInterstitial()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = SwipeUiState(loading = true)
            history.clear()
            val list = videos.largeVideos()
            _state.value = SwipeUiState(loading = false, items = list)
            loadDetails()
        }
    }

    /** Swipe up / "Trash" button: move the current video to Safe Trash. */
    fun trashCurrent() {
        val s = _state.value
        val item = s.current ?: return
        if (s.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val r = safeTrash.moveToSafeTrash(listOf(File(item.path))).single()) {
                is TrashMoveResult.Moved -> {
                    history.addLast(SwipeAction.Trashed(s.index, item, r.item.id))
                    scanner.applyRemoval(mapOf(item.path to item.sizeBytes))
                    _message.value = SwipeMessage.Trashed(item.name)
                    advance(freed = item.sizeBytes)
                }
                is TrashMoveResult.Failed -> {
                    _message.value = SwipeMessage.Failed(r.reason)
                    _state.update { it.copy(busy = false) }
                }
            }
        }
    }

    /** Swipe down / "Keep File": nothing is touched. */
    fun keepCurrent() {
        val s = _state.value
        if (s.current == null || s.busy) return
        history.addLast(SwipeAction.Kept(s.index))
        advance(freed = 0)
    }

    fun undo() {
        val last = history.removeLastOrNull() ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            if (last is SwipeAction.Trashed) {
                val r = safeTrash.restoreFromTrash(last.trashId)
                if (r is TrashRestoreResult.Restored) {
                    _message.value = SwipeMessage.Undone(last.item.name)
                    _state.update { it.copy(freedBytes = (it.freedBytes - last.item.sizeBytes).coerceAtLeast(0)) }
                    scanner.startScan(force = true) // the file is back: refresh the storage map
                } else {
                    _message.value = SwipeMessage.Failed((r as TrashRestoreResult.Failed).reason)
                }
            }
            _state.update {
                it.copy(index = last.index, reviewed = (it.reviewed - 1).coerceAtLeast(0), busy = false, canUndo = history.isNotEmpty(), details = null)
            }
            loadDetails()
        }
    }

    fun restart() = load()

    fun consumeMessage() { _message.value = null }

    private fun advance(freed: Long) {
        _state.update {
            it.copy(
                index = it.index + 1,
                freedBytes = it.freedBytes + freed,
                reviewed = it.reviewed + 1,
                details = null,
                canUndo = true,
                busy = false,
            )
        }
        loadDetails()
    }

    private fun loadDetails() {
        val item = _state.value.current ?: return
        viewModelScope.launch {
            val d = videos.details(item)
            _state.update { if (it.current?.path == item.path) it.copy(details = d) else it }
        }
    }

    companion object {
        const val SESSION_AD_MIN_REVIEWED = 15
    }
}
