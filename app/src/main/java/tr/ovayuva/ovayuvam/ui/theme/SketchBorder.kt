package tr.ovayuva.ovayuvam.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.min

fun Modifier.sketchSurface(
    fill: Color,
    border: Color,
    cornerRadius: Dp = 8.dp,
    seed: Int = 0,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(fill)
    .drawBehind {
        val inset = 2.dp.toPx()
        val radius = cornerRadius.toPx()
        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset
        if (right <= left || bottom <= top) return@drawBehind
        val path = Path()
        val steps = 9
        fun wobble(index: Int): Float {
            val value = (seed * 1103515245 + index * 12345).absoluteValue
            return ((value % 100) / 100f - 0.5f) * 1.6.dp.toPx()
        }
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, startIndex: Int) {
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val x = x1 + (x2 - x1) * t + wobble(startIndex + i)
                val y = y1 + (y2 - y1) * t + wobble(startIndex + i + 31)
                if (startIndex == 0 && i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
        }
        val r = min(radius, min((right - left) / 2f, (bottom - top) / 2f))
        line(left + r, top, right - r, top, 0)
        line(right, top + r, right, bottom - r, 20)
        line(right - r, bottom, left + r, bottom, 40)
        line(left, bottom - r, left, top + r, 60)
        path.close()
        drawPath(
            path,
            border,
            style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round),
        )
    }
