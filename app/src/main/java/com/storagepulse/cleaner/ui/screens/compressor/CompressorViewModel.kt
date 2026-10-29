package com.storagepulse.cleaner.ui.screens.compressor

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepulse.cleaner.ads.AdGateResult
import com.storagepulse.cleaner.ads.AdMobManager
import com.storagepulse.cleaner.data.compressor.CompressOutcome
import com.storagepulse.cleaner.data.compressor.CompressionTier
import com.storagepulse.cleaner.data.compressor.MediaCompressorEngine
import com.storagepulse.cleaner.data.compressor.PreviewResult
import com.storagepulse.cleaner.data.compressor.TierPlan
import com.storagepulse.cleaner.data.media.LargeVideoRepository
import com.storagepulse.cleaner.data.media.VideoDetails
import com.storagepulse.cleaner.data.media.VideoItem
import com.storagepulse.cleaner.data.storage.StorageAccess
import com.storagepulse.cleaner.data.storage.StorageScannerRepository
import com.storagepulse.cleaner.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class QueueItem(
    val video: VideoItem,
    val details: VideoDetails? = null,
    val selected: Boolean = true,
)

sealed interface PreviewState {
    data object Idle : PreviewState
    data object Loading : PreviewState
    data class Ready(val result: PreviewResult) : PreviewState
    data class Failed(val message: String) : PreviewState
}

sealed interface BatchState {
    data object Idle : BatchState
    data object WaitingForAd : BatchState
    data class Running(val index: Int, val total: Int, val itemProgress: Float, val name: String) : BatchState {
        val overall: Float get() = ((index + itemProgress) / total).coerceIn(0f, 1f)
    }
    data class Done(val compressed: Int, val skipped: Int, val failed: Int, val savedBytes: Long, val adResult: AdGateResult) : BatchState
}

data class CompressorUiState(
    val loading: Boolean = true,
    val queue: List<QueueItem> = emptyList(),
    val candidates: List<VideoItem> = emptyList(),
    val tier: CompressionTier = CompressionTier.BALANCED_1080,
    val keepInSafeTrash: Boolean = true,
    val preview: PreviewState = PreviewState.Idle,
    val batch: BatchState = BatchState.Idle,
    val encoderName: String = "",
    val hevc: Boolean = true,
    val hardware: Boolean = true,
    val needsFullAccess: Boolean = false,
) {
    val selected: List<QueueItem> get() = queue.filter { it.selected }
    val focus: QueueItem? get() = selected.firstOrNull { it.details != null } ?: selected.firstOrNull()
}

@HiltViewModel
class CompressorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val engine: MediaCompressorEngine,
    private val videos: LargeVideoRepository,
    private val ads: AdMobManager,
    private val scanner: StorageScannerRepository,
    private val context: android.app.Application,
) : ViewModel() {

    /** false for Pro users: no rewarded ad before the batch. */
    val adsEnabled = ads.adsEnabled

    private val _state = MutableStateFlow(CompressorUiState())
    val state: StateFlow<CompressorUiState> = _state.asStateFlow()

    private var previewJob: Job? = null
    private var batchJob: Job? = null

    init {
        val target: String? = savedState[Routes.ARG_FOLDER]
        _state.update {
            it.copy(
                encoderName = engine.encoder.name,
                hevc = engine.encoder.mimeType == androidx.media3.common.MimeTypes.VIDEO_H265,
                hardware = engine.encoder.hardware,
                needsFullAccess = !StorageAccess.hasAllFilesAccess(context),
            )
        }
        ads.preloadRewarded()
        viewModelScope.launch { loadQueue(target) }
    }

    private suspend fun loadQueue(target: String?) {
        val file = target?.let(::File)
        val initial = when {
            file != null && file.isFile -> listOfNotNull(videos.videoAt(file.path))
            file != null && file.isDirectory -> videos.videosIn(file.path).take(MAX_QUEUE)
            else -> videos.largeVideos().take(DEFAULT_QUEUE)
        }
        val candidates = videos.largeVideos(20_000_000L)
        _state.update { it.copy(loading = false, queue = initial.map { v -> QueueItem(v) }, candidates = candidates) }
        initial.forEach { v ->
            val d = videos.details(v)
            _state.update { s -> s.copy(queue = s.queue.map { if (it.video.path == v.path) it.copy(details = d) else it }) }
        }
        refreshPreview()
    }

    fun setTier(tier: CompressionTier) {
        if (_state.value.tier == tier) return
        _state.update { it.copy(tier = tier) }
        refreshPreview()
    }

    fun setKeepInSafeTrash(on: Boolean) = _state.update { it.copy(keepInSafeTrash = on) }

    fun toggleQueued(path: String) {
        _state.update { s -> s.copy(queue = s.queue.map { if (it.video.path == path) it.copy(selected = !it.selected) else it }) }
        refreshPreview()
    }

    /** "Edit List": add a candidate video to the queue. */
    fun addToQueue(video: VideoItem) {
        if (_state.value.queue.any { it.video.path == video.path }) return toggleQueued(video.path)
        _state.update { it.copy(queue = it.queue + QueueItem(video)) }
        viewModelScope.launch {
            val d = videos.details(video)
            _state.update { s -> s.copy(queue = s.queue.map { if (it.video.path == video.path) it.copy(details = d) else it }) }
            refreshPreview()
        }
    }

    fun plan(item: QueueItem, tier: CompressionTier = _state.value.tier): TierPlan? =
        item.details?.let { engine.plan(item.video, it, tier) }

    /** Re-encodes a 2 s preview of the focused video whenever the tier or focus changes. */
    private fun refreshPreview() {
        val s = _state.value
        val focus = s.focus
        val details = focus?.details
        previewJob?.cancel()
        if (focus == null || details == null) {
            _state.update { it.copy(preview = PreviewState.Idle) }
            return
        }
        previewJob = viewModelScope.launch {
            delay(250) // debounce quick tier taps
            _state.update { it.copy(preview = PreviewState.Loading) }
            _state.update {
                it.copy(
                    preview = try {
                        PreviewState.Ready(engine.preview(focus.video, details, s.tier))
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        PreviewState.Failed(e.message ?: "Preview failed")
                    },
                )
            }
        }
    }

    /** Rewarded ad first (never blocks if no ad is available), then the batch. */
    fun startBatch(activity: Activity) {
        val s = _state.value
        if (s.batch is BatchState.Running || s.batch == BatchState.WaitingForAd) return
        val items = s.selected.filter { it.details != null }
        if (items.isEmpty()) return
        if (!StorageAccess.hasAllFilesAccess(context)) {
            _state.update { it.copy(needsFullAccess = true) }
            return
        }
        previewJob?.cancel()
        batchJob = viewModelScope.launch {
            _state.update { it.copy(batch = BatchState.WaitingForAd) }
            val ad = ads.showRewarded(activity)
            if (ad == AdGateResult.DISMISSED) {
                _state.update { it.copy(batch = BatchState.Idle) }
                return@launch
            }
            var compressed = 0
            var skipped = 0
            var failed = 0
            var saved = 0L
            items.forEachIndexed { index, item ->
                _state.update { it.copy(batch = BatchState.Running(index, items.size, 0f, item.video.name)) }
                val outcome = engine.compress(item.video, item.details!!, s.tier, s.keepInSafeTrash) { p ->
                    _state.update { st -> (st.batch as? BatchState.Running)?.let { st.copy(batch = it.copy(itemProgress = p)) } ?: st }
                }
                when (outcome) {
                    is CompressOutcome.Done -> {
                        compressed++
                        if (outcome.originalInTrash) saved += outcome.originalBytes - outcome.outputBytes
                    }
                    is CompressOutcome.Skipped -> skipped++
                    is CompressOutcome.Failed -> failed++
                }
            }
            _state.update { it.copy(batch = BatchState.Done(compressed, skipped, failed, saved, ad), queue = it.queue.map { q -> if (items.any { i -> i.video.path == q.video.path }) q.copy(selected = false) else q }) }
            scanner.startScan(force = true)
        }
    }

    fun cancelBatch() {
        batchJob?.cancel()
        _state.update { it.copy(batch = BatchState.Idle) }
    }

    fun dismissResult() = _state.update { it.copy(batch = BatchState.Idle) }

    private companion object {
        const val DEFAULT_QUEUE = 4
        const val MAX_QUEUE = 20
    }
}
