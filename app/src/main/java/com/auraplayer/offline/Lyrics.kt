package com.auraplayer.offline

import android.content.Context

data class LyricLine(val timeMs: Long, val text: String)

fun readLyrics(context: Context, song: Song): List<LyricLine> {
    val text = try { context.openFileInput("lyrics_${song.id}.lrc").bufferedReader().use { it.readText() } }
        catch (_: Exception) { return emptyList() }
    val pattern = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?]""")
    return text.lines().flatMap { line ->
        val matches = pattern.findAll(line).toList()
        if (matches.isEmpty()) listOf(LyricLine(-1, line.trim())).filter { it.text.isNotBlank() }
        else matches.map { match ->
            val (m, s, fraction) = match.destructured
            LyricLine(m.toLong() * 60000 + s.toLong() * 1000 + fraction.padEnd(3, '0').toLongOrNull().orZero(),
                line.substringAfterLast(']').trim())
        }
    }.filter { it.text.isNotBlank() }.sortedBy { it.timeMs }
}
private fun Long?.orZero() = this ?: 0L
