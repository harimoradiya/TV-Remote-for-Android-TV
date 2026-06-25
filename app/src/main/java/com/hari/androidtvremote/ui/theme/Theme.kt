package com.hari.androidtvremote.ui.theme

import android.os.Build
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView

import com.hari.androidtvremote.preference.LocalDarkTheme
import com.hari.androidtvremote.preference.LocalThemeIndex
import com.hari.androidtvremote.ui.theme.palette.LocalTonalPalettes
import com.hari.androidtvremote.ui.theme.palette.core.ProvideZcamViewingConditions
import com.hari.androidtvremote.ui.theme.palette.dynamic.extractTonalPalettesFromUserWallpaper
import com.hari.androidtvremote.ui.theme.palette.dynamicDarkColorScheme
import com.hari.androidtvremote.ui.theme.palette.dynamicLightColorScheme

/**
 * Master app theme — ReadYou-style:
 * Reads LocalThemeIndex + LocalDarkTheme from CompositionLocals,
 * selects the indexed TonalPalettes, and drives the full MaterialTheme.
 *
 * Call this from MainActivity with CompositionLocalProvider wrapping it.
 */
@Composable
fun AndroidTVRemoteTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val darkTheme = LocalDarkTheme.current
    val useDark = darkTheme.isDarkTheme()

    LaunchedEffect(useDark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (useDark) {
                view.windowInsetsController?.setSystemBarsAppearance(
                    0, APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                view.windowInsetsController?.setSystemBarsAppearance(
                    APPEARANCE_LIGHT_STATUS_BARS, APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        }
    }

    val themeIndex = LocalThemeIndex.current
    val wallpaperPalettes = extractTonalPalettesFromUserWallpaper()

    val safePaletteIndex = if (themeIndex in wallpaperPalettes.indices) themeIndex else 0
    val tonalPalettes = wallpaperPalettes[safePaletteIndex]

    val font =Typography

    ProvideZcamViewingConditions {
        CompositionLocalProvider(
            LocalTonalPalettes provides tonalPalettes.apply { Preparing() },
        ) {
            MaterialTheme(
                colorScheme = if (useDark) dynamicDarkColorScheme() else dynamicLightColorScheme(),
                typography = font,
                shapes = AppShapes,
                content = content,
            )
        }
    }
}
