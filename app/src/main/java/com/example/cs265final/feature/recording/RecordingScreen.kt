package com.example.cs265final.feature.recording

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cs265final.ui.theme.*
import java.util.Locale
import kotlinx.coroutines.delay

private val DefaultWaveformHeights = listOf(
    0.16f, 0.24f, 0.18f, 0.30f, 0.42f, 0.68f, 0.38f, 0.24f, 0.48f, 0.34f,
    0.22f, 0.50f, 0.36f, 0.60f, 0.92f, 0.72f, 0.54f, 0.80f, 0.42f, 0.26f,
    0.44f, 0.30f, 0.56f, 0.38f, 0.24f, 0.30f, 0.20f, 0.24f, 0.16f,
)

@Composable
fun WaveformBars(
    modifier: Modifier = Modifier,
    barHeights: List<Float> = DefaultWaveformHeights,
    active: Boolean = false,
    activeColor: Color = PrimaryCoral,
    inactiveColor: Color = BorderSoft,
) {
    Canvas(modifier = modifier) {
        if (barHeights.isEmpty()) return@Canvas
        val barWidth = size.width / (barHeights.size * 2f - 1f).coerceAtLeast(1f)
        val color = if (active) activeColor else inactiveColor
        barHeights.forEachIndexed { index, rawHeight ->
            val height = size.height * rawHeight.coerceIn(0.14f, 1f)
            drawRoundRect(color, Offset(index * barWidth * 2f, (size.height - height) / 2f), Size(barWidth, height), CornerRadius(barWidth / 2f, barWidth / 2f))
        }
    }
}

@Composable
fun RecordingScreen(
    title: String,
    onTitleChange: (String) -> Unit,
    transcript: String,
    onTranscriptChange: (String) -> Unit,
    listening: Boolean,
    mood: String,
    onMoodChange: (String) -> Unit,
    folder: String,
    onFolderChange: (String) -> Unit,
    message: String = "",
    onToggleMic: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    var showMetaEditor by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(listening) {
        while (listening) {
            delay(1_000L)
            elapsedSeconds += 1
        }
    }

    val clock = String.format(Locale.US, "%02d:%02d:%02d", elapsedSeconds / 3600, (elapsedSeconds % 3600) / 60, elapsedSeconds % 60)
    val statusText = "${if (listening) "กำลังบันทึก" else "พร้อมบันทึก"} • $clock"
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = SurfaceWhite,
        unfocusedContainerColor = SurfaceWhite,
        focusedBorderColor = PrimaryCoral,
        unfocusedBorderColor = BorderSoft,
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircleIconButton(36.dp, "กลับ", onClick = onCancel) { BackArrowIcon(TextPrimary, Modifier.size(20.dp)) }
                Text("บันทึกเสียง (ถอดข้อความสด)", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp), color = TextPrimary)
            }
            CircleIconButton(36.dp, "แก้ไขชื่อโน้ตและโฟลเดอร์", onClick = { showMetaEditor = !showMetaEditor }) {
                MoreVertIcon(TextPrimary, Modifier.size(20.dp))
            }
        }

        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), SurfaceTonal, border = BorderStroke(1.dp, BorderSoft)) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (listening) RecordingDotRed else SecondaryRose))
                    Text(statusText, style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp), color = TextPrimary)
                }
                WaveformBars(Modifier.fillMaxWidth().height(34.dp), active = listening, inactiveColor = SecondaryRose.copy(alpha = 0.48f))
            }
        }

        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), SurfaceWhite, border = BorderStroke(1.dp, BorderSoft), shadowElevation = 1.dp) {
            OutlinedTextField(
                value = transcript,
                onValueChange = onTranscriptChange,
                placeholder = { Text("พูดได้เลย เราจะช่วยจดให้...", color = TextSecondary) },
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = PrimaryCoral,
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                modifier = Modifier.fillMaxWidth().heightIn(min = 210.dp).padding(4.dp),
            )
        }

        if (message.isNotBlank()) {
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp), SurfaceTonal) {
                Text(message, style = MaterialTheme.typography.bodyMedium, color = SecondaryRose, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
            }
        }

        if (showMetaEditor) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("ชื่อโน้ต") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DefaultFolders.forEach { option ->
                        val selected = folder == option
                        FilterChip(
                            selected = selected,
                            onClick = { onFolderChange(if (selected) "" else option) },
                            label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SurfaceTonal, selectedLabelColor = PrimaryCoral),
                        )
                    }
                }
                OutlinedTextField(
                    value = folder,
                    onValueChange = onFolderChange,
                    label = { Text("โฟลเดอร์") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("อารมณ์ตอนนี้", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DefaultMoods.forEach { option ->
                    val selected = mood == option
                    val style = moodStyle(option)
                    Surface(
                        onClick = { onMoodChange(option) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) Color(0xFFF6E2DF) else Color(0xFFF3EEEB),
                        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) PrimaryCoral else BorderSoft),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            MoodFaceIcon(option, if (selected) PrimaryCoral else style.content, Modifier.size(24.dp))
                            Text(
                                text = option,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = if (selected) PrimaryCoral else TextSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CircleIconButton(48.dp, "ตั้งชื่อโน้ตและโฟลเดอร์", if (showMetaEditor) SurfaceTonal else SurfaceWhite, bordered = true, onClick = { showMetaEditor = !showMetaEditor }) {
                BookmarkOutlineIcon(TextPrimary, Modifier.size(20.dp))
            }
            CircleIconButton(72.dp, if (listening) "หยุดรับเสียง" else "เริ่มพูดภาษาไทย", onClick = onToggleMic) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(PrimaryCoralLight, PrimaryCoral))),
                    contentAlignment = Alignment.Center,
                ) {
                    if (listening) PauseBarsIcon(SurfaceWhite, Modifier.size(28.dp)) else MicOutlineIcon(SurfaceWhite, Modifier.size(30.dp))
                }
            }
            Surface(onClick = onSave, shape = RoundedCornerShape(16.dp), color = Color.Transparent) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(Modifier.size(48.dp), shape = CircleShape, color = SurfaceWhite, border = BorderStroke(1.dp, BorderSoft)) {
                        Box(contentAlignment = Alignment.Center) { StopSquareIcon(TextPrimary, Modifier.size(16.dp)) }
                    }
                    Text("บันทึกโน้ต", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}
