package com.storagepulse.cleaner.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** FULL = whole disk (All Files Access); SCOPED = photos/videos the user allowed. */
enum class AccessMode { FULL, SCOPED }

/** Telemetry accent (Screen 10). OBSIDIAN is the default emerald. */
enum class AccentColor { OBSIDIAN, INDIGO, AMBER }

private val Context.settingsStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val accessModeKey = stringPreferencesKey("access_mode")

    val accessMode: Flow<AccessMode?> = context.settingsStore.data.map { prefs ->
        prefs[accessModeKey]?.let { runCatching { AccessMode.valueOf(it) }.getOrNull() }
    }

    suspend fun setAccessMode(mode: AccessMode) {
        context.settingsStore.edit { it[accessModeKey] = mode.name }
    }

    private val trashRetentionKey = intPreferencesKey("trash_retention_days")

    /** Safe Trash retention: 7 / 30 / 60 days, default 30. */
    val trashRetentionDays: Flow<Int> = context.settingsStore.data.map { prefs ->
        prefs[trashRetentionKey]?.takeIf { it in ALLOWED_RETENTION_DAYS } ?: DEFAULT_RETENTION_DAYS
    }

    suspend fun setTrashRetentionDays(days: Int) {
        require(days in ALLOWED_RETENTION_DAYS)
        context.settingsStore.edit { it[trashRetentionKey] = days }
    }

    private val accentKey = stringPreferencesKey("accent")

    val accent: Flow<AccentColor> = context.settingsStore.data.map { prefs ->
        prefs[accentKey]?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() } ?: AccentColor.OBSIDIAN
    }

    suspend fun setAccent(accent: AccentColor) {
        context.settingsStore.edit { it[accentKey] = accent.name }
    }

    private val weeklyNotifyKey = booleanPreferencesKey("weekly_clutter_notification")

    /** Sunday 8 PM clutter summary. Off until the user turns it on. */
    val weeklyNotification: Flow<Boolean> = context.settingsStore.data.map { it[weeklyNotifyKey] ?: false }

    suspend fun setWeeklyNotification(on: Boolean) {
        context.settingsStore.edit { it[weeklyNotifyKey] = on }
    }

    companion object {
        const val DEFAULT_RETENTION_DAYS = 30
        val ALLOWED_RETENTION_DAYS = setOf(7, 30, 60)
    }
}
