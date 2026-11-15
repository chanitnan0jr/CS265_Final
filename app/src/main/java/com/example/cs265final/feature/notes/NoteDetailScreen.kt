package com.example.cs265final.feature.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cs265final.data.Note
import com.example.cs265final.feature.playback.DefaultMediaAttrs
import com.example.cs265final.feature.playback.ensureAudibleMediaOutput
import com.example.cs265final.feature.playback.formatTime
import com.example.cs265final.feature.playback.setAudioSource
import com.example.cs265final.feature.recording.WaveformBars
import com.example.cs265final.ui.theme.*
import com.example.cs265final.writeWavFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.delay

@Composable
fun NoteDetailScreen(
    note: Note,
    dateFormat: SimpleDateFormat,
    onRename: (String) -> Boolean = { false },
    onListenOriginal: () -> Unit = {},
    onDelete: () -> Unit = {},
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var renaming by remember(note) { mutableStateOf(false) }
    var renameTitle by remember(note) { mutableStateOf(note.title) }
    var renameError by remember(note) { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val playableUri = remember(note) {
        if (note.audioUri.isBlank()) null
        else {
            val parsed = Uri.parse(note.audioUri)
            if (parsed.scheme == "file" && parsed.path?.endsWith(".pcm", ignoreCase = true) == true) {
                val pcmFile = File(requireNotNull(parsed.path))
                val wavFile = File(pcmFile.path.dropLast(4) + ".wav")
                when {
                    wavFile.exists() && wavFile.length() > 44L -> Uri.fromFile(wavFile)
                    pcmFile.exists() && pcmFile.length() > 0L -> runCatching { Uri.fromFile(writeWavFile(wavFile, pcmFile.readBytes())) }.getOrNull()
                    else -> null
                }
            } else {
                parsed
            }
        }
    }
    val mediaPlayer = remember(playableUri) { if (playableUri != null) MediaPlayer() else null }
    var mediaReady by remember(playableUri) { mutableStateOf(false) }
    var playing by remember(playableUri) { mutableStateOf(false) }
    var elapsedSeconds by remember(playableUri) { mutableIntStateOf(0) }
    var durationSeconds by remember(playableUri) { mutableIntStateOf(0) }

    val startRename = { renameTitle = note.title; renameError = ""; renaming = true }

    DisposableEffect(playableUri, mediaPlayer) {
        if (mediaPlayer != null && playableUri != null) {
            runCatching {
                mediaPlayer.setOnPreparedListener {
                    durationSeconds = (it.duration / 1000).coerceAtLeast(1)
                    mediaReady = true
                }
                mediaPlayer.setOnCompletionListener {
                    playing = false
                    elapsedSeconds = 0
                    it.seekTo(0)
                }
                mediaPlayer.setOnErrorListener { _, _, _ ->
                    mediaReady = false
                    playing = false
                    true
                }
                mediaPlayer.setAudioSource(context, playableUri, DefaultMediaAttrs)
                mediaPlayer.prepareAsync()
            }.onFailure { mediaReady = false }
        }

        onDispose { mediaPlayer?.release() }
    }

    LaunchedEffect(playing, mediaReady) {
        if (!playing || mediaPlayer == null || !mediaReady) return@LaunchedEffect
        while (playing) {
            elapsedSeconds = (mediaPlayer.currentPosition / 1000).coerceAtLeast(0)
            delay(250L)
        }
    }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1_500L)
            copied = false
        }
    }

    fun togglePlayback() {
        if (playing) {
            mediaPlayer?.pause()
            playing = false
        } else if (mediaReady && mediaPlayer != null) {
            ensureAudibleMediaOutput(context, DefaultMediaAttrs)
            mediaPlayer.start()
            playing = true
        } else {
            onListenOriginal()
        }
    }

    val shownSeconds = if (playing || elapsedSeconds > 0) elapsedSeconds else durationSeconds
    val timerText = formatTime(shownSeconds * 1000)

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(40.dp, "กลับ", SurfaceTonal, onClick = onBack) { BackArrowIcon(TextPrimary, Modifier.size(18.dp)) }
            Box {
                CircleIconButton(40.dp, "ตัวเลือกโน้ต", SurfaceTonal, onClick = { menuExpanded = true }) { MoreVertIcon(TextPrimary, Modifier.size(18.dp)) }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("แก้ไขชื่อโน้ต") },
                        leadingIcon = { PencilOutlineIcon(TextPrimary, Modifier.size(16.dp)) },
                        onClick = { menuExpanded = false; startRename() },
                    )
                    DropdownMenuItem(
                        text = { Text("ลบโน้ต", color = RecordingDotRed) },
                        leadingIcon = { TrashOutlineIcon(RecordingDotRed, Modifier.size(16.dp)) },
                        onClick = { menuExpanded = false; onDelete() },
                    )
                }
            }
        }

        if (renaming) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = renameTitle,
                    onValueChange = { renameTitle = it; renameError = "" },
                    label = { Text("ชื่อโน้ต") },
                    singleLine = true,
                    isError = renameError.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (renameError.isNotEmpty()) Text(renameError, color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val trimmed = renameTitle.trim()
                        when {
                            trimmed.isEmpty() -> renameError = "กรุณาใส่ชื่อโน้ต"
                            onRename(trimmed) -> renaming = false
                            else -> renameError = "ไม่สามารถบันทึกชื่อโน้ตได้ กรุณาลองอีกครั้ง"
                        }
                    }) { Text("บันทึกชื่อ") }
                    TextButton(onClick = { renaming = false; renameError = "" }) { Text("ยกเลิก") }
                }
            }
        } else {
            Text(note.title, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold), color = TextPrimary)
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CalendarNavIcon(selected = false, color = SecondaryRose, modifier = Modifier.size(15.dp))
                Text(dateFormat.format(Date(note.time)), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
            VerticalDivider(Modifier.height(16.dp), color = BorderSoft)
            PillTag(note.mood) { MoodFaceIcon(note.mood, PrimaryCoral, Modifier.size(15.dp)) }
            VerticalDivider(Modifier.height(16.dp), color = BorderSoft)
            PillTag(note.folder) { FolderNavIcon(selected = false, color = SecondaryRose, modifier = Modifier.size(15.dp)) }
        }

        Surface(
            onClick = ::togglePlayback,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(999.dp),
            color = Color(0xFFF5E6E3),
            border = BorderStroke(1.dp, BorderSoft),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(PrimaryCoral), contentAlignment = Alignment.Center) {
                    if (playing) PauseBarsIcon(SurfaceWhite, Modifier.size(14.dp)) else PlayTriangleIcon(SurfaceWhite, Modifier.size(13.dp))
                }
                WaveformBars(Modifier.weight(1f).height(24.dp), active = playing || mediaReady, activeColor = PrimaryCoral)
                VerticalDivider(Modifier.height(20.dp), color = BorderSoft)
                Text(timerText, style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp), color = TextPrimary, modifier = Modifier.padding(end = 6.dp))
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = SurfaceWhite,
            border = BorderStroke(1.dp, BorderSoft),
            shadowElevation = 1.dp,
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(note.body, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 27.sp), color = TextPrimary)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillTag(note.mood) { TagOutlineIcon(SecondaryRose, Modifier.size(14.dp)) }
                    PillTag(note.folder) { FolderNavIcon(selected = false, color = SecondaryRose, modifier = Modifier.size(14.dp)) }
                }
            }
        }

        Surface(
            onClick = ::togglePlayback,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(999.dp),
            color = PrimaryCoral,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                HeadphonesIcon(SurfaceWhite, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (playing) "หยุดเล่นเสียง" else "ฟังเสียงต้นฉบับ", style = MaterialTheme.typography.titleMedium, color = SurfaceWhite)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionPill(
                modifier = Modifier.weight(1.45f),
                label = if (copied) "คัดลอกแล้ว" else "คัดลอกข้อความ",
                onClick = {
                    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(note.title, note.body))
                    copied = true
                },
            ) { CopyOutlineIcon(TextPrimary, Modifier.size(15.dp)) }

            ActionPill(Modifier.weight(0.85f), "แก้ไข", onClick = startRename) { PencilOutlineIcon(TextPrimary, Modifier.size(15.dp)) }

            ActionPill(
                modifier = Modifier.weight(0.8f),
                label = "แชร์",
                onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, note.title)
                        putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.body}")
                    }
                    context.startActivity(Intent.createChooser(share, null))
                },
            ) { ShareOutlineIcon(TextPrimary, Modifier.size(15.dp)) }
        }
    }
}

@Composable
private fun PillTag(label: String, icon: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(999.dp), color = SurfaceTonal) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            icon()
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1)
        }
    }
}

@Composable
private fun ActionPill(
    modifier: Modifier,
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(999.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderSoft),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            icon()
            Spacer(Modifier.width(5.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
