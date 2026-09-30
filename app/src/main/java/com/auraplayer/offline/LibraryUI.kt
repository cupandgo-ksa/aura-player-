package com.auraplayer.offline

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SongListPage(model: PlayerModel, light: Boolean, source: List<Song>, play: (Song, List<Song>) -> Unit, more: (Song) -> Unit) {
    val fg = if (light) UiInk else Color.White
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("Recent") }
    var openSort by remember { mutableStateOf(false) }
    val filtered = source.filter {
        query.isBlank() || it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
    }.let { list ->
        when (sort) {
            "Title" -> list.sortedBy { it.title.lowercase() }
            "Artist" -> list.sortedBy { it.artist.lowercase() }
            "Album" -> list.sortedBy { it.album.lowercase() }
            else -> list.sortedByDescending { it.dateAdded }
        }
    }

    Column(Modifier.fillMaxSize()) {
        GlassBox(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp), light, 20) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = fg.copy(.46f), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(9.dp))
                BasicTextField(query, { query = it }, singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = fg, fontSize = 15.sp), modifier = Modifier.weight(1f),
                    decorationBox = { inner -> if (query.isBlank()) Text("Search songs, artists, albums", color = fg.copy(.40f), fontSize = 15.sp); inner() })
                if (query.isNotBlank()) IconButton(onClick = { query = "" }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, "Clear", tint = fg.copy(.58f), modifier = Modifier.size(17.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${filtered.size} songs", color = fg.copy(.53f), fontSize = 13.sp, modifier = Modifier.weight(1f))
            Box {
                TextButton(onClick = { openSort = true }) { Icon(Icons.Rounded.Sort, null, tint = fg, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text(sort, color = fg) }
                DropdownMenu(openSort, { openSort = false }) {
                    listOf("Recent", "Title", "Artist", "Album").forEach { value ->
                        DropdownMenuItem({ Text(value) }, { sort = value; openSort = false })
                    }
                }
            }
        }
        if (filtered.isEmpty()) EmptyMusic(light) else LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
            items(filtered, key = { it.id }) { song -> SongRow(song, model, light, { play(song, filtered) }, { more(song) }) }
        }
    }
}

@Composable
private fun SongRow(song: Song, model: PlayerModel, light: Boolean, play: () -> Unit, more: () -> Unit) {
    val fg = if (light) UiInk else Color.White
    val active = model.song?.id == song.id
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = play).padding(horizontal = 7.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AlbumArt(song, Modifier.size(54.dp)); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = if (active) WaterBlue else fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${song.artist} • ${timeLabel(song.duration)}", color = fg.copy(.50f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (model.states[song.id]?.favorite == true) Icon(Icons.Rounded.Favorite, null, tint = WaterBlue, modifier = Modifier.size(16.dp))
        IconButton(onClick = more) { Icon(Icons.Rounded.MoreHoriz, "More", tint = fg.copy(.58f)) }
    }
}

@Composable
fun PlaylistsPage(model: PlayerModel, light: Boolean, open: (PlaylistRow) -> Unit, create: () -> Unit) {
    val fg = if (light) UiInk else Color.White
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        GlassBox(Modifier.fillMaxWidth().clickable(onClick = create), light, 22) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AddCircle, null, tint = WaterBlue, modifier = Modifier.size(30.dp)); Spacer(Modifier.width(12.dp)); Text("New Playlist", color = fg, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn {
            items(model.playlists, key = { it.id }) { row ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { open(row) }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassBox(Modifier.size(52.dp), light, 16) { Icon(Icons.Rounded.QueueMusic, null, tint = WaterBlue, modifier = Modifier.align(Alignment.Center).size(27.dp)) }
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(row.name, color = fg, fontWeight = FontWeight.SemiBold); Text("${row.ids().size} songs", color = fg.copy(.50f), fontSize = 12.sp) }
                    Icon(Icons.Rounded.ChevronRight, null, tint = fg.copy(.40f))
                }
            }
        }
    }
}

@Composable
fun FoldersPage(model: PlayerModel, light: Boolean, open: (String) -> Unit) {
    val fg = if (light) UiInk else Color.White
    val groups = model.visibleSongs.groupBy { it.folder }.toList().sortedBy { it.first.lowercase() }
    LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp)) {
        items(groups, key = { it.first }) { (folder, songs) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { open(folder) }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassBox(Modifier.size(52.dp), light, 16) { Icon(Icons.Rounded.Folder, null, tint = WaterBlue, modifier = Modifier.align(Alignment.Center).size(27.dp)) }
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
                    Text(folder.substringAfterLast('/'), color = fg, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${songs.size} songs", color = fg.copy(.50f), fontSize = 12.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = fg.copy(.40f))
            }
        }
    }
}

@Composable
fun SongMenu(song: Song, model: PlayerModel, light: Boolean, dismiss: () -> Unit, share: () -> Unit, delete: () -> Unit, lyrics: () -> Unit) {
    val fg = if (light) UiInk else Color.White
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(song, Modifier.size(62.dp)); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = fg.copy(.52f), maxLines = 1)
            }
        }
        Spacer(Modifier.height(12.dp))
        MenuAction(Icons.Rounded.PlayArrow, "Play Now", fg) { model.play(song); dismiss() }
        MenuAction(Icons.Rounded.SkipNext, "Play Next", fg) { model.playNext(song); dismiss() }
        MenuAction(Icons.Rounded.QueueMusic, "Add to Queue", fg) { model.addToQueue(song); dismiss() }
        MenuAction(if (model.states[song.id]?.favorite == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            if (model.states[song.id]?.favorite == true) "Remove Favorite" else "Favorite", fg) { model.favorite(song.id); dismiss() }
        MenuAction(Icons.Rounded.PushPin, if (model.states[song.id]?.pinned == true) "Unpin" else "Pin to Home", fg) { model.pin(song.id); dismiss() }
        if (model.playlists.isNotEmpty()) {
            Text("Add to playlist", color = fg.copy(.46f), fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp, top = 6.dp))
            model.playlists.take(4).forEach { row -> MenuAction(Icons.Rounded.PlaylistAdd, row.name, fg) { model.addToPlaylist(row, song.id); dismiss() } }
        }
        MenuAction(Icons.Rounded.Lyrics, "Import Offline Lyrics", fg, lyrics)
        MenuAction(Icons.Rounded.Share, "Share Audio", fg, share)
        MenuAction(Icons.Rounded.Info, "${audioType(song)} • ${timeLabel(song.duration)} • ${song.album}", fg) { }
        MenuAction(Icons.Rounded.DeleteOutline, "Delete from Device", Color(0xFFFF6B6B), delete)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MenuAction(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, fg: Color, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = click).padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(12.dp)); Text(text, color = fg, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
