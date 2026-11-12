package com.lyldz.ab.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.core.Activity
import com.lyldz.ab.core.dailyGoalMl
import com.lyldz.ab.data.Settings

private val needsNotifPermission = Build.VERSION.SDK_INT >= 33

/** Adım 0: bilgiler. Sonraki adımlar: her izin ayrı bir ekranda, tek tek istenir. */
@Composable
fun OnboardingScreen(onDone: (Settings) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var weight by rememberSaveable { mutableStateOf("70") }
    var activityIdx by rememberSaveable { mutableIntStateOf(0) }
    var start by rememberSaveable { mutableIntStateOf(6 * 60) }
    var end by rememberSaveable { mutableIntStateOf(18 * 60) }
    val activity = Activity.entries[activityIdx]
    val kg = weight.toIntOrNull()
    val valid = kg != null && kg in 30..250

    fun build() = Settings(weightKg = kg ?: 70, activity = activity, windowStartMin = start, windowEndMin = end)
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onDone(build()) // izin verilse de verilmese de devam
    }
    BackHandler(enabled = step == 1) { step = 0 }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (step == 0) {
            Text(stringResource(R.string.onb_title), style = MaterialTheme.typography.headlineLarge)

            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter(Char::isDigit).take(3) },
                label = { Text(stringResource(R.string.onb_weight)) },
                isError = !valid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.onb_activity), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Activity.entries.forEachIndexed { i, a ->
                    FilterChip(
                        selected = i == activityIdx,
                        onClick = { activityIdx = i },
                        label = { Text(stringResource(activityLabel(a))) },
                    )
                }
            }

            Text(stringResource(R.string.onb_window), style = MaterialTheme.typography.titleMedium)
            TimeStepper(stringResource(R.string.onb_from), start) { start = it.coerceIn(0, end - 60) }
            TimeStepper(stringResource(R.string.onb_to), end) { end = it.coerceIn(start + 60, 23 * 60 + 30) }

            Text(
                stringResource(R.string.onb_goal, fmtMl(dailyGoalMl(kg ?: 70, activity, null))),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(stringResource(R.string.onb_goal_note), color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(
                onClick = { if (needsNotifPermission) step = 1 else onDone(build()) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(if (needsNotifPermission) R.string.onb_next else R.string.onb_start)) }
        } else {
            Text(stringResource(R.string.perm_notif_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.onb_notif_why), style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.perm_allow)) }
            OutlinedButton(
                onClick = { onDone(build()) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.perm_skip)) }
        }
    }
}

fun activityLabel(a: Activity): Int = when (a) {
    Activity.NONE -> R.string.activity_none
    Activity.MEDIUM -> R.string.activity_medium
    Activity.HIGH -> R.string.activity_high
}

@Composable
private fun TimeStepper(label: String, minutes: Int, onChange: (Int) -> Unit) {
    val earlier = stringResource(R.string.earlier)
    val later = stringResource(R.string.later)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$label  ${fmtTime(minutes)}", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(
            onClick = { onChange(minutes - 30) },
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = earlier },
        ) { Text("\u2212") }
        OutlinedButton(
            onClick = { onChange(minutes + 30) },
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = later },
        ) { Text("+") }
    }
}
