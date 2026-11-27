package com.example.autocorecarsimulator.custom

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val GaugeBackground = Color(0xFF06191D)
private val GaugeBlue = Color(0xFF078CE0)
private val GaugeRed = Color(0xFFFF2638)
private val GaugeTrack = Color(0xFF172D35)
private val GaugeWhite = Color(0xFFEAF5FF)

@Composable
fun SpeedometerGaugeModern(
    speed: Int,
    maxSpeed: Int = 220,
    selectedGear: String = "P",
    onSpeedChange: (Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val safeMax = maxSpeed.coerceAtLeast(1)
    val safeSpeed = speed.coerceIn(0, safeMax)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {

            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = min(size.width * 0.43f, size.height * 0.43f)
            val stroke = 5.dp.toPx()

            val arcTopLeft = Offset(cx - radius, cy - radius)

            val arcSize = androidx.compose.ui.geometry.Size(
                radius * 2f,
                radius * 2f
            )

            val glowRadius = radius * 0.98f

            // Subtle blue glow on the left side
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GaugeBlue.copy(alpha = 0.20f),
                        GaugeBlue.copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    center = Offset(cx - radius * 0.35f, cy),
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = Offset(cx - radius * 0.35f, cy)
            )

            // Subtle red glow on the right side
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GaugeRed.copy(alpha = 0.14f),
                        GaugeRed.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(cx + radius * 0.35f, cy),
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = Offset(cx + radius * 0.35f, cy)
            )

            // Dark gauge track
            drawArc(
                color = GaugeTrack,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Butt)
            )

            // Blue operating range: 0–160 km/h
            val blueSweep = 270f * (160f / safeMax).coerceIn(0f, 1f)

            drawArc(
                color = GaugeBlue,
                startAngle = 135f,
                sweepAngle = blueSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Butt)
            )

            // Redline zone: 160 km/h to maximum
            val redStart = 135f + blueSweep
            val redSweep = 270f - blueSweep

            drawArc(
                color = GaugeRed,
                startAngle = redStart,
                sweepAngle = redSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Butt)
            )

            // Native canvas for text
            val nativeCanvas = drawContext.canvas.nativeCanvas

            // Blue numeric labels
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(7, 140, 224)
                textSize = 15.dp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(
                    Typeface.SANS_SERIF,
                    Typeface.NORMAL
                )
            }

            // Red numeric labels
            val redLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(255, 38, 56)
                textSize = 15.dp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(
                    Typeface.SANS_SERIF,
                    Typeface.NORMAL
                )
            }

            // Tick marks and numeric labels
            for (value in 0..safeMax step 10) {

                val fraction = value.toFloat() / safeMax
                val angle = Math.toRadians(135.0 + fraction * 270.0)

                val isMajor = value % 20 == 0

                val outerRadius = radius - stroke * 1.1f

                val innerRadius = outerRadius -
                        if (isMajor) 13.dp.toPx() else 6.dp.toPx()

                val start = Offset(
                    cx + cos(angle).toFloat() * innerRadius,
                    cy + sin(angle).toFloat() * innerRadius
                )

                val end = Offset(
                    cx + cos(angle).toFloat() * outerRadius,
                    cy + sin(angle).toFloat() * outerRadius
                )

                val tickColor = if (value >= 160) {
                    GaugeRed
                } else {
                    GaugeBlue
                }

                drawLine(
                    color = tickColor,
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) {
                        2.5.dp.toPx()
                    } else {
                        1.5.dp.toPx()
                    }
                )

                if (isMajor && value % 40 == 0) {

                    val labelRadius = when (value) {
                        160, 200 -> radius * 0.72f
                        else -> radius * 0.76f
                    }

                    val labelX = cx +
                            cos(angle).toFloat() * labelRadius

                    val labelY = cy +
                            sin(angle).toFloat() * labelRadius

                    nativeCanvas.drawText(
                        value.toString(),
                        labelX,
                        labelY + labelPaint.textSize / 3f,
                        if (value >= 160) {
                            redLabelPaint
                        } else {
                            labelPaint
                        }
                    )
                }
            }

            // Slim needle
            val needleAngle = Math.toRadians(
                135.0 + safeSpeed.toDouble() / safeMax * 270.0
            )

            val needleLength = radius * 0.72f

            val needleEnd = Offset(
                cx + cos(needleAngle).toFloat() * needleLength,
                cy + sin(needleAngle).toFloat() * needleLength
            )

            // Thin, crisp red needle
            drawLine(
                color = GaugeRed,
                start = Offset(cx, cy),
                end = needleEnd,
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Butt
            )

            // Small central pivot
            drawCircle(
                color = Color(0xFF27333D),
                radius = 6.dp.toPx(),
                center = Offset(cx, cy)
            )

            drawCircle(
                color = GaugeRed,
                radius = 3.dp.toPx(),
                center = Offset(cx, cy)
            )

            // GEAR INDICATOR

            val boxWidth = 64.dp.toPx()
            val boxHeight = 42.dp.toPx()

            val boxTop = cy + radius * 0.40f
            val boxLeft = cx - boxWidth / 2f
            val boxRight = cx + boxWidth / 2f
            val boxBottom = boxTop + boxHeight

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(210, 225, 235)
                style = Paint.Style.STROKE
                strokeWidth = 0.5.dp.toPx()
            }

            val gearLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(155, 170, 194)
                textSize = 10.dp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

            // Rounded rectangular border
            nativeCanvas.drawRoundRect(
                boxLeft,
                boxTop,
                boxRight,
                boxBottom,
                7.dp.toPx(),
                7.dp.toPx(),
                borderPaint
            )

            // Remove middle portion of top border
            val labelWidth =
                gearLabelPaint.measureText("GEAR") + 8.dp.toPx()

            val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(23, 31, 51)
                style = Paint.Style.FILL
            }

            nativeCanvas.drawRect(
                cx - labelWidth / 2f,
                boxTop - 2.dp.toPx(),
                cx + labelWidth / 2f,
                boxTop + 2.dp.toPx(),
                clearPaint
            )

            // GEAR label
            nativeCanvas.drawText(
                "GEAR",
                cx,
                boxTop + gearLabelPaint.textSize / 3f,
                gearLabelPaint
            )

            // Selected gear colour
            val gearColor = when (selectedGear) {
                "P" -> Color.White
                "R" -> Color(0xFFFF2638)
                "N" -> Color(0xFF35D68A)
                "D" -> Color(0xFF329BFF)
                else -> GaugeWhite
            }

            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = gearColor.toArgb()
                textSize = 24.dp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT
            }

            // Selected gear centred inside box
            nativeCanvas.drawText(
                selectedGear,
                cx,
                boxTop + boxHeight / 2f + valuePaint.textSize / 3f,
                valuePaint
            )
        }
    }
}
