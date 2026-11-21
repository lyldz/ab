package com.lyldz.ab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.core.DayStat
import com.lyldz.ab.ui.theme.Ab
import com.lyldz.ab.ui.theme.AbColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Son 30 günün günlük toplamları, en yeni üstte. */
@Composable
fun HistoryScreen(stats: List<DayStat>, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val fmt = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.nav_history), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ab.strong)
        // yalnızca kayıt olan günler ve bugün; yeni kullanıcıya 29 boş kart gösterilmez
        val shown = stats.filter { it.ml > 0 || it.day == LocalDate.now() }.asReversed()
        if (shown.none { it.ml > 0 }) Text(stringResource(R.string.history_empty), color = Ab.muted)
        shown.forEach { d ->
            val dayText = d.day.format(fmt)
            val desc = if (d.reached) {
                stringResource(R.string.history_day_reached, dayText, fmtMl(d.ml), fmtMl(d.goalMl))
            } else {
                stringResource(R.string.history_day_progress, dayText, fmtMl(d.ml), fmtMl(d.goalMl))
            }
            Card(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = desc },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (d.reached) AbColors.Aqua1.copy(alpha = if (Ab.isDark) 0.45f else 0.70f) else Ab.line),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(dayText, fontWeight = FontWeight.SemiBold, color = Ab.strong)
                        Text(
                            "${fmtMl(d.ml)} / ${fmtMl(d.goalMl)} ml" + if (d.reached) "  \u2713" else "",
                            fontWeight = if (d.reached) FontWeight.SemiBold else FontWeight.Normal,
                            color = when {
                                d.reached -> MaterialTheme.colorScheme.primary
                                d.ml > 0 -> Ab.strong
                                else -> Ab.faint
                            },
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (d.ml.toFloat() / d.goalMl.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp)),
                        color = if (d.reached) AbColors.Teal else AbColors.Aqua1,
                        trackColor = Ab.line,
                    )
                }
            }
        }
    }
}
