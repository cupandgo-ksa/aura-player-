package com.auraplayer.offline

import android.Manifest
import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private val songs = mutableListOf<Song>()
    @Volatile private var libraryJson: String = "[]"
    @Volatile private var webReady = false
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = pushPlayerState()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = pushPlayerState()
        override fun onPlaybackStateChanged(playbackState: Int) = pushPlayerState()
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = pushPlayerState()
        override fun onRepeatModeChanged(repeatMode: Int) = pushPlayerState()
    }

    private val poll = object : Runnable {
        override fun run() {
            pushPlayerState()
            handler.postDelayed(this, 400)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideStatusBar()

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
            if (allowed) scanLibrary()
            else Toast.makeText(this, "Music access is needed to show your songs", Toast.LENGTH_LONG).show()
        }

        webView = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            overScrollMode = WebView.OVER_SCROLL_NEVER
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mediaPlaybackRequiresUserGesture = false
            addJavascriptInterface(NativeBridge(), "Android")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    webReady = true
                    ensureMusicPermission()
                    pushLibrary()
                    pushPlayerState()
                }
            }
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(webView)

        connectController()
        handler.post(poll)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webReady) webView.evaluateJavascript(
                    "window.handleNativeBack ? window.handleNativeBack() : Android.exitApp()",
                    null
                ) else moveTaskToBack(true)
            }
        })
    }

    private fun permissionName(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun ensureMusicPermission() {
        val permission = permissionName()
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            scanLibrary()
        } else permissionLauncher.launch(permission)
    }

    private fun scanLibrary() {
        lifecycleScope.launch {
            val result = LocalLibrary.scan(this@MainActivity)
                .filter { it.duration >= 10_000L }
            songs.clear()
            songs.addAll(result)
            libraryJson = buildLibraryJson(result).toString()
            pushLibrary()
        }
    }

    private fun buildLibraryJson(list: List<Song>): JSONArray {
        val arr = JSONArray()
        list.forEach { s ->
            val art = if (s.albumId > 0) {
                ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    s.albumId
                ).toString()
            } else ""
            arr.put(
                JSONObject()
                    .put("id", s.id.toString())
                    .put("title", s.title)
                    .put("artist", s.artist)
                    .put("album", s.album)
                    .put("durationMs", s.duration)
                    .put("folder", s.folder)
                    .put("artwork", art)
            )
        }
        return arr
    }

    private fun item(song: Song): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
        if (song.albumId > 0) {
            metadata.setArtworkUri(
                ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    song.albumId
                )
            )
        }
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(metadata.build())
            .build()
    }

    private fun connectController() {
        if (controller != null || controllerFuture != null) return
        val future = MediaController.Builder(
            this,
            SessionToken(this, ComponentName(this, MusicService::class.java))
        ).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                controller = future.get().also { it.addListener(playerListener) }
                pushPlayerState()
            } catch (_: Exception) {
                controller = null
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun pushLibrary() {
        if (!webReady) return
        val quoted = JSONObject.quote(libraryJson)
        webView.post {
            webView.evaluateJavascript("window.nativeSetLibrary && window.nativeSetLibrary($quoted)", null)
        }
    }

    private fun currentSongId(player: Player): String {
        val media = player.currentMediaItem ?: return ""
        if (media.mediaId.isNotBlank()) return media.mediaId
        val uri = media.localConfiguration?.uri?.toString().orEmpty()
        return songs.firstOrNull { it.uri.toString() == uri }?.id?.toString().orEmpty()
    }

    private fun pushPlayerState() {
        val p = controller ?: return
        if (!webReady) return
        val duration = p.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
        val repeat = when (p.repeatMode) {
            Player.REPEAT_MODE_ONE -> "one"
            Player.REPEAT_MODE_ALL -> "all"
            else -> "off"
        }
        val json = JSONObject()
            .put("id", currentSongId(p))
            .put("playing", p.isPlaying)
            .put("position", p.currentPosition.coerceAtLeast(0L))
            .put("duration", duration)
            .put("shuffle", p.shuffleModeEnabled)
            .put("repeat", repeat)
            .toString()
        val quoted = JSONObject.quote(json)
        webView.post {
            webView.evaluateJavascript("window.nativePlayerState && window.nativePlayerState($quoted)", null)
        }
    }

    inner class NativeBridge {
        @JavascriptInterface
        fun getLibrary(): String = libraryJson

        @JavascriptInterface
        fun rescan() {
            runOnUiThread { ensureMusicPermission() }
        }

        @JavascriptInterface
        fun play(id: String) {
            runOnUiThread {
                val target = songs.firstOrNull { it.id.toString() == id } ?: return@runOnUiThread
                val p = controller ?: return@runOnUiThread
                val index = songs.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
                p.setMediaItems(songs.map(::item), index, 0L)
                p.prepare()
                p.play()
                pushPlayerState()
            }
        }

        @JavascriptInterface
        fun toggle() {
            runOnUiThread {
                controller?.let { p ->
                    if (p.isPlaying) p.pause()
                    else if (p.mediaItemCount > 0) {
                        p.prepare()
                        p.play()
                    } else if (songs.isNotEmpty()) {
                        p.setMediaItems(songs.map(::item), 0, 0L)
                        p.prepare()
                        p.play()
                    }
                }
                pushPlayerState()
            }
        }

        @JavascriptInterface
        fun next() {
            runOnUiThread {
                controller?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
                pushPlayerState()
            }
        }

        @JavascriptInterface
        fun previous() {
            runOnUiThread {
                controller?.let {
                    if (it.currentPosition > 3000) it.seekTo(0)
                    else if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem()
                }
                pushPlayerState()
            }
        }

        @JavascriptInterface
        fun seekPercent(percent: Double) {
            runOnUiThread {
                controller?.let { p ->
                    val d = p.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: return@let
                    p.seekTo((d * (percent.coerceIn(0.0, 100.0) / 100.0)).toLong())
                }
                pushPlayerState()
            }
        }

        @JavascriptInterface
        fun setShuffle(enabled: Boolean) {
            runOnUiThread {
                controller?.shuffleModeEnabled = enabled
                pushPlayerState()
            }
        }

        @JavascriptInterface
        fun setRepeat(mode: String) {
            runOnUiThread {
                controller?.repeatMode = when (mode) {
                    "one" -> Player.REPEAT_MODE_ONE
                    "all" -> Player.REPEAT_MODE_ALL
                    else -> Player.REPEAT_MODE_OFF
                }
                pushPlayerState()
            }
        }

        @JavascriptInterface
        fun setVolume(value: Double) {
            runOnUiThread {
                controller?.volume = value.toFloat().coerceIn(0f, 1f)
            }
        }

        @JavascriptInterface
        fun setSleep(minutes: Int) {
            startService(Intent(this@MainActivity, MusicService::class.java).apply {
                action = MusicService.SLEEP
                putExtra("seconds", minutes.coerceAtLeast(0) * 60L)
            })
        }

        @JavascriptInterface
        fun addWidget() {
            runOnUiThread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val manager = AppWidgetManager.getInstance(this@MainActivity)
                    if (manager.isRequestPinAppWidgetSupported) {
                        val provider = ComponentName(this@MainActivity, MusicWidgetProvider::class.java)
                        val requested = manager.requestPinAppWidget(provider, null, null)
                        if (!requested) {
                            Toast.makeText(this@MainActivity, "Open Widgets and add A S Music", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Open Widgets and add A S Music", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Open Widgets and add A S Music", Toast.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        fun exitApp() {
            runOnUiThread { moveTaskToBack(true) }
        }
    }

    private fun hideStatusBar() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusBar()
    }

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        controller?.removeListener(playerListener)
        controllerFuture?.let(MediaController::releaseFuture)
        controller = null
        webView.removeJavascriptInterface("Android")
        webView.destroy()
        super.onDestroy()
    }
}
