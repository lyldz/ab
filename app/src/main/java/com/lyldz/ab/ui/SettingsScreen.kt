package com.lyldz.ab.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.core.Activity
import com.lyldz.ab.core.GOAL_WARN_ML
import com.lyldz.ab.core.tempExtraMl
import com.lyldz.ab.data.Settings
import com.lyldz.ab.data.WeatherApi
import com.lyldz.ab.data.currentCoarse
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/** Değişiklikler yerelde tutulur; yalnızca "Kaydet" yazar, geri/vazgeç atar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(s: Settings, onSave: (Settings) -> Unit, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(s.name) }
    var themeMode by rememberSaveable { mutableIntStateOf(s.themeMode) }
    var weight by rememberSaveable { mutableStateOf(s.weightKg.toString()) }
    var activityIdx by rememberSaveable { mutableIntStateOf(s.activity.ordinal) }
    var start by rememberSaveable { mutableIntStateOf(s.windowStartMin) }
    var end by rememberSaveable { mutableIntStateOf(s.windowEndMin) }
    var cup by rememberSaveable { mutableIntStateOf(s.cupMl) }
    var useActivity by rememberSaveable { mutableStateOf(s.useActivity) }
    var useWeather by rememberSaveable { mutableStateOf(s.useWeather) }
    var manual by rememberSaveable { mutableStateOf(s.goalOverrideMl != null) }
    var manualText by rememberSaveable { mutableStateOf((s.goalOverrideMl ?: s.goalMl(LocalDate.now())).toString()) }
    var city by rememberSaveable { mutableStateOf(s.city) }
    var lat by rememberSaveable { mutableStateOf(s.lat) }
    var lon by rememberSaveable { mutableStateOf(s.lon) }
    var cityDialog by remember { mutableStateOf(false) }
    var locMessage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    BackHandler(onBack = onBack)

    fun useMyLocation() {
        scope.launch {
            val pos = currentCoarse(ctx)
            locMessage = pos == null
            if (pos != null) {
                lat = pos.first
                lon = pos.second
                city = null
            }
        }
    }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) useMyLocation() else locMessage = true
    }

    val kg = weight.toIntOrNull()
    val weightOk = kg != null && kg in 30..250
    val manualMl = manualText.toIntOrNull()?.takeIf { it in 500..8000 }
    val placeChanged = lat != s.lat || lon != s.lon
    val draft = s.copy(
        name = name.trim(),
        themeMode = themeMode,
        weightKg = kg ?: s.weightKg,
        activity = Activity.entries[activityIdx],
        windowStartMin = start,
        windowEndMin = end,
        cupMl = cup,
        useActivity = useActivity,
        useWeather = useWeather,
        goalOverrideMl = if (manual) manualMl else null,
        city = city,
        lat = lat,
        lon = lon,
        // yer değişince eski sıcaklık geçersiz; kaydedince yeniden çekilir
        tempC = if (placeChanged) null else s.tempC,
        tempDay = if (placeChanged) null else s.tempDay,
    )
    val today = LocalDate.now()
    val goal = draft.goalMl(today)
    val valid = weightOk && (!manual || manualMl != null)
    val earlier = stringResource(R.string.earlier)
    val later = stringResource(R.string.later)

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(30) },
            label = { Text(stringResource(R.string.set_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.set_theme), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark).forEachIndexed { i, label ->
                FilterChip(selected = themeMode == i, onClick = { themeMode = i }, label = { Text(stringResource(label)) })
            }
        }

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

        Text(stringResource(R.string.weather_title), style = MaterialTheme.typography.titleMedium)
        SwitchRow(stringResource(R.string.set_use_weather), useWeather) { useWeather = it }
        val cityText = when {
            city != null -> city!!
            lat != null -> stringResource(R.string.weather_city_location)
            else -> stringResource(R.string.weather_city_none)
        }
        Text(stringResource(R.string.weather_city, cityText), style = MaterialTheme.typography.bodyLarge)
        draft.tempToday(today)?.let {
            Text(
                stringResource(R.string.weather_today, it.roundToInt().toString(), fmtMl(tempExtraMl(it))),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { cityDialog = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.weather_search))
            }
            OutlinedButton(
                onClick = { locLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.weather_use_location)) }
            if (lat != null) {
                TextButton(
                    onClick = { lat = null; lon = null; city = null },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.weather_clear)) }
            }
        }
        if (locMessage) Text(stringResource(R.string.weather_no_location), color = MaterialTheme.colorScheme.tertiary)
        Text(
            stringResource(R.string.weather_credit),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )

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

    if (cityDialog) {
        CityDialog(
            onDismiss = { cityDialog = false },
            onPick = { c ->
                cityDialog = false
                city = c.label
                lat = c.lat
                lon = c.lon
                locMessage = false
            },
        )
    }
}

/** Şehir adı yazılır; yalnızca bu metin Open-Meteo arama servisine gider. */
@Composable
internal fun CityDialog(onDismiss: () -> Unit, onPick: (WeatherApi.City) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherApi.City>?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.city_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(60) },
                    label = { Text(stringResource(R.string.city_query)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        busy = true
                        scope.launch {
                            results = WeatherApi.searchCities(query)
                            busy = false
                        }
                    },
                    enabled = query.trim().length >= 2 && !busy,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.city_search_btn)) }
                results?.let { list ->
                    if (list.isEmpty()) {
                        Text(stringResource(R.string.city_none), color = MaterialTheme.colorScheme.tertiary)
                    }
                    list.forEach { c ->
                        TextButton(onClick = { onPick(c) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(c.label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
