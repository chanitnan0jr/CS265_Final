package com.example.cs265final.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
private fun IconCanvas(
    modifier: Modifier,
    strokeRatio: Float = 0.08f,
    block: DrawScope.(w: Float, h: Float, stroke: Stroke) -> Unit,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(size.minDimension * strokeRatio, cap = StrokeCap.Round, join = StrokeJoin.Round)
        block(size.width, size.height, stroke)
    }
}

private fun DrawScope.polyPath(vararg coords: Float, close: Boolean = true): Path = Path().apply {
    moveTo(size.width * coords[0], size.height * coords[1])
    for (i in 2 until coords.size step 2) lineTo(size.width * coords[i], size.height * coords[i + 1])
    if (close) close()
}

private fun DrawScope.sLine(color: Color, stroke: Stroke, x1: Float, y1: Float, x2: Float, y2: Float) =
    drawLine(color, Offset(size.width * x1, size.height * y1), Offset(size.width * x2, size.height * y2), stroke.width, StrokeCap.Round)

@Composable
fun AppWaveformLogo(modifier: Modifier = Modifier.size(26.dp)) = IconCanvas(modifier) { w, h, _ ->
    val barW = w * 0.065f
    listOf(0.32f, 0.56f, 0.88f, 0.64f, 0.44f, 0.72f, 0.36f).forEachIndexed { idx, ratio ->
        val bh = h * ratio
        drawRoundRect(PrimaryCoral, Offset(idx * barW * 2.05f, (h - bh) / 2f), Size(barW, bh), CornerRadius(barW / 2f, barW / 2f))
    }
}

@Composable
fun BellIcon(color: Color = TextPrimary, modifier: Modifier = Modifier.size(22.dp)) = IconCanvas(modifier) { w, h, s ->
    val bell = Path().apply {
        moveTo(w * 0.25f, h * 0.68f); lineTo(w * 0.25f, h * 0.44f)
        cubicTo(w * 0.25f, h * 0.22f, w * 0.75f, h * 0.22f, w * 0.75f, h * 0.44f)
        lineTo(w * 0.75f, h * 0.68f); lineTo(w * 0.86f, h * 0.78f); lineTo(w * 0.14f, h * 0.78f); close()
    }
    drawPath(bell, color, style = s)
    drawArc(color, 0f, 180f, false, Offset(w * 0.4f, h * 0.78f), Size(w * 0.2f, h * 0.14f), style = s)
}

@Composable
fun MicOutlineIcon(color: Color = SurfaceWhite, modifier: Modifier = Modifier.size(34.dp)) = IconCanvas(modifier) { w, h, s ->
    drawRoundRect(color, Offset(w * 0.34f, h * 0.1f), Size(w * 0.32f, h * 0.48f), CornerRadius(w * 0.16f, w * 0.16f), style = s)
    drawArc(color, 0f, 180f, false, Offset(w * 0.22f, h * 0.26f), Size(w * 0.56f, h * 0.46f), style = s)
    sLine(color, s, 0.5f, 0.72f, 0.5f, 0.88f)
    sLine(color, s, 0.34f, 0.88f, 0.66f, 0.88f)
}

@Composable
fun PauseBarsIcon(color: Color = SurfaceWhite, modifier: Modifier = Modifier.size(28.dp)) = IconCanvas(modifier) { w, h, _ ->
    val barW = w * 0.18f
    val barH = h * 0.58f
    val top = (h - barH) / 2f
    listOf(0.24f, 0.58f).forEach { drawRoundRect(color, Offset(w * it, top), Size(barW, barH), CornerRadius(barW / 2f, barW / 2f)) }
}

@Composable
fun StopSquareIcon(color: Color = TextPrimary, modifier: Modifier = Modifier.size(16.dp)) = IconCanvas(modifier) { w, h, _ ->
    drawRoundRect(color, Offset(w * 0.12f, h * 0.12f), Size(w * 0.76f, h * 0.76f), CornerRadius(w * 0.2f, w * 0.2f))
}

@Composable
fun PlayTriangleIcon(color: Color = PrimaryCoral, modifier: Modifier = Modifier.size(14.dp)) = IconCanvas(modifier) { _, _, _ ->
    drawPath(polyPath(0.22f, 0.12f, 0.88f, 0.5f, 0.22f, 0.88f), color)
}

@Composable
fun MoreVertIcon(color: Color = TextSecondary, modifier: Modifier = Modifier.size(20.dp)) = IconCanvas(modifier) { w, h, _ ->
    val r = size.minDimension * 0.08f
    listOf(0.25f, 0.5f, 0.75f).forEach { drawCircle(color, r, Offset(w * 0.5f, h * it)) }
}

@Composable
fun BackArrowIcon(color: Color = TextPrimary, modifier: Modifier = Modifier.size(22.dp)) = IconCanvas(modifier, 0.09f) { _, _, s ->
    sLine(color, s, 0.82f, 0.5f, 0.2f, 0.5f)
    sLine(color, s, 0.45f, 0.25f, 0.2f, 0.5f)
    sLine(color, s, 0.45f, 0.75f, 0.2f, 0.5f)
}

@Composable
fun CloudUploadIcon(color: Color = TextPrimary, modifier: Modifier = Modifier.size(22.dp)) = IconCanvas(modifier) { w, h, s ->
    drawArc(color, 180f, 180f, false, Offset(w * 0.26f, h * 0.22f), Size(w * 0.46f, h * 0.4f), style = s)
    drawArc(color, 90f, 180f, false, Offset(w * 0.1f, h * 0.42f), Size(w * 0.34f, h * 0.34f), style = s)
    drawArc(color, 270f, 180f, false, Offset(w * 0.56f, h * 0.42f), Size(w * 0.34f, h * 0.34f), style = s)
    sLine(color, s, 0.27f, 0.76f, 0.73f, 0.76f)
    sLine(color, s, 0.5f, 0.84f, 0.5f, 0.52f)
    sLine(color, s, 0.39f, 0.62f, 0.5f, 0.5f)
    sLine(color, s, 0.61f, 0.62f, 0.5f, 0.5f)
}

@Composable
fun NewNoteEditIcon(color: Color = TextPrimary, modifier: Modifier = Modifier.size(20.dp)) = IconCanvas(modifier, 0.085f) { _, _, s ->
    drawPath(polyPath(0.56f, 0.18f, 0.2f, 0.18f, 0.2f, 0.84f, 0.82f, 0.84f, 0.82f, 0.46f, close = false), color, style = s)
    sLine(color, s, 0.46f, 0.56f, 0.86f, 0.16f)
}

@Composable
fun SearchIcon(color: Color = TextPrimary, modifier: Modifier = Modifier.size(22.dp)) = IconCanvas(modifier, 0.085f) { w, _, s ->
    drawCircle(color, w * 0.28f, Offset(w * 0.44f, w * 0.44f), style = s)
    drawLine(color, Offset(w * 0.64f, w * 0.64f), Offset(w * 0.86f, w * 0.86f), s.width, StrokeCap.Round)
}

@Composable
fun HomeNavIcon(selected: Boolean, color: Color, modifier: Modifier = Modifier.size(22.dp)) = IconCanvas(modifier, 0.085f) { _, _, s ->
    val path = polyPath(0.15f, 0.46f, 0.5f, 0.16f, 0.85f, 0.46f, 0.85f, 0.84f, 0.15f, 0.84f)
    if (selected) drawPath(path, color) else drawPath(path, color, style = s)
}

@Composable
fun FolderNavIcon(selected: Boolean, color: Color, modifier: Modifier = Modifier.size(22.dp)) = IconCanvas(modifier, 0.085f) { _, _, s ->
    val path = polyPath(0.14f, 0.28f, 0.42f, 0.28f, 0.5f, 0.38f, 0.86f, 0.38f, 0.86f, 0.8f, 0.14f, 0.8f)
    if (selected) drawPath(path, color) else drawPath(path, color, style = s)
}

@Composable
fun CalendarNavIcon(selected: Boolean, color: Color, modifier: Modifier = Modifier.size(22.dp)) = IconCanvas(modifier, 0.085f) { w, h, s ->
    drawRoundRect(color, Offset(w * 0.16f, h * 0.24f), Size(w * 0.68f, h * 0.6f), CornerRadius(w * 0.1f, w * 0.1f), style = s)
    sLine(color, s, 0.16f, 0.44f, 0.84f, 0.44f)
    sLine(color, s, 0.34f, 0.14f, 0.34f, 0.32f)
    sLine(color, s, 0.66f, 0.14f, 0.66f, 0.32f)
    if (selected) drawCircle(color, w * 0.07f, Offset(w * 0.5f, h * 0.64f))
}

@Composable
fun GraduationCapIcon(color: Color, modifier: Modifier = Modifier.size(22.dp)) = IconCanvas(modifier) { w, h, s ->
    drawPath(polyPath(0.1f, 0.42f, 0.5f, 0.22f, 0.9f, 0.42f, 0.5f, 0.62f), color, style = s)
    val base = Path().apply {
        moveTo(w * 0.28f, h * 0.52f); lineTo(w * 0.28f, h * 0.72f)
        cubicTo(w * 0.4f, h * 0.82f, w * 0.6f, h * 0.82f, w * 0.72f, h * 0.72f)
        lineTo(w * 0.72f, h * 0.52f)
    }
    drawPath(base, color, style = s)
    sLine(color, s, 0.86f, 0.45f, 0.86f, 0.7f)
}

@Composable
fun HeartOutlineIcon(color: Color, modifier: Modifier = Modifier.size(22.dp)) = IconCanvas(modifier) { w, h, s ->
    val heart = Path().apply {
        moveTo(w * 0.5f, h * 0.82f)
        cubicTo(w * 0.12f, h * 0.56f, w * 0.12f, h * 0.24f, w * 0.34f, h * 0.22f)
        cubicTo(w * 0.44f, h * 0.21f, w * 0.5f, h * 0.32f, w * 0.5f, h * 0.32f)
        cubicTo(w * 0.5f, h * 0.32f, w * 0.56f, h * 0.21f, w * 0.66f, h * 0.22f)
        cubicTo(w * 0.88f, h * 0.24f, w * 0.88f, h * 0.56f, w * 0.5f, h * 0.82f)
    }
    drawPath(heart, color, style = s)
}

@Composable
fun BriefcaseOutlineIcon(color: Color, modifier: Modifier = Modifier.size(22.dp)) = IconCanvas(modifier) { w, h, s ->
    drawRoundRect(color, Offset(w * 0.16f, h * 0.34f), Size(w * 0.68f, h * 0.48f), CornerRadius(w * 0.1f, w * 0.1f), style = s)
    drawRoundRect(color, Offset(w * 0.36f, h * 0.2f), Size(w * 0.28f, h * 0.14f), CornerRadius(w * 0.06f, w * 0.06f), style = s)
    sLine(color, s, 0.16f, 0.54f, 0.84f, 0.54f)
}

@Composable
fun BookmarkOutlineIcon(color: Color, modifier: Modifier = Modifier.size(20.dp)) = IconCanvas(modifier, 0.085f) { _, _, s ->
    drawPath(polyPath(0.26f, 0.16f, 0.74f, 0.16f, 0.74f, 0.84f, 0.5f, 0.66f, 0.26f, 0.84f), color, style = s)
}

@Composable
fun MoodFaceIcon(mood: String, color: Color, modifier: Modifier = Modifier.size(26.dp)) = IconCanvas(modifier) { w, h, s ->
    drawCircle(color, w * 0.42f, Offset(w * 0.5f, h * 0.5f), style = s)
    drawCircle(color, w * 0.04f, Offset(w * 0.36f, h * 0.42f))
    drawCircle(color, w * 0.04f, Offset(w * 0.64f, h * 0.42f))
    when (mood) {
        "ดีมาก", "สุข" -> drawArc(color, 15f, 150f, false, Offset(w * 0.3f, h * 0.46f), Size(w * 0.4f, h * 0.26f), style = s)
        "ดี", "สงบ" -> drawArc(color, 25f, 130f, false, Offset(w * 0.34f, h * 0.5f), Size(w * 0.32f, h * 0.18f), style = s)
        "เครียด" -> sLine(color, s, 0.35f, 0.64f, 0.65f, 0.64f)
        "เศร้า" -> drawArc(color, 200f, 140f, false, Offset(w * 0.34f, h * 0.58f), Size(w * 0.32f, h * 0.2f), style = s)
        else -> drawArc(color, 20f, 140f, false, Offset(w * 0.36f, h * 0.52f), Size(w * 0.28f, h * 0.14f), style = s)
    }
}
