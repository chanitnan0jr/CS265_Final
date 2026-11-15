package com.example.cs265final.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
