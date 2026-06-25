package com.hari.androidtvremote.utils

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import fi.iki.elonen.NanoHTTPD
import java.io.FilterInputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class WebServer(
    private val context: Context,
    host: String,
    port: Int
) : NanoHTTPD(host, port) {

    private data class MediaEntry(
        val uri: Uri,
        val mimeType: String,
        val displayName: String
    )

    private val mediaRegistry = ConcurrentHashMap<String, MediaEntry>()

    fun registerMedia(uri: Uri, mimeType: String, displayName: String): String {
        val token = UUID.randomUUID().toString()
        mediaRegistry[token] = MediaEntry(
            uri = uri,
            mimeType = mimeType,
            displayName = displayName.ifBlank { token }
        )
        return "/media/$token/${Uri.encode(displayName.ifBlank { token })}"
    }

    fun clearRegisteredMedia() {
        mediaRegistry.clear()
    }

    override fun serve(session: IHTTPSession): Response {
        if (session.method == Method.OPTIONS) {
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "").apply {
                addHeader("Access-Control-Allow-Origin", "*")
                addHeader("Access-Control-Allow-Headers", "origin,accept,content-type,range")
                addHeader("Access-Control-Allow-Methods", "GET,HEAD,OPTIONS")
            }
        }

        if (session.uri == "/health") {
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "ok").apply {
                addHeader("Access-Control-Allow-Origin", "*")
            }
        }

        val segments = session.uri.trim('/').split('/')
        if (segments.size < 2 || segments.firstOrNull() != "media") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }

        val entry = mediaRegistry[segments[1]]
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Media not found")

        return serveMedia(entry, session.headers["range"])
    }

    private fun serveMedia(entry: MediaEntry, rangeHeader: String?): Response {
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(entry.uri, "r")
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Unable to open media")

        val fileSize = parcelFileDescriptor.statSize
        val startAndEnd = parseRange(rangeHeader, fileSize)
        if (startAndEnd == null && !rangeHeader.isNullOrBlank()) {
            parcelFileDescriptor.close()
            return newFixedLengthResponse(
                Response.Status.RANGE_NOT_SATISFIABLE,
                MIME_PLAINTEXT,
                "Requested range not satisfiable"
            )
        }

        val start = startAndEnd?.first ?: 0L
        val end = startAndEnd?.second ?: (fileSize - 1L).coerceAtLeast(0L)
        val contentLength = if (fileSize >= 0L) (end - start + 1L).coerceAtLeast(0L) else -1L

        val fileInputStream = ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor).apply {
            if (start > 0L) {
                channel.position(start)
            }
        }
        val inputStream = ClosableRangeInputStream(
            input = fileInputStream,
            bytesToRead = contentLength,
            onClose = {
                fileInputStream.close()
            }
        )

        val response = if (contentLength >= 0L) {
            newFixedLengthResponse(
                if (startAndEnd != null) Response.Status.PARTIAL_CONTENT else Response.Status.OK,
                entry.mimeType,
                inputStream,
                contentLength
            )
        } else {
            newChunkedResponse(Response.Status.OK, entry.mimeType, inputStream)
        }

        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Accept-Ranges", "bytes")
        response.addHeader("Content-Disposition", "inline; filename=\"${entry.displayName}\"")
        if (startAndEnd != null && fileSize >= 0L) {
            response.addHeader("Content-Range", "bytes $start-$end/$fileSize")
        }
        return response
    }

    private fun parseRange(rangeHeader: String?, fileSize: Long): Pair<Long, Long>? {
        if (rangeHeader.isNullOrBlank() || fileSize <= 0L) {
            return null
        }

        val rangeValue = rangeHeader.removePrefix("bytes=").trim()
        val parts = rangeValue.split('-', limit = 2)
        if (parts.isEmpty()) {
            return null
        }

        val start = parts[0].toLongOrNull()
        val end = parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.toLongOrNull()

        return when {
            start == null && end != null -> {
                val suffixStart = (fileSize - end).coerceAtLeast(0L)
                suffixStart to (fileSize - 1L)
            }

            start != null -> {
                if (start >= fileSize) {
                    null
                } else {
                    val safeEnd = (end ?: fileSize - 1L).coerceAtMost(fileSize - 1L)
                    start to safeEnd
                }
            }

            else -> null
        }
    }

    private class ClosableRangeInputStream(
        input: InputStream,
        private val bytesToRead: Long,
        private val onClose: () -> Unit
    ) : FilterInputStream(input) {

        private var remaining = bytesToRead

        override fun read(): Int {
            if (remaining == 0L) {
                return -1
            }
            val value = super.read()
            if (value != -1 && remaining > 0L) {
                remaining--
            }
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (remaining == 0L) {
                return -1
            }
            val allowedLength = if (remaining < 0L) {
                length
            } else {
                minOf(length.toLong(), remaining).toInt()
            }
            val count = super.read(buffer, offset, allowedLength)
            if (count > 0 && remaining > 0L) {
                remaining -= count.toLong()
            }
            return count
        }

        override fun close() {
            try {
                super.close()
            } finally {
                onClose()
            }
        }
    }
}
