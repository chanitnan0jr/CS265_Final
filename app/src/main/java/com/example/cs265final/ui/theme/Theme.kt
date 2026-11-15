package com.example.cs265final.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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

private val MoodStyles = mapOf(
    "ดีมาก" to MoodStyle(Color(0xFFE8EFE9), Color(0xFF5C8A67)),
    "ดี" to MoodStyle(Color(0xFFFBF0E4), Color(0xFFC98A4B)),
    "ปกติ" to MoodStyle(Color(0xFFF6E2DF), PrimaryCoral),
    "เครียด" to MoodStyle(Color(0xFFECE9F2), Color(0xFF6B677E)),
    "เศร้า" to MoodStyle(Color(0xFFEAE8EF), Color(0xFF5F5B70)),
)

private val DefaultMoodStyle = MoodStyle(SurfaceTonal, SecondaryRose)
val DefaultMoods = MoodStyles.keys.toList()
fun moodStyle(mood: String): MoodStyle = MoodStyles[mood] ?: DefaultMoodStyle

private val FolderTints = mapOf(
    "การเรียน" to Color(0xFFEAE3EE),
    "ระบายความรู้สึก" to Color(0xFFF4E7E5),
    "งาน / โปรเจกต์" to Color(0xFFF7ECE2),
    "ไอเดีย" to Color(0xFFE9EEE8),
)

val DefaultFolders = FolderTints.keys.toList()
fun folderTint(folder: String): Color = FolderTints[folder] ?: SurfaceTonal

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

val Typography = Typography(
    headlineMedium = style(24, 34, FontWeight.Bold),
    headlineSmall = style(20, 28, FontWeight.SemiBold),
    titleLarge = style(18, 26, FontWeight.SemiBold),
    titleMedium = style(16, 24, FontWeight.SemiBold),
    bodyLarge = style(16, 26),
    bodyMedium = style(14, 20),
    labelSmall = style(12, 16, FontWeight.Medium),
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryCoral,
    secondary = SecondaryRose,
    background = BackgroundCream,
    surface = SurfaceWhite,
    surfaceVariant = SurfaceTonal,
    onPrimary = SurfaceWhite,
    onSecondary = SurfaceWhite,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderSoft,
)

@Composable
fun Cs265FinalTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColorScheme, typography = Typography, content = content)
}
