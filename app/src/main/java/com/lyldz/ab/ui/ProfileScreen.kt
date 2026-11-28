package com.lyldz.ab.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.core.Activity
import com.lyldz.ab.core.tempExtraMl
import com.lyldz.ab.data.Settings
import com.lyldz.ab.data.currentCoarse
import com.lyldz.ab.ui.theme.Ab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Ana ekranın sağ üstündeki profil düğmesinden açılır: kişisel bilgiler (isim, kilo, hareket),
 * günlük su hedefi (otomatik veya elle) ve konum/hava durumu ayarları burada yönetilir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(s: Settings, onSave: (Settings) -> Unit, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(s.name) }
    var weight by rememberSaveable { mutableStateOf(s.weightKg.toString()) }
    var activityIdx by rememberSaveable { mutableIntStateOf(s.activity.ordinal) }
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
        weightKg = kg ?: s.weightKg,
        activity = Activity.entries[activityIdx],
        useActivity = useActivity,
        useWeather = useWeather,
        goalOverrideMl = if (manual) manualMl else null,
        city = city,
        lat = lat,
        lon = lon,
        tempC = if (placeChanged) null else s.tempC,
        tempDay = if (placeChanged) null else s.tempDay,
    )
    val today = LocalDate.now()
    val goal = draft.goalMl(today)
    val valid = weightOk && (!manual || manualMl != null)
    val latestDraft by rememberUpdatedState(draft)
    val latestSaved by rememberUpdatedState(s)
    val latestValid by rememberUpdatedState(valid)
    val latestOnSave by rememberUpdatedState(onSave)

    LaunchedEffect(draft, valid) {
        if (valid && draft != s) {
            delay(500)
            onSave(draft)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (latestValid && latestDraft != latestSaved) {
                latestOnSave(latestDraft)
            }
        }
    }

    val backDesc = stringResource(R.string.back_desc)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Ab.card).border(1.dp, Ab.line, CircleShape)
                    .clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = backDesc },
                contentAlignment = Alignment.Center,
            ) {
                Text("\u2190", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ab.strong)
            }
            Text(
                stringResource(R.string.grp_profile_goal),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Ab.strong,
            )
        }

        SettingsGroup(stringResource(R.string.grp_profile)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = { Text(stringResource(R.string.set_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter(Char::isDigit).take(3) },
                label = { Text(stringResource(R.string.onb_weight)) },
                isError = !weightOk,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.onb_activity), style = MaterialTheme.typography.titleSmall)
            ActivityChips(activityIdx) { activityIdx = it }
        }

        SettingsGroup(stringResource(R.string.grp_goal)) {
            Text(stringResource(R.string.onb_goal_vol, fmtVol(goal, s.unit)), style = MaterialTheme.typography.titleLarge)
            SwitchRow(stringResource(R.string.set_use_activity), useActivity, stringResource(R.string.desc_use_activity)) { useActivity = it }
            SwitchRow(stringResource(R.string.set_use_weather), useWeather, stringResource(R.string.desc_use_weather)) { useWeather = it }
            if (useWeather) {
                HorizontalDivider(color = Ab.line)
                val cityText = when {
                    city != null -> city!!
                    lat != null -> stringResource(R.string.weather_city_location)
                    else -> stringResource(R.string.weather_city_none)
                }
                Text(stringResource(R.string.weather_city, cityText), style = MaterialTheme.typography.bodyMedium, color = Ab.strong)
                draft.tempToday(today)?.let {
                    Text(
                        stringResource(R.string.weather_today_vol, it.roundToInt().toString(), fmtVol(tempExtraMl(it), s.unit)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Ab.muted,
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
                Text(stringResource(R.string.weather_credit), color = Ab.muted, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(color = Ab.line)
            }
            SwitchRow(stringResource(R.string.set_override), manual, stringResource(R.string.desc_override)) { manual = it }
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
            if (goal > s.goalWarnMl) {
                Text(stringResource(R.string.set_goal_warn_dyn, fmtVol(s.goalWarnMl, s.unit)), color = MaterialTheme.colorScheme.tertiary)
            }
            Text(stringResource(R.string.onb_goal_note), style = MaterialTheme.typography.bodySmall, color = Ab.muted)
        }

        Text(
            stringResource(if (valid) R.string.settings_autosave else R.string.settings_invalid),
            style = MaterialTheme.typography.bodySmall,
            color = if (valid) Ab.muted else MaterialTheme.colorScheme.error,
        )
    }

    if (cityDialog) {
        CityDialog(
            timeoutMs = s.networkTimeoutSec * 1_000,
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
