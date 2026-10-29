package com.storagepulse.cleaner

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import com.storagepulse.cleaner.data.trash.TrashPurgeWorker
import com.storagepulse.cleaner.ads.AdMobManager
import com.storagepulse.cleaner.data.billing.ProRepository
import com.storagepulse.cleaner.ui.widgets.WidgetRefreshWorker
import dagger.hilt.android.HiltAndroidApp
import com.storagepulse.cleaner.data.settings.SettingsRepository
import com.storagepulse.cleaner.ui.widgets.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class StoragePulseApp : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var ads: AdMobManager
    @Inject lateinit var pro: ProRepository
    @Inject lateinit var settings: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        TrashPurgeWorker.schedule(this)
        WidgetRefreshWorker.schedulePeriodic(this)
        pro.refreshQuietly()
        // Widgets show Pro-only layouts and the accent: redraw them when either changes.
        appScope.launch {
            combine(pro.isPro, settings.accent) { p, a -> p to a }
                .distinctUntilChanged()
                .drop(1)
                .collect { WidgetUpdater.updateAll(this@StoragePulseApp) }
        }
    }

    /** Coil with video-frame thumbnails (for heavyweight videos and the swipe deck). */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
