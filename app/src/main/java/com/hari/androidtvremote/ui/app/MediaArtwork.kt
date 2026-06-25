package com.hari.androidtvremote.ui.app

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MediaArtwork(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    isVideo: Boolean = false,
    fallback: @Composable (() -> Unit)? = null
) {
    if (!isVideo) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        } else {
            fallback?.invoke()
        }
        return
    }

    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, model) {
        value = withContext(Dispatchers.IO) {
            model?.let { loadVideoThumbnail(context, it) }
        }
    }

    Box(modifier = modifier) {
        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            fallback?.invoke()
        }
    }
}

private fun loadVideoThumbnail(context: Context, model: String): Bitmap? {
    return runCatching {
        val uri = Uri.parse(model)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(uri, Size(720, 720), null)
        } else {
            val videoId = ContentUris.parseId(uri)
            MediaStore.Video.Thumbnails.getThumbnail(
                context.contentResolver,
                videoId,
                MediaStore.Video.Thumbnails.MINI_KIND,
                null
            )
        }
    }.getOrNull()
}
