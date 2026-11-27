package com.example.autocoreividashboard.custom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

@Composable
fun VehicleIllustration(
    headlightsOn: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.aspectRatio(1.5f)
    ) {
        val sx = size.width / 300f
        val sy = size.height / 200f

        scale(sx, sy, pivot = Offset.Zero) {

            // Ground shadow
            drawOval(
                color = Color(0x55000000),
                topLeft = Offset(55f, 165f),
                size = Size(190f, 18f)
            )

            // Tyres
            drawRoundRect(
                color = Color(0xFF252C38),
                topLeft = Offset(72f, 133f),
                size = Size(35f, 43f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f)
            )

            drawRoundRect(
                color = Color(0xFF252C38),
                topLeft = Offset(193f, 133f),
                size = Size(35f, 43f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f)
            )

            // Main vehicle body
            val body = Path().apply {
                moveTo(48f, 145f)
                lineTo(48f, 105f)
                quadraticTo(49f, 88f, 65f, 82f)
                lineTo(86f, 42f)
                quadraticTo(93f, 29f, 111f, 29f)
                lineTo(189f, 29f)
                quadraticTo(207f, 29f, 214f, 42f)
                lineTo(235f, 82f)
                quadraticTo(251f, 88f, 252f, 105f)
                lineTo(252f, 145f)
                quadraticTo(252f, 161f, 235f, 164f)
                lineTo(65f, 164f)
                quadraticTo(48f, 161f, 48f, 145f)
                close()
            }

            drawPath(
                path = body,
                color = Color(0xFF168CFF)
            )

            // Body highlight
            drawPath(
                path = body,
                color = Color(0xFF55B8FF),
                style = Stroke(width = 2f)
            )

            // Windshield
            val windshield = Path().apply {
                moveTo(96f, 48f)
                quadraticTo(100f, 40f, 112f, 40f)
                lineTo(188f, 40f)
                quadraticTo(200f, 40f, 204f, 48f)
                lineTo(220f, 82f)
                lineTo(80f, 82f)
                close()
            }

            drawPath(
                path = windshield,
                color = Color(0xFF9BD9F5)
            )

            // Windshield reflection
            drawLine(
                color = Color(0xFFD9F4FF),
                start = Offset(105f, 47f),
                end = Offset(91f, 75f),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // Windshield divider
            drawLine(
                color = Color(0xFF168CFF),
                start = Offset(150f, 41f),
                end = Offset(150f, 82f),
                strokeWidth = 3f
            )

            // Front grille
            drawRoundRect(
                color = Color(0xFF182536),
                topLeft = Offset(112f, 112f),
                size = Size(76f, 27f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)
            )

            // Grille detail
            drawLine(
                color = Color(0xFF526782),
                start = Offset(125f, 125f),
                end = Offset(175f, 125f),
                strokeWidth = 2f
            )

            // Left headlight
            drawRoundRect(
                color = if (headlightsOn)
                    Color(0xFFFFF4B0)
                else Color(0xFFB9DDF2),
                topLeft = Offset(57f, 91f),
                size = Size(43f, 21f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f)
            )

            // Right headlight
            drawRoundRect(
                color = if (headlightsOn)
                    Color(0xFFFFF4B0)
                else Color(0xFFB9DDF2),
                topLeft = Offset(200f, 91f),
                size = Size(43f, 21f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f)
            )

            // Headlight glow
            if (headlightsOn) {
                drawCircle(
                    color = Color(0x44FFF4B0),
                    radius = 22f,
                    center = Offset(78f, 101f)
                )
                drawCircle(
                    color = Color(0x44FFF4B0),
                    radius = 22f,
                    center = Offset(222f, 101f)
                )
            }

            // Side mirrors
            drawRoundRect(
                color = Color(0xFF168CFF),
                topLeft = Offset(29f, 83f),
                size = Size(25f, 13f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
            )

            drawRoundRect(
                color = Color(0xFF168CFF),
                topLeft = Offset(246f, 83f),
                size = Size(25f, 13f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
            )

            // Lower bumper
            drawRoundRect(
                color = Color(0xFF0D4F91),
                topLeft = Offset(62f, 146f),
                size = Size(176f, 10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f)
            )

            // Centre emblem
            drawCircle(
                color = Color(0xFF55B8FF),
                radius = 7f,
                center = Offset(150f, 99f),
                style = Stroke(width = 2f)
            )
        }
    }
}
