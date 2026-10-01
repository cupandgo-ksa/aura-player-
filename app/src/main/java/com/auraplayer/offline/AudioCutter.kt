package com.auraplayer.offline

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File

/** Exports a real, independently playable AAC/M4A clip; never edits the source. */
class AudioCutter(private val context: Context) {
    private var transformer: Transformer? = null

    fun export(song: Song, fromMs: Long, toMs: Long, completed: (Result<Uri>) -> Unit) {
        if (transformer != null) {
            completed(Result.failure(IllegalStateException("Wait for the current export")))
            return
        }
        if (fromMs < 0 || toMs <= fromMs || toMs > song.duration + 1000L) {
            completed(Result.failure(IllegalArgumentException("Choose a valid part of the song")))
            return
        }
        val temp = File.createTempFile("as_music_clip_", ".m4a", context.cacheDir)
        temp.delete() // Transformer creates the output file itself.
        val clip = MediaItem.Builder().setUri(song.uri)
            .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(fromMs).setEndPositionMs(toMs).build()).build()
        val edited = EditedMediaItem.Builder(clip).setRemoveVideo(true).build()
        try {
            val worker = Transformer.Builder(context)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        transformer = null
                        try { completed(Result.success(storeClip(temp, song.title))) }
                        catch (error: Exception) { completed(Result.failure(error)) }
                        finally { temp.delete() }
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        transformer = null
                        temp.delete()
                        completed(Result.failure(exportException))
                    }
                }).build()
            transformer = worker
            worker.start(edited, temp.absolutePath)
        } catch (error: Exception) {
            transformer = null
            temp.delete()
            completed(Result.failure(error))
        }
    }

    private fun storeClip(temp: File, title: String): Uri {
        val name = title.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ").trim().take(65).ifBlank { "Music" } + " - clip ${System.currentTimeMillis()}.m4a"
        val oldDevicePath = if (Build.VERSION.SDK_INT < 29) {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
            if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("Cannot create Ringtones folder")
            File(dir, name).absolutePath
        } else null
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.TITLE, name.removeSuffix(".m4a"))
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.IS_RINGTONE, 1)
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES + "/A S Music")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            } else put(MediaStore.Audio.Media.DATA, oldDevicePath)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Could not save clip")
        try {
            resolver.openOutputStream(uri)?.use { output -> temp.inputStream().use { it.copyTo(output) } }
                ?: throw IllegalStateException("Could not write clip")
            if (Build.VERSION.SDK_INT >= 29) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            }
            return uri
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }
}
