package com.example.cs265final.feature.playback

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun PlaybackScreen(uri: Uri, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val lifecycle = (context as ComponentActivity).lifecycle
    var player by remember(uri) { mutableStateOf<MediaPlayer?>(null) }
    var ready by remember(uri) { mutableStateOf(false) }
    var playing by remember(uri) { mutableStateOf(false) }
    var position by remember(uri) { mutableIntStateOf(0) }
    var duration by remember(uri) { mutableIntStateOf(0) }
    var error by remember(uri) { mutableStateOf("") }

    DisposableEffect(uri) {
        val media = MediaPlayer()
        player = media
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && ready && playing) {
                media.pause()
                playing = false
            }
        }
        lifecycle.addObserver(observer)
        try {
            media.setOnPreparedListener {
                duration = it.duration.coerceAtLeast(0)
                ready = true
            }
            media.setOnCompletionListener {
                playing = false
                position = duration
            }
            media.setOnErrorListener { _, _, _ ->
                ready = false
                playing = false
                error = "ไม่สามารถเล่นไฟล์เสียงนี้ได้"
                true
            }
            media.setDataSource(context, uri)
            media.prepareAsync()
        } catch (_: Exception) {
            error = "ไม่สามารถเปิดไฟล์เสียงนี้ได้"
        }
        onDispose {
            lifecycle.removeObserver(observer)
            media.release()
            player = null
        }
    }

    LaunchedEffect(playing, player) {
        while (playing) {
            position = player?.currentPosition ?: 0
            delay(200)
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text("‹ กลับ") }
        Text("ฟังไฟล์เสียง", style = MaterialTheme.typography.headlineSmall)
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        else if (!ready) Text("กำลังเปิดไฟล์เสียง...")
        Slider(
            value = position.toFloat().coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
            onValueChange = { value -> position = value.toInt(); player?.seekTo(position) },
            valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
            enabled = ready && duration > 0,
        )
        Text("${formatTime(position)} / ${formatTime(duration)}")
        Button(enabled = ready, onClick = {
            player?.let {
                if (playing) { it.pause(); playing = false }
                else { if (position >= duration) { it.seekTo(0); position = 0 }; it.start(); playing = true }
            }
        }) { Text(if (playing) "หยุดชั่วคราว" else "เล่น") }
        Text("การแสดงคำตามเสียงต้องใช้ข้อความที่มีเวลาแต่ละคำ")
    }
}

private fun formatTime(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
