package com.example.cs265final.feature.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cs265final.data.Note
import com.example.cs265final.data.AudioSource
import com.example.cs265final.data.filterNotes
import com.example.cs265final.ui.theme.*
import java.text.SimpleDateFormat

private val SourceFilters = listOf("ทั้งหมด", "เสียงที่บันทึก", "ไฟล์ที่อัปโหลด")

@Composable
fun LibraryScreen(
    notes: List<Note>,
    dateFormat: SimpleDateFormat,
    onSelect: (Note) -> Unit,
    onSearchClick: () -> Unit = {},
    onDelete: (Note) -> Unit = {},
) {
    var activeFilter by remember { mutableStateOf("ทั้งหมด") }
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    val folderNames = remember(notes) {
        DefaultFolders + notes.map { it.folder }.filter { it.isNotBlank() && it !in DefaultFolders }.distinct()
    }
    val countsByFolder = remember(notes) { notes.groupingBy { it.folder }.eachCount() }
    val filteredNotes = remember(notes, activeFilter, selectedFolder) {
        val source = when (activeFilter) {
            "ไฟล์ที่อัปโหลด" -> AudioSource.UPLOADED
            "เสียงที่บันทึก" -> AudioSource.RECORDED
            else -> null
        }
        notes.filterNotes(folder = selectedFolder, audioSource = source)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FolderNavIcon(selected = false, color = TextPrimary, modifier = Modifier.size(22.dp))
                Text("คลังโน้ต", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircleIconButton(32.dp, "ค้นหา", onClick = onSearchClick) { SearchIcon(TextPrimary, Modifier.size(20.dp)) }
                MoreVertIcon(TextPrimary, Modifier.size(20.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SourceFilters.forEach { label ->
                val selected = activeFilter == label
                Surface(
                    onClick = {
                        activeFilter = label
                        if (label == "ทั้งหมด") selectedFolder = null
                    },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) SecondaryRose else SurfaceTonal,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = if (selected) SurfaceWhite else TextSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("โฟลเดอร์", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            TextButton(onClick = { selectedFolder = null }) {
                Text(if (selectedFolder != null) "แสดงทั้งหมด" else "จัดการ >", style = MaterialTheme.typography.bodyMedium, color = SecondaryRose)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            folderNames.chunked(2).forEach { rowFolders ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowFolders.forEach { folderName ->
                        val selected = selectedFolder == folderName
                        Surface(
                            onClick = { selectedFolder = if (selected) null else folderName },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            color = folderTint(folderName),
                            border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) PrimaryCoral else BorderSoft),
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                FolderGlyph(folderName)
                                Text(folderName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${countsByFolder[folderName] ?: 0} รายการ", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                    if (rowFolders.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(selectedFolder?.let { "โน้ตในโฟลเดอร์: $it" } ?: "โน้ตล่าสุด", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            TextButton(onClick = { selectedFolder = null; activeFilter = "ทั้งหมด" }) {
                Text("ดูทั้งหมด >", style = MaterialTheme.typography.bodyMedium, color = SecondaryRose)
            }
        }

        if (filteredNotes.isEmpty()) {
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), SurfaceWhite, border = BorderStroke(1.dp, BorderSoft)) {
                Text("ยังไม่มีโน้ตในหมวดนี้", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(20.dp))
            }
        } else {
            filteredNotes.forEach { NoteCard(it, dateFormat, showMoodBadge = false, onDelete = onDelete, onClick = onSelect) }
        }
    }
}

@Composable
private fun FolderGlyph(name: String) = when (name) {
    "การเรียน" -> GraduationCapIcon(TextPrimary, Modifier.size(22.dp))
    "ระบายความรู้สึก" -> HeartOutlineIcon(SecondaryRose, Modifier.size(22.dp))
    "งาน / โปรเจกต์" -> BriefcaseOutlineIcon(PrimaryCoral, Modifier.size(22.dp))
    else -> BookmarkOutlineIcon(TextPrimary, Modifier.size(22.dp))
}
