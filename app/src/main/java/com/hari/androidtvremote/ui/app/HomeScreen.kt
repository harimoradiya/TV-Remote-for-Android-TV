package com.hari.androidtvremote.ui.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.hari.androidtvremote.BuildConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.hari.androidtvremote.androidLib.remote.Remotemessage
import kotlinx.coroutines.launch

// Outline icons for unselected state — ReadYou uses outlined/filled toggle
private val HomeTab.outlinedIcon: ImageVector
    get() = when (this) {
        HomeTab.Remote -> Icons.Outlined.Tv
        HomeTab.Cast -> Icons.Outlined.Cast
    }

private val HomeTab.filledIcon: ImageVector
    get() = when (this) {
        HomeTab.Remote -> Icons.Filled.Tv
        HomeTab.Cast -> Icons.Filled.Cast
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentTab: HomeTab,
    activePadMode: RemotePadMode,
    defaultPadMode: RemotePadMode,
    sessionState: TvRemoteUiState,
    hapticsEnabled: Boolean,
    remoteShelfMode: RemoteShelfMode,
    remoteApps: List<RemoteShortcutApp>,
    onTabSelected: (HomeTab) -> Unit,
    onCyclePadMode: () -> Unit,
    onOpenDiscovery: () -> Unit,
    onOpenSettings: () -> Unit,
    onPower: () -> Unit,
    onQuickApp: (String) -> Unit,
    onRemoteKey: (Remotemessage.RemoteKeyCode) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onKeyboardText: (String) -> Unit,
    onKeyboardBackspace: (Int) -> Unit,
    onKeyboardEnter: () -> Unit,
    onToggleVoice: () -> Unit,
    onOpenCastPlayer: (MediaItemUi) -> Unit,
    onUserRated: () -> Unit = {},
    onUserFeedbackClicked: () -> Unit = {},
    onDismissRatingPrompt: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(sessionState.statusMessage, sessionState.isError) {
        val message = sessionState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
    }

    fun performHaptic() {
        if (hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun handleAction(action: () -> Unit) {
        performHaptic()
        action()
    }

    fun handleRemoteAction(action: () -> Unit) {
        performHaptic()
        if (sessionState.connectedDevice != null) {
            action()
        } else {
            onOpenDiscovery()
        }
    }

    AppBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Column {
                            Text(
                                text = sessionState.connectedDevice?.name ?: "Android TV Remote",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { handleRemoteAction(onPower) }
                        ) {
                            Icon(Icons.Filled.PowerSettingsNew, contentDescription = "Power")
                        }
                        IconButton(onClick = onOpenDiscovery) {
                            Icon(
                                imageVector = if (sessionState.connectedDevice != null) {
                                    Icons.Filled.CastConnected
                                } else {
                                    Icons.Filled.Cast
                                },
                                contentDescription = if (sessionState.connectedDevice != null) {
                                    "Connected device"
                                } else {
                                    "Discover"
                                }
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                )
            },
            bottomBar = {
                // ── ReadYou-style bottom nav ──
                Column {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = Color.Transparent,   // transparent — backdrop shows through
                        tonalElevation = 0.dp
                    ) {
                        HomeTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            val iconColor = animateColorAsState(
                                targetValue = if (selected)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(220),
                                label = "iconColor"
                            )

                            NavigationBarItem(
                                selected = selected,
                                onClick = { onTabSelected(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.filledIcon else tab.outlinedIcon,
                                        contentDescription = tab.label,
                                        tint = iconColor.value,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    // pill indicator — matches ReadYou's secondaryContainer pill
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Crossfade(targetState = currentTab, label = "tabContent") { tab ->
                when (tab) {
                    HomeTab.Remote -> RemoteScreen(
                        modifier = Modifier.padding(innerPadding),
                        activePadMode = activePadMode,
                        defaultPadMode = defaultPadMode,
                        sessionState = sessionState,
                        remoteShelfMode = remoteShelfMode,
                        quickApps = remoteApps,
                        onRequireConnection = onOpenDiscovery,
                        onCyclePadMode = onCyclePadMode,
                        onQuickApp = { appName -> handleRemoteAction { onQuickApp(appName) } },
                        onRemoteKey = { key -> handleRemoteAction { onRemoteKey(key) } },
                        onVolumeUp = { handleRemoteAction(onVolumeUp) },
                        onVolumeDown = { handleRemoteAction(onVolumeDown) },
                        onKeyboardText = { text ->
                            if (sessionState.connectedDevice != null) {
                                onKeyboardText(text)
                            } else {
                                onOpenDiscovery()
                            }
                        },
                        onKeyboardBackspace = { count ->
                            if (sessionState.connectedDevice != null) {
                                onKeyboardBackspace(count)
                            } else {
                                onOpenDiscovery()
                            }
                        },
                        onKeyboardEnter = {
                            if (sessionState.connectedDevice != null) {
                                onKeyboardEnter()
                            } else {
                                onOpenDiscovery()
                            }
                        },
                        onToggleVoice = { handleRemoteAction(onToggleVoice) }
                    )

                    HomeTab.Cast -> CastScreen(
                        modifier = Modifier.padding(innerPadding),
                        isConnected = sessionState.connectedDevice != null,
                        deviceName = sessionState.connectedDevice?.name,
                        castState = sessionState.cast,
                        onOpenCastPlayer = { mediaItem ->
                            handleAction { onOpenCastPlayer(mediaItem) }
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Starting cast for ${mediaItem.title}",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    val context = LocalContext.current
    if (sessionState.showRatingPrompt) {
        AlertDialog(
            onDismissRequest = onDismissRatingPrompt,
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Enjoying the Remote?",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = "If you love using this remote, please take a moment to rate us on the Play Store. Your support means the world to the developers!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUserRated()
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
                        }
                    }
                ) {
                    Text("Rate 5 Stars", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onUserFeedbackClicked()
                            val feedbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("harimordiya123@gmail.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "Feedback on Android TV Remote App")
                                putExtra(Intent.EXTRA_TEXT, "App Version: ${BuildConfig.VERSION_NAME}\n\nFeedback:")
                            }
                            try {
                                context.startActivity(Intent.createChooser(feedbackIntent, "Send Feedback via"))
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    ) {
                        Text("Give Feedback")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismissRatingPrompt) {
                        Text("Maybe Later")
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}
