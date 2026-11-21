package com.lyldz.ab.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.lyldz.ab.core.GOAL_WARN_ML
import com.lyldz.ab.core.STOP_BEFORE_BED_MIN
import com.lyldz.ab.core.seriesName
import com.lyldz.ab.core.tempExtraMl
import com.lyldz.ab.data.Settings
import com.lyldz.ab.data.Updater
import com.lyldz.ab.data.WeatherApi
import com.lyldz.ab.data.currentCoarse
import com.lyldz.ab.ui.theme.Ab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

private const val SETTINGS_LAST_END_MIN = 23 * 60 + 30
private const val SETTINGS_MAX_BED_MIN = SETTINGS_LAST_END_MIN + STOP_BEFORE_BED_MIN

/** Değişiklikler geçerliyse yarım saniye sonra kendiliğinden kaydedilir (kaydet düğmesi yok); geçersiz alan kırmızı kalır, kaydedilmez. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(s: Settings, onSave: (Settings) -> Unit, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(s.name) }
    var themeMode by rememberSaveable { mutableIntStateOf(s.themeMode) }
    var language by rememberSaveable { mutableStateOf(s.language) }
    var remindersEnabled by rememberSaveable { mutableStateOf(s.remindersEnabled) }
    var weight by rememberSaveable { mutableStateOf(s.weightKg.toString()) }
    var activityIdx by rememberSaveable { mutableIntStateOf(s.activity.ordinal) }
    var start by rememberSaveable { mutableIntStateOf(s.windowStartMin) }
    var end by rememberSaveable { mutableIntStateOf(s.windowEndMin) }
    var useActivity by rememberSaveable { mutableStateOf(s.useActivity) }
    var useWeather by rememberSaveable { mutableStateOf(s.useWeather) }
    var manual by rememberSaveable { mutableStateOf(s.goalOverrideMl != null) }
    var manualText by rememberSaveable { mutableStateOf((s.goalOverrideMl ?: s.goalMl(LocalDate.now())).toString()) }
    var city by rememberSaveable { mutableStateOf(s.city) }
    var lat by rememberSaveable { mutableStateOf(s.lat) }
    var lon by rememberSaveable { mutableStateOf(s.lon) }
    var cityDialog by remember { mutableStateOf(false) }
    var timeDialog by remember { mutableStateOf<Int?>(null) } // 0 kalkış, 1 yatış
    var locMessage by remember { mutableStateOf(false) }
    var restoredFromBackup by remember { mutableStateOf(false) }
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
        language = language,
        remindersEnabled = remindersEnabled,
        weightKg = kg ?: s.weightKg,
        activity = Activity.entries[activityIdx],
        windowStartMin = start,
        windowEndMin = end,
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
    val latestDraft by rememberUpdatedState(draft)
    val latestSaved by rememberUpdatedState(s)
    val latestValid by rememberUpdatedState(valid)
    val latestOnSave by rememberUpdatedState(onSave)
    val latestRestored by rememberUpdatedState(restoredFromBackup)
    // otomatik kaydet: kullanıcı kaydet düğmesine basmayı unutup sekme değiştirince ayar kaybolmasın
    LaunchedEffect(draft, valid) {
        if (valid && draft != s) {
            delay(500)
            onSave(draft)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (!latestRestored && latestValid && latestDraft != latestSaved) {
                latestOnSave(latestDraft)
            }
        }
    }

    val version = remember { Updater.currentVersion(ctx) }
    val series = remember(version) { seriesName(version) }
    val versionBadge = if (series.isEmpty()) version else "$version · $series"

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge)

        // 1. Profil ve Hedef (+ Hava & Konum birleşik)
        SettingsGroup(stringResource(R.string.grp_profile_goal)) {
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

            HorizontalDivider(color = Ab.line)
            Text(stringResource(R.string.onb_goal, fmtMl(goal)), style = MaterialTheme.typography.titleLarge)
            SwitchRow(stringResource(R.string.set_use_activity), useActivity) { useActivity = it }
            SwitchRow(stringResource(R.string.set_use_weather), useWeather) { useWeather = it }
            if (useWeather) {
                val cityText = when {
                    city != null -> city!!
                    lat != null -> stringResource(R.string.weather_city_location)
                    else -> stringResource(R.string.weather_city_none)
                }
                Text(stringResource(R.string.weather_city, cityText), style = MaterialTheme.typography.bodyMedium, color = Ab.strong)
                draft.tempToday(today)?.let {
                    Text(
                        stringResource(R.string.weather_today, it.roundToInt().toString(), fmtMl(tempExtraMl(it))),
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
            }
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
            Text(stringResource(R.string.onb_goal_note), style = MaterialTheme.typography.bodySmall, color = Ab.muted)
        }

        // 2. Gün Düzeni ve Hatırlatmalar (TimePicker + Hatırlatma aç/kapat + İzinler & Pil)
        SettingsGroup(stringResource(R.string.grp_day_reminders)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeCard(stringResource(R.string.onb_from), start, Modifier.weight(1f)) { timeDialog = 0 }
                TimeCard(stringResource(R.string.onb_to), (end + STOP_BEFORE_BED_MIN) % 1440, Modifier.weight(1f)) { timeDialog = 1 }
            }
            Text(
                stringResource(R.string.window_note, fmtTime(start), fmtTime(end)),
                style = MaterialTheme.typography.bodySmall,
                color = Ab.muted,
            )
            SwitchRow(stringResource(R.string.set_reminders), remindersEnabled) { remindersEnabled = it }
            HorizontalDivider(color = Ab.line)
            PermissionsContent()
        }

        // 3. Görünüm
        SettingsGroup(stringResource(R.string.grp_look)) {
            Text(stringResource(R.string.set_theme), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark).forEachIndexed { i, label ->
                    FilterChip(selected = themeMode == i, onClick = { themeMode = i }, label = { Text(stringResource(label)) })
                }
            }
            Text(stringResource(R.string.set_language), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("" to R.string.lang_system, "tr" to R.string.lang_tr, "en" to R.string.lang_en).forEach { (tag, label) ->
                    FilterChip(
                        selected = language == tag,
                        onClick = {
                            if (language != tag) {
                                language = tag
                                val next = draft.copy(language = tag)
                                if (valid) onSave(next)
                                if (AppLocale.apply(ctx, tag)) {
                                    (ctx as? android.app.Activity)?.recreate()
                                }
                            }
                        },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
        }

        // 4. Veri ve Yedekleme (katlanabilir)
        CollapsibleSettingsGroup(title = stringResource(R.string.grp_data)) {
            BackupSection(onRestored = {
                restoredFromBackup = true
                onBack()
            })
        }

        // 5. Uygulama ve Hakkında (katlanabilir, tekil sürüm etiketi)
        CollapsibleSettingsGroup(title = stringResource(R.string.grp_app), badge = versionBadge) {
            UpdateSection(showCurrentVersion = false)
            HorizontalDivider(color = Ab.line)
            Text(stringResource(R.string.about_license), style = MaterialTheme.typography.bodySmall, color = Ab.muted)
            Text(stringResource(R.string.about_source), style = MaterialTheme.typography.bodySmall, color = Ab.muted)
        }

        Text(
            stringResource(if (valid) R.string.settings_autosave else R.string.settings_invalid),
            style = MaterialTheme.typography.bodySmall,
            color = if (valid) Ab.muted else MaterialTheme.colorScheme.error,
        )
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
    timeDialog?.let { which ->
        val bed = end + STOP_BEFORE_BED_MIN
        TimePickDialog(
            initialMin = (if (which == 0) start else bed) % 1440,
            onDismiss = { timeDialog = null },
            onPick = { h, m ->
                timeDialog = null
                val picked = h * 60 + m
                if (which == 0) {
                    start = picked.coerceIn(0, (end - 60).coerceAtLeast(0))
                } else {
                    val b = if (picked < start + 3 * 60) picked + 1440 else picked
                    val clampedBed = b.coerceIn(start + 3 * 60, SETTINGS_MAX_BED_MIN)
                    end = (clampedBed - STOP_BEFORE_BED_MIN).coerceIn(start + 60, SETTINGS_LAST_END_MIN)
                }
            },
        )
    }
}

/** Ayarlar'da gruplanmış bölüm: başlık + içerik, kenarlıklı yuvarlak kart. */
@Composable
internal fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surface).border(1.dp, Ab.line, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

/** Seyrek kullanılan ayar grupları için dokununca açılan katlanabilir kart. */
@Composable
private fun CollapsibleSettingsGroup(
    title: String,
    badge: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    val toggleDesc = stringResource(if (expanded) R.string.collapse_section else R.string.expand_section)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surface).border(1.dp, Ab.line, shape),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .clickable(role = Role.Button) { expanded = !expanded }
                .semantics(mergeDescendants = true) { contentDescription = "$title, $toggleDesc" }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (badge != null) {
                Text(badge, style = MaterialTheme.typography.bodySmall, color = Ab.muted)
            }
            Text(if (expanded) "\u25B4" else "\u25BE", color = Ab.muted, style = MaterialTheme.typography.titleMedium)
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
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
