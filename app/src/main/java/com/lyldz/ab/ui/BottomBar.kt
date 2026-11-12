package com.lyldz.ab.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R

/** Ana sayfa, İstatistik, Ayarlar. Simgeler bağımlılıksız, Canvas ile çizilir. */
@Composable
fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(tab == 0, { onTab(0) }, { HomeIcon() }, label = { Text(stringResource(R.string.nav_home)) })
        NavigationBarItem(tab == 1, { onTab(1) }, { ChartIcon() }, label = { Text(stringResource(R.string.nav_stats)) })
        NavigationBarItem(tab == 2, { onTab(2) }, { SlidersIcon() }, label = { Text(stringResource(R.string.settings)) })
    }
}

@Composable
private fun HomeIcon() {
    val c = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val k = size.width / 24f
        val p = Path().apply {
            moveTo(12 * k, 3 * k); lineTo(3 * k, 11 * k); lineTo(5 * k, 11 * k); lineTo(5 * k, 20 * k)
            lineTo(10 * k, 20 * k); lineTo(10 * k, 14 * k); lineTo(14 * k, 14 * k); lineTo(14 * k, 20 * k)
            lineTo(19 * k, 20 * k); lineTo(19 * k, 11 * k); lineTo(21 * k, 11 * k); close()
        }
        drawPath(p, c)
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
private fun SlidersIcon() {
    val c = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val k = size.width / 24f
        listOf(6f to 8f, 12f to 16f, 18f to 10f).forEach { (y, knob) ->
            drawLine(c, Offset(3 * k, y * k), Offset(21 * k, y * k), 2 * k, StrokeCap.Round)
            drawCircle(c, 2.8f * k, Offset(knob * k, y * k))
        }
    }
}
