package com.asvideo.offline

import android.media.audiofx.Equalizer
import androidx.media3.common.Player
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
    private var equalizer: Equalizer? = null
    private var appliedSound = ""
    private fun attachEqualizer(sessionId: Int) {
        equalizer?.release(); equalizer = null; appliedSound = ""
        if (sessionId > 0) try { equalizer = Equalizer(0, sessionId) } catch (_: Exception) {}
        getSharedPreferences("as_video", MODE_PRIVATE).edit().putBoolean("equalizerAvailable", equalizer != null).apply()
    }
    private fun updateSound(mode: String) {
        if (appliedSound == mode) return
        val eq = equalizer ?: return
        try {
            val range = eq.bandLevelRange
            for (band in 0 until eq.numberOfBands.toInt()) {
                val frequency = eq.getCenterFreq(band.toShort()) / 1000
                val gain = when (mode) {
                    "Bass" -> if (frequency < 250) 500 else 0
                    "Voice" -> if (frequency in 500..4000) 350 else -150
                    "Movie" -> if (frequency < 250 || frequency > 4000) 300 else 100
                    else -> 0
                }
                eq.setBandLevel(band.toShort(), gain.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
            }
            eq.enabled = mode != "Off"; appliedSound = mode
        } catch (_: Exception) {
            getSharedPreferences("as_video", MODE_PRIVATE).edit().putBoolean("equalizerAvailable", false).apply()
        }
    }
    private val sleepCheck = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences("as_video", MODE_PRIVATE)
            updateSound(prefs.getString("soundMode", "Off") ?: "Off")
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
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) { attachEqualizer(audioSessionId) }
        })
        session = MediaSession.Builder(this, player).build()
        handler.post(sleepCheck)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        handler.removeCallbacks(sleepCheck)
        equalizer?.release()
        session?.release()
        player.release()
        session = null
        super.onDestroy()
    }
}
