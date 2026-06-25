package com.hari.androidtvremote.ui.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBackIosNew

import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.hari.androidtvremote.androidLib.remote.Remotemessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


private data class RemoteMediaAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)


@Composable
fun RemoteScreen(
    modifier: Modifier = Modifier,
    activePadMode: RemotePadMode,
    defaultPadMode: RemotePadMode,
    sessionState: TvRemoteUiState,
    remoteShelfMode: RemoteShelfMode,
    quickApps: List<RemoteShortcutApp>,
    onRequireConnection: () -> Unit,
    onCyclePadMode: () -> Unit,
    onQuickApp: (String) -> Unit,
    onRemoteKey: (Remotemessage.RemoteKeyCode) -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onKeyboardText: (String) -> Unit,
    onKeyboardBackspace: (Int) -> Unit,
    onKeyboardEnter: () -> Unit,
    onToggleVoice: () -> Unit,
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) onToggleVoice()
    }

    val isConnected = sessionState.connectedDevice != null
    val mediaActions = remember(onRemoteKey) {
        listOf(
            RemoteMediaAction(
                icon = Icons.Filled.FastRewind,
                contentDescription = "Rewind",
                onClick = { onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_REWIND) }
            ),
            RemoteMediaAction(
                icon = Icons.Filled.PlayArrow,
                contentDescription = "Play or pause",
                onClick = { onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_PLAY_PAUSE) }
            ),
            RemoteMediaAction(
                icon = Icons.Filled.FastForward,
                contentDescription = "Fast forward",
                onClick = { onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_FAST_FORWARD) }
            ),
            RemoteMediaAction(
                icon = Icons.Filled.Stop,
                contentDescription = "Stop",
                onClick = { onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_STOP) }
            )
        )
    }
    var showKeyboardDialog by rememberSaveable { mutableStateOf(false) }
    val primaryPadMode = defaultPadMode.primaryMode()
    val isNumberPadVisible = activePadMode == RemotePadMode.NumberPad

    fun handleKeyboardOpen() {
        if (isConnected) {
            showKeyboardDialog = true
        } else {
            onRequireConnection()
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val hasTopStrip = remoteShelfMode != RemoteShelfMode.None
        val horizontalPadding = when {
            maxWidth < 360.dp -> 10.dp
            maxWidth < 420.dp -> 14.dp
            else -> 20.dp
        }
        val verticalGap = when {
            maxHeight < 620.dp -> 8.dp
            maxHeight < 760.dp -> 12.dp
            else -> 16.dp
        }
        val shelfButtonSize = when {
            maxWidth < 360.dp -> 52.dp
            maxWidth < 420.dp -> 58.dp
            else -> 64.dp
        }
        val actionIconSize = when {
            maxWidth < 360.dp -> 22.dp
            maxWidth < 420.dp -> 24.dp
            else -> 28.dp
        }
        val controlSpacing = when {
            maxWidth < 360.dp -> 8.dp
            maxWidth < 420.dp -> 10.dp
            else -> 12.dp
        }
        val rockerWidth = when {
            maxWidth < 360.dp -> 56.dp
            maxWidth < 420.dp -> 62.dp
            else -> 68.dp
        }
        val rockerHeight = when {
            maxHeight < 620.dp -> 156.dp
            maxHeight < 760.dp -> 170.dp
            else -> 184.dp
        }
        val padStageHeight = minOf(
            maxWidth - (horizontalPadding * 2),
            if (hasTopStrip) maxHeight * 0.44f else maxHeight * 0.52f,
            352.dp
        ).coerceAtLeast(300.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(verticalGap)
        ) {
            when (remoteShelfMode) {
                RemoteShelfMode.Applications -> AppShortcutStrip(
                    quickApps = quickApps,
                    onQuickApp = onQuickApp,
                    shortcutSize = shelfButtonSize
                )

                RemoteShelfMode.MediaButtons -> MediaButtonStrip(
                    actions = mediaActions,
                    buttonSize = shelfButtonSize
                )

                RemoteShelfMode.None -> Unit
            }

            Spacer(modifier = Modifier.height(15.dp))
            RemotePadStage(
                activePadMode = activePadMode,
                stageHeight = padStageHeight,
                onAction = onRemoteKey
            )
            RemotePageIndicator(
                isSecondarySelected = isNumberPadVisible
            )
            Spacer(modifier = Modifier.height(15.dp))

            RemoteControlDeck(
                isVoiceActive = sessionState.isVoiceActive,
                isNumberPadVisible = isNumberPadVisible,
                primaryPadMode = primaryPadMode,
                rockerWidth = rockerWidth,
                rockerHeight = rockerHeight,
                controlSpacing = controlSpacing,
                actionIconSize = actionIconSize,
                onKeyboard = ::handleKeyboardOpen,
                onHome = { onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_HOME) },
                onSwitchPad = onCyclePadMode,
                onMute = { onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_VOLUME_MUTE) },
                onVoice = {
                    if (hasMicPermission) {
                        onToggleVoice()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onBack = { onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_BACK) },
                onVolumeDown = onVolumeDown,
                onVolumeUp = onVolumeUp,
                onChannelUp = { onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_CHANNEL_UP) },
                onChannelDown = { onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_CHANNEL_DOWN) }
            )
        }
    }

    if (showKeyboardDialog) {
        KeyboardDialog(
            onDismiss = { showKeyboardDialog = false },
            onInsertText = onKeyboardText,
            onBackspace = { onKeyboardBackspace(1) },
            onEnter = onKeyboardEnter
        )
    }
}

@Composable
private fun AppShortcutStrip(
    quickApps: List<RemoteShortcutApp>,
    onQuickApp: (String) -> Unit,
    shortcutSize: Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        quickApps.forEach { app ->
            Column(
                modifier = Modifier.width(shortcutSize + 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(shortcutSize)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(app.accent, app.accentSecondary)
                            )
                        )
                        .clickable { onQuickApp(app.launchName) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.mark,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun MediaButtonStrip(
    actions: List<RemoteMediaAction>,
    buttonSize: Dp,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            FilledTonalButton(
                onClick = action.onClick,
                modifier = Modifier
                    .weight(1f)
                    .height(buttonSize),
                shape = RoundedCornerShape(20.dp),

                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary
                ),


                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = action.icon,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = action.contentDescription
                )
            }
        }
    }
}

@Composable
private fun RemotePadStage(
    activePadMode: RemotePadMode,
    stageHeight: Dp,
    onAction: (Remotemessage.RemoteKeyCode) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(stageHeight),
        contentAlignment = Alignment.Center
    ) {
        val basePadSize = minOf(maxWidth, stageHeight)
        val widePadWidth = minOf(maxWidth, (basePadSize * 1.08f).coerceAtLeast(basePadSize))

        AnimatedContent(targetState = activePadMode, label = "remotePad") { mode ->
            when (mode) {
                RemotePadMode.DPad -> GoogleTvDPad(
                    size = basePadSize,
                    onAction = onAction
                )

                RemotePadMode.Touchpad -> TouchpadPanel(
                    width = widePadWidth,
                    height = basePadSize,
                    onAction = onAction
                )

                RemotePadMode.NumberPad -> NumberPadPanel(
                    width = widePadWidth,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
private fun RemotePageIndicator(
    isSecondarySelected: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(2) { index ->
            val isSelected = if (index == 0) !isSecondarySelected else isSecondarySelected
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(if (isSelected) 12.dp else 10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun RemoteControlDeck(
    isVoiceActive: Boolean,
    isNumberPadVisible: Boolean,
    primaryPadMode: RemotePadMode,
    rockerWidth: Dp,
    rockerHeight: Dp,
    controlSpacing: Dp,
    actionIconSize: Dp,
    onKeyboard: () -> Unit,
    onHome: () -> Unit,
    onSwitchPad: () -> Unit,
    onMute: () -> Unit,
    onVoice: () -> Unit,
    onBack: () -> Unit,
    onVolumeDown: () -> Unit,
    onVolumeUp: () -> Unit,
    onChannelUp: () -> Unit,
    onChannelDown: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(controlSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteVerticalRocker(
            modifier = Modifier.width(rockerWidth),
            label = "VOL",
            topIcon = Icons.Filled.Add,
            bottomIcon = Icons.Filled.Remove,
            height = rockerHeight,
            iconSize = actionIconSize,
            containerColors = listOf(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onTopClick = onVolumeUp,
            onBottomClick = onVolumeDown
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(controlSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(controlSpacing)
            ) {
                RemoteActionBubble(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Keyboard,
                    contentDescription = "Keyboard",
                    onClick = onKeyboard,
                    iconSize = actionIconSize
                )
                RemoteActionBubble(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Home,
                    contentDescription = "Home",
                    onClick = onHome,
                    iconSize = actionIconSize
                )
                RemoteSwitcherBubble(
                    modifier = Modifier.weight(1f),
                    checked = isNumberPadVisible,
                    primaryPadMode = primaryPadMode,
                    onClick = onSwitchPad,
                    iconSize = actionIconSize
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(controlSpacing)
            ) {
                RemoteActionBubble(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.VolumeMute,
                    contentDescription = "Mute",
                    onClick = onMute,
                    iconSize = actionIconSize
                )
                RemoteActionBubble(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Mic,
                    contentDescription = if (isVoiceActive) "Stop voice search" else "Voice search",
                    onClick = onVoice,
                    emphasized = isVoiceActive,
                    iconSize = actionIconSize
                )
                RemoteActionBubble(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.KeyboardBackspace,
                    contentDescription = "Back",
                    onClick = onBack,
                    iconSize = actionIconSize
                )
            }
        }
        RemoteVerticalRocker(
            modifier = Modifier.width(rockerWidth),
            label = "CH",
            topIcon = Icons.Filled.KeyboardArrowUp,
            bottomIcon = Icons.Filled.KeyboardArrowDown,
            height = rockerHeight,
            iconSize = actionIconSize,
            containerColors = listOf(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onTopClick = onChannelUp,
            onBottomClick = onChannelDown
        )
    }
}

@Composable
private fun RemoteVerticalRocker(
    modifier: Modifier = Modifier,
    label: String,
    topIcon: ImageVector,
    bottomIcon: ImageVector,
    height: Dp,
    iconSize: Dp,
    containerColors: List<Color>,
    contentColor: Color,
    onTopClick: () -> Unit,
    onBottomClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(30.dp))
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh
//                Brush.verticalGradient(containerColors)

            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(30.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(onClick = onTopClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = topIcon,
                    contentDescription = "$label up",
                    modifier = Modifier.size(iconSize),
                    tint = contentColor
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                letterSpacing = 1.sp,
                color = contentColor
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(onClick = onBottomClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = bottomIcon,
                    contentDescription = "$label down",
                    modifier = Modifier.size(iconSize),
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
private fun RemoteActionBubble(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    emphasized: Boolean = false,
    iconSize: Dp,
) {
    FilledTonalIconButton(
        modifier = modifier
            .aspectRatio(1f),
        onClick = onClick,
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (emphasized) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = if (emphasized) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}


@Composable
private fun RemoteSwitcherBubble(
    modifier: Modifier = Modifier,
    checked: Boolean,
    primaryPadMode: RemotePadMode,
    onClick: () -> Unit,
    iconSize: Dp,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (checked) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (checked) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (checked) Icons.Filled.Apps else primaryPadMode.icon,
                contentDescription = "Switch remote pad",
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = if (checked) {
                    "123"
                } else if (primaryPadMode == RemotePadMode.Touchpad) {
                    "Touch"
                } else {
                    "D-pad"
                },
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun KeyboardDialog(
    onDismiss: () -> Unit,
    onInsertText: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TV Keyboard") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    val previous = text
                    text = it
                    when {
                        it.length > previous.length && it.startsWith(previous) -> {
                            onInsertText(it.removePrefix(previous))
                        }
                        it.length < previous.length && previous.startsWith(it) -> {
                            repeat(previous.length - it.length) { onBackspace() }
                        }
                        else -> {
                            val commonPrefix = previous.commonPrefixWith(it).length
                            val removedCount = previous.length - commonPrefix
                            repeat(removedCount.coerceAtLeast(0)) { onBackspace() }
                            val inserted = it.substring(commonPrefix)
                            if (inserted.isNotEmpty()) {
                                onInsertText(inserted)
                            }
                        }
                    }
                },
                placeholder = { Text("Type for your TV") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onEnter()
                    }
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = onEnter,
                enabled = true
            ) {
                Text("Enter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun GoogleTvDPad(
    size: Dp,
    onAction: (Remotemessage.RemoteKeyCode) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var activeZone by remember { mutableStateOf<DPadZone?>(null) }
    val padScale by animateFloatAsState(
        targetValue = if (activeZone != null) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
        label = "dpadScale"
    )
    val separatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val edgeOffset = if (size < 260.dp) 20.dp else 28.dp
    val sideOffset = if (size < 260.dp) 18.dp else 26.dp
    val edgeBubbleSize = if (size < 260.dp) 30.dp else 34.dp

    fun activate(zone: DPadZone, key: Remotemessage.RemoteKeyCode) {
        activeZone = zone
        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        onAction(key)
        scope.launch {
            delay(160)
            if (activeZone == zone) {
                activeZone = null
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(padScale),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.matchParentSize(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 16.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.surfaceContainer
                                )
                            )
                        )
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val outerRadius = size.value / 2f
                        val innerRadius = outerRadius * 0.40f
                        listOf(45f, 135f, 225f, 315f).forEach { angle ->
                            val radians = Math.toRadians(angle.toDouble()).toFloat()
                            val start = Offset(
                                x = center.x + cos(radians) * innerRadius,
                                y = center.y + sin(radians) * innerRadius
                            )
                            val end = Offset(
                                x = center.x + cos(radians) * outerRadius,
                                y = center.y + sin(radians) * outerRadius
                            )
                            drawLine(
                                color = separatorColor,
                                start = start,
                                end = end,
                                strokeWidth = 3f
                            )
                        }
                    }

                    DirectionalZone(
                        modifier = Modifier.matchParentSize(),
                        zone = DPadZone.Up,
                        activeZone = activeZone,
                        icon = {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Up")
                        },
                        iconAlignment = Alignment.TopCenter,
                        iconOffsetY = edgeOffset,
                        bubbleSize = edgeBubbleSize,
                        onClick = {
                            activate(
                                DPadZone.Up,
                                Remotemessage.RemoteKeyCode.KEYCODE_DPAD_UP
                            )
                        }
                    )
                    DirectionalZone(
                        modifier = Modifier.matchParentSize(),
                        zone = DPadZone.Right,
                        activeZone = activeZone,
                        icon = {
                            Icon(
                                Icons.Filled.KeyboardArrowRight,
                                contentDescription = "Right"
                            )
                        },
                        iconAlignment = Alignment.CenterEnd,
                        iconOffsetX = sideOffset * -1f,
                        bubbleSize = edgeBubbleSize,
                        onClick = {
                            activate(
                                DPadZone.Right,
                                Remotemessage.RemoteKeyCode.KEYCODE_DPAD_RIGHT
                            )
                        }
                    )
                    DirectionalZone(
                        modifier = Modifier.matchParentSize(),
                        zone = DPadZone.Down,
                        activeZone = activeZone,
                        icon = {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Down")
                        },
                        iconAlignment = Alignment.BottomCenter,
                        iconOffsetY = edgeOffset * -1f,
                        bubbleSize = edgeBubbleSize,
                        onClick = {
                            activate(
                                DPadZone.Down,
                                Remotemessage.RemoteKeyCode.KEYCODE_DPAD_DOWN
                            )
                        }
                    )
                    DirectionalZone(
                        modifier = Modifier.matchParentSize(),
                        zone = DPadZone.Left,
                        activeZone = activeZone,
                        icon = {
                            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Left")
                        },
                        iconAlignment = Alignment.CenterStart,
                        iconOffsetX = sideOffset,
                        bubbleSize = edgeBubbleSize,
                        onClick = {
                            activate(
                                DPadZone.Left,
                                Remotemessage.RemoteKeyCode.KEYCODE_DPAD_LEFT
                            )
                        }
                    )
                }
            }

            val centerPressed = activeZone == DPadZone.Center
            val centerScale by animateFloatAsState(
                targetValue = if (centerPressed) 0.96f else 1f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 560f),
                label = "centerScale"
            )
            Surface(
                modifier = Modifier
                    .size(size * 0.41f)
                    .scale(centerScale)
                    .clip(CircleShape)
                    .clickable {
                        activate(DPadZone.Center, Remotemessage.RemoteKeyCode.KEYCODE_DPAD_CENTER)
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 18.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
//                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectionalZone(
    modifier: Modifier = Modifier,
    zone: DPadZone,
    activeZone: DPadZone?,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    iconAlignment: Alignment,
    iconOffsetX: Dp = 0.dp,
    iconOffsetY: Dp = 0.dp,
    bubbleSize: Dp = 34.dp,
) {
    val isActive = activeZone == zone
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
        label = "${zone.name}Overlay"
    )
    val zoneScale by animateFloatAsState(
        targetValue = if (isActive) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
        label = "${zone.name}Scale"
    )

    Box(
        modifier = modifier
            .scale(zoneScale)
            .clip(zone.shape)
            .background(
                Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f * overlayAlpha),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f * overlayAlpha),
                        Color.Transparent
                    )
                )
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.04f * overlayAlpha),
                    zone.shape
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = iconOffsetX, y = iconOffsetY),
            contentAlignment = iconAlignment
        ) {
            Box(
                modifier = Modifier
                    .size(bubbleSize)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f + (0.14f * overlayAlpha)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        }
    }
}

private enum class DPadZone(val startAngle: Float) {
    Up(225f),
    Right(315f),
    Down(45f),
    Left(135f),
    Center(0f);

    val shape
        get() = directionalSectorShape(startAngle)
}

private fun directionalSectorShape(
    startAngle: Float,
    sweepAngle: Float = 90f,
    innerRadiusFraction: Float = 0.40f,
) = GenericShape { size, _ ->
    val path = Path()
    val outerRadius = size.minDimension / 2f
    val innerRadius = outerRadius * innerRadiusFraction
    val center = Offset(size.width / 2f, size.height / 2f)
    val outerRect = Rect(Offset.Zero, size)
    val innerRect = Rect(
        left = center.x - innerRadius,
        top = center.y - innerRadius,
        right = center.x + innerRadius,
        bottom = center.y + innerRadius
    )

    fun pointOnCircle(radius: Float, angleDegrees: Float): Offset {
        val radians = Math.toRadians(angleDegrees.toDouble()).toFloat()
        return Offset(
            x = center.x + cos(radians) * radius,
            y = center.y + sin(radians) * radius
        )
    }

    val startPoint = pointOnCircle(outerRadius, startAngle)
    path.moveTo(startPoint.x, startPoint.y)
    path.arcTo(outerRect, startAngle, sweepAngle, false)
    path.arcTo(innerRect, startAngle + sweepAngle, -sweepAngle, false)
    path.close()
    addPath(path)
}

@Composable
private fun TouchpadPanel(
    width: Dp,
    height: Dp,
    onAction: (Remotemessage.RemoteKeyCode) -> Unit,
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isTouchActive by remember { mutableStateOf(false) }
    val dotScale by animateFloatAsState(
        targetValue = if (isTouchActive) 1.15f else 1f,
        label = "touchpadDot"
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(34.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.24f),
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.18f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(34.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isTouchActive = true
                        tryAwaitRelease()
                        dragOffset = Offset.Zero
                        isTouchActive = false
                    },
                    onTap = {
                        onAction(Remotemessage.RemoteKeyCode.KEYCODE_DPAD_CENTER)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragOffset = Offset.Zero
                        isTouchActive = true
                    },
                    onDragCancel = {
                        dragOffset = Offset.Zero
                        isTouchActive = false
                    },
                    onDragEnd = {
                        val horizontal = abs(dragOffset.x)
                        val vertical = abs(dragOffset.y)
                        val threshold = 54f
                        when {
                            horizontal < threshold && vertical < threshold ->
                                onAction(Remotemessage.RemoteKeyCode.KEYCODE_DPAD_CENTER)

                            horizontal > vertical && dragOffset.x > 0f ->
                                onAction(Remotemessage.RemoteKeyCode.KEYCODE_DPAD_RIGHT)

                            horizontal > vertical && dragOffset.x < 0f ->
                                onAction(Remotemessage.RemoteKeyCode.KEYCODE_DPAD_LEFT)

                            dragOffset.y > 0f ->
                                onAction(Remotemessage.RemoteKeyCode.KEYCODE_DPAD_DOWN)

                            else ->
                                onAction(Remotemessage.RemoteKeyCode.KEYCODE_DPAD_UP)
                        }

                        dragOffset = Offset.Zero
                        isTouchActive = false
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(10.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(28.dp)
                )
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (dragOffset.x / 5f).roundToInt(),
                        y = (dragOffset.y / 5f).roundToInt()
                    )
                }
                .scale(dotScale)
                .size(if (isTouchActive) 16.dp else 10.dp)
                .zIndex(0f)
                .background(
                    color = if (isTouchActive) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
                    },
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun NumberPadPanel(
    width: Dp,
    onAction: (Remotemessage.RemoteKeyCode) -> Unit,
) {
    val keys = remember {
        listOf(
            "1" to Remotemessage.RemoteKeyCode.KEYCODE_1,
            "2" to Remotemessage.RemoteKeyCode.KEYCODE_2,
            "3" to Remotemessage.RemoteKeyCode.KEYCODE_3,
            "4" to Remotemessage.RemoteKeyCode.KEYCODE_4,
            "5" to Remotemessage.RemoteKeyCode.KEYCODE_5,
            "6" to Remotemessage.RemoteKeyCode.KEYCODE_6,
            "7" to Remotemessage.RemoteKeyCode.KEYCODE_7,
            "8" to Remotemessage.RemoteKeyCode.KEYCODE_8,
            "9" to Remotemessage.RemoteKeyCode.KEYCODE_9,
            "*" to Remotemessage.RemoteKeyCode.KEYCODE_STAR,
            "0" to Remotemessage.RemoteKeyCode.KEYCODE_0,
            "#" to Remotemessage.RemoteKeyCode.KEYCODE_POUND
        )
    }

    val buttonPadding = if (width < 260.dp) 12.dp else 16.dp
    val gridSpacing = if (width < 260.dp) 8.dp else 12.dp

    Column(
        modifier = Modifier
            .width(width)
            .clip(RoundedCornerShape(28.dp))
            .background(
                
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.16f)
                    )
                )
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(gridSpacing)
    ) {
        keys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gridSpacing)
            ) {
                row.forEach { (label, keyCode) ->
                    FilledTonalButton(
                        onClick = { onAction(keyCode) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(vertical = buttonPadding)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    }
}
