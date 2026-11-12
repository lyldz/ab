package com.lyldz.ab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.core.PACE_TOLERANCE_ML
import com.lyldz.ab.core.nextPlan
import com.lyldz.ab.core.paceMl
import com.lyldz.ab.core.roundTo10
import com.lyldz.ab.core.tempExtraMl
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.data.Intake
import com.lyldz.ab.data.SOURCE_NOTIFICATION
import com.lyldz.ab.data.SOURCE_WIDGET
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    today: AbRepository.Today,
    nowMin: Int,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onDelete: (Intake) -> Unit,
    onRestore: (Intake) -> Unit,
    onEdit: (Intake, Int, Int) -> Unit,
    onSetCup: (Int) -> Unit,
    openCustom: Boolean = false,
) {
    val s = today.settings
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var custom by rememberSaveable { mutableStateOf(openCustom) }
    var editing by remember { mutableStateOf<Intake?>(null) }
    var cupDialog by remember { mutableStateOf(false) }

    fun showUndo(message: String, undo: () -> Unit) {
        scope.launch {
            snack.currentSnackbarData?.dismiss()
            val r = snack.showSnackbar(message, context.getString(R.string.undo), duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) undo()
        }
    }

    fun add(ml: Int) {
        onAdd(ml)
        showUndo(context.getString(R.string.added, fmtMl(ml)), onUndo)
    }

    val remaining = today.goalMl - today.drankMl
    val showPace = remaining > 0 && nowMin in s.windowStartMin..s.windowEndMin
    val pace = paceMl(today.goalMl, s.windowStartMin, s.windowEndMin, nowMin)
    val plan = nextPlan(nowMin, s.windowStartMin, s.windowEndMin, today.goalMl, today.drankMl, s.cupMl, today.anchorMin)

    Scaffold(snackbarHost = { SnackbarHost(snack) }, contentWindowInsets = WindowInsets.statusBars) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    when {
                        remaining <= 0 -> Text(stringResource(R.string.home_plan_done), style = MaterialTheme.typography.titleLarge)
                        plan == null -> Text(stringResource(R.string.home_plan_none), style = MaterialTheme.typography.titleLarge)
                        else -> {
                            val diff = pace - today.drankMl
                            Text(
                                when {
                                    diff >= PACE_TOLERANCE_ML -> stringResource(R.string.pace_behind, fmtMl(roundTo10(diff)))
                                    diff <= -PACE_TOLERANCE_ML -> stringResource(R.string.pace_ahead, fmtMl(roundTo10(-diff)))
                                    else -> stringResource(R.string.pace_on)
                                },
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            // sıradaki hatırlatma saatine kadar tempoda olmak için gereken toplam
                            val cum = paceMl(today.goalMl, s.windowStartMin, s.windowEndMin, plan.nextAtMin)
                            val need = cum - today.drankMl
                            Text(
                                if (need > 0) {
                                    stringResource(R.string.pace_checkpoint, fmtTime(plan.nextAtMin), fmtMl(cum), fmtMl(roundTo10(need)))
                                } else {
                                    stringResource(R.string.pace_checkpoint_done, fmtTime(plan.nextAtMin), fmtMl(cum))
                                },
                            )
                        }
                    }
                }
            }

            val fraction = (today.drankMl.toFloat() / today.goalMl).coerceIn(0f, 1f)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Card(
                    Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Stat(stringResource(R.string.stat_goal), fmtMl(today.goalMl))
                        Stat(stringResource(R.string.stat_drank), fmtMl(today.drankMl))
                        Stat(stringResource(R.string.stat_left), fmtMl(remaining.coerceAtLeast(0)))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${fmtMl(today.goalMl)} ml", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    WaterOrb(
                        fraction,
                        if (showPace) pace.toFloat() / today.goalMl else null,
                        stringResource(R.string.home_progress, fmtMl(today.drankMl), fmtMl(today.goalMl)),
                        Modifier.size(170.dp),
                    )
                    Text("${(fraction * 100).roundToInt()}%", style = MaterialTheme.typography.titleLarge)
                    if (showPace) {
                        Text(
                            stringResource(R.string.pitcher_legend),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(150.dp),
                        )
                    }
                }
            }

            OutlinedButton(onClick = { cupDialog = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cup_button, fmtMl(s.cupMl)))
            }

            val temp = s.tempToday(LocalDate.now())
            if (s.useWeather && temp != null) {
                val extra = tempExtraMl(temp)
                val degrees = temp.roundToInt().toString()
                Text(
                    if (extra > 0 && s.goalOverrideMl == null) stringResource(R.string.home_weather, degrees, fmtMl(extra))
                    else stringResource(R.string.home_weather_none, degrees),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(s.cupMl, 250, 500, 750, 1000).distinct().forEach { ml ->
                    FilledTonalButton(onClick = { add(ml) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.add_ml, fmtMl(ml)))
                    }
                }
                OutlinedButton(onClick = { custom = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.add_custom))
                }
            }

            Text(stringResource(R.string.records_title), style = MaterialTheme.typography.titleLarge)
            if (today.entries.isEmpty()) {
                Text(stringResource(R.string.records_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            today.entries.forEach { e ->
                EntryRow(
                    e,
                    onEdit = { editing = e },
                    onDelete = {
                        onDelete(e)
                        showUndo(context.getString(R.string.deleted, fmtMl(e.ml))) { onRestore(e) }
                    },
                )
            }
        }
    }

    if (cupDialog) CupDialog(s.cupMl, onDismiss = { cupDialog = false }, onPick = { cupDialog = false; onSetCup(it) })
    if (custom) CustomDialog(onDismiss = { custom = false }, onAdd = { custom = false; add(it) })
    editing?.let { e ->
        EditDialog(e, onDismiss = { editing = null }, onSave = { ml, minute -> editing = null; onEdit(e, ml, minute) })
    }
}

private fun minuteOfDay(atMs: Long): Int =
    Instant.ofEpochMilli(atMs).atZone(ZoneId.systemDefault()).let { it.hour * 60 + it.minute }

@Composable
private fun EntryRow(e: Intake, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val more = stringResource(R.string.more_options)
    val source = stringResource(
        when (e.source) {
            SOURCE_NOTIFICATION -> R.string.src_notification
            SOURCE_WIDGET -> R.string.src_widget
            else -> R.string.src_app
        },
    )
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${fmtTime(minuteOfDay(e.atMs))}  ·  ${fmtMl(e.ml)} ml", style = MaterialTheme.typography.titleMedium)
                Text(source, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Box {
                TextButton(
                    onClick = { menu = true },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = more },
                ) { Text("⋮") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.menu_edit)) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.menu_delete)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun EditDialog(e: Intake, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var text by remember { mutableStateOf(e.ml.toString()) }
    var minute by remember { mutableIntStateOf(minuteOfDay(e.atMs)) }
    val ml = text.toIntOrNull()?.takeIf { it in 1..5000 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(4) },
                    suffix = { Text("ml") },
                    isError = ml == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Stepper(
                    stringResource(R.string.edit_time), fmtTime(minute),
                    stringResource(R.string.earlier), stringResource(R.string.later),
                    { minute = (minute - 5).coerceIn(0, 1439) }, { minute = (minute + 5).coerceIn(0, 1439) },
                )
            }
        },
        confirmButton = { TextButton(onClick = { ml?.let { onSave(it, minute) } }, enabled = ml != null) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun CustomDialog(onDismiss: () -> Unit, onAdd: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val ml = text.toIntOrNull()?.takeIf { it in 1..5000 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(4) },
                suffix = { Text("ml") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = { TextButton(onClick = { ml?.let(onAdd) }, enabled = ml != null) { Text(stringResource(R.string.ok_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text("$value ml", style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CupDialog(current: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    var custom by remember { mutableStateOf("") }
    val customMl = custom.toIntOrNull()?.takeIf { it in 50..1000 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cup_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(100, 150, 200, 250, 300, 400, 500).forEach { ml ->
                        val selected = ml == current
                        Column(
                            Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(ml) }).padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.size(56.dp).clip(CircleShape).background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                contentAlignment = Alignment.Center,
                            ) { CupIcon(ml) }
                            Text("${fmtMl(ml)} ml", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.cup_custom_hint)) },
                    suffix = { Text("ml") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = { TextButton(onClick = { customMl?.let(onPick) }, enabled = customMl != null) { Text(stringResource(R.string.cup_set)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
