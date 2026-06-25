package com.hari.androidtvremote.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ─── DataStore ───────────────────────────────────────────────────────────────

val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(name = "appearance_prefs")

object AppearanceKeys {
    val THEME_INDEX = intPreferencesKey("theme_index")
    val DARK_THEME = intPreferencesKey("dark_theme")
    val BASIC_FONT = intPreferencesKey("basic_font")
    val CUSTOM_PRIMARY_COLOR = stringPreferencesKey("custom_primary_color")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
}

// ─── CompositionLocals ───────────────────────────────────────────────────────

val LocalThemeIndex = compositionLocalOf { 0 }
val LocalCustomPrimaryColor = compositionLocalOf { "" }
val LocalDynamicColor = compositionLocalOf { true }
// LocalDarkTheme and LocalBasicFont are in their own files

// ─── DataStore helpers ───────────────────────────────────────────────────────

fun Context.themeIndexFlow(): Flow<Int> =
    appearanceDataStore.data.map { it[AppearanceKeys.THEME_INDEX] ?: 0 }

fun Context.darkThemeFlow(): Flow<Int> =
    appearanceDataStore.data.map { it[AppearanceKeys.DARK_THEME] ?: DarkThemePreference.UseDeviceTheme.value }


fun Context.customColorFlow(): Flow<String> =
    appearanceDataStore.data.map { it[AppearanceKeys.CUSTOM_PRIMARY_COLOR] ?: "" }

fun Context.dynamicColorFlow(): Flow<Boolean> =
    appearanceDataStore.data.map { it[AppearanceKeys.DYNAMIC_COLOR] ?: true }

suspend fun Context.putThemeIndex(value: Int) {
    appearanceDataStore.edit { it[AppearanceKeys.THEME_INDEX] = value }
}

suspend fun Context.putDarkTheme(value: Int) {
    appearanceDataStore.edit { it[AppearanceKeys.DARK_THEME] = value }
}

suspend fun Context.putBasicFont(value: Int) {
    appearanceDataStore.edit { it[AppearanceKeys.BASIC_FONT] = value }
}

suspend fun Context.putCustomColor(value: String) {
    appearanceDataStore.edit { it[AppearanceKeys.CUSTOM_PRIMARY_COLOR] = value }
}
