
package com.example.autocoreividashboard.custom

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.atan2

private val GaugeBlue = Color(0xFF078CE0)
private val GaugeRed = Color(0xFFFF2638)
private val GaugeTrack = Color(0xFF172D35)

@Composable
fun SpeedometerGauge2(
    speed: Int,
    maxSpeed: Int = 220,
    onSpeedChange: (Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val safeMax = maxSpeed.coerceAtLeast(1)
    val safeSpeed = speed.coerceIn(0, safeMax)

    Box(
        modifier = modifier
            .pointerInput(enabled, safeMax) {
                if (!enabled) return@pointerInput

                fun updateSpeed(position: Offset) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    val dx = position.x - cx
                    val dy = position.y - cy

                    var angle = Math.toDegrees(
                        atan2(dy.toDouble(), dx.toDouble())
                    ).toFloat()

                    if (angle < 135f) angle += 360f

                    val progress = ((angle - 135f) / 270f)
                        .coerceIn(0f, 1f)

                    onSpeedChange((progress * safeMax).roundToInt())
                }

                detectTapGestures {
                    updateSpeed(it)
                }
            }
            .pointerInput(enabled, safeMax) {
                if (!enabled) return@pointerInput

                detectDragGestures { change, _ ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    val dx = change.position.x - cx
                    val dy = change.position.y - cy

                    var angle = Math.toDegrees(
                        atan2(dy.toDouble(), dx.toDouble())
                    ).toFloat()

                    if (angle < 135f) angle += 360f

                    val progress = ((angle - 135f) / 270f)
                        .coerceIn(0f, 1f)

                    onSpeedChange((progress * safeMax).roundToInt())
                    change.consume()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .drawWithCache {
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    val radius = min(
                        size.width * 0.43f,
                        size.height * 0.43f
                    )

                    val stroke = 5.dp.toPx()
                    val glowRadius = radius * 0.98f

                    val center = Offset(cx, cy)
                    val leftGlowCenter = Offset(
                        cx - radius * 0.35f,
                        cy
                    )
                    val rightGlowCenter = Offset(
                        cx + radius * 0.35f,
                        cy
                    )

                    val arcTopLeft = Offset(
                        cx - radius,
                        cy - radius
                    )

                    val arcSize = Size(
                        radius * 2f,
                        radius * 2f
                    )

                    val blueSweep = 270f *
                            (160f / safeMax).coerceIn(0f, 1f)

                    val blueGlow = Brush.radialGradient(
                        colors = listOf(
                            GaugeBlue.copy(alpha = 0.20f),
                            GaugeBlue.copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        center = leftGlowCenter,
                        radius = glowRadius
                    )

                    val redGlow = Brush.radialGradient(
                        colors = listOf(
                            GaugeRed.copy(alpha = 0.14f),
                            GaugeRed.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = rightGlowCenter,
                        radius = glowRadius
                    )

                    val labelPaint = Paint(
                        Paint.ANTI_ALIAS_FLAG
                    ).apply {
                        color = android.graphics.Color.rgb(7, 140, 224)
                        textSize = 15.dp.toPx()
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(
                            Typeface.SANS_SERIF,
                            Typeface.NORMAL
                        )
                    }

                    val redLabelPaint = Paint(
                        Paint.ANTI_ALIAS_FLAG
                    ).apply {
                        color = android.graphics.Color.rgb(255, 38, 56)
                        textSize = 15.dp.toPx()
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(
                            Typeface.SANS_SERIF,
                            Typeface.NORMAL
                        )
                    }

                    // Cache tick mark geometry.
                    val ticks = (0..safeMax step 10).map { value ->
                        val fraction = value.toFloat() / safeMax
                        val angle = Math.toRadians(
                            135.0 + fraction * 270.0
                        )

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

                        val labelRadius = radius * 0.78f

                        val labelPosition = Offset(
                            cx + cos(angle).toFloat() * labelRadius,
                            cy + sin(angle).toFloat() * labelRadius
                        )

                        Triple(value, start, end) to labelPosition
                    }

                    onDrawBehind {
                        // Static background glow.
                        drawCircle(
                            brush = blueGlow,
                            radius = glowRadius,
                            center = leftGlowCenter
                        )

                        drawCircle(
                            brush = redGlow,
                            radius = glowRadius,
                            center = rightGlowCenter
                        )

                        // Gauge track.
                        drawArc(
                            color = GaugeTrack,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(
                                stroke,
                                cap = StrokeCap.Butt
                            )
                        )

                        // Blue operating range.
                        drawArc(
                            color = GaugeBlue,
                            startAngle = 135f,
                            sweepAngle = blueSweep,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(
                                stroke,
                                cap = StrokeCap.Butt
                            )
                        )

                        // Redline range.
                        drawArc(
                            color = GaugeRed,
                            startAngle = 135f + blueSweep,
                            sweepAngle = 270f - blueSweep,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(
                                stroke,
                                cap = StrokeCap.Butt
                            )
                        )

                        // Tick marks and labels.
                        val nativeCanvas =
                            drawContext.canvas.nativeCanvas

                        ticks.forEach { (tick, labelPosition) ->
                            val (value, start, end) = tick
                            val isMajor = value % 20 == 0

                            val tickColor =
                                if (value >= 160) GaugeRed
                                else GaugeBlue

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
                                nativeCanvas.drawText(
                                    value.toString(),
                                    labelPosition.x,
                                    labelPosition.y +
                                            labelPaint.textSize / 3f,
                                    if (value >= 160) {
                                        redLabelPaint
                                    } else {
                                        labelPaint
                                    }
                                )
                            }
                        }

                        // Dynamic layer: needle
                        val needleAngle = Math.toRadians(
                            135.0 +
                                    safeSpeed.toDouble() / safeMax * 270.0
                        )

                        val needleLength = radius * 0.72f

                        val needleEnd = Offset(
                            cx + cos(needleAngle).toFloat() *
                                    needleLength,
                            cy + sin(needleAngle).toFloat() *
                                    needleLength
                        )

                        drawLine(
                            color = GaugeRed,
                            start = center,
                            end = needleEnd,
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Butt
                        )

                        drawCircle(
                            color = Color(0xFF27333D),
                            radius = 6.dp.toPx(),
                            center = center
                        )

                        drawCircle(
                            color = GaugeRed,
                            radius = 3.dp.toPx(),
                            center = center
                        )
                    }
                }
        )
    }
}
