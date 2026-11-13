package com.lyldz.ab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
        Text(stringResource(R.string.nav_history), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        // yalnızca kayıt olan günler ve bugün; yeni kullanıcıya 29 boş kart gösterilmez
        val shown = stats.filter { it.ml > 0 || it.day == LocalDate.now() }.asReversed()
        if (shown.none { it.ml > 0 }) Text(stringResource(R.string.history_empty), color = Ab.muted)
        shown.forEach { d ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(d.day.format(fmt), fontWeight = FontWeight.SemiBold)
                        Text(
                            "${fmtMl(d.ml)} / ${fmtMl(d.goalMl)} ml" + if (d.reached) "  \u2713" else "",
                            color = if (d.ml > 0) Ab.strong else Ab.faint,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (d.ml.toFloat() / d.goalMl).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = AbColors.Aqua1,
                        trackColor = Ab.line,
                    )
                }
            }
        }
    }
}
