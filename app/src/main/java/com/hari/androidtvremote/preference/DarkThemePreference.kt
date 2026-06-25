package com.hari.androidtvremote.preference

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

// ─── Dark Theme ─────────────────────────────────────────────────────────────

val LocalDarkTheme = compositionLocalOf<DarkThemePreference> { DarkThemePreference.UseDeviceTheme }

enum class DarkThemePreference(val value: Int) {
    UseDeviceTheme(0),
    Dark(1),
    Light(2);

    @Composable
    @ReadOnlyComposable
    fun isDarkTheme(): Boolean = when (this) {
        UseDeviceTheme -> isSystemInDarkTheme()
        Dark -> true
        Light -> false
    }

    fun toDisplayName(): String = when (this) {
        UseDeviceTheme -> "Follow system"
        Dark -> "Dark"
        Light -> "Light"
    }

    companion object {
        val default = UseDeviceTheme
        val values = enumValues<DarkThemePreference>().toList()

        fun fromInt(value: Int): DarkThemePreference = when (value) {
            1 -> Dark
            2 -> Light
            else -> UseDeviceTheme
        }
    }
}

@Composable
operator fun DarkThemePreference.not(): DarkThemePreference =
    when (this) {
        DarkThemePreference.UseDeviceTheme -> if (isSystemInDarkTheme()) DarkThemePreference.Light else DarkThemePreference.Dark
        DarkThemePreference.Dark -> DarkThemePreference.Light
        DarkThemePreference.Light -> DarkThemePreference.Dark
    }