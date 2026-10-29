package com.storagepulse.cleaner.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import com.storagepulse.cleaner.data.settings.AccessMode
import com.storagepulse.cleaner.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StorageDisclosureViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    /** Suspends until saved, so navigation happens only after the choice is persisted. */
    suspend fun saveAccessMode(mode: AccessMode) = settings.setAccessMode(mode)
}
