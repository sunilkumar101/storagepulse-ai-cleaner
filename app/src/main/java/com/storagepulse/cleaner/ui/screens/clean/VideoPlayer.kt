package com.storagepulse.cleaner.ui.screens.clean

import android.content.Context
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

/** Holds one ExoPlayer for the screen and exposes playback state to Compose. */
class DeckPlayer(context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_OFF }
    var isPlaying by mutableStateOf(false)
        private set
    var positionMs by mutableLongStateOf(0L)
        internal set
    var durationMs by mutableLongStateOf(0L)
        private set
    /** Display aspect ratio, after rotation. */
    var aspect by mutableFloatStateOf(16f / 9f)
        private set
    /** Rotation the decoder did not apply (phone portrait videos on a TextureView). */
    var rotation by mutableIntStateOf(0)
        private set
    var firstFrame by mutableStateOf(false)
        private set
    private var loadedUri: String? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) durationMs = player.duration.coerceAtLeast(0)
            if (state == Player.STATE_ENDED) { player.pause(); player.seekTo(0) }
        }
        override fun onVideoSizeChanged(size: VideoSize) {
            if (size.width <= 0 || size.height <= 0) return
            @Suppress("DEPRECATION")
            val rot = ((size.unappliedRotationDegrees % 360) + 360) % 360
            rotation = rot
            val w = size.width * size.pixelWidthHeightRatio
            aspect = if (rot == 90 || rot == 270) size.height / w else w / size.height
        }
        override fun onRenderedFirstFrame() { firstFrame = true }
    }

    init { player.addListener(listener) }

    fun load(uri: String) {
        if (uri == loadedUri) return
        loadedUri = uri
        firstFrame = false
        positionMs = 0
        durationMs = 0
        rotation = 0
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = false
    }

    /** Close the file before it is moved to Safe Trash. */
    fun unload() {
        loadedUri = null
        player.stop()
        player.clearMediaItems()
        firstFrame = false
    }

    fun toggle() = if (player.isPlaying) player.pause() else player.play()

    fun seekTo(ms: Long) {
        player.seekTo(ms)
        positionMs = ms
    }

    fun release() {
        player.removeListener(listener)
        player.release()
    }
}

@Composable
fun rememberDeckPlayer(): DeckPlayer {
    val context = LocalContext.current
    val deck = remember { DeckPlayer(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) deck.player.pause() }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            deck.release()
        }
    }
    // Poll position while playing so the scrub bar moves.
    LaunchedEffect(deck.isPlaying) {
        while (deck.isPlaying) {
            deck.positionMs = deck.player.currentPosition
            delay(200)
        }
    }
    return deck
}

/** Video surface letterboxed to the real aspect ratio, with a poster until the first frame. */
@Composable
fun DeckVideoSurface(deck: DeckPlayer, posterUri: String, modifier: Modifier = Modifier) {
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        Box(Modifier.aspectRatio(deck.aspect.coerceIn(0.3f, 3f)), contentAlignment = Alignment.Center) {
            // TextureView so the card can tilt/fade while swiping; rotation the decoder did not
            // apply (unappliedRotationDegrees) is applied here.
            AndroidView(
                factory = { ctx -> TextureView(ctx).also { deck.player.setVideoTextureView(it) } },
                modifier = Modifier.fillMaxSize().rotateVideo(deck.rotation),
            )
            if (!deck.firstFrame) {
                AsyncImage(
                    model = posterUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * Applies the rotation the decoder left to the app. For 90°/270° the TextureView is laid out
 * with swapped width/height and rotated into the box, so the picture fills it upright.
 */
private fun Modifier.rotateVideo(degrees: Int): Modifier = when (degrees) {
    90, 270 -> layout { measurable, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val placeable = measurable.measure(Constraints.fixed(h, w))
        layout(w, h) {
            placeable.placeWithLayer((w - placeable.width) / 2, (h - placeable.height) / 2) {
                rotationZ = degrees.toFloat()
            }
        }
    }
    180 -> this.then(Modifier.layout { m, c ->
        val p = m.measure(c)
        layout(p.width, p.height) { p.placeWithLayer(0, 0) { rotationZ = 180f } }
    })
    else -> this
}
