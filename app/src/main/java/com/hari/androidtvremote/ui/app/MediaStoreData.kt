package com.hari.androidtvremote.ui.app

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhotoAlbum(
    val id: String,
    val name: String,
    val coverUri: String,
    val count: Int
)

data class VideoFolder(
    val id: String,
    val name: String,
    val coverUri: String,
    val count: Int
)

data class AudioAlbum(
    val id: String,
    val name: String,
    val artUri: String?,
    val count: Int
)

class MediaPermissionState(context: Context) {
    private val permissions = requiredMediaPermissions()
    var hasMediaPermissions by mutableStateOf(checkMediaPermissions(context, permissions))
        private set

    val requiredPermissions: Array<String> = permissions

    fun refresh(context: Context) {
        hasMediaPermissions = checkMediaPermissions(context, permissions)
    }
}

@Composable
fun rememberMediaPermissions(): MediaPermissionState {
    val context = LocalContext.current
    return remember(context) { MediaPermissionState(context) }
}

fun requiredMediaPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun checkMediaPermissions(context: Context, permissions: Array<String>): Boolean {
    return permissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

suspend fun queryMedia(context: Context, tab: CastTab): List<MediaItemUi> {
    return withContext(Dispatchers.IO) {
        when (tab) {
            CastTab.Photos -> queryImages(context)
            CastTab.Videos -> queryVideos(context)
            CastTab.Audio -> queryAudio(context)
            CastTab.Display -> emptyList()
        }
    }
}

// ─── Album (bucket) querying ──────────────────────────────────────────────────

suspend fun queryAlbums(context: Context): List<PhotoAlbum> = withContext(Dispatchers.IO) {
    val albums = linkedMapOf<String, PhotoAlbum>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    )
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null, null,
        "${MediaStore.Images.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val bucketIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        while (cursor.moveToNext()) {
            val bucketId = cursor.getString(bucketIdIdx) ?: continue
            if (!albums.containsKey(bucketId)) {
                val imageId = cursor.getLong(idIdx)
                val coverUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId)
                albums[bucketId] = PhotoAlbum(
                    id = bucketId,
                    name = cursor.getString(bucketNameIdx) ?: "Photos",
                    coverUri = coverUri.toString(),
                    count = 1
                )
            } else {
                albums[bucketId] = albums[bucketId]!!.copy(count = albums[bucketId]!!.count + 1)
            }
        }
    }
    albums.values.toList()
}

suspend fun queryPhotosInAlbum(context: Context, bucketId: String): List<MediaItemUi> = withContext(Dispatchers.IO) {
    queryImages(context, bucketId)
}

private fun queryImages(context: Context, bucketId: String? = null): List<MediaItemUi> {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.MIME_TYPE
    )
    val selection = bucketId?.let { "${MediaStore.Images.Media.BUCKET_ID} = ?" }
    val selectionArgs = bucketId?.let { arrayOf(it) }
    val items = mutableListOf<MediaItemUi>()
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection, selection, selectionArgs,
        "${MediaStore.Images.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val mimeTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            items += MediaItemUi(
                id = id,
                title = cursor.getString(titleIndex) ?: "Photo",
                subtitle = cursor.getString(bucketIndex) ?: "",
                uri = uri.toString(),
                thumbnailUri = uri.toString(), // Same URI — Coil handles downscaling
                mimeType = cursor.getString(mimeTypeIndex) ?: "image/*",
                kind = MediaKind.Photo,
                collectionId = bucketId
            )
        }
    }
    return items
}

private fun queryVideos(context: Context): List<MediaItemUi> {
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.MIME_TYPE
    )
    val items = mutableListOf<MediaItemUi>()
    context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection, null, null,
        "${MediaStore.Video.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val mimeTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            val videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            val duration = cursor.getLong(durationIndex)
            items += MediaItemUi(
                id = id,
                title = cursor.getString(titleIndex) ?: "Video",
                subtitle = formatDuration(duration),
                uri = videoUri.toString(),
                thumbnailUri = videoUri.toString(),
                mimeType = cursor.getString(mimeTypeIndex) ?: "video/*",
                kind = MediaKind.Video,
                durationMs = duration
            )
        }
    }
    return items
}

private fun queryAudio(context: Context): List<MediaItemUi> {
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID
    )
    val items = mutableListOf<MediaItemUi>()
    context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection, null, null,
        "${MediaStore.Audio.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val mimeTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val albumIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
            val duration = cursor.getLong(durationIndex)
            val albumId = cursor.getLong(albumIdIndex)
            val artUri = Uri.withAppendedPath(
                Uri.parse("content://media/external/audio/albumart"),
                albumId.toString()
            ).toString()
            items += MediaItemUi(
                id = id,
                title = cursor.getString(titleIndex) ?: "Audio",
                subtitle = cursor.getString(artistIndex) ?: "",
                uri = uri.toString(),
                thumbnailUri = artUri,
                mimeType = cursor.getString(mimeTypeIndex) ?: "audio/*",
                kind = MediaKind.Audio,
                durationMs = duration,
                collectionId = albumId.toString()
            )
        }
    }
    return items
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

// ─── Video Folder queries ─────────────────────────────────────────────────────

suspend fun queryVideoFolders(context: Context): List<VideoFolder> = withContext(Dispatchers.IO) {
    val folders = linkedMapOf<String, VideoFolder>()
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.BUCKET_ID,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME
    )
    context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection, null, null,
        "${MediaStore.Video.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val bucketIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
        val bucketNameIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        while (cursor.moveToNext()) {
            val bucketId = cursor.getString(bucketIdIdx) ?: continue
            if (!folders.containsKey(bucketId)) {
                val videoId = cursor.getLong(idIdx)
                val coverUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId)
                folders[bucketId] = VideoFolder(
                    id = bucketId,
                    name = cursor.getString(bucketNameIdx) ?: "Videos",
                    coverUri = coverUri.toString(),
                    count = 1
                )
            } else {
                folders[bucketId] = folders[bucketId]!!.copy(count = folders[bucketId]!!.count + 1)
            }
        }
    }
    folders.values.toList()
}

suspend fun queryVideosInFolder(context: Context, bucketId: String): List<MediaItemUi> =
    withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_ID
        )
        val items = mutableListOf<MediaItemUi>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Video.Media.BUCKET_ID} = ?",
            arrayOf(bucketId),
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val mimeTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val bucketIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val duration = cursor.getLong(durationIndex)
                items += MediaItemUi(
                    id = id,
                    title = cursor.getString(titleIndex) ?: "Video",
                    subtitle = formatDuration(duration),
                    uri = videoUri.toString(),
                    thumbnailUri = videoUri.toString(),
                    mimeType = cursor.getString(mimeTypeIndex) ?: "video/*",
                    kind = MediaKind.Video,
                    durationMs = duration,
                    collectionId = cursor.getString(bucketIdIndex)
                )
            }
        }
        items
    }

// ─── Audio Album queries ──────────────────────────────────────────────────────

suspend fun queryAudioAlbums(context: Context): List<AudioAlbum> = withContext(Dispatchers.IO) {
    val albums = linkedMapOf<String, AudioAlbum>()
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.ALBUM
    )
    context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection, null, null,
        "${MediaStore.Audio.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val albumIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val albumNameIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        while (cursor.moveToNext()) {
            val albumId = cursor.getLong(albumIdIdx).toString()
            if (!albums.containsKey(albumId)) {
                val artUri = Uri.withAppendedPath(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                ).toString()
                albums[albumId] = AudioAlbum(
                    id = albumId,
                    name = cursor.getString(albumNameIdx) ?: "Unknown Album",
                    artUri = artUri,
                    count = 1
                )
            } else {
                albums[albumId] = albums[albumId]!!.copy(count = albums[albumId]!!.count + 1)
            }
        }
    }
    albums.values.toList()
}

suspend fun queryAudiosInAlbum(context: Context, albumId: String): List<MediaItemUi> =
    withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val items = mutableListOf<MediaItemUi>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.ALBUM_ID} = ?",
            arrayOf(albumId),
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val mimeTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val duration = cursor.getLong(durationIndex)
                val artUri = Uri.withAppendedPath(
                    Uri.parse("content://media/external/audio/albumart"),
                    cursor.getLong(albumIdIndex).toString()
                ).toString()
                items += MediaItemUi(
                    id = id,
                    title = cursor.getString(titleIndex) ?: "Audio",
                    subtitle = cursor.getString(artistIndex) ?: "",
                    uri = uri.toString(),
                    thumbnailUri = artUri,
                    mimeType = cursor.getString(mimeTypeIndex) ?: "audio/*",
                    kind = MediaKind.Audio,
                    durationMs = duration,
                    collectionId = cursor.getLong(albumIdIndex).toString()
                )
            }
        }
        items
    }
