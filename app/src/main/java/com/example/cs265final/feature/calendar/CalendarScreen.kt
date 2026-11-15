package com.example.cs265final.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cs265final.data.Note
import com.example.cs265final.feature.notes.NoteCard
import com.example.cs265final.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ThaiWeekdays = listOf("อา", "จ", "อ", "พ", "พฤ", "ศ", "ส")

private fun monthStartMillis(template: Calendar, source: Date): Long = (template.clone() as Calendar).run {
    time = source
    set(get(Calendar.YEAR), get(Calendar.MONTH), 1, 0, 0, 0)
    set(Calendar.MILLISECOND, 0)
    timeInMillis
}

@Composable
fun CalendarScreen(
    notes: List<Note>,
    dateFormat: SimpleDateFormat,
    date: String,
    onDateChange: (String) -> Unit,
    onSelect: (Note) -> Unit,
    onNew: () -> Unit = {},
    onDelete: (Note) -> Unit = {},
) {
    val parsedSelected = remember(date, dateFormat) { runCatching { dateFormat.parse(date) }.getOrNull() }
    var monthStart by remember { mutableLongStateOf(monthStartMillis(dateFormat.calendar, parsedSelected ?: Date())) }
    val monthFormat = remember(dateFormat) {
        SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("th-TH")).apply { calendar = dateFormat.calendar.clone() as Calendar }
    }

    LaunchedEffect(parsedSelected) {
        if (parsedSelected != null) monthStart = monthStartMillis(dateFormat.calendar, parsedSelected)
    }

    fun shiftMonth(delta: Int) {
        monthStart = (dateFormat.calendar.clone() as Calendar).run { timeInMillis = monthStart; add(Calendar.MONTH, delta); timeInMillis }
    }

    val noteDates = remember(notes, dateFormat) { notes.map { dateFormat.format(Date(it.time)) }.toSet() }
    val weeks = remember(monthStart, dateFormat) {
        val cal = (dateFormat.calendar.clone() as Calendar).apply { timeInMillis = monthStart }
        val leading = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY).coerceIn(0, 6)
        (List<Int?>(leading) { null } + (1..cal.getActualMaximum(Calendar.DAY_OF_MONTH))).chunked(7)
    }
    val foundNotes = remember(notes, date, dateFormat) { notes.filter { dateFormat.format(Date(it.time)) == date } }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CalendarNavIcon(selected = false, color = TextPrimary, modifier = Modifier.size(22.dp))
                Text("บันทึกรายวัน", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(onClick = { onDateChange(dateFormat.format(Date())) }, shape = RoundedCornerShape(999.dp), color = SurfaceTonal) {
                    Text("วันนี้", style = MaterialTheme.typography.labelSmall, color = SecondaryRose, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                MoreVertIcon(TextPrimary, Modifier.size(20.dp))
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, BorderSoft),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { shiftMonth(-1) }) { Text("‹", style = MaterialTheme.typography.titleLarge, color = TextPrimary) }
                    Text(monthFormat.format(Date(monthStart)), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    TextButton(onClick = { shiftMonth(1) }) { Text("›", style = MaterialTheme.typography.titleLarge, color = TextPrimary) }
                }

                Row(Modifier.fillMaxWidth()) {
                    ThaiWeekdays.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
                }

                weeks.forEach { week ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        week.forEach { day ->
                            if (day == null) {
                                Spacer(Modifier.weight(1f).height(42.dp))
                            } else {
                                val dayDate = remember(monthStart, day, dateFormat) {
                                    (dateFormat.calendar.clone() as Calendar).run {
                                        timeInMillis = monthStart; set(Calendar.DAY_OF_MONTH, day); dateFormat.format(time)
                                    }
                                }
                                val selected = dayDate == date
                                val dayCircle = if (selected) Modifier.background(Brush.linearGradient(listOf(PrimaryCoralLight, PrimaryCoral)), CircleShape) else Modifier
                                Column(
                                    modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp)).clickable { onDateChange(dayDate) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Box(modifier = Modifier.size(30.dp).then(dayCircle), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = day.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (selected) SurfaceWhite else TextPrimary,
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Box(Modifier.size(4.dp).clip(CircleShape).background(if (dayDate in noteDates) PrimaryCoral else Color.Transparent))
                                }
                            }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f).height(42.dp)) }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("บันทึกวันที่ $date", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("${foundNotes.size} รายการ", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }

        if (foundNotes.isEmpty()) {
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), SurfaceWhite, border = BorderStroke(1.dp, BorderSoft)) {
                Text("ไม่มีโน้ตในวันนี้", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(20.dp))
            }
        } else {
            foundNotes.forEach { NoteCard(it, dateFormat, showMoodBadge = true, onDelete = onDelete, onClick = onSelect) }
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            CircleIconButton(54.dp, "สร้างโน้ตใหม่", onClick = onNew) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(PrimaryCoralLight, PrimaryCoral))),
                    contentAlignment = Alignment.Center,
                ) { Text("+", fontSize = 26.sp, color = SurfaceWhite, fontWeight = FontWeight.Light) }
            }
        }
    }
}
