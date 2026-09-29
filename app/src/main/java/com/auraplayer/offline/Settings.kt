package com.auraplayer.offline

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.musicSettings by preferencesDataStore("music_settings")

data class AppSettings(
    val theme: String = "dark",
    val minSeconds: Int = 0,
    val excluded: String = "",
    val speed: Float = 1f,
    val crossfade: Int = 0,
    val normalization: String = "Off",
    val smartTransition: Boolean = false,
    val reduceMotion: Boolean = false,
    val headphonePause: Boolean = true
)

class SettingsStore(private val context: Context) {
    private val theme = stringPreferencesKey("theme")
    private val min = intPreferencesKey("min_seconds")
    private val excluded = stringPreferencesKey("excluded")
    private val speed = floatPreferencesKey("speed")
    private val crossfade = intPreferencesKey("crossfade")
    private val normalization = stringPreferencesKey("normalization")
    private val smart = booleanPreferencesKey("smart")
    private val reduce = booleanPreferencesKey("reduce")
    private val headphone = booleanPreferencesKey("headphone")
    val settings: Flow<AppSettings> = context.musicSettings.data.map {
        AppSettings(it[theme] ?: "dark", it[min] ?: 0, it[excluded] ?: "", it[speed] ?: 1f,
            it[crossfade] ?: 0, it[normalization] ?: "Off", it[smart] ?: false,
            it[reduce] ?: false, it[headphone] ?: true)
    }
    suspend fun change(key: String, value: Any) {
        context.musicSettings.edit { p ->
            when (key) {
                "theme" -> p[theme] = value as String
                "min" -> p[min] = value as Int
                "excluded" -> p[excluded] = value as String
                "speed" -> p[speed] = value as Float
                "crossfade" -> p[crossfade] = value as Int
                "normalization" -> p[normalization] = value as String
                "smart" -> p[smart] = value as Boolean
                "reduce" -> p[reduce] = value as Boolean
                "headphone" -> p[headphone] = value as Boolean
            }
        }
    }
}
