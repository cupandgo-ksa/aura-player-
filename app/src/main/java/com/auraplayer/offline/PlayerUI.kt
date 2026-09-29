package com.auraplayer.offline

import android.os.Build
import android.content.Intent
import android.media.AudioManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import java.text.DecimalFormat
import kotlin.math.abs

private val sky = Color(0xFFB8E7F6)
private val ink = Color(0xFF0A1421)
private val night = Color(0xFF0B101B)

private fun elapsed(ms: Long): String {
    val total = (ms.coerceAtLeast(0) / 1000)
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

@Composable private fun PlayerButton(icon: ImageVector, text: String, color: Color = Color.White,
    size: Int = 27, action: () -> Unit) {
    IconButton(onClick = action) { Icon(icon, text, tint = color, modifier = Modifier.size(size.dp)) }
}

@Composable private fun glass(isLight: Boolean = false): Modifier = Modifier
    .clip(RoundedCornerShape(24.dp))
    .background(if (isLight) Color.White.copy(alpha = .56f) else Color.White.copy(alpha = .095f))
    .border(1.dp, if (isLight) Color.White.copy(alpha = .8f) else Color.White.copy(alpha = .21f), RoundedCornerShape(24.dp))

@Composable private fun inkColor(light: Boolean) = if (light) ink else Color.White

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MusicApp(
    model: PlayerModel, allowed: Boolean, onRequestPermission: () -> Unit, onBackHome: () -> Unit,
    onDelete: (Song) -> Unit, onShare: (Song) -> Unit, onPickLyrics: (Song) -> Unit
) {
    var tab by remember { mutableStateOf("Home") }
    var nowPlaying by remember { mutableStateOf(false) }
    var queueScreen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Song?>(null) }
    var albumFilter by remember { mutableStateOf<String?>(null) }
    var artistFilter by remember { mutableStateOf<String?>(null) }
    var folderFilter by remember { mutableStateOf<String?>(null) }
    var playlistFilter by remember { mutableStateOf<PlaylistRow?>(null) }
    var createPlaylist by remember { mutableStateOf(false) }
    var namingPlaylist by remember { mutableStateOf<PlaylistRow?>(null) }
    var addToPlaylist by remember { mutableStateOf<Song?>(null) }
    var deleteConfirm by remember { mutableStateOf<Song?>(null) }
    var detailsSong by remember { mutableStateOf<Song?>(null) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("Date added") }
    var ascending by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    val light = model.settings.theme == "light"
    val base = if (light) Color(0xFFE6F2F5) else if (model.settings.theme == "amoled") Color.Black else night
    val foreground = inkColor(light)
    val scopeSongs = model.visibleSongs.let { all ->
        val q = query.trim()
        all.filter { s -> q.isBlank() || listOf(s.title, s.artist, s.album, s.folder).any { it.contains(q, true) } }
            .filter { albumFilter == null || it.album == albumFilter }
            .filter { artistFilter == null || it.artist == artistFilter }
            .filter { folderFilter == null || it.folder == folderFilter }
            .filter { playlistFilter == null || it.id in playlistFilter!!.ids() }
            .let { list ->
                val sorted = when(sort) {
                    "Name" -> list.sortedBy { it.title.lowercase() }
                    "Artist" -> list.sortedBy { it.artist.lowercase() }
                    "Album" -> list.sortedBy { it.album.lowercase() }
                    "Duration" -> list.sortedBy { it.duration }
                    "File size" -> list.sortedBy { it.size }
                    else -> list.sortedBy { it.dateAdded }
                }
                if (ascending) sorted else sorted.reversed()
            }
    }
    fun clearFilters() { albumFilter = null; artistFilter = null; folderFilter = null; playlistFilter = null; query = "" }
    BackHandler {
        when {
            queueScreen -> queueScreen = false
            nowPlaying -> nowPlaying = false
            selected != null -> selected = null
            albumFilter != null || artistFilter != null || folderFilter != null || playlistFilter != null -> clearFilters()
            tab != "Home" -> tab = "Home"
            else -> onBackHome()
        }
    }
    Box(Modifier.fillMaxSize().background(base)) {
        if (model.settings.theme != "amoled") Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(if (light) listOf(Color(0xFFD5EFF3), Color(0xFFF7FAF9), Color(0xFFE4E9F5))
                else listOf(Color(0xFF142F43), night, Color(0xFF192236)))))
        Column(Modifier.fillMaxSize()) {
            if (nowPlaying) {
                NowPlayingPage(model, light, { nowPlaying = false }, { queueScreen = true }, onPickLyrics)
            } else if (queueScreen) {
                QueuePage(model, light, { queueScreen = false }, { nameInput = ""; createPlaylist = true })
            } else {
                Row(Modifier.fillMaxWidth().padding(start = 22.dp, top = 28.dp, end = 18.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(if (albumFilter != null) albumFilter!! else if (artistFilter != null) artistFilter!!
                        else if (folderFilter != null) folderFilter!! else if (playlistFilter != null) playlistFilter!!.name else "A S Music",
                        color = foreground, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                    if (albumFilter != null || artistFilter != null || folderFilter != null || playlistFilter != null)
                        IconButton(onClick = ::clearFilters) { Icon(Icons.Rounded.Close, "Close", tint = foreground) }
                    else Icon(Icons.Rounded.MusicNote, "Music", tint = sky)
                }
                if (!allowed) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.LibraryMusic, null, tint = sky, modifier = Modifier.size(58.dp))
                        Spacer(Modifier.height(18.dp))
                        Text("Allow access to music on this phone", color = foreground, fontSize = 20.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("The app scans audio locally. Your files stay on this device.", color = foreground.copy(alpha = .65f))
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = onRequestPermission) { Text("Allow music access") }
                    }
                } else {
                    when {
                        tab == "Settings" -> SettingsPage(model, light, onRescan = model::scan)
                        tab == "Playlists" && playlistFilter == null -> PlaylistPage(model, light,
                            onOpen = { playlistFilter = it; tab = "Songs" },
                            onCreate = { nameInput = ""; createPlaylist = true },
                            onRename = { namingPlaylist = it; nameInput = it.name })
                        tab == "Folders" && folderFilter == null -> FolderPage(model, light) { folderFilter = it; tab = "Songs" }
                        tab == "Home" && albumFilter == null && artistFilter == null -> HomePage(model, light,
                            onPlay = { model.play(it) }, onMore = { selected = it },
                            onView = { kind, value -> clearFilters(); if (kind == "album") albumFilter = value else artistFilter = value; tab = "Songs" },
                            onPlaylist = { playlistFilter = it; tab = "Songs" })
                        else -> {
                            Column {
                                SearchField(query, { query = it }, light)
                                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("${scopeSongs.size} songs", color = foreground.copy(alpha = .7f),
                                        modifier = Modifier.weight(1f), fontSize = 13.sp)
                                    Box {
                                        TextButton(onClick = { sortOpen = true }) {
                                            Icon(Icons.Rounded.Sort, null, tint = foreground)
                                            Text(sort, color = foreground)
                                        }
                                        DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                                            listOf("Name", "Artist", "Album", "Date added", "Duration", "File size").forEach { option ->
                                                DropdownMenuItem(text = { Text(option) }, onClick = { sort = option; sortOpen = false })
                                            }
                                        }
                                    }
                                    IconButton(onClick = { ascending = !ascending }) {
                                        Icon(if (ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                            "Sort direction", tint = foreground)
                                    }
                                }
                                if (scopeSongs.isEmpty()) EmptyMusic(light) else LazyColumn(
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = if (model.song != null) 174.dp else 96.dp)) {
                                    items(scopeSongs, key = Song::id) { s ->
                                        SongRow(s, model.song?.id == s.id, model.playing, model.states[s.id]?.favorite == true,
                                            light, onClick = { model.play(s, scopeSongs) }, onMore = { selected = s })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!nowPlaying && !queueScreen && allowed) Column(Modifier.align(Alignment.BottomCenter)) {
            if (model.song != null) MiniPlayer(model, light, { nowPlaying = true })
            Navigation(tab, light) { clearFilters(); tab = it }
        }
    }
    if (selected != null) {
        val target = selected!!
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = if (light) Color(0xFFF1FAFC) else Color(0xFF1B2A39)) {
            Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 30.dp)) {
                Text(target.title, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = foreground, maxLines = 1)
                Text(target.artist, color = foreground.copy(alpha = .65f), fontSize = 14.sp)
                Spacer(Modifier.height(15.dp))
                fun close(action: () -> Unit) { selected = null; action() }
                MenuAction("Play now", Icons.Rounded.PlayArrow, foreground) { close { model.play(target) } }
                MenuAction("Play next", Icons.Rounded.QueueMusic, foreground) { close { model.playNext(target) } }
                MenuAction("Add to queue", Icons.Rounded.Add, foreground) { close { model.addToQueue(target) } }
                MenuAction("Add to playlist", Icons.Rounded.PlaylistAdd, foreground) { close { addToPlaylist = target } }
                MenuAction(if (model.states[target.id]?.favorite == true) "Unfavorite" else "Favorite", Icons.Rounded.FavoriteBorder, foreground) { close { model.favorite(target.id) } }
                MenuAction(if (model.states[target.id]?.pinned == true) "Unpin" else "Pin to top", Icons.Rounded.PushPin, foreground) { close { model.pin(target.id) } }
                MenuAction("Go to album", Icons.Rounded.Album, foreground) { close { clearFilters(); albumFilter = target.album; tab = "Songs" } }
                MenuAction("Go to artist", Icons.Rounded.Person, foreground) { close { clearFilters(); artistFilter = target.artist; tab = "Songs" } }
                MenuAction("Song details", Icons.Rounded.Info, foreground) { close { detailsSong = target } }
                MenuAction("Share audio file", Icons.Rounded.Share, foreground) { close { onShare(target) } }
                if (Build.VERSION.SDK_INT >= 30) MenuAction("Delete from device", Icons.Rounded.DeleteOutline, Color(0xFFF1A5A5)) { close { deleteConfirm = target } }
            }
        }
    }
    if (deleteConfirm != null) AlertDialog(onDismissRequest = { deleteConfirm = null },
        title = { Text("Delete this audio file?") },
        text = { Text("Android will ask you to confirm removal from your device. This cannot be undone.") },
        confirmButton = { TextButton(onClick = { onDelete(deleteConfirm!!); deleteConfirm = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleteConfirm = null }) { Text("Cancel") } })
    if (detailsSong != null) {
        val s = detailsSong!!
        AlertDialog(onDismissRequest = { detailsSong = null },
            title = { Text(s.title) },
            text = { Text("${s.artist}\n${s.album}\n${s.folder}\n${s.mime.ifBlank { "Audio" }} · ${elapsed(s.duration)} · ${DecimalFormat("#.##").format(s.size / 1048576.0)} MB") },
            confirmButton = { TextButton(onClick = { detailsSong = null }) { Text("Close") } })
    }
    if (createPlaylist || namingPlaylist != null) AlertDialog(
        onDismissRequest = { createPlaylist = false; namingPlaylist = null },
        title = { Text(if (namingPlaylist == null) "New playlist" else "Rename playlist") },
        text = { OutlinedTextField(nameInput, { nameInput = it }, label = { Text("Name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = {
            if (namingPlaylist != null) model.renamePlaylist(namingPlaylist!!, nameInput)
            else model.createPlaylist(nameInput)
            createPlaylist = false; namingPlaylist = null
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { createPlaylist = false; namingPlaylist = null }) { Text("Cancel") } })
    if (addToPlaylist != null) AlertDialog(onDismissRequest = { addToPlaylist = null },
        title = { Text("Add to playlist") },
        text = { Column { model.playlists.forEach { row -> TextButton(onClick = {
            model.addToPlaylist(row, addToPlaylist!!.id); addToPlaylist = null
        }) { Text(row.name) } } } },
        confirmButton = { TextButton(onClick = { addToPlaylist = null; createPlaylist = true }) { Text("New playlist") } })
}

@Composable private fun MenuAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, action: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = action).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(21.dp))
        Text(label, color = color, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable private fun SearchField(query: String, onChange: (String) -> Unit, light: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp).then(glass(light)).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Search, null, tint = inkColor(light).copy(alpha = .65f))
        Spacer(Modifier.width(10.dp))
        BasicTextField(query, onChange, modifier = Modifier.weight(1f), singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = inkColor(light), fontSize = 15.sp),
            decorationBox = { inner ->
                Box { if (query.isEmpty()) Text("Search songs, artists, albums, folders", color = inkColor(light).copy(alpha = .48f), fontSize = 14.sp); inner() }
            })
        if (query.isNotEmpty()) IconButton(onClick = { onChange("") }, modifier = Modifier.size(25.dp)) {
            Icon(Icons.Rounded.Close, "Clear", tint = inkColor(light))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun SongRow(s: Song, active: Boolean, playing: Boolean, favorite: Boolean,
    light: Boolean, onClick: () -> Unit, onMore: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)
        .clip(RoundedCornerShape(17.dp)).combinedClickable(onClick = onClick, onLongClick = onMore)
        .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        AlbumArt(s, Modifier.size(53.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.title, color = if (active) Color(0xFF80D4F0) else inkColor(light), maxLines = 1,
                overflow = TextOverflow.Ellipsis, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium)
            Text("${s.artist} · ${elapsed(s.duration)}", color = inkColor(light).copy(alpha = .55f),
                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (favorite) Icon(Icons.Rounded.Favorite, null, tint = Color(0xFFCE89A1), modifier = Modifier.size(15.dp))
        if (active && playing) Icon(Icons.Rounded.GraphicEq, "Playing", tint = sky, modifier = Modifier.padding(start = 8.dp))
        IconButton(onClick = onMore) { Icon(Icons.Rounded.MoreVert, "More options", tint = inkColor(light).copy(alpha = .7f)) }
    }
}

@Composable private fun EmptyMusic(light: Boolean) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.MusicOff, null, tint = sky, modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(16.dp))
        Text("No songs found", color = inkColor(light), fontSize = 18.sp)
        Text("Add audio to your phone's Music folder or adjust the library filters.", color = inkColor(light).copy(alpha = .6f))
    }
}

@Composable private fun Navigation(tab: String, light: Boolean, onTab: (String) -> Unit) {
    val tabs = listOf("Home", "Songs", "Playlists", "Folders", "Settings")
    val icons = listOf(Icons.Rounded.Home, Icons.Rounded.MusicNote, Icons.Rounded.QueueMusic, Icons.Rounded.Folder, Icons.Rounded.Tune)
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp, top = 4.dp)
        .then(glass(light)).padding(horizontal = 5.dp, vertical = 7.dp), horizontalArrangement = Arrangement.SpaceAround) {
        tabs.forEachIndexed { n, name ->
            Column(Modifier.weight(1f).clip(RoundedCornerShape(17.dp)).clickable { onTab(name) }
                .background(if (tab == name) sky.copy(alpha = if (light) .35f else .19f) else Color.Transparent)
                .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icons[n], name, tint = if (tab == name) Color(0xFF72C9EB) else inkColor(light).copy(alpha = .6f), modifier = Modifier.size(23.dp))
                Text(name, color = inkColor(light), fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable private fun MiniPlayer(model: PlayerModel, light: Boolean, open: () -> Unit) {
    val s = model.song ?: return
    Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp).then(glass(light)).clickable { open() }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AlbumArt(s, Modifier.size(46.dp))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(s.title, color = inkColor(light), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(s.artist, color = inkColor(light).copy(alpha = .6f), fontSize = 12.sp, maxLines = 1)
        }
        IconButton(onClick = model::toggle) {
            Icon(if (model.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", tint = inkColor(light))
        }
        IconButton(onClick = model::next) { Icon(Icons.Rounded.SkipNext, "Next", tint = inkColor(light)) }
    }
}

@Composable private fun HomePage(model: PlayerModel, light: Boolean, onPlay: (Song) -> Unit,
    onMore: (Song) -> Unit, onView: (String, String) -> Unit, onPlaylist: (PlaylistRow) -> Unit) {
    val all = model.visibleSongs
    val pinned = all.filter { model.states[it.id]?.pinned == true }
    val recent = all.filter { (model.states[it.id]?.lastPlayed ?: 0) > 0 }
        .sortedByDescending { model.states[it.id]?.lastPlayed }.take(5)
    val favorites = all.filter { model.states[it.id]?.favorite == true }.take(5)
    val most = all.sortedByDescending { model.states[it.id]?.plays ?: 0 }.filter { (model.states[it.id]?.plays ?: 0) > 0 }.take(5)
    LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = if (model.song == null) 110.dp else 190.dp)) {
        item {
            Text("Your music, your space.", color = inkColor(light).copy(alpha = .65f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Songs" to all.size, "Albums" to all.distinctBy(Song::album).size,
                    "Artists" to all.distinctBy(Song::artist).size).forEach { (label, count) ->
                    Column(Modifier.weight(1f).then(glass(light)).padding(14.dp)) {
                        Text("$count", color = inkColor(light), fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                        Text(label, color = inkColor(light).copy(alpha = .6f), fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(17.dp))
        }
        fun section(title: String, list: List<Song>) {
            if (list.isEmpty()) return
            item { Text(title, color = inkColor(light), fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 11.dp, bottom = 7.dp)) }
            items(list, key = { "$title-${it.id}" }) { s ->
                SongRow(s, model.song?.id == s.id, model.playing,
                    model.states[s.id]?.favorite == true, light, { onPlay(s) }, { onMore(s) })
            }
        }
        section("Pinned music", pinned.take(5))
        section("Recently played", recent)
        section("Favorites", favorites)
        section("Most played", most)
        section("Recently added", all.sortedByDescending(Song::dateAdded).take(5))
        if (model.playlists.isNotEmpty()) item {
            Text("Playlists", color = inkColor(light), fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 7.dp))
            model.playlists.take(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).then(glass(light)).clickable { onPlaylist(row) }.padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.QueueMusic, null, tint = sky)
                    Text(row.name, color = inkColor(light), modifier = Modifier.padding(start = 12.dp).weight(1f))
                    Text("${row.ids().size}", color = inkColor(light).copy(alpha = .5f))
                }
            }
        }
        if (all.isNotEmpty()) {
            item { Text("Albums", color = inkColor(light), fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 7.dp)) }
            items(all.map(Song::album).filter(String::isNotBlank).distinct().take(5)) { name ->
                Text(name, color = inkColor(light), modifier = Modifier.fillMaxWidth().clickable { onView("album", name) }.padding(13.dp))
            }
            item { Text("Artists", color = inkColor(light), fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 7.dp)) }
            items(all.map(Song::artist).distinct().take(5)) { name ->
                Text(name, color = inkColor(light), modifier = Modifier.fillMaxWidth().clickable { onView("artist", name) }.padding(13.dp))
            }
        } else item { EmptyMusic(light) }
    }
}

@Composable private fun FolderPage(model: PlayerModel, light: Boolean, onOpen: (String) -> Unit) {
    val grouped = model.visibleSongs.groupBy(Song::folder).toList().sortedBy { it.first.lowercase() }
    LazyColumn(contentPadding = PaddingValues(18.dp, 6.dp, 18.dp, 140.dp)) {
        items(grouped) { (folder, songs) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).then(glass(light))
                .clickable { onOpen(folder) }.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Folder, null, tint = sky, modifier = Modifier.size(27.dp))
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(folder.substringAfterLast('/'), color = inkColor(light), maxLines = 1)
                    Text(folder, color = inkColor(light).copy(alpha = .5f), fontSize = 12.sp, maxLines = 1)
                }
                Text("${songs.size}", color = inkColor(light).copy(alpha = .6f))
            }
        }
    }
}

@Composable private fun PlaylistPage(model: PlayerModel, light: Boolean, onOpen: (PlaylistRow) -> Unit,
    onCreate: () -> Unit, onRename: (PlaylistRow) -> Unit) {
    var delete by remember { mutableStateOf<PlaylistRow?>(null) }
    Column {
        Button(onClick = onCreate, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Icon(Icons.Rounded.Add, null); Text("Create playlist")
        }
        LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 140.dp)) {
            items(model.playlists, key = PlaylistRow::id) { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).then(glass(light)).clickable { onOpen(row) }.padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.QueueMusic, null, tint = sky, modifier = Modifier.size(27.dp))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(row.name, color = inkColor(light), maxLines = 1)
                        Text("${row.ids().size} songs", color = inkColor(light).copy(alpha = .55f), fontSize = 12.sp)
                    }
                    IconButton(onClick = { onRename(row) }) { Icon(Icons.Rounded.Edit, "Rename", tint = inkColor(light)) }
                    IconButton(onClick = { delete = row }) { Icon(Icons.Rounded.DeleteOutline, "Delete", tint = inkColor(light)) }
                }
            }
        }
    }
    if (delete != null) AlertDialog(onDismissRequest = { delete = null },
        title = { Text("Delete playlist?") }, text = { Text("The songs will stay on your phone.") },
        confirmButton = { TextButton(onClick = { model.deletePlaylist(delete!!); delete = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { delete = null }) { Text("Cancel") } })
}

@Composable private fun QueuePage(model: PlayerModel, light: Boolean, close: () -> Unit, save: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 17.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = close) { Icon(Icons.Rounded.ArrowBack, "Back", tint = inkColor(light)) }
            Text("Playing next", color = inkColor(light), fontSize = 22.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = save) { Text("Save") }
            TextButton(onClick = model::clearQueue) { Text("Clear") }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 70.dp)) {
            items(model.queue.size) { index ->
                val s = model.queue[index]
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).then(glass(light)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    AlbumArt(s, Modifier.size(43.dp))
                    Text(s.title, color = inkColor(light), modifier = Modifier.weight(1f).padding(start = 10.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { model.moveQueue(index, index - 1) }, enabled = index > 0) {
                        Icon(Icons.Rounded.KeyboardArrowUp, "Move up", tint = inkColor(light))
                    }
                    IconButton(onClick = { model.moveQueue(index, index + 1) }, enabled = index < model.queue.lastIndex) {
                        Icon(Icons.Rounded.KeyboardArrowDown, "Move down", tint = inkColor(light))
                    }
                    IconButton(onClick = { model.removeFromQueue(index) }) {
                        Icon(Icons.Rounded.Close, "Remove", tint = inkColor(light))
                    }
                }
            }
        }
    }
}

@Composable private fun NowPlayingPage(model: PlayerModel, light: Boolean,
    close: () -> Unit, openQueue: () -> Unit, pickLyrics: (Song) -> Unit) {
    val song = model.song ?: return
    val context = LocalContext.current
    val text = inkColor(light)
    var page by remember(song.id) { mutableStateOf("Artwork") }
    var artStyle by remember { mutableStateOf("Cover") }
    var showArtStyle by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var showOutput by remember { mutableStateOf(false) }
    var seeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    var swipeX by remember { mutableFloatStateOf(0f) }
    var swipeY by remember { mutableFloatStateOf(0f) }
    var lyricRefresh by remember { mutableIntStateOf(0) }
    val lyrics = remember(song.id, page, lyricRefresh) { readLyrics(context, song) }
    val active = lyrics.indexOfLast { it.timeMs >= 0 && it.timeMs <= model.position }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 23.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            PlayerButton(Icons.Rounded.KeyboardArrowDown, "Collapse player", text, 30, close)
            Spacer(Modifier.weight(1f))
            Text("NOW PLAYING", color = text.copy(alpha = .65f), fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            PlayerButton(Icons.Rounded.Timer, "Sleep timer", text, action = { showTimer = true })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf("Artwork", "Lyrics", "Queue").forEach { choice ->
                TextButton(onClick = { if (choice == "Queue") openQueue() else page = choice }) {
                    Text(choice, color = if (page == choice) Color(0xFF76CFEB) else text.copy(alpha = .57f))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        if (page == "Artwork") {
            Box(Modifier.fillMaxWidth().height(340.dp)
                .pointerInput(song.id) {
                    detectDragGestures(onDragStart = { swipeX = 0f; swipeY = 0f },
                        onDragEnd = {
                            if (abs(swipeX) > 100 && abs(swipeX) > abs(swipeY)) {
                                if (swipeX < 0) model.next() else model.previous()
                            } else if (swipeY > 120) close()
                            else if (swipeY < -120) openQueue()
                        }) { change, delta ->
                        swipeX += delta.x; swipeY += delta.y
                        change.consume()
                    }
                }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(315.dp).background(
                    Brush.radialGradient(listOf(Color(0xFF74B7D7).copy(alpha = .33f), Color.Transparent)),
                    CircleShape))
                AlbumArt(song, Modifier.size(if (artStyle == "Vinyl") 258.dp else 288.dp)
                    .clip(if (artStyle == "Vinyl") CircleShape else RoundedCornerShape(30.dp))
                    .graphicsLayer {
                        scaleX = if (model.playing && !model.settings.reduceMotion) 1.01f else 1f
                        scaleY = scaleX
                    }, large = true)
                if (artStyle == "Vinyl") Box(Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF1A2D3C))
                    .border(2.dp, Color.White.copy(alpha = .5f), CircleShape))
            }
            Box(Modifier.align(Alignment.End)) {
                TextButton(onClick = { showArtStyle = true }) { Icon(Icons.Rounded.Style, null); Text("Cover style") }
                DropdownMenu(expanded = showArtStyle, onDismissRequest = { showArtStyle = false }) {
                    listOf("Cover", "Vinyl", "Soft glow").forEach { style ->
                        DropdownMenuItem(text = { Text(style) }, onClick = { artStyle = style; showArtStyle = false })
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().height(340.dp).then(glass(light)).verticalScroll(rememberScrollState()).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                if (lyrics.isEmpty()) {
                    Text("No offline lyrics for this song", color = text.copy(alpha = .75f))
                    TextButton(onClick = { pickLyrics(song) }) { Text("Choose an .LRC file") }
                    TextButton(onClick = { lyricRefresh++ }) { Text("Refresh lyrics") }
                } else lyrics.forEachIndexed { index, line ->
                    Text(line.text, color = if (index == active) Color(0xFF80D5F2) else text.copy(alpha = .58f),
                        fontSize = if (index == active) 19.sp else 16.sp,
                        modifier = Modifier.fillMaxWidth().clickable(enabled = line.timeMs >= 0) { model.seek(line.timeMs) }.padding(vertical = 7.dp))
                }
            }
        }
        Spacer(Modifier.height(15.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song.title, color = text, fontWeight = FontWeight.Bold, fontSize = 23.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${song.artist} · ${song.album}", color = text.copy(alpha = .68f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            PlayerButton(if (model.states[song.id]?.favorite == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                "Favorite", if (model.states[song.id]?.favorite == true) Color(0xFFEA92A9) else text,
                action = { model.favorite(song.id) })
        }
        Text(song.mime.substringAfter('/').uppercase() + "  ·  " + DecimalFormat("#.##").format(song.size / 1048576.0) + " MB",
            color = Color(0xFF9BCBDB), fontSize = 11.sp, modifier = Modifier.padding(top = 9.dp, bottom = 18.dp))
        Slider(value = if (seeking) seekValue else (model.position.toFloat() / model.duration.coerceAtLeast(1)).coerceIn(0f, 1f),
            onValueChange = { seeking = true; seekValue = it }, onValueChangeFinished = {
                model.seek((seekValue * model.duration).toLong()); seeking = false
            }, colors = SliderDefaults.colors(thumbColor = sky, activeTrackColor = sky, inactiveTrackColor = text.copy(alpha = .2f)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(elapsed(if (seeking) (seekValue * model.duration).toLong() else model.position), color = text.copy(alpha = .6f), fontSize = 12.sp)
            Text(elapsed(model.duration.takeIf { it > 0 } ?: song.duration), color = text.copy(alpha = .6f), fontSize = 12.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            PlayerButton(Icons.Rounded.Shuffle, "Shuffle", if (model.shuffle) sky else text.copy(alpha = .6f), action = model::setShuffle)
            PlayerButton(Icons.Rounded.SkipPrevious, "Previous", text, 36, model::previous)
            FilledIconButton(onClick = model::toggle, modifier = Modifier.size(68.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = sky, contentColor = ink)) {
                Icon(if (model.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", modifier = Modifier.size(39.dp))
            }
            PlayerButton(Icons.Rounded.SkipNext, "Next", text, 36, model::next)
            PlayerButton(if (model.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                "Repeat", if (model.repeat != Player.REPEAT_MODE_OFF) sky else text.copy(alpha = .6f), action = model::cycleRepeat)
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Box {
                TextButton(onClick = { showOutput = true }) { Icon(Icons.Rounded.Headphones, null); Text("Output") }
                DropdownMenu(expanded = showOutput, onDismissRequest = { showOutput = false }) {
                    val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val devices = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).joinToString { it.productName.toString() }
                    DropdownMenuItem(text = { Text(devices.ifBlank { "Phone speaker" }) }, onClick = { showOutput = false })
                }
            }
            Box {
                TextButton(onClick = { showSpeed = true }) { Icon(Icons.Rounded.Speed, null); Text("${model.settings.speed}×") }
                DropdownMenu(expanded = showSpeed, onDismissRequest = { showSpeed = false }) {
                    listOf(.5f,.75f,1f,1.25f,1.5f,1.75f,2f).forEach { speed ->
                        DropdownMenuItem(text = { Text("${speed}×") }, onClick = { model.changeSpeed(speed); showSpeed = false })
                    }
                }
            }
        }
        Spacer(Modifier.height(25.dp))
    }
    if (showTimer) AlertDialog(onDismissRequest = { showTimer = false }, title = { Text("Sleep timer") },
        text = { Column {
            listOf(10,20,30,45,60).forEach { minutes -> TextButton(onClick = {
                context.startService(Intent(context, MusicService::class.java).setAction(MusicService.SLEEP)
                    .putExtra("seconds", minutes * 60L)); showTimer = false
            }) { Text("$minutes minutes") } }
            TextButton(onClick = { context.startService(Intent(context, MusicService::class.java).setAction(MusicService.SLEEP).putExtra("end", true)); showTimer = false }) { Text("End of current song") }
            TextButton(onClick = { context.startService(Intent(context, MusicService::class.java).setAction(MusicService.SLEEP)); showTimer = false }) { Text("Cancel timer") }
        } }, confirmButton = { TextButton(onClick = { showTimer = false }) { Text("Close") } })
}

@Composable private fun SettingsPage(model: PlayerModel, light: Boolean, onRescan: () -> Unit) {
    val context = LocalContext.current
    val color = inkColor(light)
    var excludedOpen by remember { mutableStateOf(false) }
    var excludedText by remember { mutableStateOf(model.settings.excluded) }
    var eqPreset by remember { mutableStateOf("Normal") }
    var bass by remember { mutableFloatStateOf(0f) }
    LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SettingsHeading("Appearance", light) }
        item { SettingsCard(light) {
            Text("Liquid glass theme", color = color, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("light", "dark", "amoled").forEach { option ->
                    FilterChip(selected = model.settings.theme == option, onClick = { model.change("theme", option) },
                        label = { Text(option.replaceFirstChar(Char::uppercase)) })
                }
            }
            SettingSwitch("Reduce animations", model.settings.reduceMotion, color) { model.change("reduce", it) }
        } }
        item { SettingsHeading("Library", light) }
        item { SettingsCard(light) {
            SettingAction("Rescan music", "Find new audio files on this device", Icons.Rounded.Refresh, color, onRescan)
            Text("Hide audio shorter than", color = color)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 10, 30, 60).forEach { seconds ->
                    FilterChip(selected = model.settings.minSeconds == seconds,
                        onClick = { model.change("min", seconds) },
                        label = { Text(if (seconds == 0) "All" else "${seconds}s") })
                }
            }
            SettingAction("Excluded folders", "Hide recordings or voice notes", Icons.Rounded.FolderOff, color) {
                excludedText = model.settings.excluded; excludedOpen = true
            }
        } }
        item { SettingsHeading("Playback", light) }
        item { SettingsCard(light) {
            Text("Speed · ${model.settings.speed}×", color = color)
            Slider(value = model.settings.speed, onValueChange = model::changeSpeed,
                valueRange = .5f..2f, steps = 5)
            SettingSwitch("Pause when headphones disconnect", model.settings.headphonePause, color) { enabled ->
                model.change("headphone", enabled)
                context.startService(Intent(context, MusicService::class.java).setAction(MusicService.HEADPHONES).putExtra("pause", enabled))
            }
            Text("Gapless playback is handled by Media3 when the audio format supports it.", color = color.copy(alpha = .63f), fontSize = 12.sp)
        } }
        item { SettingsHeading("Equalizer", light) }
        item { SettingsCard(light) {
            Text("Presets", color = color, fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Normal", "Bass", "Vocal", "Rock", "Pop", "Classical", "Custom").forEach { preset ->
                    FilterChip(selected = eqPreset == preset, onClick = {
                        eqPreset = preset
                        context.startService(Intent(context, MusicService::class.java).setAction(MusicService.EQ).putExtra("preset", preset))
                    }, label = { Text(preset) })
                }
            }
            if (eqPreset == "Custom") {
                Text("Bass · ${bass.toInt()} dB", color = color)
                Slider(value = bass, onValueChange = {
                    bass = it
                    context.startService(Intent(context, MusicService::class.java).setAction(MusicService.EQ)
                        .putExtra("preset", "Custom").putExtra("bass", it.toInt().toShort()))
                }, valueRange = -12f..12f)
            }
            Text("Equalizer availability depends on the phone's audio engine.", color = color.copy(alpha = .6f), fontSize = 12.sp)
        } }
        item { SettingsHeading("Advanced transitions", light) }
        item { SettingsCard(light) {
            Text("Crossfade, smart transitions and loudness matching need a compatible audio path. They are unavailable in this version; playback stays gapless where supported.", color = color.copy(alpha = .7f), fontSize = 13.sp)
        } }
        item { SettingsHeading("About", light) }
        item { SettingsCard(light) {
            Text("A S media-player · 9.0", color = color, fontWeight = FontWeight.SemiBold)
            Text("Private, offline music. No ads, accounts, tracking or internet permission.", color = color.copy(alpha = .64f), fontSize = 13.sp)
        } }
    }
    if (excludedOpen) AlertDialog(onDismissRequest = { excludedOpen = false },
        title = { Text("Excluded folders") },
        text = { Column {
            Text("Enter each folder path on a new line, such as WhatsApp/Media/WhatsApp Audio.")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(excludedText, { excludedText = it }, minLines = 4, label = { Text("Folder paths") })
        } },
        confirmButton = { TextButton(onClick = { model.change("excluded", excludedText); excludedOpen = false }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { excludedOpen = false }) { Text("Cancel") } })
}

@Composable private fun SettingsHeading(text: String, light: Boolean) {
    Text(text, color = inkColor(light), fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}
@Composable private fun SettingsCard(light: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().then(glass(light)).padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
@Composable private fun SettingSwitch(text: String, checked: Boolean, color: Color, action: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = color, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = action)
    }
}
@Composable private fun SettingAction(text: String, sub: String, icon: ImageVector, color: Color, action: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = action).padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = sky)
        Column(Modifier.padding(start = 12.dp)) {
            Text(text, color = color)
            Text(sub, color = color.copy(alpha = .58f), fontSize = 12.sp)
        }
    }
}
