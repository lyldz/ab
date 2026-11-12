package com.lyldz.ab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.core.nextPlan
import com.lyldz.ab.data.AbRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    today: AbRepository.Today,
    nowMin: Int,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onOpenSettings: () -> Unit,
    openCustom: Boolean = false,
) {
    val s = today.settings
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var custom by rememberSaveable { mutableStateOf(openCustom) }

    fun add(ml: Int) {
        onAdd(ml)
        scope.launch {
            snack.currentSnackbarData?.dismiss()
            val r = snack.showSnackbar(
                context.getString(R.string.added, fmtMl(ml)),
                context.getString(R.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (r == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    val remaining = today.goalMl - today.drankMl
    val plan = nextPlan(nowMin, s.windowStartMin, s.windowEndMin, today.goalMl, today.drankMl, s.cupMl, today.anchorMin)

    Scaffold(snackbarHost = { SnackbarHost(snack) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.settings))
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    when {
                        remaining <= 0 -> Text(stringResource(R.string.home_plan_done), style = MaterialTheme.typography.titleLarge)
                        plan == null -> Text(stringResource(R.string.home_plan_none), style = MaterialTheme.typography.titleLarge)
                        else -> {
                            Text(stringResource(R.string.home_plan_amount, fmtMl(plan.amountMl)), style = MaterialTheme.typography.headlineMedium)
                            Text(
                                stringResource(R.string.home_plan_next, fmtTime(plan.nextAtMin)),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { (today.drankMl.toFloat() / today.goalMl).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                )
                Text(
                    stringResource(R.string.home_progress, fmtMl(today.drankMl), fmtMl(today.goalMl)),
                    style = MaterialTheme.typography.titleMedium,
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
        }
    }

    if (custom) CustomDialog(onDismiss = { custom = false }, onAdd = { custom = false; add(it) })
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
