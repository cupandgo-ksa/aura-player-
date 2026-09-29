package com.auraplayer.offline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val thumbnails = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 16).toInt()) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}

@Composable
fun AlbumArt(song: Song?, modifier: Modifier = Modifier, large: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap by produceState<Bitmap?>(null, song?.uri, large) {
        value = if (song == null) null else withContext(Dispatchers.IO) {
            val key = song.uri.toString() + if (large) "-large" else "-small"
            thumbnails.get(key) ?: loadArt(context, song, if (large) 900 else 160)?.also { thumbnails.put(key, it) }
        }
    }
    Box(modifier.clip(RoundedCornerShape(if (large) 30.dp else 13.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF204A63), Color(0xFF9ABFD0), Color(0xFF34526E)))), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), contentDescription = song?.album ?: "Album artwork",
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Rounded.MusicNote, contentDescription = "Music", tint = Color.White.copy(alpha = .88f),
            modifier = Modifier.fillMaxSize(.43f))
    }
}

private fun loadArt(context: Context, song: Song, edge: Int): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, song.uri)
        val data = retriever.embeddedPicture ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        val sample = generateSequence(1) { it * 2 }.takeWhile {
            bounds.outWidth / it > edge * 2 || bounds.outHeight / it > edge * 2
        }.lastOrNull() ?: 1
        BitmapFactory.decodeByteArray(data, 0, data.size, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (_: Exception) { null }
    finally { try { retriever.release() } catch (_: Exception) { } }
}
