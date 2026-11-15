package com.example.cs265final.ui.theme

import androidx.compose.ui.graphics.Color

val BackgroundCream = Color(0xFFFAF5F2)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceTonal = Color(0xFFF5ECE8)
val PrimaryCoral = Color(0xFFC87468)
val PrimaryCoralLight = Color(0xFFD98E82)
val SecondaryRose = Color(0xFFB27A76)
val TextPrimary = Color(0xFF2F2726)
val TextSecondary = Color(0xFF7D716E)
val BorderSoft = Color(0xFFEBE0DC)
val RecordingDotRed = Color(0xFFD9534F)

data class MoodStyle(val container: Color, val content: Color)

private val GreatMood = MoodStyle(Color(0xFFE8EFE9), Color(0xFF5C8A67))
private val GoodMood = MoodStyle(Color(0xFFFBF0E4), Color(0xFFC98A4B))
private val NeutralMood = MoodStyle(Color(0xFFF6E2DF), PrimaryCoral)

private val MoodStyles = mapOf(
    "ดีมาก" to GreatMood,
    "ดี" to GoodMood,
    "ปกติ" to NeutralMood,
    "เครียด" to MoodStyle(Color(0xFFECE9F2), Color(0xFF6B677E)),
    "เศร้า" to MoodStyle(Color(0xFFEAE8EF), Color(0xFF5F5B70)),
    "สุข" to GreatMood,
    "สงบ" to GoodMood,
    "เฉยๆ" to NeutralMood,
)

private val DefaultMoodStyle = MoodStyle(SurfaceTonal, SecondaryRose)

fun moodStyle(mood: String): MoodStyle = MoodStyles[mood] ?: DefaultMoodStyle

private val FolderTints = mapOf(
    "การเรียน" to Color(0xFFEAE3EE),
    "ระบายความรู้สึก" to Color(0xFFF4E7E5),
    "งาน / โปรเจกต์" to Color(0xFFF7ECE2),
    "ไอเดีย" to Color(0xFFE9EEE8),
)

fun folderTint(folder: String): Color = FolderTints[folder] ?: SurfaceTonal
