package com.lyldz.ab.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.ui.theme.Ab
import com.lyldz.ab.ui.theme.AbColors

/** Bugün, Grafikler, Geçmiş, Ayarlar. Simgeler bağımlılıksız, Canvas ile çizilir. */
@Composable
fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Ab.strong,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = AbColors.Aqua1.copy(alpha = 0.30f),
        unselectedIconColor = Ab.faint,
        unselectedTextColor = Ab.faint,
    )
    NavigationBar(containerColor = Ab.card.copy(alpha = 0.94f), tonalElevation = 0.dp) {
        NavigationBarItem(tab == 0, { onTab(0) }, { DropIcon() }, label = { Text(stringResource(R.string.nav_home)) }, colors = colors)
        NavigationBarItem(tab == 1, { onTab(1) }, { ChartIcon() }, label = { Text(stringResource(R.string.nav_stats)) }, colors = colors)
        NavigationBarItem(tab == 2, { onTab(2) }, { ClockIcon() }, label = { Text(stringResource(R.string.nav_history)) }, colors = colors)
        NavigationBarItem(tab == 3, { onTab(3) }, { PersonIcon() }, label = { Text(stringResource(R.string.settings)) }, colors = colors)
    }
}

@Composable
private fun DropIcon() {
    val c = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val k = size.width / 24f
        val p = Path().apply {
            moveTo(12 * k, 3 * k)
            cubicTo(13.5f * k, 7 * k, 19 * k, 10.5f * k, 19 * k, 15 * k)
            cubicTo(19 * k, 18.6f * k, 15.9f * k, 21 * k, 12 * k, 21 * k)
            cubicTo(8.1f * k, 21 * k, 5 * k, 18.6f * k, 5 * k, 15 * k)
            cubicTo(5 * k, 10.5f * k, 10.5f * k, 7 * k, 12 * k, 3 * k)
            close()
        }
        drawPath(p, c, style = Stroke(2 * k))
    }
}

@Composable
private fun ChartIcon() {
    val c = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val k = size.width / 24f
        listOf(Triple(4f, 13f, 7f), Triple(10f, 7f, 13f), Triple(16f, 3f, 17f)).forEach { (x, y, h) ->
            drawRoundRect(c, Offset(x * k, y * k), Size(4 * k, h * k), CornerRadius(1 * k))
        }
    }
}

@Composable
private fun ClockIcon() {
    val c = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val k = size.width / 24f
        drawCircle(c, 9 * k, Offset(12 * k, 12 * k), style = Stroke(2 * k))
        drawLine(c, Offset(12 * k, 7 * k), Offset(12 * k, 12 * k), 2 * k, StrokeCap.Round)
        drawLine(c, Offset(12 * k, 12 * k), Offset(15.5f * k, 14 * k), 2 * k, StrokeCap.Round)
    }
}

@Composable
private fun PersonIcon() {
    val c = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val k = size.width / 24f
        drawCircle(c, 4 * k, Offset(12 * k, 8 * k), style = Stroke(2 * k))
        val body = Path().apply {
            moveTo(4.5f * k, 20 * k)
            cubicTo(4.5f * k, 15.5f * k, 8 * k, 14 * k, 12 * k, 14 * k)
            cubicTo(16 * k, 14 * k, 19.5f * k, 15.5f * k, 19.5f * k, 20 * k)
        }
        drawPath(body, c, style = Stroke(2 * k, cap = StrokeCap.Round))
    }
}
