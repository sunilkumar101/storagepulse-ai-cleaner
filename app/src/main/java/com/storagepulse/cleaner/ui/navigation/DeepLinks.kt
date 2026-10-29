package com.storagepulse.cleaner.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Routes requested from outside the app (home screen widgets, the weekly notification). */
object DeepLinks {
    private val allowed = setOf(Routes.DASHBOARD, Routes.AI_CLEAN, Routes.SWIPE_DECK, Routes.SETTINGS)
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun offer(route: String?) {
        if (route != null && route in allowed) _pending.value = route
    }

    fun consume() {
        _pending.value = null
    }
}
