package com.example.autocorecarsimulator.custom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SpeedometerGauge(
    speed: Int,
    maxSpeed: Int = 180,
    onSpeedChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor = Color(0xFF29364E)
    val activeColor = Color(0xFF65B9FF)
    val tickColor = Color(0xFF9BAAC2)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(maxSpeed) {
                detectTapGestures { position ->
                    val cx = size.width / 2f
                    val cy = size.height * 0.72f
                    val angle = Math.toDegrees(
                        atan2(
                            (position.y - cy).toDouble(),
                            (position.x - cx).toDouble()
                        )
                    ).toFloat()

                    val normalized = ((angle + 180f) / 180f)
                        .coerceIn(0f, 1f)

                    onSpeedChange((normalized * maxSpeed).roundToInt())
                }
            }
            .pointerInput(maxSpeed) {
                detectDragGestures { change, _ ->
                    val cx = size.width / 2f
                    val cy = size.height * 0.72f
                    val angle = Math.toDegrees(
                        atan2(
                            (change.position.y - cy).toDouble(),
                            (change.position.x - cx).toDouble()
                        )
                    ).toFloat()

                    val normalized = ((angle + 180f) / 180f)
                        .coerceIn(0f, 1f)

                    onSpeedChange((normalized * maxSpeed).roundToInt())
                    change.consume()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 16.dp.toPx()
            val diameter = min(size.width * 0.82f, size.height * 1.55f)
            val arcWidth = diameter
            val arcHeight = diameter
            val left = (size.width - arcWidth) / 2f
            val top = size.height * 0.72f - arcHeight / 2f

            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(
                    arcWidth, arcHeight
                ),
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color = activeColor,
                startAngle = 180f,
                sweepAngle = 180f * speed.coerceIn(0, maxSpeed) / maxSpeed,
                useCenter = false,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(
                    arcWidth, arcHeight
                ),
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            for (i in 0..18) {
                val angle = Math.toRadians(180.0 + i * 10.0)
                val radius = diameter / 2f
                val centerX = size.width / 2f
                val centerY = size.height * 0.72f

                val outerRadius = radius - strokeWidth / 2f - 2.dp.toPx()
                val innerRadius = outerRadius - if (i % 3 == 0) {
                    10.dp.toPx()
                } else {
                    5.dp.toPx()
                }

                drawLine(
                    color = tickColor,
                    start = Offset(
                        centerX + cos(angle).toFloat() * innerRadius,
                        centerY + sin(angle).toFloat() * innerRadius
                    ),
                    end = Offset(
                        centerX + cos(angle).toFloat() * outerRadius,
                        centerY + sin(angle).toFloat() * outerRadius
                    ),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        /*Text(
            text = "$speed",
            color = Color.White,
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "km/h",
            color = activeColor,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Center)
        )*/


        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = "$speed",
                color = Color.White,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 50.sp
            )

            Text(
                text = "km/h",
                color = activeColor,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
        }
    }
}
