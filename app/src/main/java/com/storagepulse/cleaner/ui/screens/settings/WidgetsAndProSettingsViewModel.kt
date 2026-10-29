package com.storagepulse.cleaner.ui.screens.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepulse.cleaner.ads.AdMobManager
import com.storagepulse.cleaner.data.billing.ProEvent
import com.storagepulse.cleaner.data.billing.ProRepository
import com.storagepulse.cleaner.data.settings.AccentColor
import com.storagepulse.cleaner.data.settings.SettingsRepository
import com.storagepulse.cleaner.data.widgets.WidgetData
import com.storagepulse.cleaner.data.widgets.WidgetDataRepository
import com.storagepulse.cleaner.notify.WeeklyClutterWorker
import com.storagepulse.cleaner.ui.widgets.DailyClutterWidgetReceiver
import com.storagepulse.cleaner.ui.widgets.MinimalStripWidgetReceiver
import com.storagepulse.cleaner.ui.widgets.StorageBoostWidgetReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The 3 home screen widgets. [proOnly] = needs StoragePulse Pro. */
enum class WidgetKind(val receiver: Class<out GlanceAppWidgetReceiver>, val proOnly: Boolean) {
    BOOST_4X2(StorageBoostWidgetReceiver::class.java, true),
    CLUTTER_2X2(DailyClutterWidgetReceiver::class.java, true),
    STRIP_4X1(MinimalStripWidgetReceiver::class.java, false),
}

sealed interface SettingsMessage {
    data class Pro(val event: ProEvent) : SettingsMessage
    data object PinRequested : SettingsMessage
    data object PinUnsupported : SettingsMessage
    data object ProRequired : SettingsMessage
}

@HiltViewModel
class WidgetsAndProSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val widgetData: WidgetDataRepository,
    private val pro: ProRepository,
    private val settings: SettingsRepository,
    private val ads: AdMobManager,
) : ViewModel() {

    /** EEA/UK/CH: Google requires a way to change ad consent later. */
    val adPrivacyRequired: StateFlow<Boolean> = ads.privacyOptionsRequired

    fun showAdPrivacy(activity: Activity) = ads.showPrivacyOptions(activity)

    private fun <T> kotlinx.coroutines.flow.Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val data: StateFlow<WidgetData> = widgetData.data.state(WidgetData())
    val isPro: StateFlow<Boolean> = pro.isPro.state(false)
    val purchased: StateFlow<Boolean> = pro.purchased.state(false)
    val debugOverride: StateFlow<Boolean> = pro.debugOverride.state(false)
    val price: StateFlow<String?> = pro.price
    val retentionDays: StateFlow<Int> = settings.trashRetentionDays.state(SettingsRepository.DEFAULT_RETENTION_DAYS)
    val weekly: StateFlow<Boolean> = settings.weeklyNotification.state(false)
    val accent: StateFlow<AccentColor> = settings.accent.state(AccentColor.OBSIDIAN)

    private val _messages = MutableSharedFlow<SettingsMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<SettingsMessage> = _messages.asSharedFlow()

    init {
        // Live previews: quick refresh (no scan) every time the screen opens.
        viewModelScope.launch {
            runCatching {
                widgetData.refresh(scan = false)
                // No scan in memory yet (e.g. opened from a widget): scan once for the category bar.
                widgetData.refresh(scan = true, force = false)
            }
        }
        pro.refreshQuietly()
        viewModelScope.launch { pro.events.collect { _messages.emit(SettingsMessage.Pro(it)) } }
    }

    fun pin(kind: WidgetKind) {
        viewModelScope.launch {
            if (kind.proOnly && !isPro.value) {
                _messages.emit(SettingsMessage.ProRequired)
                return@launch
            }
            val supported = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
            val asked = supported && runCatching {
                GlanceAppWidgetManager(context).requestPinGlanceAppWidget(kind.receiver)
            }.getOrDefault(false)
            _messages.emit(if (asked) SettingsMessage.PinRequested else SettingsMessage.PinUnsupported)
        }
    }

    fun buy(activity: Activity) {
        viewModelScope.launch { pro.launchPurchase(activity) }
    }

    fun restore() = pro.restore()

    fun setDebugPro(on: Boolean) {
        viewModelScope.launch { pro.setDebugOverride(on) }
    }

    fun setRetention(days: Int) {
        viewModelScope.launch { settings.setTrashRetentionDays(days) }
    }

    /** The screen asks for POST_NOTIFICATIONS (Android 13+) before turning this on. */
    fun setWeekly(on: Boolean) {
        viewModelScope.launch {
            settings.setWeeklyNotification(on)
            if (on) WeeklyClutterWorker.schedule(context) else WeeklyClutterWorker.cancel(context)
        }
    }

    fun setAccent(a: AccentColor) {
        viewModelScope.launch { settings.setAccent(a) }
    }
}
