package com.auraplayer.offline

import android.app.Application
import android.content.ComponentName
import android.content.ContentUris
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray

class PlayerModel(private val app: Application) : AndroidViewModel(app) {
    private val dao = MusicDatabase.get(app).dao()
    private val settingsStore = SettingsStore(app)
    var songs by mutableStateOf(emptyList<Song>()); private set
    var states by mutableStateOf(emptyMap<Long, SongState>()); private set
    var playlists by mutableStateOf(emptyList<PlaylistRow>()); private set
    var settings by mutableStateOf(AppSettings()); private set
    var playing by mutableStateOf(false); private set
    var song by mutableStateOf<Song?>(null); private set
    var position by mutableStateOf(0L); private set
    var duration by mutableStateOf(0L); private set
    var queue by mutableStateOf(emptyList<Song>()); private set
    var shuffle by mutableStateOf(false); private set
    var repeat by mutableStateOf(Player.REPEAT_MODE_ALL); private set
    var connected by mutableStateOf(false); private set
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var lastCounted = -1L
    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { updatePlayer() }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updatePlayer()
            val id = mediaItem?.mediaId?.toLongOrNull() ?: return
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK || id != lastCounted) {
                lastCounted = id
                viewModelScope.launch(Dispatchers.IO) {
                    val old = dao.get(id) ?: SongState(id)
                    dao.put(old.copy(plays = old.plays + 1, lastPlayed = System.currentTimeMillis()))
                }
            }
        }
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) { updatePlayer() }
        override fun onPlaybackStateChanged(playbackState: Int) { updatePlayer() }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { updatePlayer() }
        override fun onRepeatModeChanged(repeatMode: Int) { updatePlayer() }
    }
    init {
        viewModelScope.launch { dao.states().collectLatest { states = it.associateBy(SongState::id) } }
        viewModelScope.launch { dao.playlists().collectLatest { playlists = it } }
        viewModelScope.launch { settingsStore.settings.collectLatest { settings = it; controller?.setPlaybackSpeed(it.speed) } }
        connect()
        viewModelScope.launch {
            while (true) { position = controller?.currentPosition?.coerceAtLeast(0) ?: 0; delay(350) }
        }
    }
    fun connect() {
        if (controller != null || controllerFuture != null) return
        val future = MediaController.Builder(app, SessionToken(app, ComponentName(app, MusicService::class.java))).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                controller = future.get().also { it.addListener(listener) }
                connected = true
                updatePlayer()
            } catch (_: Exception) { connected = false; controllerFuture = null }
        }, androidx.core.content.ContextCompat.getMainExecutor(app))
    }
    fun scan() { viewModelScope.launch { songs = LocalLibrary.scan(app); updatePlayer() } }
    val visibleSongs: List<Song> get() = songs.filter {
        it.duration >= settings.minSeconds * 1000L &&
            settings.excluded.split('\n').filter(String::isNotBlank).none { prefix -> it.folder.startsWith(prefix, ignoreCase = true) }
    }
    private fun item(s: Song) = MediaItem.Builder()
        .setMediaId(s.id.toString()).setUri(s.uri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist)
            .setAlbumTitle(s.album).setArtworkUri(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), s.albumId)).build())
        .build()
    private fun updatePlayer() {
        val p = controller ?: return
        playing = p.isPlaying
        duration = p.duration.takeIf { it > 0 } ?: 0
        shuffle = p.shuffleModeEnabled
        repeat = p.repeatMode
        queue = (0 until p.mediaItemCount).mapNotNull { n ->
            val media = p.getMediaItemAt(n)
            songs.find { it.id.toString() == media.mediaId } ?: songs.find { it.uri.toString() == media.localConfiguration?.uri.toString() }
        }
        song = p.currentMediaItem?.let { item ->
            songs.find { it.id.toString() == item.mediaId } ?: songs.find { it.uri.toString() == item.localConfiguration?.uri.toString() }
        }
    }
    fun play(s: Song, from: List<Song> = visibleSongs) {
        val p = controller ?: return
        val list = from.ifEmpty { listOf(s) }
        val index = list.indexOfFirst { it.id == s.id }.coerceAtLeast(0)
        p.setMediaItems(list.map(::item), index, 0)
        p.prepare(); p.play(); updatePlayer()
    }
    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else { it.prepare(); it.play() } }; updatePlayer() }
    fun next() { controller?.seekToNextMediaItem(); updatePlayer() }
    fun previous() { controller?.let { if (it.currentPosition > 3000) it.seekTo(0) else it.seekToPreviousMediaItem() }; updatePlayer() }
    fun seek(ms: Long) { controller?.seekTo(ms.coerceAtLeast(0)); position = ms }
    fun setShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }; updatePlayer() }
    fun cycleRepeat() {
        controller?.repeatMode = when (repeat) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }
        updatePlayer()
    }
    fun playNext(s: Song) { controller?.let { it.addMediaItem((it.currentMediaItemIndex + 1).coerceAtLeast(0), item(s)) }; updatePlayer() }
    fun addToQueue(s: Song) { controller?.addMediaItem(item(s)); updatePlayer() }
    fun removeFromQueue(index: Int) { controller?.let { if (index in 0 until it.mediaItemCount && index != it.currentMediaItemIndex) it.removeMediaItem(index) }; updatePlayer() }
    fun moveQueue(from: Int, to: Int) { controller?.let { if (from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount) it.moveMediaItem(from, to) }; updatePlayer() }
    fun clearQueue() { controller?.let { it.stop(); it.clearMediaItems() }; updatePlayer() }
    fun stop() = clearQueue()
    fun changeSpeed(speed: Float) { controller?.setPlaybackSpeed(speed); change("speed", speed) }
    fun change(key: String, value: Any) { viewModelScope.launch { settingsStore.change(key, value) } }
    fun favorite(id: Long) = updateState(id) { it.copy(favorite = !it.favorite) }
    fun pin(id: Long) = updateState(id) { it.copy(pinned = !it.pinned) }
    private fun updateState(id: Long, op: (SongState) -> SongState) {
        viewModelScope.launch(Dispatchers.IO) { dao.put(op(dao.get(id) ?: SongState(id))) }
    }
    fun createPlaylist(name: String, ids: List<Long> = emptyList()) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) { dao.addPlaylist(PlaylistRow(name = name.trim(), songIds = ids.joinToString(","))) }
    }
    fun renamePlaylist(row: PlaylistRow, name: String) {
        if (name.isNotBlank()) viewModelScope.launch(Dispatchers.IO) { dao.updatePlaylist(row.copy(name = name.trim())) }
    }
    fun deletePlaylist(row: PlaylistRow) { viewModelScope.launch(Dispatchers.IO) { dao.deletePlaylist(row) } }
    fun addToPlaylist(row: PlaylistRow, id: Long) {
        val ids = row.ids(); if (id !in ids) viewModelScope.launch(Dispatchers.IO) { dao.updatePlaylist(row.copy(songIds = (ids + id).joinToString(","))) }
    }
    fun removeFromPlaylist(row: PlaylistRow, id: Long) {
        viewModelScope.launch(Dispatchers.IO) { dao.updatePlaylist(row.copy(songIds = row.ids().filterNot { it == id }.joinToString(","))) }
    }
    fun saveQueue(name: String) = createPlaylist(name, queue.map(Song::id))
    override fun onCleared() {
        controller?.removeListener(listener)
        controllerFuture?.let(MediaController::releaseFuture)
        controller = null
        super.onCleared()
    }
}
