package com.hari.androidtvremote.preference

import androidx.compose.runtime.compositionLocalOf

// Stub for AMOLED dark theme — not used in this app but referenced by
// the copied TonalPalettes engine. Value is always false (disabled).
data class AmoledDarkTheme(val value: Boolean = false)
val LocalAmoledDarkTheme = compositionLocalOf { AmoledDarkTheme(false) }
