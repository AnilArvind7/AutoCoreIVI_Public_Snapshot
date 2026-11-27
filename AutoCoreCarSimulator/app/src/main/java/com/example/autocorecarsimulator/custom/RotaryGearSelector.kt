package com.example.autocorecarsimulator.custom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import kotlin.math.abs

private val DialCyan = Color(0xFF19D5E5)
private val DialText = Color(0xFFE5F4F5)
private val DialDark = Color(0xFF050B10)

@Composable
fun RotaryGearSelector(
    selectedGear: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onGearSelected: (String) -> Unit
) {
    val gearColor = when (selectedGear) {
        "P" -> Color(0xFFFFFFFF) // White
        "R" -> Color(0xFFFF2638) // Red
        "N" -> Color(0xFF35D68A) // Green
        "D" -> Color(0xFF329BFF) // Blue
        else -> DialCyan
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures { offset ->

                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        val dx = offset.x - centerX
                        val dy = offset.y - centerY

                        // Convert tap position to angle
                        val angle = Math.toDegrees(
                            atan2(dy.toDouble(), dx.toDouble())
                        )

                        val normalizedAngle = (angle + 360.0) % 360.0

                        val gearAngles = listOf(
                            "P" to 180.0,
                            "R" to 240.0,
                            "N" to 300.0,
                            "D" to 0.0
                        )

                        val nearestGear = gearAngles.minByOrNull { (_, gearAngle) ->
                            val difference = kotlin.math.abs(normalizedAngle - gearAngle)
                            minOf(difference, 360.0 - difference)
                        }

                        nearestGear?.let { (gear, gearAngle) ->
                            val difference = abs(normalizedAngle - gearAngle)
                            val angleDifference = minOf(difference, 360.0 - difference)

                            if (angleDifference <= 35.0) {
                                onGearSelected(gear)
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {

            val cx = size.width / 2f
            val cy = size.height / 2f

            val center = Offset(cx, cy)
            val radius = size.minDimension * 0.46f

            // Outer metallic ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF59636D),
                        Color(0xFFF0F3F5),
                        Color(0xFF737E88),
                        Color(0xFFE3E8EC),
                        Color(0xFF343C46),
                        Color(0xFFD6DDE2),
                        Color(0xFF59636D)
                    )
                ),
                radius = radius,
                center = center
            )

            // Inner dark ring
            drawCircle(
                color = Color(0xFF151D26),
                radius = radius - 5.dp.toPx(),
                center = center
            )

            // Glossy dial face
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF203C48),
                        Color(0xFF101E28),
                        DialDark
                    ),
                    center = center,
                    radius = radius * 0.88f
                ),
                radius = radius - 10.dp.toPx(),
                center = center
            )

            // Circular cyan rotary dial tick marks
            val outerTickRadius = radius - 12.dp.toPx()
            val tickLength = 8.dp.toPx()

            for (i in 0 until 120) {

                val angle = Math.toRadians(i * 3.0 - 90.0)

                val innerTickRadius = outerTickRadius - tickLength

                val start = Offset(
                    cx + cos(angle).toFloat() * innerTickRadius,
                    cy + sin(angle).toFloat() * innerTickRadius
                )

                val end = Offset(
                    cx + cos(angle).toFloat() * outerTickRadius,
                    cy + sin(angle).toFloat() * outerTickRadius
                )

                drawLine(
                    color = gearColor.copy(alpha = 0.75f),
                    start = start,
                    end = end,
                    strokeWidth = 0.4.dp.toPx(),
                    cap = StrokeCap.Butt
                )
            }

            // Gear positions
            val gearAngles = listOf(
                "P" to 180.0,
                "R" to 240.0,
                "N" to 300.0,
                "D" to 0.0
            )

            val nativeCanvas = drawContext.canvas.nativeCanvas

            val gearPaint = android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            ).apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 17.dp.toPx()
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.SANS_SERIF,
                    android.graphics.Typeface.BOLD
                )
            }

            gearAngles.forEach { (gear, degrees) ->

                val angle = Math.toRadians(degrees)

                val labelRadius = radius * 0.66f

                val x = cx + cos(angle).toFloat() * labelRadius
                val y = cy + sin(angle).toFloat() * labelRadius

                val isSelected = selectedGear == gear

                // Red selection arc with a gradient fading towards the center
                if (isSelected) {
                    val indicatorOuterRadius = radius - 12.dp.toPx()
                    val indicatorStroke = 24.dp.toPx()

                    val indicatorRadius =
                        indicatorOuterRadius - indicatorStroke / 2f

                    val innerEdgeRadius =
                        indicatorRadius - indicatorStroke / 2f

                    val outerEdgeRadius =
                        indicatorRadius + indicatorStroke / 2f

                    val gradientRadius = outerEdgeRadius + 1.dp.toPx()

                    /*drawArc(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                (innerEdgeRadius / gradientRadius) to
                                        Color.Transparent,
                                (outerEdgeRadius / gradientRadius) to
                                        Color(0xFFFF2638),
                                1f to Color(0xFFFF2638)
                            ),
                            center = Offset(cx, cy),
                            radius = gradientRadius
                        ),
                        startAngle = degrees.toFloat() - 17f,
                        sweepAngle = 34f,
                        useCenter = false,
                        topLeft = Offset(
                            cx - indicatorRadius,
                            cy - indicatorRadius
                        ),
                        size = Size(
                            indicatorRadius * 2f,
                            indicatorRadius * 2f
                        ),
                        style = Stroke(
                            width = indicatorStroke,
                            cap = StrokeCap.Butt
                        )
                    )*/
                    drawArc(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                (innerEdgeRadius / gradientRadius) to Color.Transparent,
                                (outerEdgeRadius / gradientRadius) to gearColor,
                                1f to gearColor
                            ),
                            center = Offset(cx, cy),
                            radius = gradientRadius
                        ),
                        startAngle = degrees.toFloat() - 17f,
                        sweepAngle = 34f,
                        useCenter = false, // Add this
                        topLeft = Offset(
                            cx - indicatorRadius,
                            cy - indicatorRadius
                        ),
                        size = Size(
                            indicatorRadius * 2f,
                            indicatorRadius * 2f
                        ),
                        style = Stroke(
                            width = indicatorStroke,
                            cap = StrokeCap.Butt
                        )
                    )
                }

                gearPaint.color = if (isSelected) {
                    gearColor.toArgb()
                } else {
                    android.graphics.Color.rgb(205, 220, 225)
                }

                gearPaint.textSize = if (isSelected) {
                    20.dp.toPx()
                } else {
                    16.dp.toPx()
                }

                nativeCanvas.drawText(
                    gear,
                    x,
                    y + gearPaint.textSize / 3f,
                    gearPaint
                )
            }

            // Centre display
            val centerPaint = android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = android.graphics.Color.rgb(225, 248, 250)
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 66.dp.toPx()
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.SANS_SERIF,
                    android.graphics.Typeface.NORMAL
                )
            }

            nativeCanvas.drawText(
                selectedGear,
                cx,
                cy + centerPaint.textSize * 0.34f,
                centerPaint
            )

            // Inner circular border
            drawCircle(
                color = DialCyan.copy(alpha = 0.25f),
                radius = radius * 0.87f,
                center = center,
                style = Stroke(1.dp.toPx())
            )
        }
    }
}
