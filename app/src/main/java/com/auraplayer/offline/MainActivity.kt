package com.auraplayer.offline

import android.Manifest
import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.media.AudioDeviceInfo
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
import android.util.Log
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
    @Volatile private var pendingPlayId: String? = null
    private var smokeTest = false
    private var smokeScheduled = false
    private var beatRequested = false
    private var visualizer: Visualizer? = null
    private var beatLevel = 0.0

    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var notificationLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var beatPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            pushPlayerState()
            if (isPlaying) maybeStartBeatVisualizer() else pushBeatLevel(0.0)
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Log.d("ASMusicTest", "TRANSITION:" + (mediaItem?.mediaMetadata?.title ?: ""))
            pushPlayerState()
        }
        override fun onPlaybackStateChanged(playbackState: Int) = pushPlayerState()
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = pushPlayerState()
        override fun onRepeatModeChanged(repeatMode: Int) = pushPlayerState()
    }

    private val poll = object : Runnable {
        override fun run() {
            pushPlayerState()
            if (beatRequested && visualizer == null) maybeStartBeatVisualizer()
            handler.postDelayed(this, 400)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        smokeTest = intent.getBooleanExtra("smokeTest", false)
        hideStatusBar()

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
            if (allowed) scanLibrary()
            else Toast.makeText(this, "Music access is needed to show your songs", Toast.LENGTH_LONG).show()
        }
        notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
        beatPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
            if (allowed) maybeStartBeatVisualizer()
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
            songs.clear()
            songs.addAll(result)
            libraryJson = buildLibraryJson(result).toString()
            primeQueueIfEmpty()
            pushLibrary()
            if (smokeTest && !smokeScheduled && result.size >= 2) {
                smokeScheduled = true
                scheduleSmokeTest()
            }
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
                    .put("size", s.size)
                    .put("folder", s.folder)
                    .put("dateAdded", s.dateAdded)
                    .put("mime", s.mime)
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
                primeQueueIfEmpty()
                val pending = pendingPlayId
                pendingPlayId = null
                if (pending != null) playSongById(pending) else pushPlayerState()
            } catch (_: Exception) {
                controller = null
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun primeQueueIfEmpty() {
        val p = controller ?: return
        if (p.mediaItemCount == 0 && songs.isNotEmpty()) {
            p.setMediaItems(songs.map(::item))
            p.prepare()
        }
    }

    private fun scheduleSmokeTest() {
        Log.d("ASMusicTest", "SMOKE_LIBRARY:" + songs.size)
        handler.postDelayed({ webView.evaluateJavascript("window.__asSmokeTap && window.__asSmokeTap(0)", null) }, 1400)
        handler.postDelayed({
            val ok = controller?.currentMediaItemIndex == 0
            Log.d("ASMusicTest", if (ok) "SMOKE_FIRST_TAP_PASS" else "SMOKE_FIRST_TAP_FAIL")
        }, 2300)
        handler.postDelayed({ webView.evaluateJavascript("window.__asSmokeTap && window.__asSmokeTap(1)", null) }, 2600)
        handler.postDelayed({
            val ok = controller?.currentMediaItemIndex == 1
            Log.d("ASMusicTest", if (ok) "SMOKE_TAP_SWITCH_PASS" else "SMOKE_TAP_SWITCH_FAIL")
        }, 3500)
        handler.postDelayed({ webView.evaluateJavascript("window.__asSmokeTap && window.__asSmokeTap(0)", null) }, 3900)
        handler.postDelayed({ webView.evaluateJavascript("document.getElementById('queueButton').click()", null) }, 4500)
        handler.postDelayed({
            webView.evaluateJavascript("document.querySelectorAll('#queueItems .queue-row').length") { rows ->
                Log.d("ASMusicTest", if (rows.toIntOrNull()?.let { it >= 3 } == true) "SMOKE_QUEUE_PASS" else "SMOKE_QUEUE_FAIL:$rows")
            }
            webView.evaluateJavascript("document.getElementById('sheetClose').click()", null)
        }, 5300)
        handler.postDelayed({
            webView.evaluateJavascript("document.getElementById('playerFavorite').click()", null)
        }, 5500)
        handler.postDelayed({
            webView.evaluateJavascript("document.getElementById('playerFavorite').getAttribute('aria-pressed')") { pressed ->
                Log.d("ASMusicTest", if (pressed?.contains("true") == true) "SMOKE_FAVORITE_PASS" else "SMOKE_FAVORITE_FAIL:$pressed")
            }
            webView.evaluateJavascript("getComputedStyle(document.getElementById('toast')).backgroundColor") { color ->
                Log.d("ASMusicTest", if (color?.contains("0, 0, 0, 0") == true || color?.contains("transparent") == true) "SMOKE_NO_BOX_PASS" else "SMOKE_NO_BOX_FAIL:$color")
            }
        }, 5900)
        handler.postDelayed({
            webView.evaluateJavascript("document.getElementById('miniOpen').click()", null)
        }, 6100)
        handler.postDelayed({
            webView.evaluateJavascript("Number(document.getElementById('player').style.getPropertyValue('--beat'))") { beat ->
                Log.d("ASMusicTest", "SMOKE_BEAT_LEVEL:$beat")
            }
        }, 7300)
        handler.postDelayed({
            val p = controller
            val ok = p != null && p.currentMediaItemIndex == 1 && p.isPlaying
            Log.d("ASMusicTest", if (ok) "SMOKE_AUTO_NEXT_PASS" else "SMOKE_AUTO_NEXT_FAIL")
        }, 8200)
        handler.postDelayed({ webView.evaluateJavascript("document.getElementById('playerNext').click()", null) }, 8400)
        handler.postDelayed({
            Log.d("ASMusicTest", if (controller?.currentMediaItemIndex == 2) "SMOKE_NEXT_BUTTON_PASS" else "SMOKE_NEXT_BUTTON_FAIL")
        }, 9000)
        handler.postDelayed({ webView.evaluateJavascript("document.getElementById('previous').click()", null) }, 9100)
        handler.postDelayed({
            Log.d("ASMusicTest", if (controller?.currentMediaItemIndex == 1) "SMOKE_PREVIOUS_BUTTON_PASS" else "SMOKE_PREVIOUS_BUTTON_FAIL")
        }, 9700)
        handler.postDelayed({
            webView.evaluateJavascript("document.getElementById('playerShuffle').click(); document.getElementById('playerRepeat').click()", null)
        }, 9800)
        handler.postDelayed({
            val p = controller
            Log.d("ASMusicTest", if (p?.shuffleModeEnabled == true) "SMOKE_SHUFFLE_BUTTON_PASS" else "SMOKE_SHUFFLE_BUTTON_FAIL")
            Log.d("ASMusicTest", if (p?.repeatMode == Player.REPEAT_MODE_ONE) "SMOKE_REPEAT_BUTTON_PASS" else "SMOKE_REPEAT_BUTTON_FAIL")
        }, 10300)
        handler.postDelayed({ moveTaskToBack(true) }, 10500)
        handler.postDelayed({
            val ok = controller?.isPlaying == true
            Log.d("ASMusicTest", if (ok) "SMOKE_BACKGROUND_PASS" else "SMOKE_BACKGROUND_FAIL")
        }, 11400)
    }

    private fun pushBeatLevel(value: Double) {
        if (!webReady || !beatRequested) return
        val safe = value.coerceIn(0.0, 1.0)
        webView.evaluateJavascript("window.nativeBeatLevel && window.nativeBeatLevel($safe)", null)
    }

    private fun releaseBeatVisualizer() {
        visualizer?.let { effect ->
            try { effect.enabled = false; effect.release() } catch (_: Exception) {}
        }
        visualizer = null
        beatLevel = 0.0
        if (webReady) webView.evaluateJavascript("window.nativeBeatLevel && window.nativeBeatLevel(0)", null)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun maybeStartBeatVisualizer() {
        if (!beatRequested || visualizer != null || !webReady ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        val sessionId = controller?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) return
        try {
            val effect = Visualizer(sessionId)
            effect.captureSize = 256
            effect.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                    if (!beatRequested || waveform == null || waveform.isEmpty()) return
                    var sum = 0.0
                    for (sample in waveform) {
                        val v = (sample.toInt() and 0xff) - 128
                        sum += v.toDouble() * v
                    }
                    val rms = kotlin.math.sqrt(sum / waveform.size) / 55.0
                    val target = (rms * 1.4).coerceIn(0.0, 1.0)
                    beatLevel = if (target > beatLevel) beatLevel * .28 + target * .72 else beatLevel * .68 + target * .32
                    webView.post { if (beatRequested) pushBeatLevel(beatLevel) }
                }
                override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {}
            }, Visualizer.getMaxCaptureRate().coerceAtMost(20000), true, false)
            effect.enabled = true
            visualizer = effect
        } catch (_: Exception) {
            releaseBeatVisualizer()
        }
    }

    private fun pushQueueState() {
        val p = controller
        if (p != null && p.mediaItemCount == 0) primeQueueIfEmpty()
        val queue = JSONArray()
        val active = controller
        if (active != null) {
            for (i in 0 until active.mediaItemCount) {
                val m = active.getMediaItemAt(i)
                queue.put(JSONObject()
                    .put("index", i)
                    .put("id", m.mediaId)
                    .put("title", m.mediaMetadata.title?.toString().orEmpty())
                    .put("artist", m.mediaMetadata.artist?.toString().orEmpty())
                    .put("current", i == active.currentMediaItemIndex))
            }
        }
        webView.evaluateJavascript(
            "window.nativeQueueState && window.nativeQueueState(${JSONObject.quote(queue.toString())})", null
        )
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
            .put("speed", p.playbackParameters.speed)
            .put("queueCount", p.mediaItemCount)
            .toString()
        val quoted = JSONObject.quote(json)
        webView.post {
            webView.evaluateJavascript("window.nativePlayerState && window.nativePlayerState($quoted)", null)
        }
    }

    private fun playSongById(id: String) {
        val target = songs.firstOrNull { it.id.toString() == id } ?: return
        Log.d("ASMusicTest", "REQUEST_PLAY:" + target.title)
        val p = controller
        if (p == null) {
            pendingPlayId = id
            connectController()
            return
        }
        val index = songs.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
        val queueMatchesLibrary = p.mediaItemCount == songs.size &&
            songs.indices.all { i -> p.getMediaItemAt(i).mediaId == songs[i].id.toString() }
        if (!queueMatchesLibrary) {
            p.setMediaItems(songs.map(::item), index, 0L)
            p.prepare()
        } else {
            p.seekTo(index, 0L)
        }
        p.play()
        pushPlayerState()
    }

    private fun navigate(direction: Int) {
        val p = controller ?: return
        // A restored or cleared queue can contain only one item even when the
        // library has many songs. Rebuild it before handling skip controls.
        if (p.mediaItemCount < 2 && songs.size > 1) {
            val currentId = currentSongId(p)
            val index = songs.indexOfFirst { it.id.toString() == currentId }.coerceAtLeast(0)
            val position = p.currentPosition.coerceAtLeast(0L)
            p.setMediaItems(songs.map(::item), index, position)
            p.prepare()
        }
        val count = p.mediaItemCount
        if (count == 0) return
        if (count > 1) {
            if (p.shuffleModeEnabled && direction > 0 && p.hasNextMediaItem()) {
                p.seekToNextMediaItem()
            } else if (p.shuffleModeEnabled && direction < 0 && p.hasPreviousMediaItem()) {
                p.seekToPreviousMediaItem()
            } else {
                val index = p.currentMediaItemIndex.coerceIn(0, count - 1)
                p.seekToDefaultPosition((index + direction + count) % count)
            }
        } else p.seekTo(0)
        p.play()
        pushPlayerState()
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
            runOnUiThread { playSongById(id) }
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
        fun next() = runOnUiThread { navigate(1) }

        @JavascriptInterface
        fun previous() = runOnUiThread { navigate(-1) }

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
        fun setSpeed(speed: Double) {
            runOnUiThread {
                controller?.setPlaybackSpeed(speed.toFloat().coerceIn(0.5f, 2.0f))
                pushPlayerState()
            }
        }

        @JavascriptInterface
        fun setEqualizer(preset: String) {
            startService(Intent(this@MainActivity, MusicService::class.java).apply {
                action = MusicService.EQ
                putExtra("preset", preset)
            })
        }

        @JavascriptInterface
        fun playNext(id: String) {
            runOnUiThread {
                val song = songs.firstOrNull { it.id.toString() == id } ?: return@runOnUiThread
                controller?.let { p ->
                    val at = (p.currentMediaItemIndex + 1).coerceAtMost(p.mediaItemCount)
                    p.addMediaItem(at, item(song))
                }
                pushPlayerState()
            }
        }

        @JavascriptInterface
        fun addToQueue(id: String) {
            runOnUiThread {
                val song = songs.firstOrNull { it.id.toString() == id } ?: return@runOnUiThread
                controller?.addMediaItem(item(song))
                pushPlayerState()
            }
        }

        @JavascriptInterface
        fun removeQueue(index: Int) {
            runOnUiThread {
                controller?.let { p ->
                    if (index in 0 until p.mediaItemCount && index != p.currentMediaItemIndex) p.removeMediaItem(index)
                }
                pushPlayerState()
            }
        }

        @JavascriptInterface
        fun moveQueue(from: Int, to: Int) {
            runOnUiThread {
                controller?.let { p ->
                    if (from in 0 until p.mediaItemCount && to in 0 until p.mediaItemCount && from != to) {
                        p.moveMediaItem(from, to)
                    }
                }
                pushPlayerState()
            }
        }

        @JavascriptInterface
        fun clearQueue() {
            runOnUiThread {
                controller?.let { p ->
                    if (p.mediaItemCount == 0) return@let
                    val currentIndex = p.currentMediaItemIndex.coerceAtLeast(0)
                    val currentItem = p.currentMediaItem
                    val pos = p.currentPosition
                    val wasPlaying = p.isPlaying
                    p.clearMediaItems()
                    if (currentItem != null) {
                        p.setMediaItem(currentItem, pos)
                        p.prepare()
                        if (wasPlaying) p.play()
                    }
                }
                pushPlayerState()
            }
        }

        @JavascriptInterface
        fun requestQueue() = runOnUiThread { pushQueueState() }

        @JavascriptInterface
        fun startBeatVisualizer() = runOnUiThread {
            beatRequested = true
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                maybeStartBeatVisualizer()
            } else {
                beatPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        @JavascriptInterface
        fun stopBeatVisualizer() = runOnUiThread {
            beatRequested = false
            releaseBeatVisualizer()
        }

        @JavascriptInterface
        fun stopPlayback() {
            startService(Intent(this@MainActivity, MusicService::class.java).apply {
                action = MusicService.STOP
            })
        }

        @JavascriptInterface
        fun sleepEndOfSong() {
            startService(Intent(this@MainActivity, MusicService::class.java).apply {
                action = MusicService.SLEEP
                putExtra("end", true)
            })
        }

        @JavascriptInterface
        fun getAudioOutput(): String {
            val audio = getSystemService(AUDIO_SERVICE) as AudioManager
            val devices = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val active = devices.firstOrNull { d ->
                when (d.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_DEVICE -> true
                    else -> false
                }
            }
            return when (active?.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth"
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headphones"
                AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio"
                else -> "Phone Speaker"
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

    override fun onPause() {
        releaseBeatVisualizer()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (beatRequested) maybeStartBeatVisualizer()
    }

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        releaseBeatVisualizer()
        controller?.removeListener(playerListener)
        controllerFuture?.let(MediaController::releaseFuture)
        controller = null
        webView.removeJavascriptInterface("Android")
        webView.destroy()
        super.onDestroy()
    }
}
