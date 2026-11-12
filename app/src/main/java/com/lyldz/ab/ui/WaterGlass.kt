package com.lyldz.ab.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Hafif eğimli, alt köşeleri yuvarlak bir bardak silueti. */
private fun glassPath(w: Float, h: Float): Path = Path().apply {
    val t = h * 0.03f
    val b = h * 0.97f
    val rad = w * 0.09f
    val l1 = w * 0.06f
    val r1 = w * 0.94f
    val l2 = w * 0.20f
    val r2 = w * 0.80f
    moveTo(l1, t)
    lineTo(r1, t)
    lineTo(r2, b - rad)
    quadraticBezierTo(r2, b, r2 - rad, b)
    lineTo(l2 + rad, b)
    quadraticBezierTo(l2, b, l2, b - rad)
    close()
}

/** Dolan bardak: seviye yaylı animasyonla yükselir, su yüzeyi sürekli dalgalanır. */
@Composable
fun WaterGlass(fraction: Float, description: String, modifier: Modifier = Modifier) {
    val level by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "level",
    )
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "phase",
    )
    val water = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier.semantics { contentDescription = description }) {
        val glass = glassPath(size.width, size.height)
        drawPath(glass, track)
        if (level > 0.001f) {
            val top = size.height * 0.03f
            val bottom = size.height * 0.97f
            val surfaceY = bottom - (bottom - top) * level
            val amp = 4.dp.toPx()
            val wave = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, surfaceY)
                var x = 0f
                while (x <= size.width) {
                    lineTo(x, surfaceY + amp * sin(x / size.width * 2f * PI.toFloat() * 1.5f + phase))
                    x += 4f
                }
                lineTo(size.width, size.height)
                close()
            }
            clipPath(glass) {
                drawPath(wave, Brush.verticalGradient(listOf(water.copy(alpha = 0.65f), water), startY = surfaceY, endY = bottom))
            }
        }
        drawPath(glass, line, style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round))
    }
}

/** Bardak boyutunu gösteren küçük simge; büyük hacim daha büyük çizilir. */
@Composable
fun CupIcon(ml: Int, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.onSurface
    Canvas(modifier.size(36.dp)) {
        val s = 0.55f + 0.45f * (ml / 500f).coerceAtMost(1f)
        val w = size.width * s
        val h = size.height * s
        translate((size.width - w) / 2f, size.height - h) {
            drawPath(glassPath(w, h), line, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
        }
    }
}
