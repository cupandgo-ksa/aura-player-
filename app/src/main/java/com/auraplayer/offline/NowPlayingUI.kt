package com.auraplayer.offline

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlin.math.abs

@Composable
fun NowPlayingPage(
    model: PlayerModel,
    light: Boolean,
    collapse: () -> Unit,
    queue: () -> Unit,
    more: () -> Unit,
    pickLyrics: (Song) -> Unit
) {
    val song = model.song ?: return
    val fg = if (light) UiInk else Color.White
    var showLyrics by remember { mutableStateOf(false) }
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    val scale by animateFloatAsState(if (model.playing) 1f else .94f, label = "art")

    Box(Modifier.fillMaxSize().pointerInput(song.id) {
        detectDragGestures(
            onDragStart = { dx = 0f; dy = 0f },
            onDrag = { change, drag -> change.consume(); dx += drag.x; dy += drag.y },
            onDragEnd = {
                if (abs(dx) > abs(dy) && abs(dx) > 100f) { if (dx < 0) model.next() else model.previous() }
                else if (dy > 120f) collapse() else if (dy < -120f) queue()
            }
        )
    }) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = collapse) { Icon(Icons.Rounded.KeyboardArrowDown, "Collapse", tint = fg, modifier = Modifier.size(30.dp)) }
                Text("NOW PLAYING", color = fg.copy(.50f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = more) { Icon(Icons.Rounded.MoreHoriz, "More", tint = fg) }
            }
            Spacer(Modifier.height(12.dp))
            AlbumArt(song, Modifier.fillMaxWidth().aspectRatio(1f).graphicsLayer { scaleX = scale; scaleY = scale }, large = true)
            Spacer(Modifier.height(23.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(song.title, color = fg, fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, color = WaterBlue, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { model.favorite(song.id) }) {
                    Icon(if (model.states[song.id]?.favorite == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "Favorite", tint = if (model.states[song.id]?.favorite == true) WaterBlue else fg)
                }
            }
            Text("${audioType(song)} • ${song.album}", color = fg.copy(.42f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Slider(
                value = model.position.toFloat().coerceIn(0f, model.duration.toFloat().coerceAtLeast(1f)),
                onValueChange = { model.seek(it.toLong()) },
                valueRange = 0f..model.duration.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(thumbColor = fg, activeTrackColor = fg, inactiveTrackColor = fg.copy(.20f))
            )
            Row(Modifier.fillMaxWidth()) {
                Text(timeLabel(model.position), color = fg.copy(.48f), fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("-${timeLabel((model.duration - model.position).coerceAtLeast(0L))}", color = fg.copy(.48f), fontSize = 11.sp)
            }
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { model.setShuffle() }) { Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (model.shuffle) WaterBlue else fg.copy(.62f)) }
                IconButton(onClick = { model.previous() }, modifier = Modifier.size(62.dp)) { Icon(Icons.Rounded.SkipPrevious, "Previous", tint = fg, modifier = Modifier.size(38.dp)) }
                GlassBox(Modifier.size(72.dp).clickable { model.toggle() }, light, 36) {
                    Icon(if (model.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play", tint = fg, modifier = Modifier.align(Alignment.Center).size(43.dp))
                }
                IconButton(onClick = { model.next() }, modifier = Modifier.size(62.dp)) { Icon(Icons.Rounded.SkipNext, "Next", tint = fg, modifier = Modifier.size(38.dp)) }
                IconButton(onClick = { model.cycleRepeat() }) {
                    Icon(if (model.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "Repeat", tint = if (model.repeat != Player.REPEAT_MODE_OFF) WaterBlue else fg.copy(.62f))
                }
            }
            Spacer(Modifier.height(14.dp))
            GlassBox(Modifier.fillMaxWidth(), light, 24) {
                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    PlayerTool(Icons.Rounded.Lyrics, "Lyrics", fg) { showLyrics = true }
                    PlayerTool(Icons.Rounded.QueueMusic, "Queue", fg, queue)
                    PlayerTool(Icons.Rounded.PushPin, if (model.states[song.id]?.pinned == true) "Pinned" else "Pin", fg) { model.pin(song.id) }
                    PlayerTool(Icons.Rounded.MoreHoriz, "More", fg, more)
                }
            }
        }
        if (showLyrics) LyricsPage(model, song, light, { showLyrics = false }, { pickLyrics(song) })
    }
}

@Composable
private fun PlayerTool(icon: ImageVector, text: String, fg: Color, click: () -> Unit) {
    Column(Modifier.width(74.dp).clickable(onClick = click).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, text, tint = fg, modifier = Modifier.size(22.dp)); Text(text, color = fg.copy(.58f), fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun LyricsPage(model: PlayerModel, song: Song, light: Boolean, close: () -> Unit, importLyrics: () -> Unit) {
    val fg = if (light) UiInk else Color.White
    val context = LocalContext.current
    val lines = remember(song.id) { readLyrics(context, song) }
    Box(Modifier.fillMaxSize().background(if (light) Color(0xFFF1FAFF).copy(.98f) else Color(0xFF071019).copy(.98f))) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = close) { Icon(Icons.Rounded.KeyboardArrowDown, "Close", tint = fg) }
                Column(Modifier.weight(1f)) { Text(song.title, color = fg, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("Offline Lyrics", color = fg.copy(.50f), fontSize = 12.sp) }
                TextButton(onClick = importLyrics) { Text("Import LRC") }
            }
            if (lines.isEmpty()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.Lyrics, null, tint = WaterBlue, modifier = Modifier.size(52.dp)); Spacer(Modifier.height(12.dp))
                    Text("No lyrics yet", color = fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Import a local .LRC file", color = fg.copy(.52f)); Spacer(Modifier.height(14.dp)); Button(onClick = importLyrics) { Text("Import Lyrics") }
                }
            } else {
                val active = lines.indexOfLast { it.timeMs >= 0 && it.timeMs <= model.position }
                LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp)) {
                    items(lines) { line ->
                        val on = lines.indexOf(line) == active
                        Text(line.text, color = fg.copy(if (on) 1f else .36f), fontSize = if (on) 25.sp else 21.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold, lineHeight = 31.sp,
                            modifier = Modifier.fillMaxWidth().clickable(enabled = line.timeMs >= 0) { model.seek(line.timeMs) }.padding(vertical = 9.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun QueuePage(model: PlayerModel, light: Boolean, back: () -> Unit, save: () -> Unit) {
    val fg = if (light) UiInk else Color.White
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Rounded.ArrowBack, "Back", tint = fg) }
            Column(Modifier.weight(1f)) { Text("Playing Next", color = fg, fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("${model.queue.size} songs", color = fg.copy(.48f), fontSize = 12.sp) }
            TextButton(onClick = save) { Text("Save") }
        }
        LazyColumn(contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 16.dp)) {
            items(model.queue.mapIndexed { index, song -> index to song }, key = { "${it.second.id}-${it.first}" }) { (index, song) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    AlbumArt(song, Modifier.size(48.dp)); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text(song.title, color = if (model.song?.id == song.id) WaterBlue else fg, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(song.artist, color = fg.copy(.48f), fontSize = 12.sp, maxLines = 1) }
                    IconButton(onClick = { if (index > 0) model.moveQueue(index, index - 1) }) { Icon(Icons.Rounded.KeyboardArrowUp, "Up", tint = fg.copy(.55f)) }
                    IconButton(onClick = { if (index < model.queue.lastIndex) model.moveQueue(index, index + 1) }) { Icon(Icons.Rounded.KeyboardArrowDown, "Down", tint = fg.copy(.55f)) }
                    IconButton(onClick = { model.removeFromQueue(index) }) { Icon(Icons.Rounded.Close, "Remove", tint = fg.copy(.55f)) }
                }
            }
        }
    }
}
