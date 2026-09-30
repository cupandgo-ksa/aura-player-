package com.auraplayer.offline

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.media3.common.Player

class MusicWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { manager.updateAppWidget(it, buildViews(context)) }
    }

    companion object {
        fun updateAll(context: Context, player: Player) {
            val item = player.currentMediaItem
            val title = item?.mediaMetadata?.title?.toString().orEmpty().ifBlank { "A S Music" }
            val artist = item?.mediaMetadata?.artist?.toString().orEmpty().ifBlank { "Personal Music Player" }
            context.getSharedPreferences("widget_state", Context.MODE_PRIVATE).edit()
                .putString("title", title)
                .putString("artist", artist)
                .putBoolean("playing", player.isPlaying)
                .apply()

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MusicWidgetProvider::class.java))
            ids.forEach { manager.updateAppWidget(it, buildViews(context)) }
        }

        private fun buildViews(context: Context): RemoteViews {
            val prefs = context.getSharedPreferences("widget_state", Context.MODE_PRIVATE)
            val playing = prefs.getBoolean("playing", false)
            return RemoteViews(context.packageName, R.layout.music_widget).apply {
                setTextViewText(R.id.widget_title, prefs.getString("title", "A S Music") ?: "A S Music")
                setTextViewText(R.id.widget_artist, prefs.getString("artist", "Personal Music Player") ?: "Personal Music Player")
                setImageViewResource(R.id.widget_play, if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)

                val openIntent = Intent(context, MainActivity::class.java)
                val openPending = PendingIntent.getActivity(
                    context, 40, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_root, openPending)
                setOnClickPendingIntent(R.id.widget_icon, openPending)

                fun control(request: Int, actionName: String): PendingIntent {
                    return PendingIntent.getService(
                        context,
                        request,
                        Intent(context, MusicService::class.java).apply { action = actionName },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                setOnClickPendingIntent(R.id.widget_previous, control(41, MusicService.WIDGET_PREVIOUS))
                setOnClickPendingIntent(R.id.widget_play, control(42, MusicService.WIDGET_TOGGLE))
                setOnClickPendingIntent(R.id.widget_next, control(43, MusicService.WIDGET_NEXT))
            }
        }
    }
}
