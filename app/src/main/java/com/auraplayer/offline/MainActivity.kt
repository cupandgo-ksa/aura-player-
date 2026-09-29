package com.auraplayer.offline

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val model: PlayerModel = viewModel()
            val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
            var allowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) }
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                allowed = it
                if (it) model.scan()
            }
            LaunchedEffect(allowed) {
                if (allowed) model.scan() else permissionLauncher.launch(permission)
            }
            val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == RESULT_OK) model.scan()
            }
            var lyricSongId by remember { mutableLongStateOf(0L) }
            val lyricLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null && lyricSongId != 0L) {
                    contentResolver.openInputStream(uri)?.use { input ->
                        openFileOutput("lyrics_${lyricSongId}.lrc", MODE_PRIVATE).use { output -> input.copyTo(output) }
                    }
                }
            }
            MusicApp(model, allowed, onRequestPermission = { permissionLauncher.launch(permission) },
                onBackHome = { moveTaskToBack(true) },
                onDelete = { song ->
                    if (Build.VERSION.SDK_INT >= 30) {
                        try {
                            val pending = MediaStore.createDeleteRequest(contentResolver, listOf(song.uri))
                            deleteLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                        } catch (_: Exception) { }
                    }
                },
                onShare = { song ->
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = song.mime.ifBlank { "audio/*" }
                        putExtra(Intent.EXTRA_STREAM, song.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }, "Share audio"))
                },
                onPickLyrics = { song ->
                    lyricSongId = song.id
                    lyricLauncher.launch(arrayOf("text/*", "application/octet-stream"))
                })
        }
    }
}
