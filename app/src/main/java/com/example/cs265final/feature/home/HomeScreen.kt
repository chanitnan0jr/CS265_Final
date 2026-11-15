package com.example.cs265final.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cs265final.data.Note
import com.example.cs265final.feature.notes.NoteCard
import com.example.cs265final.feature.recording.WaveformBars
import com.example.cs265final.ui.theme.*
import java.text.SimpleDateFormat

private val SideWaveHeights = listOf(0.20f, 0.34f, 0.62f, 0.42f, 0.84f, 0.54f, 0.28f)

@Composable
fun HomeScreen(
    notes: List<Note>,
    dateFormat: SimpleDateFormat,
    onNew: () -> Unit,
    onSelect: (Note) -> Unit,
    onUpload: () -> Unit = onNew,
    onViewAll: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppWaveformLogo(modifier = Modifier.size(26.dp))
                Text(
                    text = "Voice2Note",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                )
            }
            BellIcon(color = TextPrimary, modifier = Modifier.size(22.dp))
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            SoftMountainBackdrop(modifier = Modifier.matchParentSize())
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("วันนี้อยากบันทึกอะไร?", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp), color = TextPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "พูดได้เลย ไม่ว่าจะเป็นความคิด\nความรู้สึก หรือโน้ตการเรียน",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(26.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WaveformBars(Modifier.width(58.dp).height(34.dp), SideWaveHeights, active = false, inactiveColor = PrimaryCoral.copy(alpha = 0.5f))
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(PrimaryCoral.copy(alpha = 0.14f))
                            .clickable { onNew() }
                            .semantics { contentDescription = "เริ่มบันทึกเสียง" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(94.dp).clip(CircleShape).background(PrimaryCoral.copy(alpha = 0.24f)), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier.size(78.dp).clip(CircleShape).background(Brush.linearGradient(listOf(PrimaryCoralLight, PrimaryCoral))),
                                contentAlignment = Alignment.Center,
                            ) { MicOutlineIcon(color = SurfaceWhite, modifier = Modifier.size(34.dp)) }
                        }
                    }
                    WaveformBars(Modifier.width(58.dp).height(34.dp), SideWaveHeights.reversed(), active = false, inactiveColor = PrimaryCoral.copy(alpha = 0.5f))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickActionCard("อัปโหลดไฟล์เสียง\n(MP3)", 12, onUpload) { CloudUploadIcon(TextPrimary, Modifier.size(22.dp)) }
            QuickActionCard("สร้างโน้ตใหม่", 13, onNew) { NewNoteEditIcon(TextPrimary, Modifier.size(20.dp)) }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("บันทึกล่าสุด", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                TextButton(onClick = onViewAll) { Text("ดูทั้งหมด >", style = MaterialTheme.typography.bodyMedium, color = SecondaryRose) }
            }
            if (notes.isEmpty()) {
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), SurfaceWhite, border = BorderStroke(1.dp, BorderSoft)) {
                    Text(
                        text = "ยังไม่มีโน้ต เริ่มเล่าเรื่องของคุณได้เลย",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp, horizontal = 16.dp),
                    )
                }
            } else {
                notes.take(3).forEach { NoteCard(it, dateFormat, showMoodBadge = false, onClick = onSelect) }
            }
        }
    }
}

@Composable
private fun RowScope.QuickActionCard(
    label: String,
    fontSize: Int,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderSoft),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            icon()
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = fontSize.sp), color = TextPrimary, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun SoftMountainBackdrop(modifier: Modifier = Modifier) = Canvas(modifier = modifier) {
    drawHill(0.44f, 0.25f, 0.22f, 0.56f, 0.54f, 0.30f, Color(0xFFF3E4E0))
    drawHill(0.64f, 0.34f, 0.42f, 0.70f, 0.72f, 0.54f, Color(0xFFF8EEEC))
    drawHill(0.84f, 0.36f, 0.94f, 0.68f, 0.78f, 0.86f, BackgroundCream)
}

private fun DrawScope.drawHill(y0: Float, cx1: Float, cy1: Float, cx2: Float, cy2: Float, y1: Float, color: Color) {
    val (w, h) = size.width to size.height
    drawPath(
        Path().apply {
            moveTo(0f, h * y0)
            cubicTo(w * cx1, h * cy1, w * cx2, h * cy2, w, h * y1)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        },
        color = color,
    )
}
