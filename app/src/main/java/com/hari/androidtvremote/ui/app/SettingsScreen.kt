package com.hari.androidtvremote.ui.app

import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        autoReconnectEnabled = true,
        onBack = {},
        onOpenRemoteControls = {},
        onOpenAppearance = {},
        onOpenTipsSupport = {},
        onAutoReconnectChange = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    autoReconnectEnabled: Boolean,
    onBack: () -> Unit,
    onOpenRemoteControls: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenTipsSupport: () -> Unit,
    onAutoReconnectChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    AppBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                SettingsTopBar(
                    title = "Settings",
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
            ) {
                item {
                    SettingSubtitle(text = "Remote")
                }
                item {
                    SettingGroupItem(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        title = "Remote Controls",
                        desc = "Personalize controls, layout, and quick actions",
                        icon = Icons.Filled.SettingsRemote,
                        onClick = onOpenRemoteControls
                    )
                }

                item {
                    SettingSubtitle(text = "Appearance")
                }
                item {
                    SettingGroupItem(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        title = "Color & style",
                        desc = "Wallpaper colors, basic colors, and dark theme",
                        icon = Icons.Outlined.Palette,
                        onClick = onOpenAppearance
                    )
                }

                item {
                    SettingSubtitle(text = "Tips & Support")
                }
                item {
                    SettingGroupItem(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        title = "Support & about",
                        desc = "About, sponsor, and project support details",
                        icon = Icons.Outlined.TipsAndUpdates,
                        onClick = onOpenTipsSupport
                    )
                }

                item {
                    SettingSubtitle(text = "Connection")
                }
                item {
                    SettingItemRow(
                        title = "Auto reconnect",
                        desc = "Automatically reconnect to the last paired device",
                        icon = Icons.Outlined.Autorenew,
                        onClick = { onAutoReconnectChange(!autoReconnectEnabled) },
                        action = {
                            Switch(
                                checked = autoReconnectEnabled,
                                onCheckedChange = onAutoReconnectChange
                            )
                        }
                    )
                }
            }
        }
    }
}
