package com.auraplayer.offline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomePage(model: PlayerModel, light: Boolean, play: (Song) -> Unit, more: (Song) -> Unit) {
    val songs = model.visibleSongs
    val pinned = songs.filter { model.states[it.id]?.pinned == true }.take(10)
    val favorites = songs.filter { model.states[it.id]?.favorite == true }.take(10)
    val recent = songs.filter { (model.states[it.id]?.lastPlayed ?: 0L) > 0L }
        .sortedByDescending { model.states[it.id]?.lastPlayed ?: 0L }.take(10)
    val most = songs.sortedByDescending { model.states[it.id]?.plays ?: 0 }
        .filter { (model.states[it.id]?.plays ?: 0) > 0 }.take(10)

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            GlassBox(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), light, 30) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Offline Library", color = if (light) UiInk else Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("${songs.size} songs • private on-device", color = (if (light) UiInk else Color.White).copy(.56f), fontSize = 13.sp)
                    }
                    Icon(Icons.Rounded.CloudOff, null, tint = WaterBlue, modifier = Modifier.size(30.dp))
                }
            }
        }
        if (pinned.isNotEmpty()) item { MusicShelf("Pinned", pinned, light, play, more) }
        if (recent.isNotEmpty()) item { MusicShelf("Recently Played", recent, light, play, more) }
        if (favorites.isNotEmpty()) item { MusicShelf("Favorites", favorites, light, play, more) }
        if (most.isNotEmpty()) item { MusicShelf("Most Played", most, light, play, more) }
        if (songs.isNotEmpty()) item { MusicShelf("Recently Added", songs.sortedByDescending { it.dateAdded }.take(12), light, play, more) }
        if (songs.isEmpty()) item { EmptyMusic(light) }
    }
}

@Composable
private fun MusicShelf(title: String, songs: List<Song>, light: Boolean, play: (Song) -> Unit, more: (Song) -> Unit) {
    val fg = if (light) UiInk else Color.White
    Column(Modifier.padding(top = 12.dp)) {
        Text(title, color = fg, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            items(songs, key = { it.id }) { song ->
                Column(Modifier.width(142.dp).clickable { play(song) }) {
                    AlbumArt(song, Modifier.size(142.dp))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(song.title, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, color = fg.copy(.54f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { more(song) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.MoreHoriz, "More", tint = fg.copy(.60f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
