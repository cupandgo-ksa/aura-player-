package com.asvideo.offline

import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class VideoService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private val sleepCheck = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences("as_video", MODE_PRIVATE)
            val deadline = prefs.getLong("sleepDeadline", 0)
            if (deadline > 0 && System.currentTimeMillis() >= deadline) {
                player.pause(); prefs.edit().putLong("sleepDeadline", 0).apply()
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this, DefaultRenderersFactory(this).setEnableDecoderFallback(true)).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            setHandleAudioBecomingNoisy(true)
        }
        session = MediaSession.Builder(this, player).build()
        handler.post(sleepCheck)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        handler.removeCallbacks(sleepCheck)
        session?.release()
        player.release()
        session = null
        super.onDestroy()
    }
}
