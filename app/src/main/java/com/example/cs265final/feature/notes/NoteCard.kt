package com.example.cs265final.feature.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cs265final.data.Note
import com.example.cs265final.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date

private val PlayBadgeTint = Color(0xFFF6E6E3)

@Composable
fun NoteCard(
    note: Note,
    format: SimpleDateFormat,
    showMoodBadge: Boolean = false,
    onDelete: ((Note) -> Unit)? = null,
    onClick: (Note) -> Unit,
) {
    val moodVisual = moodStyle(note.mood)
    Card(
        onClick = { onClick(note) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(PlayBadgeTint),
                contentAlignment = Alignment.Center,
            ) { PlayTriangleIcon(color = PrimaryCoral, modifier = Modifier.size(13.dp)) }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(note.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (note.body.isNotBlank() && !showMoodBadge) {
                    Text(note.body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    text = "${format.format(Date(note.time))} • ${note.folder}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (showMoodBadge) {
                Surface(shape = RoundedCornerShape(999.dp), color = moodVisual.container) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MoodFaceIcon(mood = note.mood, color = moodVisual.content, modifier = Modifier.size(15.dp))
                        Text(note.mood, style = MaterialTheme.typography.labelSmall, color = moodVisual.content, maxLines = 1)
                    }
                }
            } else if (onDelete != null) {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    CircleIconButton(32.dp, "ตัวเลือก ${note.title}", onClick = { menuOpen = true }) {
                        MoreVertIcon(color = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("ลบโน้ต", color = RecordingDotRed) },
                            leadingIcon = { TrashOutlineIcon(RecordingDotRed, Modifier.size(16.dp)) },
                            onClick = { menuOpen = false; onDelete(note) },
                        )
                    }
                }
            } else {
                MoreVertIcon(color = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}
