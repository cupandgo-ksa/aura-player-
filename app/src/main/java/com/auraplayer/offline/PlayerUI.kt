package com.auraplayer.offline

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicApp(
    model: PlayerModel,
    allowed: Boolean,
    onRequestPermission: () -> Unit,
    onBackHome: () -> Unit,
    onDelete: (Song) -> Unit,
    onShare: (Song) -> Unit,
    onPickLyrics: (Song) -> Unit
) {
    val light = model.settings.theme == "light"
    val fg = if (light) UiInk else Color.White
    var tab by remember { mutableStateOf(MusicTab.Home) }
    var nowPlaying by remember { mutableStateOf(false) }
    var queueOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Song?>(null) }
    var folderFilter by remember { mutableStateOf<String?>(null) }
    var playlistFilter by remember { mutableStateOf<PlaylistRow?>(null) }
    var playlistDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }

    BackHandler {
        when {
            queueOpen -> queueOpen = false
            nowPlaying -> nowPlaying = false
            selected != null -> selected = null
            folderFilter != null -> folderFilter = null
            playlistFilter != null -> playlistFilter = null
            tab != MusicTab.Home -> tab = MusicTab.Home
            else -> onBackHome()
        }
    }

    WaterBackground(model.settings.theme) {
        Column(Modifier.fillMaxSize()) {
            when {
                queueOpen -> QueuePage(model, light, { queueOpen = false }, { playlistName = ""; playlistDialog = true })
                nowPlaying -> NowPlayingPage(model, light, { nowPlaying = false }, { queueOpen = true }, { model.song?.let { selected = it } }, onPickLyrics)
                else -> {
                    TopHeader(playlistFilter?.name ?: folderFilter ?: tab.label, light)
                    Box(Modifier.weight(1f)) {
                        if (!allowed) PermissionPage(light, onRequestPermission)
                        else when {
                            playlistFilter != null -> SongListPage(model, light, model.visibleSongs.filter { it.id in playlistFilter!!.ids() }, { s, list -> model.play(s, list) }, { selected = it })
                            folderFilter != null -> SongListPage(model, light, model.visibleSongs.filter { it.folder == folderFilter }, { s, list -> model.play(s, list) }, { selected = it })
                            tab == MusicTab.Home -> HomePage(model, light, { model.play(it) }, { selected = it })
                            tab == MusicTab.Songs -> SongListPage(model, light, model.visibleSongs, { s, list -> model.play(s, list) }, { selected = it })
                            tab == MusicTab.Playlists -> PlaylistsPage(model, light, { playlistFilter = it }, { playlistName = ""; playlistDialog = true })
                            tab == MusicTab.Folders -> FoldersPage(model, light) { folderFilter = it }
                            tab == MusicTab.Settings -> SettingsPage(model, light)
                        }
                    }
                    if (allowed) {
                        if (model.song != null) MiniPlayer(model, light) { nowPlaying = true }
                        BottomBar(tab, light) { tab = it; folderFilter = null; playlistFilter = null }
                    }
                }
            }
        }
    }

    if (selected != null) {
        val song = selected!!
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            containerColor = if (light) Color(0xFFF2FAFF) else Color(0xFF101721),
            contentColor = fg
        ) {
            SongMenu(song, model, light, { selected = null }, { onShare(song) }, { onDelete(song); selected = null }, { onPickLyrics(song); selected = null })
        }
    }

    if (playlistDialog) {
        AlertDialog(
            onDismissRequest = { playlistDialog = false },
            title = { Text("New Playlist") },
            text = { OutlinedTextField(playlistName, { playlistName = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    if (queueOpen) model.saveQueue(playlistName) else model.createPlaylist(playlistName)
                    playlistDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { playlistDialog = false }) { Text("Cancel") } }
        )
    }
}
