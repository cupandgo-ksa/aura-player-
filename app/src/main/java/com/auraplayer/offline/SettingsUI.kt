package com.auraplayer.offline

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsPage(model: PlayerModel, light: Boolean) {
    val fg = if (light) UiInk else Color.White
    val context = LocalContext.current
    val widgetManager = remember(context) { AppWidgetManager.getInstance(context) }
    var sleepMenu by remember { mutableStateOf(false) }
    var eqMenu by remember { mutableStateOf(false) }
    var eqName by remember { mutableStateOf("Normal") }

    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingGroup("Appearance", light) {
                Text("Theme", color = fg, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("light" to "Light", "dark" to "Dark", "amoled" to "AMOLED").forEach { (key, label) ->
                        FilterChip(selected = model.settings.theme == key, onClick = { model.change("theme", key) }, label = { Text(label) })
                    }
                }
            }
        }
        item {
            SettingGroup("Playback", light) {
                Box {
                    SettingRow("Equalizer", eqName, Icons.Rounded.Equalizer, fg, true) { eqMenu = true }
                    DropdownMenu(expanded = eqMenu, onDismissRequest = { eqMenu = false }) {
                        listOf("Normal", "Bass", "Vocal", "Rock", "Pop", "Classical").forEach { preset ->
                            DropdownMenuItem(text = { Text(preset) }, onClick = {
                                eqName = preset; eqMenu = false
                                context.startService(Intent(context, MusicService::class.java).apply {
                                    action = MusicService.EQ; putExtra("preset", preset)
                                })
                            })
                        }
                    }
                }
                HorizontalDivider(color = fg.copy(.10f))
                Box {
                    SettingRow("Sleep Timer", "10–60 min / end of song", Icons.Rounded.Bedtime, fg, true) { sleepMenu = true }
                    DropdownMenu(expanded = sleepMenu, onDismissRequest = { sleepMenu = false }) {
                        listOf(10, 20, 30, 45, 60).forEach { min ->
                            DropdownMenuItem(text = { Text("$min minutes") }, onClick = {
                                context.startService(Intent(context, MusicService::class.java).apply {
                                    action = MusicService.SLEEP; putExtra("seconds", min * 60L)
                                }); sleepMenu = false
                            })
                        }
                        DropdownMenuItem(text = { Text("End of current song") }, onClick = {
                            context.startService(Intent(context, MusicService::class.java).apply {
                                action = MusicService.SLEEP; putExtra("end", true)
                            }); sleepMenu = false
                        })
                    }
                }
                HorizontalDivider(color = fg.copy(.10f))
                Text("Playback Speed", color = fg, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                        FilterChip(selected = model.settings.speed == speed, onClick = { model.changeSpeed(speed) }, label = { Text("${speed}x") })
                    }
                }
            }
        }
        item {
            SettingGroup("Library", light) {
                SettingRow("Scan New Music", "Refresh songs from this phone", Icons.Rounded.Refresh, fg, true) { model.scan() }
                HorizontalDivider(color = fg.copy(.10f))
                Text("Hide audio shorter than", color = fg, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 10, 30, 60).forEach { sec ->
                        FilterChip(selected = model.settings.minSeconds == sec, onClick = { model.change("min", sec) }, label = { Text(if (sec == 0) "All" else "${sec}s") })
                    }
                }
            }
        }
        item {
            SettingGroup("Home Screen", light) {
                SettingRow("Add Music Widget", "Previous • Play/Pause • Next", Icons.Rounded.Home, fg, true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && widgetManager.isRequestPinAppWidgetSupported) {
                        val provider = ComponentName(context, MusicWidgetProvider::class.java)
                        val requested = widgetManager.requestPinAppWidget(provider, null, null)
                        if (!requested) Toast.makeText(context, "Open Widgets and add A S Music", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Open Widgets and add A S Music", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        item {
            SettingGroup("About", light) {
                SettingRow("A S Music", "Personal music player", Icons.Rounded.MusicNote, fg)
                HorizontalDivider(color = fg.copy(.10f))
                SettingRow("Developer", "Mohammed Ashik", Icons.Rounded.Person, fg)
                HorizontalDivider(color = fg.copy(.10f))
                SettingRow("Private & local", "No ads • no login • no tracking", Icons.Rounded.PrivacyTip, fg)
                HorizontalDivider(color = fg.copy(.10f))
                SettingRow("Version", "10.0", Icons.Rounded.Info, fg)
            }
        }
    }
}

@Composable
private fun SettingGroup(title: String, light: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val fg = if (light) UiInk else Color.White
    Column {
        Text(title, color = fg.copy(.52f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
        GlassBox(Modifier.fillMaxWidth(), light, 24) { Column(Modifier.fillMaxWidth().padding(16.dp), content = content) }
    }
}

@Composable
private fun SettingRow(title: String, sub: String, icon: ImageVector, fg: Color, clickable: Boolean = false, click: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().then(if (clickable) Modifier.clickable(onClick = click) else Modifier).padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = WaterBlue, modifier = Modifier.size(23.dp)); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(title, color = fg, fontWeight = FontWeight.SemiBold); Text(sub, color = fg.copy(.48f), fontSize = 12.sp) }
        if (clickable) Icon(Icons.Rounded.ChevronRight, null, tint = fg.copy(.36f))
    }
}
