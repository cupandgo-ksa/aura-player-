package com.auraplayer.offline

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.media.audiofx.Equalizer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.json.JSONArray
import org.json.JSONObject

class MusicService : MediaSessionService() {
    companion object {
        const val SLEEP = "com.auraplayer.offline.SLEEP"
        const val STOP = "com.auraplayer.offline.STOP"
        const val EQ = "com.auraplayer.offline.EQ"
        const val HEADPHONES = "com.auraplayer.offline.HEADPHONES"
        const val WIDGET_PREVIOUS = "com.auraplayer.offline.WIDGET_PREVIOUS"
        const val WIDGET_TOGGLE = "com.auraplayer.offline.WIDGET_TOGGLE"
        const val WIDGET_NEXT = "com.auraplayer.offline.WIDGET_NEXT"
    }
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private var sleepJob: Runnable? = null
    private var stopAtEnd = false
    private var equalizer: Equalizer? = null
    private val savePosition = object : Runnable {
        override fun run() {
            persist()
            handler.postDelayed(this, 3000)
        }
    }
    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (stopAtEnd && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        stopAtEnd = false
                        player.pause()
                        player.seekTo(0)
                    }
                    persist()
                }
                override fun onIsPlayingChanged(isPlaying: Boolean) = persist()
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState != Player.STATE_ENDED) return
                    if (stopAtEnd) {
                        stopAtEnd = false
                        player.pause()
                    }
                    persist()
                }
            })
        }
        restore()
        session = MediaSession.Builder(this, player).build()
        MusicWidgetProvider.updateAll(this, player)
        handler.post(savePosition)
    }
    private fun persist() {
        if (!::player.isInitialized) return
        val items = JSONArray()
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            items.put(JSONObject().put("id", item.mediaId)
                .put("uri", item.localConfiguration?.uri.toString())
                .put("title", item.mediaMetadata.title?.toString().orEmpty())
                .put("artist", item.mediaMetadata.artist?.toString().orEmpty())
                .put("album", item.mediaMetadata.albumTitle?.toString().orEmpty())
                .put("artwork", item.mediaMetadata.artworkUri?.toString().orEmpty()))
        }
        getSharedPreferences("session", MODE_PRIVATE).edit()
            .putString("items", items.toString()).putInt("index", player.currentMediaItemIndex.coerceAtLeast(0))
            .putLong("position", player.currentPosition.coerceAtLeast(0)).putBoolean("shuffle", player.shuffleModeEnabled)
            .putInt("repeat", player.repeatMode).putBoolean("playing", player.isPlaying).apply()
        MusicWidgetProvider.updateAll(this, player)
    }
    private fun restore() {
        val prefs = getSharedPreferences("session", MODE_PRIVATE)
        try {
            val array = JSONArray(prefs.getString("items", "[]"))
            val items = (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val meta = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(o.optString("title"))
                    .setArtist(o.optString("artist"))
                    .setAlbumTitle(o.optString("album"))
                o.optString("artwork").takeIf { it.isNotBlank() }?.let { meta.setArtworkUri(android.net.Uri.parse(it)) }
                MediaItem.Builder()
                    .setMediaId(o.optString("id"))
                    .setUri(o.getString("uri"))
                    .setMediaMetadata(meta.build())
                    .build()
            }
            if (items.isNotEmpty()) {
                player.setMediaItems(items, prefs.getInt("index", 0).coerceIn(0, items.lastIndex), prefs.getLong("position", 0))
                player.prepare()
            }
            player.shuffleModeEnabled = prefs.getBoolean("shuffle", false)
            player.repeatMode = prefs.getInt("repeat", Player.REPEAT_MODE_ALL)
        } catch (_: Exception) { }
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            SLEEP -> {
                sleepJob?.let(handler::removeCallbacks)
                stopAtEnd = intent.getBooleanExtra("end", false)
                val seconds = intent.getLongExtra("seconds", 0)
                sleepJob = if (seconds > 0) Runnable { player.pause(); sleepJob = null } else null
                sleepJob?.let { handler.postDelayed(it, seconds * 1000) }
            }
            STOP -> { player.stop(); player.clearMediaItems(); stopSelf() }
            HEADPHONES -> player.setHandleAudioBecomingNoisy(intent.getBooleanExtra("pause", true))
            WIDGET_PREVIOUS -> {
                val count = player.mediaItemCount
                if (count > 1) player.seekToDefaultPosition((player.currentMediaItemIndex.coerceAtLeast(0) - 1 + count) % count)
                else if (count == 1) player.seekTo(0)
                if (count > 0) player.play()
            }
            WIDGET_TOGGLE -> {
                if (player.mediaItemCount > 0) {
                    if (player.isPlaying) player.pause() else {
                        player.prepare()
                        player.play()
                    }
                }
            }
            WIDGET_NEXT -> {
                val count = player.mediaItemCount
                if (count > 1) player.seekToDefaultPosition((player.currentMediaItemIndex.coerceAtLeast(0) + 1) % count)
                else if (count == 1) player.seekTo(0)
                if (count > 0) player.play()
            }
            EQ -> {
                try {
                    equalizer?.release()
                    equalizer = Equalizer(0, player.audioSessionId).apply {
                        enabled = true
                        val level = intent.getShortExtra("bass", 0)
                        val preset = intent.getStringExtra("preset") ?: "Normal"
                        val levels = when (preset) {
                            "Bass" -> intArrayOf(6, 5, 2, 0, 0)
                            "Vocal" -> intArrayOf(-2, 1, 5, 4, 1)
                            "Rock" -> intArrayOf(5, 3, -1, 3, 5)
                            "Pop" -> intArrayOf(-1, 2, 5, 3, -1)
                            "Classical" -> intArrayOf(4, 3, 2, 3, -1)
                            else -> intArrayOf(level.toInt(), 0, 0, 0, 0)
                        }
                        for (i in 0 until numberOfBands.toInt()) {
                            val db = levels[(i * 5 / numberOfBands.toInt()).coerceIn(0, 4)]
                            setBandLevel(i.toShort(), (db * 100).toShort().coerceIn(bandLevelRange[0], bandLevelRange[1]))
                        }
                    }
                } catch (_: Exception) { equalizer = null }
            }
        }
        MusicWidgetProvider.updateAll(this, player)
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onDestroy() {
        handler.removeCallbacks(savePosition)
        sleepJob?.let(handler::removeCallbacks)
        persist()
        equalizer?.release()
        session?.release()
        player.release()
        session = null
        super.onDestroy()
    }
}
