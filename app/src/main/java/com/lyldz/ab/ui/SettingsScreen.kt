package com.lyldz.ab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.core.Activity
import com.lyldz.ab.core.GOAL_WARN_ML
import com.lyldz.ab.data.Settings
import java.time.LocalDate

/** Değişiklikler yerelde tutulur; yalnızca "Kaydet" yazar, geri/vazgeç atar. */
@Composable
fun SettingsScreen(s: Settings, onSave: (Settings) -> Unit, onBack: () -> Unit) {
    var weight by rememberSaveable { mutableStateOf(s.weightKg.toString()) }
    var activityIdx by rememberSaveable { mutableIntStateOf(s.activity.ordinal) }
    var start by rememberSaveable { mutableIntStateOf(s.windowStartMin) }
    var end by rememberSaveable { mutableIntStateOf(s.windowEndMin) }
    var cup by rememberSaveable { mutableIntStateOf(s.cupMl) }
    var useActivity by rememberSaveable { mutableStateOf(s.useActivity) }
    var useWeather by rememberSaveable { mutableStateOf(s.useWeather) }
    var manual by rememberSaveable { mutableStateOf(s.goalOverrideMl != null) }
    var manualText by rememberSaveable { mutableStateOf((s.goalOverrideMl ?: s.goalMl(LocalDate.now())).toString()) }
    BackHandler(onBack = onBack)

    val kg = weight.toIntOrNull()
    val weightOk = kg != null && kg in 30..250
    val manualMl = manualText.toIntOrNull()?.takeIf { it in 500..8000 }
    val draft = s.copy(
        weightKg = kg ?: s.weightKg,
        activity = Activity.entries[activityIdx],
        windowStartMin = start,
        windowEndMin = end,
        cupMl = cup,
        useActivity = useActivity,
        useWeather = useWeather,
        goalOverrideMl = if (manual) manualMl else null,
    )
    val goal = draft.goalMl(LocalDate.now())
    val valid = weightOk && (!manual || manualMl != null)
    val earlier = stringResource(R.string.earlier)
    val later = stringResource(R.string.later)

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge)

        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it.filter(Char::isDigit).take(3) },
            label = { Text(stringResource(R.string.onb_weight)) },
            isError = !weightOk,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.onb_activity), style = MaterialTheme.typography.titleMedium)
        ActivityChips(activityIdx) { activityIdx = it }
        SwitchRow(stringResource(R.string.set_use_activity), useActivity) { useActivity = it }
        SwitchRow(stringResource(R.string.set_use_weather), useWeather) { useWeather = it }

        Text(stringResource(R.string.onb_window), style = MaterialTheme.typography.titleMedium)
        Stepper(stringResource(R.string.onb_from), fmtTime(start), earlier, later,
            { start = (start - 30).coerceIn(0, end - 60) }, { start = (start + 30).coerceIn(0, end - 60) })
        Stepper(stringResource(R.string.onb_to), fmtTime(end), earlier, later,
            { end = (end - 30).coerceIn(start + 60, 23 * 60 + 30) }, { end = (end + 30).coerceIn(start + 60, 23 * 60 + 30) })

        Stepper(stringResource(R.string.set_cup), "${fmtMl(cup)} ml", stringResource(R.string.less), stringResource(R.string.more),
            { cup = (cup - 50).coerceIn(100, 1000) }, { cup = (cup + 50).coerceIn(100, 1000) })

        Text(stringResource(R.string.onb_goal, fmtMl(goal)), style = MaterialTheme.typography.titleLarge)
        SwitchRow(stringResource(R.string.set_override), manual) { manual = it }
        if (manual) {
            OutlinedTextField(
                value = manualText,
                onValueChange = { manualText = it.filter(Char::isDigit).take(4) },
                label = { Text(stringResource(R.string.set_goal_ml)) },
                isError = manualMl == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (goal > GOAL_WARN_ML) {
            Text(stringResource(R.string.set_goal_warn), color = MaterialTheme.colorScheme.tertiary)
        }
        Text(stringResource(R.string.onb_goal_note), color = MaterialTheme.colorScheme.onSurfaceVariant)

        Button(
            onClick = { onSave(draft) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text(stringResource(R.string.save)) }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(R.string.cancel))
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
