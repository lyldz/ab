package com.lyldz.ab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.core.DayStat
import com.lyldz.ab.core.summarize
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(stats: List<DayStat>, days: Int, onDays: (Int) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val sum = summarize(stats)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(days == 7, { onDays(7) }, { Text(stringResource(R.string.stats_week)) })
            FilterChip(days == 30, { onDays(30) }, { Text(stringResource(R.string.stats_month)) })
        }

        if (stats.isEmpty() || sum.recordedDays == 0) {
            Text(stringResource(R.string.stats_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard(stringResource(R.string.stats_avg), "${fmtMl(sum.avgMl)} ml", null, Modifier.weight(1f))
                SummaryCard(
                    stringResource(R.string.stats_reached),
                    stringResource(R.string.stats_days_fmt, sum.reachedDays.toString(), sum.recordedDays.toString()),
                    null,
                    Modifier.weight(1f),
                )
                sum.best?.let {
                    SummaryCard(
                        stringResource(R.string.stats_best),
                        "${fmtMl(it.ml)} ml",
                        it.day.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())),
                        Modifier.weight(1f),
                    )
                }
            }

            val desc = stringResource(R.string.stats_chart_desc)
            BarChart(stats, Modifier.fillMaxWidth().height(180.dp).semantics { contentDescription = desc })
            Row(Modifier.fillMaxWidth()) {
                stats.forEachIndexed { i, d ->
                    val label = if (days == 7) {
                        d.day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    } else if ((stats.size - 1 - i) % 5 == 0) {
                        d.day.dayOfMonth.toString()
                    } else {
                        ""
                    }
                    Text(
                        label,
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Text(
                stringResource(R.string.stats_legend),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, sub: String?, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleMedium)
            if (sub != null) Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Her gün bir çubuk (hedefe ulaşıldıysa tam renk); arkasındaki soluk zemin o günün hedef yüksekliğidir. */
@Composable
private fun BarChart(stats: List<DayStat>, modifier: Modifier) {
    val bar = MaterialTheme.colorScheme.primary
    val dim = bar.copy(alpha = 0.45f)
    val axis = MaterialTheme.colorScheme.surfaceVariant
    val maxV = (stats.maxOf { maxOf(it.ml, it.goalMl) } * 1.1f).coerceAtLeast(1f)
    Canvas(modifier) {
        val slot = size.width / stats.size
        val barW = slot * 0.6f
        drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
        stats.forEachIndexed { i, d ->
            val x = i * slot + (slot - barW) / 2f
            val goalH = size.height * (d.goalMl / maxV)
            drawRoundRect(axis.copy(alpha = 0.6f), Offset(x, size.height - goalH), Size(barW, goalH), CornerRadius(4.dp.toPx()))
            val h = size.height * (d.ml / maxV)
            drawRoundRect(if (d.reached) bar else dim, Offset(x, size.height - h), Size(barW, h), CornerRadius(4.dp.toPx()))
        }
    }
}
