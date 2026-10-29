package com.storagepulse.cleaner.ui.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepulse.cleaner.ads.AdMobManager
import com.storagepulse.cleaner.data.settings.AccessMode
import com.storagepulse.cleaner.data.settings.SettingsRepository
import com.storagepulse.cleaner.data.storage.StorageAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Skips the disclosure screen when access was already granted on a previous launch. */
@HiltViewModel
class AppStartViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    ads: AdMobManager,
) : ViewModel() {

    /** Free users see one adaptive banner above the bottom nav; Pro users never do. */
    val adsEnabled: StateFlow<Boolean> = ads.adsEnabled
    val bannerUnitId: String = ads.bannerUnitId

    private val _startRoute = MutableStateFlow<String?>(null)
    val startRoute: StateFlow<String?> = _startRoute

    init {
        viewModelScope.launch {
            _startRoute.value = if (hasAccess()) Routes.DASHBOARD else Routes.ONBOARDING
        }
    }

    /** All Files Access, or the media permissions in Scoped Mode. */
    suspend fun hasAccess(): Boolean {
        val mode = settings.accessMode.first()
        return StorageAccess.hasAllFilesAccess(context) ||
            (mode == AccessMode.SCOPED && StorageAccess.hasMediaAccess(context))
    }
}
