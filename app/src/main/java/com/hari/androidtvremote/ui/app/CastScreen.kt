package com.hari.androidtvremote.ui.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Unified enum for bucket-level navigation state.
 * Each tab starts showing "buckets" (albums/folders) and drills into items.
 */
private sealed interface BucketSelection {
    object None : BucketSelection
    data class Photo(val id: String, val name: String) : BucketSelection
    data class Video(val id: String, val name: String) : BucketSelection
    data class Audio(val id: String, val name: String) : BucketSelection
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastScreen(
    modifier: Modifier = Modifier,
    isConnected: Boolean,
    deviceName: String?,
    castState: CastPlaybackUiState,
    onOpenCastPlayer: (MediaItemUi) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(CastTab.Photos) }
    var bucket by remember { mutableStateOf<BucketSelection>(BucketSelection.None) }

    val context = LocalContext.current
    val permissions = rememberMediaPermissions()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissions.refresh(context)
    }

    // ── Photo albums ──────────────────────────────────────────────────────────
    val photoAlbums by produceState(emptyList<PhotoAlbum>(), permissions.hasMediaPermissions, selectedTab) {
        value = if (permissions.hasMediaPermissions && selectedTab == CastTab.Photos) queryAlbums(context)
        else emptyList()
    }
    // ── Video folders ─────────────────────────────────────────────────────────
    val videoFolders by produceState(emptyList<VideoFolder>(), permissions.hasMediaPermissions, selectedTab) {
        value = if (permissions.hasMediaPermissions && selectedTab == CastTab.Videos) queryVideoFolders(context)
        else emptyList()
    }
    // ── Audio albums ──────────────────────────────────────────────────────────
    val audioAlbums by produceState(emptyList<AudioAlbum>(), permissions.hasMediaPermissions, selectedTab) {
        value = if (permissions.hasMediaPermissions && selectedTab == CastTab.Audio) queryAudioAlbums(context)
        else emptyList()
    }

    // ── Items inside selected bucket ──────────────────────────────────────────
    val bucketItems by produceState(emptyList<MediaItemUi>(), bucket) {
        value = when (val b = bucket) {
            is BucketSelection.Photo -> queryPhotosInAlbum(context, b.id)
            is BucketSelection.Video -> queryVideosInFolder(context, b.id)
            is BucketSelection.Audio -> queryAudiosInAlbum(context, b.id)
            BucketSelection.None -> emptyList()
        }
    }

    val bucketName: String? = when (val b = bucket) {
        is BucketSelection.Photo -> b.name
        is BucketSelection.Video -> b.name
        is BucketSelection.Audio -> b.name
        BucketSelection.None -> null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Tab bar ───────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CastTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            bucket = BucketSelection.None
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }

        // ── Status banner ─────────────────────────────────────────────────────
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (isConnected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (isConnected) {
                        if (castState.isCasting && castState.activeMedia != null)
                            "Casting: ${castState.activeMedia.title}"
                        else "Pick media to cast to ${deviceName ?: "your TV"}"
                    } else "Connect to a TV first",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConnected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Display placeholder ───────────────────────────────────────────────
        if (selectedTab == CastTab.Display) {
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Display casting", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Pick Photos, Videos, or Audio to start casting.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            return@LazyColumn
        }

        // ── Permission gate ───────────────────────────────────────────────────
        if (!permissions.hasMediaPermissions) {
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Media access needed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Button(onClick = { launcher.launch(permissions.requiredPermissions) }) { Text("Grant Permission") }
                    }
                }
            }
            return@LazyColumn
        }

        // ── Back row when inside a bucket ─────────────────────────────────────
        if (bucketName != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { bucket = BucketSelection.None }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(15.dp))
                        Text(
                            text = bucketName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Items inside the bucket
            if (bucketItems.isEmpty()) {
                item { Text("No items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                when (selectedTab) {
                    CastTab.Photos -> {
                        // 3-col photo grid
                        items(bucketItems.chunked(3)) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                row.forEach { item ->
                                    PhotoThumb(
                                        modifier = Modifier.weight(1f),
                                        item = item,
                                        enabled = isConnected,
                                        onClick = { onOpenCastPlayer(item) }
                                    )
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    CastTab.Videos, CastTab.Audio -> {
                        // 2-col media cards
                        items(bucketItems.chunked(2)) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { item ->
                                    MediaCard(
                                        modifier = Modifier.weight(1f),
                                        mediaItem = item,
                                        enabled = isConnected,
                                        onClick = { onOpenCastPlayer(item) }
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    else -> {}
                }
            }
            return@LazyColumn
        }

        // ── Bucket grid (top-level) ───────────────────────────────────────────
        when (selectedTab) {
            CastTab.Photos -> {
                item {
                    Text(
                        "Albums",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (photoAlbums.isEmpty()) {
                    item { Text("No albums found", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(photoAlbums.chunked(2)) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { album ->
                                PhotoAlbumCard(
                                    modifier = Modifier.weight(1f),
                                    album = album,
                                    onClick = { bucket = BucketSelection.Photo(album.id, album.name) }
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            CastTab.Videos -> {
                item {
                    Text(
                        "Folders",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (videoFolders.isEmpty()) {
                    item { Text("No video folders found", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(videoFolders.chunked(2)) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { folder ->
                                VideoFolderCard(
                                    modifier = Modifier.weight(1f),
                                    folder = folder,
                                    onClick = { bucket = BucketSelection.Video(folder.id, folder.name) }
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            CastTab.Audio -> {
                item {
                    Text(
                        "Albums",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (audioAlbums.isEmpty()) {
                    item { Text("No audio albums found", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(audioAlbums.chunked(2)) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { album ->
                                AudioAlbumCard(
                                    modifier = Modifier.weight(1f),
                                    album = album,
                                    onClick = { bucket = BucketSelection.Audio(album.id, album.name) }
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

// ─── Photo Album card ─────────────────────────────────────────────────────────

@Composable
private fun PhotoAlbumCard(modifier: Modifier, album: PhotoAlbum, onClick: () -> Unit) {
    BucketCard(
        modifier = modifier,
        coverUri = album.coverUri,
        name = album.name,
        count = album.count,
        icon = Icons.Filled.Folder,
        onClick = onClick
    )
}

// ─── Video Folder card ────────────────────────────────────────────────────────

@Composable
private fun VideoFolderCard(modifier: Modifier, folder: VideoFolder, onClick: () -> Unit) {
    BucketCard(
        modifier = modifier,
        coverUri = folder.coverUri,
        name = folder.name,
        count = folder.count,
        icon = Icons.Filled.PlayArrow,
        onClick = onClick
    )
}

// ─── Audio Album card ─────────────────────────────────────────────────────────

@Composable
private fun AudioAlbumCard(modifier: Modifier, album: AudioAlbum, onClick: () -> Unit) {
    BucketCard(
        modifier = modifier,
        coverUri = album.artUri,
        name = album.name,
        count = album.count,
        icon = Icons.Filled.Album,
        onClick = onClick
    )
}

// ─── Generic bucket card ──────────────────────────────────────────────────────

@Composable
private fun BucketCard(
    modifier: Modifier,
    coverUri: String?,
    name: String,
    count: Int,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier

            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (coverUri != null) {
                MediaArtwork(
                    model = coverUri,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    isVideo = icon == Icons.Filled.PlayArrow,
                    modifier = Modifier.matchParentSize()
                )
                // Bottom scrim
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                            )
                        )
                )
            } else {
                // Fallback icon
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.
            width(4.dp))

            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Photo thumb (3-column) ───────────────────────────────────────────────────

@Composable
private fun PhotoThumb(
    modifier: Modifier = Modifier,
    item: MediaItemUi,
    enabled: Boolean,
    onClick: () -> Unit
) {
    AsyncImage(
        model = item.thumbnailUri ?: item.uri,
        contentDescription = item.title,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
    )
}

// ─── Video / Audio media card (2-column) ──────────────────────────────────────

@Composable
private fun MediaCard(
    modifier: Modifier = Modifier,
    mediaItem: MediaItemUi,
    enabled: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer
                                )
                            )
                        )
                )
                if (mediaItem.thumbnailUri != null) {
                    MediaArtwork(
                        model = mediaItem.thumbnailUri,
                        contentDescription = mediaItem.title,
                        contentScale = ContentScale.Crop,
                        isVideo = mediaItem.kind == MediaKind.Video,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (mediaItem.kind == MediaKind.Video) Icons.Filled.PlayArrow else Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(mediaItem.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (mediaItem.subtitle.isNotBlank()) {
                    Text(mediaItem.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
