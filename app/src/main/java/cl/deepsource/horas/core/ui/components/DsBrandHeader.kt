package cl.deepsource.horas.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DsBrandHeader(
    modifier: Modifier = Modifier,
    iconSize: Dp = 38.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(iconSize)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = (minOf(w, h) / 2f) * 0.88f
            val strokeWidthPx = 2.5.dp.toPx()

            // Outer Hexagon
            val path = Path().apply {
                for (i in 0 until 6) {
                    val angleRad = Math.toRadians((60 * i - 90).toDouble())
                    val x = cx + (r * Math.cos(angleRad)).toFloat()
                    val y = cy + (r * Math.sin(angleRad)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }

            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            // Inner hexagon follows the inset geometry of the reference logo.
            val inner = Path().apply {
                for (i in 0 until 6) {
                    val angle = Math.toRadians((60 * i - 90).toDouble())
                    val x = cx + (r * 0.67f * Math.cos(angle)).toFloat()
                    val y = cy + (r * 0.67f * Math.sin(angle)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(inner, Color.White, style = Stroke(width = strokeWidthPx, join = StrokeJoin.Round))
            // Inner 3D Cube Y-lines from center (cx, cy)
            // Line 1: Center to top
            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(cx, cy),
                end = androidx.compose.ui.geometry.Offset(cx, cy - r),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )

            // Line 2: Center to bottom-right
            val x2 = cx + (r * Math.cos(Math.toRadians(30.0))).toFloat()
            val y2 = cy + (r * Math.sin(Math.toRadians(30.0))).toFloat()
            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(cx, cy),
                end = androidx.compose.ui.geometry.Offset(x2, y2),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )

            // Line 3: Center to bottom-left
            val x3 = cx + (r * Math.cos(Math.toRadians(150.0))).toFloat()
            val y3 = cy + (r * Math.sin(Math.toRadians(150.0))).toFloat()
            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(cx, cy),
                end = androidx.compose.ui.geometry.Offset(x3, y3),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )
        }

        Spacer(Modifier.width(10.dp))

        Column {
            Text(
                text = "DEEP",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                letterSpacing = 0.sp,
            )
            Text(
                text = "SOURCE",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                letterSpacing = 0.sp,
            )
        }
    }
}

