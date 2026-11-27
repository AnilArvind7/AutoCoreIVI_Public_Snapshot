package com.example.autocoreividashboard.custom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogClock(
    modifier: Modifier = Modifier
) {
    var currentTime by remember {
        mutableStateOf(LocalTime.now())
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000)
        }
    }

    Canvas(
        modifier = modifier.aspectRatio(1f)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.46f

        // Outer clock ring
        drawCircle(
            color = Color(0xFF168CFF),
            radius = radius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )

        // Inner ring
        drawCircle(
            color = Color(0xFF25364F),
            radius = radius * 0.88f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Hour markers
        for (i in 0 until 60) {
            val angle = Math.toRadians(i * 6.0 - 90.0)

            val isHour = i % 5 == 0
            val outerRadius = radius * 0.82f
            val innerRadius = if (isHour) {
                radius * 0.70f
            } else {
                radius * 0.77f
            }

            val start = Offset(
                center.x + cos(angle).toFloat() * innerRadius,
                center.y + sin(angle).toFloat() * innerRadius
            )

            val end = Offset(
                center.x + cos(angle).toFloat() * outerRadius,
                center.y + sin(angle).toFloat() * outerRadius
            )

            drawLine(
                color = if (isHour) Color(0xFF55B8FF)
                else Color(0xFF526782),
                start = start,
                end = end,
                strokeWidth = if (isHour) 3.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Calculate hand angles
        val hourAngle = Math.toRadians(
            ((currentTime.hour % 12) +
                    currentTime.minute / 60.0) * 30.0 - 90.0
        )

        val minuteAngle = Math.toRadians(
            (currentTime.minute + currentTime.second / 60.0) *
                    6.0 - 90.0
        )

        val secondAngle = Math.toRadians(
            currentTime.second * 6.0 - 90.0
        )

        // Hour hand
        drawLine(
            color = Color.White,
            start = center,
            end = Offset(
                center.x + cos(hourAngle).toFloat() * radius * 0.45f,
                center.y + sin(hourAngle).toFloat() * radius * 0.45f
            ),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Minute hand
        drawLine(
            color = Color(0xFF55B8FF),
            start = center,
            end = Offset(
                center.x + cos(minuteAngle).toFloat() * radius * 0.68f,
                center.y + sin(minuteAngle).toFloat() * radius * 0.68f
            ),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Second hand
        drawLine(
            color = Color(0xFFFF555F),
            start = center,
            end = Offset(
                center.x + cos(secondAngle).toFloat() * radius * 0.76f,
                center.y + sin(secondAngle).toFloat() * radius * 0.76f
            ),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Center hub
        drawCircle(
            color = Color(0xFF168CFF),
            radius = 6.dp.toPx(),
            center = center
        )

        drawCircle(
            color = Color.White,
            radius = 2.5.dp.toPx(),
            center = center
        )
    }
}
