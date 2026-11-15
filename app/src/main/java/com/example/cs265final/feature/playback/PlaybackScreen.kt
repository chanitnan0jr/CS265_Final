package com.example.cs265final.feature.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
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
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import java.util.Locale

internal val DefaultMediaAttrs: AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .build()

@Composable
fun PlaybackScreen(uri: Uri, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val lifecycle = (context as ComponentActivity).lifecycle
    val player = remember(uri) { MediaPlayer() }
    var ready by remember(uri) { mutableStateOf(false) }
    var playing by remember(uri) { mutableStateOf(false) }
    var position by remember(uri) { mutableIntStateOf(0) }
    var duration by remember(uri) { mutableIntStateOf(0) }
    var error by remember(uri) { mutableStateOf("") }

    DisposableEffect(uri, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && ready && playing) {
                player.pause()
                playing = false
            }
        }
        lifecycle.addObserver(observer)
        try {
            player.setOnPreparedListener {
                duration = it.duration.coerceAtLeast(0)
                ready = true
            }
            player.setOnCompletionListener {
                playing = false
                position = duration
            }
            player.setOnErrorListener { _, _, _ ->
                ready = false
                playing = false
                error = "ไม่สามารถเล่นไฟล์เสียงนี้ได้"
                true
            }
            player.setAudioSource(context, uri, DefaultMediaAttrs)
            player.prepareAsync()
        } catch (_: Exception) {
            error = "ไม่สามารถเปิดไฟล์เสียงนี้ได้"
        }
        onDispose {
            lifecycle.removeObserver(observer)
            player.release()
        }
    }

    LaunchedEffect(playing, player) {
        while (playing) {
            position = player.currentPosition
            delay(200.milliseconds)
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text("‹ กลับ") }
        Text("ฟังไฟล์เสียง", style = MaterialTheme.typography.headlineSmall)
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        else if (!ready) Text("กำลังเปิดไฟล์เสียง...")
        Slider(
            value = position.toFloat().coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
            onValueChange = { value -> position = value.toInt(); player.seekTo(position) },
            valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
            enabled = ready && duration > 0,
        )
        Text("${formatTime(position)} / ${formatTime(duration)}")
        Button(enabled = ready, onClick = {
            if (playing) {
                player.pause()
                playing = false
            } else {
                ensureAudibleMediaOutput(context, DefaultMediaAttrs)
                if (duration > 0) {
                    if (position >= duration) {
                        player.seekTo(0)
                        position = 0
                    }
                }
                player.start()
                playing = true
            }
        }) { Text(if (playing) "หยุดชั่วคราว" else "เล่น") }
        Text("การแสดงคำตามเสียงต้องใช้ข้อความที่มีเวลาแต่ละคำ")
    }
}

internal fun formatTime(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}

internal fun ensureAudibleMediaOutput(context: Context, attrs: AudioAttributes) {
    val am = context.getSystemService(AudioManager::class.java) ?: return
    if (am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
        val target = (am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2).coerceAtLeast(1)
        runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0) }
    }
    if (Build.VERSION.SDK_INT >= 26) {
        am.requestAudioFocus(AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs).build())
    } else {
        @Suppress("DEPRECATION")
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }
}

internal fun MediaPlayer.setAudioSource(context: Context, uri: Uri, attrs: AudioAttributes) {
    setAudioAttributes(attrs)
    setVolume(1f, 1f)
    val loaded = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { setDataSource(it.fileDescriptor); true } ?: false
    }.getOrDefault(false)
    if (!loaded) setDataSource(context, uri)
}
