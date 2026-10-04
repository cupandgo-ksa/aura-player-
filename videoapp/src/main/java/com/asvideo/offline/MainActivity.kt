package com.asvideo.offline

import android.Manifest
import android.app.KeyguardManager
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.ContentUris
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFFEAF7F7)
private val Muted = Color(0xFF9DB7C0)
private val Aqua = Color(0xFF73D8FF)
private val Card = Color(0x99263F4A)
private val Stroke = Color(0x558CB8BC)
private val Dark = Color(0xFF0D2029)

data class VideoEntry(val uri: Uri, val name: String, val folder: String, val duration: Long, val size: Long, val date: Long)

class MainActivity : ComponentActivity() {
    private val Ink get() = if (theme == "light") Color(0xFF07111D) else Color(0xFFEAF7F7)
    private val Muted get() = Ink.copy(alpha = .57f)
    private val Card get() = if (theme == "light") Color.White.copy(alpha = .62f) else Color.White.copy(alpha = .105f)
    private val library = mutableStateListOf<VideoEntry>()
    private var controller by mutableStateOf<MediaController?>(null)
    private var selected by mutableStateOf<VideoEntry?>(null)
    private var playerOpen by mutableStateOf(false)
    private var controlsVisible by mutableStateOf(true)
    private var locked by mutableStateOf(false)
    private var fullscreen by mutableStateOf(false)
    private var pip by mutableStateOf(false)
    private var playing by mutableStateOf(false)
    private var position by mutableLongStateOf(0L)
    private var duration by mutableLongStateOf(0L)
    private var speed by mutableFloatStateOf(1f)
    private var resizeMode by mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT)
    private var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    private var shuffle by mutableStateOf(false)
    private var dialog by mutableStateOf("")
    private var subtitleUri: Uri? = null
    private var favorites by mutableStateOf<Set<String>>(emptySet())
    private var theme by mutableStateOf("dark")
    private var subtitleSize by mutableFloatStateOf(.0533f)
    private var subtitleYellow by mutableStateOf(false)
    private var zoom by mutableFloatStateOf(1f)
    private var backgroundAudio by mutableStateOf(true)
    private var playbackError by mutableStateOf("")
    private var loading by mutableStateOf(false)
    private var sleepJob: Job? = null
    private var vaultUnlocked by mutableStateOf(false)
    private var vaultBusy by mutableStateOf(false)
    private var vaultEntries by mutableStateOf<List<PrivateVault.Entry>>(emptyList())
    private var privatePlaying = false
    private var externalPicker = false
    private val vault by lazy { PrivateVault(this) }
    private var lastResumeSave = 0L
    private val unlockVault = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        externalPicker = false
        if (result.resultCode == RESULT_OK) {
            vaultUnlocked = true
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            vaultEntries = vault.entries()
            dialog = "vault"
        }
    }
    private val importPrivate = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        externalPicker = false
        if (uri != null && vaultUnlocked) {
            val name = queryName(uri) ?: "Private video"
            vaultBusy = true
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) { vault.importVideo(uri, name) }
                    vaultEntries = vault.entries()
                } catch (e: Exception) { playbackError = "Could not save private copy. Check available storage." }
                finally { vaultBusy = false }
            }
        }
    }
    private var gestureText by mutableStateOf("")
    private var sleepMinutes by mutableIntStateOf(0)
    private var seekPreview by mutableFloatStateOf(-1f)
    private val prefs by lazy { getSharedPreferences("as_video", MODE_PRIVATE) }
    private var future: ListenableFuture<MediaController>? = null
    private var pendingVideo: VideoEntry? = null
    private val audio by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }

    private val permission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { scanVideos() }
    private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val name = queryName(uri) ?: "Selected video"
            val entry = VideoEntry(uri, name, "Opened files", 0, 0, System.currentTimeMillis())
            if (library.none { it.uri == uri }) library.add(0, entry)
            val saved = prefs.getStringSet("opened", emptySet()).orEmpty().toMutableSet()
            saved.add(uri.toString() + "|" + name.replace("|", " "))
            prefs.edit().putStringSet("opened", saved).apply()
            playVideo(entry)
        }
    }
    private val pickSubtitle = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            subtitleUri = uri
            reloadWithSubtitle()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(13,32,41)
        window.navigationBarColor = android.graphics.Color.rgb(13,32,41)
        favorites = prefs.getStringSet("favorites", emptySet()).orEmpty().toSet()
        theme = prefs.getString("theme", "dark") ?: "dark"
        backgroundAudio = prefs.getBoolean("backgroundAudio", true)
        subtitleSize = prefs.getFloat("subtitleSize", .0533f)
        subtitleYellow = prefs.getBoolean("subtitleYellow", false)
        vault.clearCache()
        connect()
        requestLibraryPermission()
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Aqua, onSurface = Ink, surface = Dark)) { App() } }
        intent?.let(::handleOpenIntent)
        lifecycleScope.launch {
            while (isActive) {
                controller?.let { p ->
                    position = p.currentPosition.coerceAtLeast(0)
                    duration = p.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
                    playing = p.isPlaying
                    if (p.isPlaying && position > 1000 && !privatePlaying && System.currentTimeMillis() - lastResumeSave > 5000) {
                        saveResume(); lastResumeSave = System.currentTimeMillis()
                    }
                }
                val deadline = prefs.getLong("sleepDeadline", 0)
                sleepMinutes = if (deadline > System.currentTimeMillis()) ((deadline - System.currentTimeMillis() + 59999) / 60000).toInt() else 0
                delay(350)
            }
        }
        if (intent.getBooleanExtra("smokeTest", false) && (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            lifecycleScope.launch {
                delay(3000)
                Log.d("ASVideoTest", "VIDEO_LIBRARY:" + library.size)
                if (library.isNotEmpty()) playVideo(library[0])
                delay(7000)
                Log.d("ASVideoTest", "VIDEO_STATE:controller=" + (controller != null) + ",state=" + controller?.playbackState +
                    ",playWhenReady=" + controller?.playWhenReady + ",index=" + controller?.currentMediaItemIndex +
                    ",buffered=" + controller?.bufferedPosition + ",position=" + controller?.currentPosition)
                Log.d("ASVideoTest", if (controller?.isPlaying == true) "VIDEO_PLAY_PASS" else "VIDEO_PLAY_FAIL")
                Log.d("ASVideoTest", if (playerOpen) "VIDEO_UI_PASS" else "VIDEO_UI_FAIL")
                val p = controller ?: return@launch
                p.pause(); delay(500)
                Log.d("ASVideoTest", if (!p.isPlaying) "VIDEO_PAUSE_PASS" else "VIDEO_PAUSE_FAIL")
                p.seekTo(2000); delay(1000)
                Log.d("ASVideoTest", if (kotlin.math.abs(p.currentPosition - 2000) < 750) "VIDEO_SEEK_PASS" else "VIDEO_SEEK_FAIL")
                p.setPlaybackSpeed(1.5f); delay(300)
                Log.d("ASVideoTest", if (p.playbackParameters.speed == 1.5f) "VIDEO_SPEED_PASS" else "VIDEO_SPEED_FAIL")
                selected?.let { video ->
                    toggleFavorite(video)
                    Log.d("ASVideoTest", if (favorites.contains(video.uri.toString())) "VIDEO_FAVORITE_PASS" else "VIDEO_FAVORITE_FAIL")
                    toggleFavorite(video); saveResume()
                    Log.d("ASVideoTest", if (prefs.getLong("resume_" + video.uri, 0) > 0) "VIDEO_RESUME_PASS" else "VIDEO_RESUME_FAIL")
                    val vaultOk = withContext(Dispatchers.IO) {
                        try {
                            val entry = vault.importVideo(video.uri, video.name)
                            val opened = vault.open(entry)
                            val original = contentResolver.openInputStream(video.uri)!!.use { it.readBytes() }
                            val decoded = java.io.File(opened.path!!).readBytes()
                            vault.clearCache()
                            original.contentEquals(decoded)
                        } catch (e: Exception) { Log.e("ASVideo", "Vault smoke test", e); false }
                    }
                    Log.d("ASVideoTest", if (vaultOk) "VIDEO_VAULT_PASS" else "VIDEO_VAULT_FAIL")
                }
                startSleep(5); startSleep(0)
                Log.d("ASVideoTest", if (prefs.getLong("sleepDeadline", -1) == 0L) "VIDEO_TIMER_PASS" else "VIDEO_TIMER_FAIL")
                theme = "light"; delay(600); theme = "dark"
                toggleFullscreen(); controlsVisible = true; delay(1500)
                Log.d("ASVideoTest", if (fullscreen) "VIDEO_FULLSCREEN_PASS" else "VIDEO_FULLSCREEN_FAIL")
                p.setPlaybackSpeed(1f)
            }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleOpenIntent(intent) }
    private fun handleOpenIntent(intent: Intent) {
        val uri = intent.data ?: return
        if (intent.action != Intent.ACTION_VIEW) return
        val entry = VideoEntry(uri, queryName(uri) ?: "Opened video", "Opened files", 0, 0, System.currentTimeMillis())
        if (library.none { it.uri == uri }) library.add(0, entry)
        playVideo(entry)
    }

    private fun queryName(uri: Uri): String? = try {
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    } catch (_: Exception) { null }

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Log.e("ASVideo", "Playback error " + error.errorCodeName, error)
            playbackError = "This video could not play (" + error.errorCodeName + "). Try another file or check device codec support."
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            selected = library.firstOrNull { it.uri.toString() == mediaItem?.mediaId } ?: selected
            subtitleUri = null
            zoom = 1f
            if (!privatePlaying && selected != null) {
                prefs.edit().putLong("watched_" + selected!!.uri, System.currentTimeMillis()).apply()
            }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d("ASVideoTest", "VIDEO_STATE_CHANGE:" + playbackState)
            loading = playbackState == Player.STATE_BUFFERING
            if (playbackState == Player.STATE_ENDED && !privatePlaying) selected?.let { prefs.edit().putLong("resume_" + it.uri, 0).apply() }
            if (playbackState == Player.STATE_READY) duration = controller?.duration?.coerceAtLeast(0) ?: 0
        }
    }

    private fun connect() {
        val f = MediaController.Builder(this, SessionToken(this, ComponentName(this, VideoService::class.java))).buildAsync()
        future = f
        f.addListener({
            try {
                controller = f.get().also { it.addListener(playerListener) }
                val p = controller
                playing = p?.isPlaying == true
                selected = library.firstOrNull { it.uri.toString() == p?.currentMediaItem?.mediaId } ?: selected
                pendingVideo?.let { video -> pendingVideo = null; playVideo(video) }
            } catch (e: Exception) { Log.e("ASVideo", "Session connection failed", e) }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestLibraryPermission() {
        val required = if (Build.VERSION.SDK_INT >= 34)
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        else if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        val fullPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
        val partialPermission = Build.VERSION.SDK_INT >= 34 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        if (ContextCompat.checkSelfPermission(this, fullPermission) != PackageManager.PERMISSION_GRANTED && !partialPermission)
            permission.launch(required)
        else scanVideos()
    }

    private fun scanVideos() {
        lifecycleScope.launch {
            val found = withContext(Dispatchers.IO) {
                val result = mutableListOf<VideoEntry>()
                val columns = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME, MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DATE_ADDED)
                try {
                    contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, columns, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC")?.use { c ->
                        while (c.moveToNext()) {
                            val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, c.getLong(0))
                            result.add(VideoEntry(uri, c.getString(1) ?: "Video", c.getString(2) ?: "Videos",
                                c.getLong(3), c.getLong(4), c.getLong(5)))
                        }
                    }
                } catch (e: Exception) { Log.w("ASVideo", "Media scan", e) }
                result
            }
            val opened = prefs.getStringSet("opened", emptySet()).orEmpty().mapNotNull {
                val pair = it.split("|", limit = 2)
                if (pair.size == 2) VideoEntry(Uri.parse(pair[0]), pair[1], "Opened files", 0, 0, 0) else null
            }
            val merged = (opened + found).distinctBy { it.uri }
            library.clear(); library.addAll(merged)
            controller?.currentMediaItem?.let { item -> selected = library.firstOrNull { it.uri.toString() == item.mediaId } ?: selected }
        }
    }

    private fun item(video: VideoEntry, subtitle: Uri? = null): MediaItem {
        val builder = MediaItem.Builder().setUri(video.uri).setMediaId(video.uri.toString())
            .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(video.name).build())
        if (subtitle != null) {
            val extension = (queryName(subtitle) ?: subtitle.toString()).substringAfterLast('.', "").lowercase()
            val mime = when (extension) { "vtt" -> MimeTypes.TEXT_VTT; "ass", "ssa" -> MimeTypes.TEXT_SSA; else -> MimeTypes.APPLICATION_SUBRIP }
            builder.setSubtitleConfigurations(listOf(MediaItem.SubtitleConfiguration.Builder(subtitle).setMimeType(mime)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()))
        }
        return builder.build()
    }

    private fun playVideo(video: VideoEntry) {
        saveResume()
        if (privatePlaying) { controller?.stop(); privatePlaying = false; vault.clearCache(); window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
        selected = video; playerOpen = true; controlsVisible = true; locked = false; subtitleUri = null
        playbackError = ""; zoom = 1f
        prefs.edit().putLong("watched_" + video.uri, System.currentTimeMillis()).apply()
        val p = controller ?: run { pendingVideo = video; return }
        val list = (library.toList() + video).distinctBy { it.uri }
        val index = list.indexOfFirst { it.uri == video.uri }.coerceAtLeast(0)
        p.setMediaItems(list.map(::item), index, prefs.getLong("resume_" + video.uri, 0L))
        p.setPlaybackSpeed(speed); p.shuffleModeEnabled = shuffle; p.repeatMode = repeatMode
        p.prepare(); p.play()
    }

    private fun reloadWithSubtitle() {
        val p = controller ?: return
        val video = selected ?: return
        val index = p.currentMediaItemIndex
        if (index < 0) return
        val at = p.currentPosition.coerceAtLeast(0)
        val wasPlaying = p.isPlaying
        p.replaceMediaItem(index, item(video, subtitleUri))
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).build()
        p.seekTo(index, at)
        p.prepare()
        if (wasPlaying) p.play()
    }

    private fun saveResume() {
        if (privatePlaying) return
        val p = controller ?: return
        val uri = p.currentMediaItem?.mediaId ?: return
        val at = if (p.playbackState == Player.STATE_ENDED) 0L else p.currentPosition.coerceAtLeast(0)
        prefs.edit().putLong("resume_" + uri, at).apply()
    }

    private fun openVault() {
        val manager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val intent = manager.createConfirmDeviceCredentialIntent("Private videos", "Unlock with your phone PIN, password or pattern")
        if (intent != null) { externalPicker = true; unlockVault.launch(intent) }
        else playbackError = "Set a screen lock on your phone to use Private videos."
    }

    private fun openPrivate(entry: PrivateVault.Entry) {
        if (!vaultUnlocked || vaultBusy) return
        vaultBusy = true
        saveResume(); controller?.stop()
        lifecycleScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) { vault.open(entry) }
                if (!vaultUnlocked) { vault.clearCache(); return@launch }
                privatePlaying = true; selected = VideoEntry(uri, entry.name, "Private videos", 0, entry.size, 0)
                controller?.setMediaItem(item(selected!!)); controller?.prepare(); controller?.play()
                dialog = ""; playerOpen = true
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } catch (e: Exception) { playbackError = "Could not open this private video. Check available storage." }
            finally { vaultBusy = false }
        }
    }

    private fun closePlayer() {
        saveResume()
        if (privatePlaying) {
            controller?.stop(); privatePlaying = false; vault.clearCache()
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        playerOpen = false
    }

    override fun onStop() {
        saveResume()
        if (externalPicker) { super.onStop(); return }
        vaultUnlocked = false
        if (dialog == "vault") dialog = ""
        if (privatePlaying) closePlayer()
        else if (!pip && !backgroundAudio) controller?.pause()
        super.onStop()
    }

    private fun startSleep(minutes: Int) {
        sleepMinutes = minutes
        prefs.edit().putLong("sleepDeadline", if (minutes > 0) System.currentTimeMillis() + minutes * 60000L else 0).apply()
    }

    private fun toggleFullscreen() {
        fullscreen = !fullscreen
        requestedOrientation = if (fullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val bars = WindowInsetsControllerCompat(window, window.decorView)
        if (fullscreen) bars.hide(WindowInsetsCompat.Type.systemBars())
        else bars.show(WindowInsetsCompat.Type.systemBars())
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(controller?.videoSize?.let { size -> if (size.width > 0 && size.height > 0) Rational(size.width, size.height) else Rational(16, 9) } ?: Rational(16, 9)).build())
            } catch (e: Exception) { Log.w("ASVideo", "PiP", e) }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pip = isInPictureInPictureMode
    }

    override fun onDestroy() {
        saveResume()
        sleepJob?.cancel()
        controller?.removeListener(playerListener)
        future?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }

    @Composable private fun App() {
        BackHandler(playerOpen || dialog.isNotEmpty()) {
            when {
                dialog.isNotEmpty() -> dialog = ""
                locked -> locked = false
                fullscreen -> toggleFullscreen()
                else -> closePlayer()
            }
        }
        LaunchedEffect(controlsVisible, playing, locked, gestureText) {
            if (controlsVisible && playing && !locked && gestureText.isEmpty()) { delay(4000); controlsVisible = false }
        }
        val background = when (theme) {
            "light" -> listOf(Color(0xFFDFF7FF), Color(0xFFF8FBFF), Color(0xFFE9EEFF))
            "amoled" -> listOf(Color.Black, Color.Black)
            else -> listOf(Color(0xFF17364E), Color(0xFF070B12), Color(0xFF17152A))
        }
        CompositionLocalProvider(LocalContentColor provides Ink) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(background))) {
            AnimatedContent(targetState = playerOpen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { open ->
                if (open) PlayerScreen() else LibraryScreen()
            }
            if (dialog.isNotEmpty()) Options()
            if (playbackError.isNotEmpty()) AlertDialog(onDismissRequest = { playbackError = "" },
                title = { Text("Video player") }, text = { Text(playbackError) },
                confirmButton = { TextButton(onClick = { playbackError = "" }) { Text("OK") } })
        }
        }
    }

    @Composable private fun LibraryScreen() {
        var query by remember { mutableStateOf("") }
        var tab by remember { mutableStateOf("All") }
        var sort by remember { mutableStateOf("Recent") }
        val filtered = library.filter {
            it.name.contains(query, true) || it.folder.contains(query, true)
        }.let { list ->
            when (tab) {
                "Favorites" -> list.filter { favorites.contains(it.uri.toString()) }
                "History" -> list.filter { prefs.getLong("watched_" + it.uri, 0) > 0 }.sortedByDescending { prefs.getLong("watched_" + it.uri, 0) }
                else -> list
            }
        }.let { list ->
            when (sort) {
                "Name" -> list.sortedBy { it.name.lowercase() }
                "Duration" -> list.sortedByDescending { it.duration }
                else -> if (tab == "History") list else list.sortedByDescending { it.date }
            }
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(43.dp).clip(RoundedCornerShape(15.dp)).background(Aqua.copy(alpha=.23f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayCircle, null, tint = Aqua, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("A S VIDEO", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("YOUR OFFLINE CINEMA", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
                }
                CircleButton(Icons.Default.Settings, "Settings") { dialog = "settings" }
                Spacer(Modifier.width(6.dp))
                CircleButton(Icons.Default.Add, "Open video") { pickVideo.launch(arrayOf("video/*")) }
            }
            Spacer(Modifier.height(33.dp))
            Text("Your moments,\nbeautifully played.", color = Ink, fontSize = 32.sp, lineHeight = 37.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(20.dp))
            Glass {
                Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = Muted)
                    Spacer(Modifier.width(10.dp))
                    TextField(value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f),
                        singleLine = true, placeholder = { Text("Search videos or folders", fontSize = 14.sp, color = Muted) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = Ink, cursorColor = Aqua))
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Folders", "Favorites", "History").forEach { name ->
                    Pill(name, tab == name) { tab = name }
                }
            }
            Spacer(Modifier.height(19.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(when (tab) { "Favorites" -> "Favorites"; "Folders" -> "Folders"; else -> "Local videos" },
                    color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { sort = when (sort) { "Recent" -> "Name"; "Name" -> "Duration"; else -> "Recent" } }) {
                    Icon(Icons.Default.Sort, null, tint = Aqua, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(sort, color = Aqua, fontSize = 12.sp)
                }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (tab == "Folders") {
                    val groups = filtered.groupBy { it.folder }
                    if (groups.isEmpty()) item { EmptyLibrary() }
                    groups.forEach { (name, videos) ->
                        item(key = name) {
                            var expanded by remember { mutableStateOf(false) }
                            Glass {
                                Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FolderOpen, null, tint = Aqua)
                                    Spacer(Modifier.width(12.dp))
                                    Text(name, color = Ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(videos.size.toString(), color = Muted, fontSize = 12.sp)
                                }
                            }
                            if (expanded) videos.forEach { VideoRow(it, favorites.contains(it.uri.toString())) }
                        }
                    }
                } else {
                    if (filtered.isEmpty()) item { EmptyLibrary() }
                    items(filtered, key = { it.uri.toString() }) { item -> VideoRow(item, favorites.contains(item.uri.toString())) }
                }
            }
        }
    }

    @Composable private fun EmptyLibrary() {
        Column(Modifier.fillMaxWidth().padding(top = 55.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.VideoLibrary, null, tint = Aqua.copy(alpha=.7f), modifier = Modifier.size(46.dp))
            Spacer(Modifier.height(12.dp))
            Text("No videos here yet", color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("Allow access or open a video file", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(13.dp))
            Pill("Open file", true) { pickVideo.launch(arrayOf("video/*")) }
            TextButton(onClick = { requestLibraryPermission() }) { Text("Scan device", color = Aqua) }
        }
    }

    @Composable private fun VideoRow(video: VideoEntry, favorite: Boolean) {
        Glass {
            Row(Modifier.fillMaxWidth().clickable { playVideo(video) }.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                VideoThumb(video, Modifier.width(92.dp).height(62.dp).clip(RoundedCornerShape(13.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(video.name.substringBeforeLast('.'), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(video.folder + "  ·  " + time(video.duration), color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { toggleFavorite(video) }, modifier = Modifier.size(36.dp)) {
                    Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite",
                        tint = if (favorite) Color(0xFFF49C9E) else Muted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    @Composable private fun VideoThumb(video: VideoEntry, modifier: Modifier) {
        val context = LocalContext.current
        var bitmap by remember(video.uri) { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(video.uri) {
            bitmap = withContext(Dispatchers.IO) {
                try { context.contentResolver.loadThumbnail(video.uri, Size(256, 160), null) } catch (_: Exception) { null }
            }
        }
        Box(modifier.background(Brush.linearGradient(listOf(Color(0xFF548F9A), Color(0xFF243E56)))), contentAlignment = Alignment.Center) {
            if (bitmap != null) Image(bitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Default.PlayArrow, null, tint = Ink.copy(alpha=.8f), modifier = Modifier.size(29.dp))
        }
    }

    private fun toggleFavorite(video: VideoEntry) {
        val set = prefs.getStringSet("favorites", emptySet()).orEmpty().toMutableSet()
        if (!set.add(video.uri.toString())) set.remove(video.uri.toString())
        prefs.edit().putStringSet("favorites", set).apply()
        favorites = set.toSet()
    }

    @Composable private fun PlayerScreen() {
        val p = controller
        val video = selected
        Column(Modifier.fillMaxSize().then(if (!fullscreen && !pip) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier)) {
            if (!fullscreen && !pip) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircleButton(Icons.Default.KeyboardArrowDown, "Back") { closePlayer() }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NOW PLAYING", color = Aqua, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("A S VIDEO", color = Muted, fontSize = 12.sp)
                    }
                    CircleButton(Icons.Default.MoreVert, "More options") { dialog = "more" }
                }
                Spacer(Modifier.height(12.dp))
            }
            Box(Modifier.fillMaxWidth().then(if (fullscreen || pip) Modifier.weight(1f) else Modifier.aspectRatio(16f / 9f)).background(Color.Black)) {
                AndroidView(factory = { context ->
                    PlayerView(context).apply { useController = false; player = p; resizeMode = this@MainActivity.resizeMode; keepScreenOn = true }
                }, update = {
                    it.player = p; it.resizeMode = resizeMode; it.keepScreenOn = playing
                    it.subtitleView?.apply {
                        setApplyEmbeddedFontSizes(false)
                        setFractionalTextSize(subtitleSize)
                        setStyle(CaptionStyleCompat(if (subtitleYellow) android.graphics.Color.YELLOW else android.graphics.Color.WHITE,
                            android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE, android.graphics.Color.BLACK, null))
                    }
                }, modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom })
                if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center).size(38.dp), color = Aqua)
                if (!pip) {
                    var dragStartX by remember { mutableFloatStateOf(0f) }
                    var dragX by remember { mutableFloatStateOf(0f) }
                    var dragY by remember { mutableFloatStateOf(0f) }
                    var dragAxis by remember { mutableIntStateOf(0) }
                    var dragPosition by remember { mutableLongStateOf(0L) }
                    var dragVolume by remember { mutableIntStateOf(0) }
                    var dragBrightness by remember { mutableFloatStateOf(.5f) }
                    Box(Modifier.fillMaxSize()
                        .pointerInput(p, locked) {
                            detectTapGestures(onTap = { controlsVisible = !controlsVisible }, onDoubleTap = { point ->
                                if (locked) return@detectTapGestures
                                when {
                                    point.x < size.width * .4f -> p?.seekTo((p.currentPosition - 10000).coerceAtLeast(0))
                                    point.x > size.width * .6f -> p?.seekTo((p.currentPosition + 10000).coerceAtMost(duration.coerceAtLeast(0)))
                                    else -> if (p?.isPlaying == true) p.pause() else p?.play()
                                }
                            })
                        }
                        .pointerInput(p, locked) {
                            detectTransformGestures { _, _, factor, _ ->
                                if (!locked && factor != 1f) zoom = (zoom * factor).coerceIn(1f, 4f)
                            }
                        }
                        .pointerInput(p, locked) {
                            detectDragGestures(onDragStart = {
                                dragStartX = it.x; dragX = 0f; dragY = 0f; dragAxis = 0
                                dragPosition = p?.currentPosition ?: 0
                                dragVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                                dragBrightness = window.attributes.screenBrightness.takeIf { b -> b >= 0 } ?: .5f
                            }, onDragEnd = { gestureText = "" }, onDragCancel = { gestureText = "" }) { change, amount ->
                                if (locked) return@detectDragGestures
                                change.consume(); dragX += amount.x; dragY += amount.y
                                if (dragAxis == 0 && kotlin.math.abs(dragX) + kotlin.math.abs(dragY) > 12) {
                                    dragAxis = if (kotlin.math.abs(dragX) > kotlin.math.abs(dragY)) 1 else 2
                                }
                                if (dragAxis == 1) {
                                    val next = (dragPosition + (dragX / size.width * 120000).toLong()).coerceIn(0, duration.coerceAtLeast(0))
                                    p?.seekTo(next); gestureText = "Seek  " + time(next)
                                } else if (dragAxis == 2 && dragStartX < size.width / 2) {
                                    val attrs = window.attributes
                                    attrs.screenBrightness = (dragBrightness - dragY / size.height).coerceIn(.02f, 1f)
                                    window.attributes = attrs
                                    gestureText = "Brightness  " + (attrs.screenBrightness * 100).toInt() + "%"
                                } else if (dragAxis == 2) {
                                    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                    val level = (dragVolume - dragY / size.height * max).toInt().coerceIn(0, max)
                                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0)
                                    gestureText = "Volume  " + (level * 100 / max) + "%"
                                }
                            }
                        })
                    if (gestureText.isNotEmpty()) Text(gestureText, color = Ink, modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(20.dp))
                        .background(Dark.copy(alpha=.8f)).padding(14.dp), fontSize = 13.sp)
                    Crossfade(targetState = controlsVisible || locked, modifier = Modifier.fillMaxSize(), label = "videoControls") { visible ->
                        if (visible) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.52f), Color.Transparent, Color.Black.copy(alpha=.6f))))) {
                            CircleButton(if (locked) Icons.Default.LockOpen else Icons.Default.Lock, if (locked) "Unlock" else "Lock",
                                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) { locked = !locked; controlsVisible = !locked }
                            if (!locked) {
                                Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                    CircleButton(Icons.Default.Replay10, "Back ten seconds") { p?.seekTo((p.currentPosition - 10000).coerceAtLeast(0)) }
                                    CircleButton(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pause" else "Play", big = true) {
                                        if (p?.isPlaying == true) p.pause() else p?.play()
                                    }
                                    CircleButton(Icons.Default.Forward10, "Forward ten seconds") { p?.seekTo(p.currentPosition + 10000) }
                                }
                                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    if (fullscreen) {
                                        Slider(value = (if (seekPreview >= 0) seekPreview else position.toFloat()).coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
                                            onValueChange = { seekPreview = it },
                                            onValueChangeFinished = { if (seekPreview >= 0) p?.seekTo(seekPreview.toLong()); seekPreview = -1f },
                                            valueRange = 0f..duration.coerceAtLeast(1).toFloat())
                                    }
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        if (fullscreen) {
                                            Text(time(position) + " / " + time(duration), color = Color.White, fontSize = 11.sp)
                                            CircleButton(Icons.Default.SkipPrevious, "Previous") { p?.seekToPreviousMediaItem() }
                                            CircleButton(Icons.Default.SkipNext, "Next") { p?.seekToNextMediaItem() }
                                        }
                                        Spacer(Modifier.weight(1f))
                                        CircleButton(Icons.Default.MoreVert, "Options") { dialog = "more" }
                                        CircleButton(Icons.Default.Subtitles, "Subtitles") { dialog = "subtitles" }
                                        CircleButton(Icons.Default.AspectRatio, "Aspect ratio") { dialog = "aspect" }
                                        CircleButton(if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, "Fullscreen") { toggleFullscreen() }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (!fullscreen && !pip) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
                    Spacer(Modifier.height(22.dp))
                    Text(video?.name?.substringBeforeLast('.') ?: "Select a video", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.SemiBold,
                        lineHeight = 28.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(video?.folder ?: "", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(25.dp))
                    Slider(value = (if (seekPreview >= 0) seekPreview else position.toFloat()).coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
                        onValueChange = { seekPreview = it }, onValueChangeFinished = { p?.seekTo(seekPreview.toLong()); seekPreview = -1f },
                        valueRange = 0f..duration.coerceAtLeast(1).toFloat(), colors = SliderDefaults.colors(thumbColor = Aqua, activeTrackColor = Aqua))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(time(position), color = Muted, fontSize = 11.sp)
                        Text(time(duration), color = Muted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        CircleButton(Icons.Default.Shuffle, "Shuffle", selected = shuffle) { shuffle = !shuffle; p?.shuffleModeEnabled = shuffle }
                        CircleButton(Icons.Default.SkipPrevious, "Previous video") { p?.seekToPreviousMediaItem() }
                        CircleButton(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", big = true) {
                            if (p?.isPlaying == true) p.pause() else p?.play()
                        }
                        CircleButton(Icons.Default.SkipNext, "Next video") { p?.seekToNextMediaItem() }
                        CircleButton(Icons.Default.Repeat, "Repeat", selected = repeatMode != Player.REPEAT_MODE_OFF) {
                            repeatMode = when (repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }
                            p?.repeatMode = repeatMode
                        }
                    }
                    Spacer(Modifier.height(25.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        ActionChip("Speed " + speed + "×", Icons.Default.Speed, Modifier.weight(1f)) { dialog = "speed" }
                        ActionChip("Tracks", Icons.Default.Tune, Modifier.weight(1f)) { dialog = "tracks" }
                        ActionChip("PiP", Icons.Default.PictureInPictureAlt, Modifier.weight(1f)) { enterPip() }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("UP NEXT", color = Aqua, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(10.dp))
                    val currentIndex = p?.currentMediaItemIndex ?: -1
                    val queue = (0 until (p?.mediaItemCount ?: 0)).mapNotNull { index ->
                        val id = p?.getMediaItemAt(index)?.mediaId
                        library.firstOrNull { it.uri.toString() == id }?.let { index to it }
                    }
                    queue.filter { it.first > currentIndex }.take(8).forEach { (queueIndex, next) ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { p?.seekTo(queueIndex, 0); p?.play() }
                            .padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            VideoThumb(next, Modifier.width(65.dp).height(43.dp).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.width(10.dp))
                            Text(next.name.substringBeforeLast('.'), color = Ink, fontSize = 12.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(time(next.duration), color = Muted, fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    @Composable private fun Options() {
        val type = dialog
        androidx.compose.ui.window.Dialog(onDismissRequest = { dialog = "" }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(if (theme == "light") Color(0xFFF0F8FF) else Color(0xFF1B3440))
                .verticalScroll(rememberScrollState()).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(when (type) { "speed" -> "Playback speed"; "aspect" -> "Video fit"; "tracks" -> "Audio tracks"; "subtitles" -> "Subtitles"; "settings" -> "Settings"; "timer" -> "Sleep timer"; "vault" -> "Private videos"; else -> "Video options" },
                        color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { dialog = "" }) { Icon(Icons.Default.Close, "Close", tint = Muted) }
                }
                Spacer(Modifier.height(8.dp))
                when (type) {
                    "speed" -> listOf(.25f,.5f,.75f,1f,1.25f,1.5f,1.75f,2f,3f,4f).forEach { v ->
                        OptionRow(v.toString() + "×", speed == v) { speed = v; controller?.setPlaybackSpeed(v); dialog = "" }
                    }
                    "aspect" -> listOf("Fit" to AspectRatioFrameLayout.RESIZE_MODE_FIT, "Fill" to AspectRatioFrameLayout.RESIZE_MODE_FILL,
                        "Crop" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM).forEach { (label, value) ->
                        OptionRow(label, resizeMode == value) { resizeMode = value; dialog = "" }
                    }
                    "settings" -> {
                        Text("Appearance", color = Muted, fontSize = 12.sp)
                        listOf("dark" to "Water glass · dark", "light" to "Water glass · light", "amoled" to "AMOLED black").forEach { (value, label) ->
                            OptionRow(label, theme == value) { theme = value; prefs.edit().putString("theme", value).apply() }
                        }
                        OptionRow("Background audio", backgroundAudio) {
                            backgroundAudio = !backgroundAudio; prefs.edit().putBoolean("backgroundAudio", backgroundAudio).apply()
                        }
                        OptionRow("Private videos", false) { dialog = ""; openVault() }
                        OptionRow("Scan videos again", false) { dialog = ""; requestLibraryPermission() }
                        Text("Developer: Mohammed Ashik", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 16.dp))
                    }
                    "vault" -> {
                        Text("Encrypted copies protected by your phone screen lock. Import keeps the original video in its current folder.", color = Muted, fontSize = 12.sp)
                        if (vaultBusy) { CircularProgressIndicator(color = Aqua); Text("Please wait…", color = Muted) }
                        else {
                            OptionRow("Import a private copy", false) { externalPicker = true; importPrivate.launch(arrayOf("video/*")) }
                            vaultEntries.forEach { entry -> OptionRow(entry.name, false) { openPrivate(entry) } }
                            OptionRow("Lock private folder", false) { vaultUnlocked = false; dialog = "" }
                        }
                    }
                    "timer" -> {
                        OptionRow("Off", sleepMinutes == 0) { startSleep(0); dialog = "" }
                        listOf(5, 15, 30, 45, 60, 90).forEach { minutes ->
                            OptionRow("$minutes minutes", sleepMinutes == minutes) { startSleep(minutes); dialog = "" }
                        }
                    }
                    "subtitles" -> {
                        Text("Subtitle size", color = Muted, fontSize = 12.sp)
                        Slider(value = subtitleSize, onValueChange = { subtitleSize = it }, valueRange = .025f.. .09f,
                            onValueChangeFinished = { prefs.edit().putFloat("subtitleSize", subtitleSize).apply() })
                        OptionRow("Yellow subtitles", subtitleYellow) {
                            subtitleYellow = !subtitleYellow; prefs.edit().putBoolean("subtitleYellow", subtitleYellow).apply()
                        }
                        OptionRow("Embedded subtitle tracks", false) { dialog = "tracks" }
                        OptionRow("Choose .srt / .vtt / .ass file", false) { dialog = ""; pickSubtitle.launch(arrayOf("*/*")) }
                        OptionRow("Subtitles off", false) {
                            controller?.let { p -> p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build() }
                            dialog = ""
                        }
                        OptionRow("Subtitles on", true) {
                            controller?.let { p -> p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).build() }
                            dialog = ""
                        }
                    }
                    "tracks" -> {
                        val groups = controller?.currentTracks?.groups.orEmpty().filter { it.type == C.TRACK_TYPE_AUDIO || it.type == C.TRACK_TYPE_TEXT }
                        if (groups.isEmpty()) Text("No alternate tracks in this video", color = Muted, fontSize = 13.sp)
                        groups.forEach { group ->
                            for (i in 0 until group.length) {
                                if (!group.isTrackSupported(i)) continue
                                val format = group.getTrackFormat(i)
                                OptionRow((if (group.type == C.TRACK_TYPE_AUDIO) "Audio" else "Subtitle") + " · " +
                                    (format.label ?: format.language ?: "Track " + (i + 1)), group.isTrackSelected(i)) {
                                    controller?.let { p ->
                                        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                            .setTrackTypeDisabled(group.type, false)
                                            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(i))).build()
                                    }
                                    dialog = ""
                                }
                            }
                        }
                    }
                    else -> {
                        OptionRow("Open another video", false) { dialog = ""; pickVideo.launch(arrayOf("video/*")) }
                        OptionRow("Choose subtitles", false) { dialog = "subtitles" }
                        OptionRow("Playback speed", false) { dialog = "speed" }
                        OptionRow("Video fit", false) { dialog = "aspect" }
                        OptionRow("Audio and subtitle tracks", false) { dialog = "tracks" }
                        OptionRow("Reset zoom", zoom != 1f) { zoom = 1f; dialog = "" }
                        OptionRow("Sleep timer", sleepMinutes > 0) { dialog = "timer" }
                        OptionRow("Settings", false) { dialog = "settings" }
                    }
                }
            }
        }
    }

    @Composable private fun OptionRow(label: String, active: Boolean, click: () -> Unit) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = click).padding(vertical = 12.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = if (active) Aqua else Ink, modifier = Modifier.weight(1f), fontSize = 13.sp)
            if (active) Icon(Icons.Default.Check, null, tint = Aqua, modifier = Modifier.size(18.dp))
        }
    }

    @Composable private fun Glass(content: @Composable () -> Unit) {
        val shape = RoundedCornerShape(24.dp)
        val light = theme == "light"
        Box(Modifier.shadow(16.dp, shape).clip(shape).background(Brush.verticalGradient(
            if (light) listOf(Color.White.copy(.78f), Color.White.copy(.50f), Color.White.copy(.38f))
            else listOf(Color.White.copy(.18f), Color.White.copy(.105f), Color.White.copy(.065f))))
            .border(1.dp, Color.White.copy(if (light) .92f else .26f), shape)) { content() }
    }

    @Composable private fun Pill(text: String, active: Boolean, click: () -> Unit) {
        Surface(onClick = click, shape = RoundedCornerShape(15.dp), color = if (active) Aqua.copy(alpha=.21f) else Card,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (active) Aqua.copy(alpha=.65f) else Stroke.copy(alpha=.35f))) {
            Text(text, color = if (active) Aqua else Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }

    @Composable private fun CircleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String,
        modifier: Modifier = Modifier, big: Boolean = false, selected: Boolean = false, click: () -> Unit) {
        Surface(onClick = click, modifier = modifier.size(if (big) 66.dp else 42.dp),
            shape = CircleShape,
            color = if (big || selected) Aqua.copy(alpha=.28f) else Card,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (big || selected) Aqua.copy(alpha=.7f) else Stroke.copy(alpha=.45f)),
            shadowElevation = if (big) 11.dp else 2.dp) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = if (big || selected) Aqua else Ink, modifier = Modifier.size(if (big) 31.dp else 22.dp))
            }
        }
    }

    @Composable private fun ActionChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, click: () -> Unit) {
        Surface(onClick = click, modifier = modifier, shape = RoundedCornerShape(15.dp), color = Card,
            border = androidx.compose.foundation.BorderStroke(1.dp, Stroke.copy(alpha=.5f))) {
            Row(Modifier.padding(vertical = 11.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Aqua, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, color = Ink, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

private fun time(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, (seconds / 60) % 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)
}
