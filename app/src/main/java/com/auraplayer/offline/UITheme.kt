package com.auraplayer.offline

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val WaterBlue = Color(0xFF73D8FF)
val UiInk = Color(0xFF07111D)
val UiNight = Color(0xFF070B12)

enum class MusicTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home),
    Songs("Songs", Icons.Rounded.LibraryMusic),
    Playlists("Playlists", Icons.Rounded.QueueMusic),
    Folders("Folders", Icons.Rounded.Folder),
    Settings("Settings", Icons.Rounded.Settings)
}

fun timeLabel(ms: Long): String {
    val sec = ms.coerceAtLeast(0L) / 1000L
    return "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"
}

fun audioType(song: Song): String = when {
    song.mime.contains("flac", true) -> "FLAC"
    song.mime.contains("wav", true) -> "WAV"
    song.mime.contains("aac", true) -> "AAC"
    song.mime.contains("ogg", true) -> "OGG"
    song.mime.contains("opus", true) -> "OPUS"
    song.mime.contains("mp4", true) -> "M4A"
    else -> "MP3"
}

@Composable
fun GlassBox(modifier: Modifier = Modifier, light: Boolean, radius: Int = 24, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(radius.dp)
    val glass = if (light)
        Brush.verticalGradient(listOf(Color.White.copy(.78f), Color.White.copy(.50f), Color.White.copy(.38f)))
    else
        Brush.verticalGradient(listOf(Color.White.copy(.18f), Color.White.copy(.105f), Color.White.copy(.065f)))
    Box(
        modifier
            .shadow(16.dp, shape, clip = false)
            .clip(shape)
            .background(glass)
            .border(1.dp, if (light) Color.White.copy(.92f) else Color.White.copy(.26f), shape)
    ) {
        content()
        Box(
            Modifier.align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(1.dp)
                .background(Color.White.copy(if (light) .82f else .32f))
        )
    }
}

@Composable
fun WaterBackground(theme: String, content: @Composable BoxScope.() -> Unit) {
    val light = theme == "light"
    val amoled = theme == "amoled"
    val background = if (light)
        Brush.verticalGradient(listOf(Color(0xFFDFF7FF), Color(0xFFF8FBFF), Color(0xFFE9EEFF)))
    else
        Brush.verticalGradient(listOf(Color(0xFF17364E), UiNight, Color(0xFF17152A)))
    Box(
        Modifier.fillMaxSize().then(
            if (amoled) Modifier.background(Color.Black) else Modifier.background(background)
        )
    ) {
        if (!amoled) {
            Box(Modifier.size(300.dp).offset((-105).dp, 55.dp).clip(CircleShape).background(WaterBlue.copy(if (light) .18f else .14f)))
            Box(Modifier.size(360.dp).align(Alignment.BottomEnd).offset(135.dp, 115.dp).clip(CircleShape).background(Color(0xFF9187FF).copy(if (light) .12f else .10f)))
            Box(Modifier.size(180.dp).align(Alignment.CenterEnd).offset(95.dp, (-120).dp).clip(CircleShape).background(Color.White.copy(if (light) .20f else .045f)))
        }
        content()
    }
}

@Composable
fun TopHeader(title: String, light: Boolean) {
    val fg = if (light) UiInk else Color.White
    Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, top = 24.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("A S Music", color = fg.copy(.57f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(title, color = fg, fontSize = 31.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        GlassBox(Modifier.size(44.dp), light, 22) {
            Icon(Icons.Rounded.MusicNote, "Music", tint = WaterBlue, modifier = Modifier.align(Alignment.Center).size(24.dp))
        }
    }
}

@Composable
fun PermissionPage(light: Boolean, request: () -> Unit) {
    val fg = if (light) UiInk else Color.White
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        GlassBox(Modifier.size(88.dp), light, 44) { Icon(Icons.Rounded.LibraryMusic, null, tint = WaterBlue, modifier = Modifier.align(Alignment.Center).size(43.dp)) }
        Spacer(Modifier.height(20.dp))
        Text("Your music. Your phone.", color = fg, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Your local music stays private on this phone.", color = fg.copy(.58f), fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = request, colors = ButtonDefaults.buttonColors(containerColor = WaterBlue, contentColor = UiInk)) { Text("Allow Music Access") }
    }
}

@Composable
fun MiniPlayer(model: PlayerModel, light: Boolean, open: () -> Unit) {
    val song = model.song ?: return
    val fg = if (light) UiInk else Color.White
    GlassBox(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 5.dp).height(68.dp).clickable(onClick = open), light, 23) {
        Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(song, Modifier.size(52.dp)); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = fg, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = fg.copy(.50f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { model.toggle() }) { Icon(if (model.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play", tint = fg) }
            IconButton(onClick = { model.next() }) { Icon(Icons.Rounded.SkipNext, "Next", tint = fg) }
        }
    }
}

@Composable
fun BottomBar(selected: MusicTab, light: Boolean, change: (MusicTab) -> Unit) {
    val fg = if (light) UiInk else Color.White
    GlassBox(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 6.dp).height(70.dp), light, 26) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            MusicTab.entries.forEach { tab ->
                val active = selected == tab
                Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable { change(tab) }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(tab.icon, tab.label, tint = if (active) WaterBlue else fg.copy(.50f), modifier = Modifier.size(23.dp))
                    Text(tab.label, color = if (active) WaterBlue else fg.copy(.50f), fontSize = 9.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun EmptyMusic(light: Boolean) {
    val fg = if (light) UiInk else Color.White
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.MusicOff, null, tint = WaterBlue, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("No music found", color = fg, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("Copy audio to the phone and scan again.", color = fg.copy(.50f), fontSize = 13.sp)
    }
}
