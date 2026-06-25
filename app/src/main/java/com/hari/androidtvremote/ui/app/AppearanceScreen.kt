package com.hari.androidtvremote.ui.app

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hari.androidtvremote.preference.AppearanceKeys
import com.hari.androidtvremote.preference.DarkThemePreference
import com.hari.androidtvremote.preference.LocalDarkTheme
import com.hari.androidtvremote.preference.LocalThemeIndex
import com.hari.androidtvremote.preference.appearanceDataStore
import com.hari.androidtvremote.ui.theme.palette.TonalPalettes
import com.hari.androidtvremote.ui.theme.palette.dynamic.extractTonalPalettesFromUserWallpaper
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    val scope = rememberCoroutineScope()
    val currentDarkTheme = LocalDarkTheme.current
    val themeIndex = LocalThemeIndex.current
    val palettes = extractTonalPalettesFromUserWallpaper()
    val basicPalettes = palettes.take(BASIC_PALETTE_COUNT)
    val wallpaperPalettes = palettes.drop(BASIC_PALETTE_COUNT)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    fun updateThemeIndex(index: Int) {
        scope.launch {
            context.appearanceDataStore.edit { prefs ->
                prefs[AppearanceKeys.THEME_INDEX] = index
            }
        }
    }

    fun updateDarkTheme(enabled: Boolean) {
        scope.launch {
            context.appearanceDataStore.edit { prefs ->
                prefs[AppearanceKeys.DARK_THEME] = if (enabled) {
                    DarkThemePreference.Dark.value
                } else {
                    DarkThemePreference.Light.value
                }
            }
        }
    }

    AppBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                SettingsTopBar(
                    title = "Appearance",
                    onBack = onBack,
                    scrollBehavior = scrollBehavior
                )
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {


                item {
                    SettingSubtitle(text = "Basic Colors")
                }
                item {
                    PaletteRow(
                        palettes = basicPalettes,
                        selectedIndex = themeIndex,
                        indexOffset = 0,
                        emptyMessage = "Basic colors are unavailable right now.",
                        onSelect = ::updateThemeIndex
                    )
                }
                item {
                    SettingSubtitle(text = "Wallpaper Colors")
                }
                item {
                    PaletteRow(
                        palettes = wallpaperPalettes,
                        selectedIndex = themeIndex,
                        indexOffset = BASIC_PALETTE_COUNT,
                        emptyMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            "No wallpaper colors were detected on this device."
                        } else {
                            "Wallpaper colors require Android 8.1 or newer."
                        },
                        onSelect = ::updateThemeIndex
                    )
                }
                item {
                    SettingSubtitle(text = "Dark Theme")
                }
                item {
                    SettingItemRow(
                        title = "Dark theme",
                        desc = if (currentDarkTheme == DarkThemePreference.Dark) {
                            "Enabled"
                        } else {
                            "Disabled"
                        },
                        icon = Icons.Outlined.Palette,
                        onClick = { updateDarkTheme(currentDarkTheme != DarkThemePreference.Dark) },
                        action = {
                            Switch(
                                checked = currentDarkTheme == DarkThemePreference.Dark,
                                onCheckedChange = ::updateDarkTheme
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteRow(
    palettes: List<TonalPalettes>,
    selectedIndex: Int,
    indexOffset: Int,
    emptyMessage: String,
    onSelect: (Int) -> Unit,
) {
    if (palettes.isEmpty()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            palettes.forEachIndexed { index, palette ->
                PaletteChip(
                    selected = selectedIndex == indexOffset + index,
                    palette = palette,
                    onClick = { onSelect(indexOffset + index) }
                )
            }
        }
    }
}

@Composable
private fun PaletteChip(
    selected: Boolean,
    palette: TonalPalettes,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Surface(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(16.dp)
                .size(48.dp),
            shape = CircleShape,
            color = palette primary 90,
        ) {
            Box {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .offset((-24).dp, 24.dp),
                    color = palette tertiary 90,
                ) {}
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .offset(24.dp, 24.dp),
                    color = palette secondary 60,
                ) {}
                AnimatedVisibility(
                    visible = selected,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                    exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Selected",
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}

private const val BASIC_PALETTE_COUNT = 4
