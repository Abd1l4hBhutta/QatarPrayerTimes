package com.example.qatarprayertimes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.qatarprayertimes.data.PrayerId
import com.example.qatarprayertimes.ui.theme.Accent
import androidx.compose.ui.graphics.Color

@Composable
fun PrayerIcon(
    id: PrayerId,
    modifier: Modifier = Modifier,
    color: Color = Accent,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        fun p(x: Float, y: Float) = Offset(x * w, y * h)
        when (id) {
            PrayerId.FAJR -> {
                drawLine(color, p(0.16f, 0.72f), p(0.84f, 0.72f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.5f, 0.72f), p(0.5f, 0.42f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.32f, 0.56f), p(0.5f, 0.42f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.68f, 0.56f), p(0.5f, 0.42f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.5f, 0.28f), p(0.5f, 0.18f), stroke.width, StrokeCap.Round)
            }
            PrayerId.SUNRISE -> {
                drawLine(color, p(0.16f, 0.74f), p(0.84f, 0.74f), stroke.width, StrokeCap.Round)
                drawArc(color, 200f, 140f, false, p(0.28f, 0.38f), androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.44f), style = stroke)
                drawLine(color, p(0.5f, 0.28f), p(0.5f, 0.16f), stroke.width, StrokeCap.Round)
            }
            PrayerId.DHUHR -> {
                drawCircle(color, radius = w * 0.16f, center = p(0.5f, 0.5f), style = stroke)
                drawLine(color, p(0.5f, 0.14f), p(0.5f, 0.28f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.5f, 0.72f), p(0.5f, 0.86f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.14f, 0.5f), p(0.28f, 0.5f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.72f, 0.5f), p(0.86f, 0.5f), stroke.width, StrokeCap.Round)
            }
            PrayerId.ASR -> {
                drawCircle(color, radius = w * 0.14f, center = p(0.5f, 0.42f), style = stroke)
                drawLine(color, p(0.16f, 0.78f), p(0.84f, 0.78f), stroke.width, StrokeCap.Round)
                drawLine(color, p(0.5f, 0.16f), p(0.5f, 0.26f), stroke.width, StrokeCap.Round)
            }
            PrayerId.MAGHRIB -> {
                drawLine(color, p(0.16f, 0.72f), p(0.84f, 0.72f), stroke.width, StrokeCap.Round)
                drawArc(color, 180f, 180f, false, p(0.28f, 0.42f), androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.44f), style = stroke)
            }
            PrayerId.ISHA -> {
                drawArc(color, 40f, 280f, false, p(0.22f, 0.22f), androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.56f), style = stroke)
                drawCircle(color, radius = w * 0.035f, center = p(0.76f, 0.28f))
            }
        }
    }
}
