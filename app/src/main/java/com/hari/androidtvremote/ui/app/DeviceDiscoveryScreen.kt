package com.hari.androidtvremote.ui.app

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay


@Preview()
@Composable
fun SamplePreview() {

}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDiscoveryScreen(
    sessionState: TvRemoteUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onSubmitPairingCode: (String) -> Unit,
    onCancelPairing: () -> Unit,
    onClearStatus: () -> Unit
) {
    val wifiState by rememberWifiUiState()
    val transition = rememberInfiniteTransition(label = "wifi")
    val scale = transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wifiPulse"
    )

    LaunchedEffect(sessionState.statusMessage) {
        if (sessionState.statusMessage != null) {
            delay(2500)
            onClearStatus()
        }
    }

    var disconnectPendingDevice by remember { mutableStateOf<DeviceUiModel?>(null) }

    AppBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    },
                    title = {
                        Column {
                            Text("Discover", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // WiFi status banner
                item {
                    AnimatedContent(targetState = wifiState.isConnected, label = "wifiState") { connected ->
                        if (connected) {
                            WifiBanner(ssid = wifiState.ssid, pulseScale = scale.value)
                        } else {
                            WifiOffBanner()
                        }
                    }
                }

                // Status chip
                item {
                    AnimatedVisibility(
                        visible = sessionState.statusMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        StatusChip(
                            text = sessionState.statusMessage ?: "",
                            isError = sessionState.isError
                        )
                    }
                }

                // Section header
                if (wifiState.isConnected) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    sessionState.discoveredDevices.isEmpty() -> "Searching for TVs…"
                                    sessionState.discoveredDevices.size == 1 -> "1 TV found"
                                    else -> "${sessionState.discoveredDevices.size} TVs found"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (sessionState.discoveredDevices.isEmpty()) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (sessionState.discoveredDevices.isEmpty()) {
                        item {
                            EmptyDevicesHint()
                        }
                    } else {
                        items(sessionState.discoveredDevices, key = { it.id }) { device ->
                            DeviceRow(
                                device = device,
                                isBusy = sessionState.isConnecting && device.isConnecting,
                                onConnect = { onConnect(device.id) },
                                onDisconnect = { disconnectPendingDevice = device }
                            )
                        }
                    }
                }
            }
        }
    }

    // Disconnect confirmation dialog
    disconnectPendingDevice?.let { device ->
        AlertDialog(
            onDismissRequest = { disconnectPendingDevice = null },
            icon = { Icon(Icons.Filled.LinkOff, contentDescription = null) },
            title = { Text("Disconnect?") },
            text = { Text("Disconnect from ${device.name}?") },
            confirmButton = {
                Button(onClick = {
                    disconnectPendingDevice = null
                    onDisconnect()
                }) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = { disconnectPendingDevice = null }) { Text("Cancel") }
            }
        )
    }

    if (sessionState.pairingRequired) {
        PairingDialog(
            pairingError = sessionState.pairingError,
            requestId = sessionState.pairingRequestId,
            onConfirm = onSubmitPairingCode,
            onDismiss = onCancelPairing
        )
    }
}

// ─── WiFi Banner ─────────────────────────────────────────────────────────────

@Composable
private fun WifiBanner(ssid: String?, pulseScale: Float) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .scale(pulseScale)
                    .background(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.NetworkWifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "Scanning network",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = ssid?.let { "\"$it\"" } ?: "Wi-Fi connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun WifiOffBanner() {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Wi-Fi is off",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Connect to the same network as your TV",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
            TextButton(
                onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                    } else {
                        Intent(Settings.ACTION_WIFI_SETTINGS)
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            ) {
                Text("Open", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}



// ─── Status Chip ─────────────────────────────────────────────────────────────

@Composable
private fun StatusChip(text: String, isError: Boolean) {
    val bg = if (isError) MaterialTheme.colorScheme.errorContainer
             else MaterialTheme.colorScheme.tertiaryContainer
    val fg = if (isError) MaterialTheme.colorScheme.onErrorContainer
             else MaterialTheme.colorScheme.onTertiaryContainer
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bg
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = fg
        )
    }
}

// ─── Empty Hint ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyDevicesHint() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Text(
            text = "Make sure your Android TV is on and connected to the same Wi-Fi.\nDevices usually appear within a few seconds.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
        )
    }
}

// ─── Device Row ──────────────────────────────────────────────────────────────

@Composable
private fun DeviceRow(
    device: DeviceUiModel,
    isBusy: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val borderColor = when {
        device.isConnected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isBusy) {
                if (device.isConnected) onDisconnect() else onConnect()
            },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (device.isConnected) 2.dp else 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (device.isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else device.accent.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tv,
                        contentDescription = null,
                        tint = if (device.isConnected) MaterialTheme.colorScheme.primary else device.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Name + meta
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = device.ipAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                // Status badge + action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status label
                    when {
                        device.isConnected -> {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Connected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        device.isPaired -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "Paired",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "New",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(2.dp))

                    // Action button
                    if (device.isConnected) {
                        OutlinedButton(
                            onClick = onDisconnect,
                            enabled = !isBusy,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                Icons.Filled.LinkOff,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Disconnect", style = MaterialTheme.typography.labelSmall)
                        }
                    } else if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        FilledTonalButton(
                            onClick = onConnect,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                if (device.isPaired) "Reconnect" else "Connect",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Progress bar when connecting
            if (isBusy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PairingDialog(
    pairingError: String?,
    requestId: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(requestId) {
        code = ""
        delay(250)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pairing Code") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (pairingError != null) {
                    Text(
                        text = pairingError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    "Enter the 6-digit code shown on your TV screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { input ->
                        code = input
                            .uppercase()
                            .filter { it.isLetterOrDigit() }
                            .take(6)
                    },
                    label = { Text("Code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = { Text("${code.length} / 6") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code) },
                enabled = code.length == 6
            ) {
                Text("Pair")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
