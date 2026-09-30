package com.auraplayer.offline

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.media3.common.Player
import org.json.JSONArray

class MusicWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it, null) }
    }

    companion object {
        fun updateAll(context: Context, player: Player?) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MusicWidgetProvider::class.java))
            ids.forEach { updateOne(context, manager, it, player) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int, player: Player?) {
            val views = RemoteViews(context.packageName, R.layout.music_widget)
            val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

            var title = player?.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
            var artist = player?.currentMediaItem?.mediaMetadata?.artist?.toString().orEmpty()
            val playing = player?.isPlaying ?: prefs.getBoolean("playing", false)

            if (title.isBlank()) {
                try {
                    val array = JSONArray(prefs.getString("items", "[]"))
                    if (array.length() > 0) {
                        val index = prefs.getInt("index", 0).coerceIn(0, array.length() - 1)
                        val item = array.getJSONObject(index)
                        title = item.optString("title", "A S Music")
                        artist = item.optString("artist", "")
                    }
                } catch (_: Exception) { }
            }

            if (title.isBlank()) title = "A S Music"
            if (artist.isBlank()) artist = "Tap to open your music"

            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_artist, artist)
            views.setTextViewText(R.id.widget_play, if (playing) "Ⅱ" else "▶")

            val openApp = PendingIntent.getActivity(
                context, 200,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)
            views.setOnClickPendingIntent(R.id.widget_prev, servicePending(context, MusicService.WIDGET_PREVIOUS, 201))
            views.setOnClickPendingIntent(R.id.widget_play, servicePending(context, MusicService.WIDGET_TOGGLE, 202))
            views.setOnClickPendingIntent(R.id.widget_next, servicePending(context, MusicService.WIDGET_NEXT, 203))
            manager.updateAppWidget(id, views)
        }

        private fun servicePending(context: Context, action: String, request: Int): PendingIntent =
            PendingIntent.getService(
                context, request,
                Intent(context, MusicService::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }
}
