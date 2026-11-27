package com.example.autocorecarsimulator.custom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

@Composable
fun BatteryMeter(
    percentage: Int,
    modifier: Modifier = Modifier
) {
    val level = percentage.coerceIn(0, 100)

    val barColor = when {
        level <= 10 -> Color(0xFFFF3434)  // Red
        level <= 20 -> Color(0xFFFF813D)  // Orange
        level <= 40 -> Color(0xFFFFD52A)  // Yellow
        else -> Color(0xFF13D66B)         // Green
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(96.dp)
        ) {
            val terminalWidth = size.width * 0.025f
            val bodyWidth = size.width - terminalWidth
            val bodyHeight = size.height * 0.78f
            val top = (size.height - bodyHeight) / 2f
            val corner = 5.dp.toPx()
            val outlineWidth = 2.dp.toPx()

            // White battery body outline
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(0f, top),
                size = Size(
                    bodyWidth,
                    bodyHeight
                ),
                cornerRadius = CornerRadius(
                    corner,
                    corner
                ),
                style = Stroke(width = outlineWidth)
            )

            // White positive terminal
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(
                    bodyWidth,
                    size.height * 0.39f
                ),
                size = Size(
                    terminalWidth,
                    size.height * 0.22f
                ),
                cornerRadius = CornerRadius(
                    2.dp.toPx(),
                    2.dp.toPx()
                )
            )

            // Ten charge segments
            val segmentCount = 10
            val padding = 4.dp.toPx()
            val gap = 2.dp.toPx()
            val innerWidth = bodyWidth - padding * 2
            val innerHeight = bodyHeight - padding * 2
            val segmentWidth =
                (innerWidth - gap * (segmentCount - 1)) / segmentCount

            val activeSegments = ceil(level / 10.0).toInt()

            for (i in 0 until segmentCount) {
                if (i < activeSegments) {
                    val left = padding + i * (segmentWidth + gap)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(
                            left,
                            top + padding
                        ),
                        size = Size(
                            segmentWidth,
                            innerHeight
                        ),
                        cornerRadius = CornerRadius(
                            2.dp.toPx(),
                            2.dp.toPx()
                        )
                    )
                }
            }
        }

        // Percentage outside the battery
        Text(
            text = "$level%",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}
