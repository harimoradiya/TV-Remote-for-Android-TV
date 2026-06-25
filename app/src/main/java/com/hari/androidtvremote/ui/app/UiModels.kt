package com.hari.androidtvremote.ui.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class HomeTab(val label: String, val icon: ImageVector) {
    Remote("Remote", Icons.Filled.Tv),
    Cast("Cast", Icons.Filled.Cast)
}

enum class RemotePadMode(val label: String, val icon: ImageVector) {
    Touchpad("Touchpad", Icons.Filled.TouchApp),
    DPad("D-pad", Icons.Filled.GridView),
    NumberPad("Number", Icons.Filled.Apps)
}

enum class CastTab(val label: String, val icon: ImageVector) {
    Photos("Photos", Icons.Filled.PhotoLibrary),
    Videos("Videos", Icons.Filled.VideoLibrary),
    Audio("Audio", Icons.Filled.Headphones),
    Display("Display", Icons.Filled.Cast)
}

enum class MediaKind(val label: String, val placeholderIcon: ImageVector) {
    Photo("Photo", Icons.Filled.PhotoLibrary),
    Video("Video", Icons.Filled.VideoLibrary),
    Audio("Audio", Icons.Filled.GraphicEq)
}

data class DeviceUiModel(
    val id: String,
    val name: String,
    val type: String,
    val ipAddress: String,
    val isPaired: Boolean,
    val supportsCast: Boolean,
    val accent: Color,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false
)

data class MediaItemUi(
    val id: Long,
    val title: String,
    val subtitle: String,
    val uri: String,
    val thumbnailUri: String? = null,  // video thumbnail, album art, or photo uri
    val mimeType: String,
    val kind: MediaKind,
    val durationMs: Long = 0L,
    val collectionId: String? = null
)

data class QuickAppChip(
    val name: String,
    val accent: Color
)

data class RemoteControlItem(
    val label: String,
    val icon: ImageVector
)

fun RemotePadMode.next(): RemotePadMode {
    val nextIndex = (ordinal + 1) % RemotePadMode.entries.size
    return RemotePadMode.entries[nextIndex]
}

fun RemotePadMode.primaryMode(): RemotePadMode = when (this) {
    RemotePadMode.NumberPad -> RemotePadMode.Touchpad
    else -> this
}

fun RemotePadMode.toggleNumberPad(primaryMode: RemotePadMode): RemotePadMode =
    if (this == RemotePadMode.NumberPad) primaryMode.primaryMode() else RemotePadMode.NumberPad
