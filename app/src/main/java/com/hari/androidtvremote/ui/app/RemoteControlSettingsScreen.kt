package com.hari.androidtvremote.ui.app

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Preview(showBackground = true)
@Composable
fun RemoteControlSettingsScreenPreview() {
    RemoteControlSettingsScreen(
        defaultPadMode = RemotePadMode.Touchpad,
        hapticsEnabled = true,
        keepScreenAwake = true,
        remoteShelfMode = RemoteShelfMode.Applications,
        remoteApps = resolveRemoteShortcutApps(defaultRemoteShortcutOrder()),
        onBack = {},
        onDefaultPadModeChange = {},
        onHapticsChange = {},
        onKeepScreenAwakeChange = {},
        onRemoteShelfModeChange = {},
        onRemoteAppOrderChange = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlSettingsScreen(
    defaultPadMode: RemotePadMode,
    hapticsEnabled: Boolean,
    keepScreenAwake: Boolean,
    remoteShelfMode: RemoteShelfMode,
    remoteApps: List<RemoteShortcutApp>,
    onBack: () -> Unit,
    onDefaultPadModeChange: (RemotePadMode) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onKeepScreenAwakeChange: (Boolean) -> Unit,
    onRemoteShelfModeChange: (RemoteShelfMode) -> Unit,
    onRemoteAppOrderChange: (List<String>) -> Unit,
) {
    var showAppOrderDialog by rememberSaveable { mutableStateOf(false) }
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
                    title = "Remote Controls",
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
                    SettingSubtitle(text = "Remote Layout")
                }
                item {
                    DefaultPadModeSelector(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        defaultPadMode = defaultPadMode.primaryMode(),
                        onDefaultPadModeChange = onDefaultPadModeChange
                    )
                }

                item {
                    SettingSubtitle(text = "Remote Controls")
                }
                item {
                    SettingItemRow(
                        title = "Haptic feedback",
                        desc = "Vibrate on button presses",
                        icon = Icons.Filled.TouchApp,
                        onClick = { onHapticsChange(!hapticsEnabled) },
                        action = {
                            Switch(
                                checked = hapticsEnabled,
                                onCheckedChange = onHapticsChange
                            )
                        }
                    )
                }
                item {
                    SettingItemRow(
                        title = "Keep screen awake",
                        desc = "Prevent sleep while using the remote",
                        icon = Icons.Filled.Tv,
                        onClick = { onKeepScreenAwakeChange(!keepScreenAwake) },
                        action = {
                            Switch(
                                checked = keepScreenAwake,
                                onCheckedChange = onKeepScreenAwakeChange
                            )
                        }
                    )
                }
                item {
                    SettingItemRow(
                        title = "Top strip",
                        desc = remoteShelfMode.description,
                        icon = Icons.Filled.Apps,
                    )
                }
                item {
                    ThemeChipRow(
                        options = RemoteShelfMode.entries,
                        selected = remoteShelfMode,
                        labelOf = { it.label },
                        onSelect = onRemoteShelfModeChange
                    )
                }
                item {
                    SettingSubtitle(text = "Manage Applications")
                }
                item {
                    SettingItemRow(
                        title = "Quick-launch order",
                        desc = if (remoteShelfMode == RemoteShelfMode.Applications) {
                            "Drag and arrange the app positions shown on the remote"
                        } else {
                            "Set the app order now for when you use the Applications strip"
                        },
                        icon = Icons.Filled.Apps,
                        onClick = {
                            showAppOrderDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showAppOrderDialog) {
        RemoteAppOrderDialog(
            apps = remoteApps,
            onDismiss = { showAppOrderDialog = false },
            onSave = { reorderedApps ->
                onRemoteAppOrderChange(reorderedApps.map(RemoteShortcutApp::id))
                showAppOrderDialog = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Drag state holder
// ─────────────────────────────────────────────────────────────────────────────
private class DragDropState(
    val items: SnapshotStateList<RemoteShortcutApp>
) {
    var draggingId    by mutableStateOf<String?>(null)
    var dragOffsetPx  by mutableFloatStateOf(0f)
    var itemHeightPx  by mutableFloatStateOf(0f)   // measured at runtime

    /** True while a drag is in progress */
    val isDragging get() = draggingId != null

    fun onDragStart(id: String) {
        draggingId   = id
        dragOffsetPx = 0f
    }

    fun onDrag(deltaY: Float) {
        if (draggingId == null) return
        dragOffsetPx += deltaY

        val step = itemHeightPx.takeIf { it > 0f } ?: return
        var idx  = items.indexOfFirst { it.id == draggingId } .takeIf { it >= 0 } ?: return

        // Swap DOWN
        while (dragOffsetPx > step / 2f && idx < items.lastIndex) {
            val moved = items.removeAt(idx)
            items.add(idx + 1, moved)
            dragOffsetPx -= step
            idx++
        }
        // Swap UP
        while (dragOffsetPx < -step / 2f && idx > 0) {
            val moved = items.removeAt(idx)
            items.add(idx - 1, moved)
            dragOffsetPx += step
            idx--
        }
    }

    fun onDragEnd() {
        draggingId   = null
        dragOffsetPx = 0f
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialog  — nearly full-screen so all items are visible without scrolling
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RemoteAppOrderDialog(
    apps: List<RemoteShortcutApp>,
    onDismiss: () -> Unit,
    onSave: (List<RemoteShortcutApp>) -> Unit
) {
    val workingApps = remember(apps) { apps.toMutableStateList() }
    val dragState   = remember(workingApps) { DragDropState(workingApps) }
    val listState   = rememberLazyListState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows  = false
        )
    ) {
        // Outer box fills the screen; padding reveals the dimmed scrim behind
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),           // ← fills almost all vertical space
                shape           = RoundedCornerShape(32.dp),
                color           = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation  = 8.dp,
                shadowElevation = 24.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // ── Gradient header ──────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                )
                            )
                            .padding(start = 24.dp, end = 12.dp, top = 24.dp, bottom = 20.dp)
                    ) {
                        Column(modifier = Modifier.align(Alignment.CenterStart)) {
                            Text(
                                text       = "Quick-launch order",
                                style      = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text  = "Long-press ≡ then drag to reorder",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick  = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )

                    // ── List — weight(1f) expands to fill all remaining space ─
                    LazyColumn(
                        state   = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),            // ← key: pushes footer to bottom
                        contentPadding          = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement     = Arrangement.spacedBy(8.dp),
                        userScrollEnabled       = !dragState.isDragging
                    ) {
                        itemsIndexed(
                            items = workingApps,
                            key   = { _, app -> app.id }
                        ) { index, app ->
                            DraggableAppItem(
                                app       = app,
                                index     = index,
                                dragState = dragState
                            )
                        }
                    }

                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )

                    // ── Footer ────────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {

                        OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                        Button(onClick = { onSave(workingApps.toList()) }) { Text("Save order") }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Single draggable row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DraggableAppItem(
    app: RemoteShortcutApp,
    index: Int,
    dragState: DragDropState
) {
    val density    = LocalDensity.current
    val isDragging = dragState.draggingId == app.id

    val offsetDp by animateDpAsState(
        targetValue   = if (isDragging) with(density) { dragState.dragOffsetPx.toDp() } else 0.dp,
        animationSpec = spring(stiffness = if (isDragging) 600f else 1800f),
        label         = "offset_${app.id}"
    )
    val elevation by animateDpAsState(
        targetValue   = if (isDragging) 10.dp else 0.dp,
        animationSpec = spring(stiffness = 400f),
        label         = "elevation_${app.id}"
    )
    val scale by animateFloatAsState(
        targetValue   = if (isDragging) 1.03f else 1f,
        animationSpec = spring(stiffness = 400f),
        label         = "scale_${app.id}"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                // Capture actual item height (includes spacing gap via Arrangement)
                if (size.height > 0 && dragState.itemHeightPx == 0f)
                    dragState.itemHeightPx = size.height.toFloat()
            }
            .offset(y = offsetDp)
            .zIndex(if (isDragging) 2f else 0f)
            .shadow(elevation = elevation, shape = RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isDragging) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f)
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── Position badge ────────────────────────────────────────────────────
        Surface(
            shape    = CircleShape,
            color    = if (isDragging) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text       = "${index + 1}",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = if (isDragging) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // ── App label ─────────────────────────────────────────────────────────
        Text(
            text       = app.label,
            modifier   = Modifier.weight(1f),
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = if (isDragging) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )

        // ── "First" chip on position 0 while not dragging ─────────────────────
        if (index == 0 && !dragState.isDragging) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    text       = "First",
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        // ── Drag handle ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isDragging)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
                .pointerInput(app.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart  = { dragState.onDragStart(app.id) },
                        onDrag       = { change, dragAmount ->
                            change.consume()
                            dragState.onDrag(dragAmount.y)
                        },
                        onDragEnd    = { dragState.onDragEnd() },
                        onDragCancel = { dragState.onDragEnd() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Filled.DragHandle,
                contentDescription = "Drag to reorder ${app.label}",
                modifier           = Modifier.size(22.dp),
                tint = if (isDragging) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun DefaultPadModeSelector(
    modifier: Modifier = Modifier,
    defaultPadMode: RemotePadMode,
    onDefaultPadModeChange: (RemotePadMode) -> Unit
) {
    val layoutModes = remember {
        listOf(RemotePadMode.Touchpad, RemotePadMode.DPad)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        layoutModes.forEach { mode ->
            val isSelected = defaultPadMode == mode
            FilledTonalButton(
                onClick = { onDefaultPadModeChange(mode) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (mode == RemotePadMode.Touchpad) {
                            Icons.Filled.TouchApp
                        } else {
                            Icons.Filled.GridView
                        },
                        contentDescription = null
                    )
                    Text(
                        text = mode.label,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun <T> ThemeChipRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .then(
                        if (isSelected) Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ) else Modifier
                    )
                    .clickable { onSelect(option) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labelOf(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
