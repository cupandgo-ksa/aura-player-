package com.asvideo.offline

import android.content.Context
import android.net.Uri
import java.io.File

/** Makes an adjusted local SRT/VTT copy. The selected subtitle is never edited. */
object SubtitleTiming {
    private val timestamp = Regex("(?:(\\d{2,}):)?(\\d{2}):(\\d{2})([,.])(\\d{3})")
    fun shift(context: Context, uri: Uri, extension: String, delayMs: Long): Uri {
        require(extension == "srt" || extension == "vtt")
        val original = requireNotNull(context.contentResolver.openInputStream(uri)).bufferedReader().use { it.readText() }
        val shifted = original.lineSequence().joinToString("\n") { line ->
            if (!line.contains("-->")) line else timestamp.replace(line) { match ->
                val hours = match.groupValues[1].toLongOrNull() ?: 0
                val total = (hours * 3600000 + match.groupValues[2].toLong() * 60000 +
                    match.groupValues[3].toLong() * 1000 + match.groupValues[5].toLong() + delayMs).coerceAtLeast(0)
                "%02d:%02d:%02d%s%03d".format(java.util.Locale.US, total / 3600000, (total / 60000) % 60,
                    (total / 1000) % 60, match.groupValues[4], total % 1000)
            }
        }
        val file = File(context.cacheDir, "adjusted_subtitle.$extension").apply { writeText(shifted) }
        return Uri.fromFile(file)
    }
}
