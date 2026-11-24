package com.lyldz.ab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.core.DayStat
import com.lyldz.ab.ui.theme.Ab
import com.lyldz.ab.ui.theme.AbColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Son 30 günün günlük toplamları, en yeni üstte. Geçmiş günlere unutulan kayıt eklenebilir. */
@Composable
fun HistoryScreen(
    stats: List<DayStat>,
    onBack: () -> Unit,
    defaultCupMl: Int = 200,
    unit: Int = UNIT_ML,
    onAddForDay: ((LocalDate, Int, Int) -> Unit)? = null,
) {
    BackHandler(onBack = onBack)
    val fmt = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    val today = stats.lastOrNull()?.day ?: LocalDate.now()
    var addingDay by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.nav_history),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Ab.strong,
            )
            if (onAddForDay != null) {
                OutlinedButton(
                    onClick = { addingDay = today.minusDays(1) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.history_add_past))
                }
            }
        }
        // yalnızca kayıt olan günler ve bugün; yeni kullanıcıya 29 boş kart gösterilmez
        val shown = stats.filter { it.ml > 0 || it.day == today }.asReversed()
        if (shown.none { it.ml > 0 }) Text(stringResource(R.string.history_empty), color = Ab.muted)
        shown.forEach { d ->
            val dayText = d.day.format(fmt)
            val desc = if (d.reached) {
                stringResource(R.string.history_day_reached, dayText, fmtMl(d.ml), fmtMl(d.goalMl))
            } else {
                stringResource(R.string.history_day_progress, dayText, fmtMl(d.ml), fmtMl(d.goalMl))
            }
            val shape = RoundedCornerShape(20.dp)
            Card(
                Modifier.fillMaxWidth()
                    .clip(shape)
                    .let { m -> if (onAddForDay != null) m.clickable(role = Role.Button) { addingDay = d.day } else m }
                    .semantics(mergeDescendants = true) { contentDescription = desc },
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (d.reached) AbColors.Aqua1.copy(alpha = if (Ab.isDark) 0.45f else 0.70f) else Ab.line),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(dayText, fontWeight = FontWeight.SemiBold, color = Ab.strong)
                        Text(
                            "${fmtVolNum(d.ml, unit)} / ${fmtVol(d.goalMl, unit)}" + if (d.reached) "  \u2713" else "",
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

    if (onAddForDay != null) {
        addingDay?.let { initialDay ->
            PastDayAddDialog(
                today = today,
                initialDay = initialDay,
                defaultCupMl = defaultCupMl,
                onDismiss = { addingDay = null },
                onAdd = { day, ml, min ->
                    addingDay = null
                    onAddForDay(day, ml, min)
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PastDayAddDialog(
    today: LocalDate,
    initialDay: LocalDate,
    defaultCupMl: Int,
    onDismiss: () -> Unit,
    onAdd: (LocalDate, Int, Int) -> Unit,
) {
    val chipFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val recentDays = remember(today) { (0L..6L).map { today.minusDays(it) } }
    var selectedDay by remember(initialDay) { mutableStateOf(initialDay) }
    var text by remember { mutableStateOf(defaultCupMl.toString()) }
    var minute by remember { mutableIntStateOf(12 * 60) }
    val ml = text.toIntOrNull()?.takeIf { it in 1..5000 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.history_pick_day), style = MaterialTheme.typography.labelLarge, color = Ab.muted)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recentDays.forEach { d ->
                        FilterChip(
                            selected = d == selectedDay,
                            onClick = { selectedDay = d },
                            label = { Text(d.format(chipFmt)) },
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val mlUnit = stringResource(R.string.unit_ml)
                    listOf(200, 250, 330, 500).forEach { preset ->
                        FilterChip(
                            selected = text == preset.toString(),
                            onClick = { text = preset.toString() },
                            label = { Text("$preset $mlUnit") },
                        )
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(4) },
                    suffix = { Text(stringResource(R.string.unit_ml)) },
                    isError = ml == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Stepper(
                    stringResource(R.string.edit_time),
                    fmtTime(minute),
                    stringResource(R.string.earlier),
                    stringResource(R.string.later),
                    { minute = (minute - 15).coerceIn(0, 1439) },
                    { minute = (minute + 15).coerceIn(0, 1439) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { ml?.let { onAdd(selectedDay, it, minute) } }, enabled = ml != null) {
                Text(stringResource(R.string.ok_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
