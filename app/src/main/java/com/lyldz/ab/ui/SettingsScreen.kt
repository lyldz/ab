package com.lyldz.ab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.core.STOP_BEFORE_BED_MIN
import com.lyldz.ab.core.seriesName
import com.lyldz.ab.data.Settings
import com.lyldz.ab.data.Updater
import com.lyldz.ab.data.WeatherApi
import com.lyldz.ab.ui.theme.Ab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SETTINGS_LAST_END_MIN = 23 * 60 + 30
private const val SETTINGS_MAX_BED_MIN = SETTINGS_LAST_END_MIN + STOP_BEFORE_BED_MIN

/**
 * Uygulama ayarları (4 sade grup): Gün düzeni ve hatırlatmalar, Görünüm (Tema/Dil/Birim),
 * katlanabilir Veri ve yedekleme, katlanabilir Uygulama ve hakkında.
 * Kişisel bilgiler, hedef ve konum ana ekranın sağ üstündeki Profil ekranındadır.
 */
@Composable
fun SettingsScreen(s: Settings, onSave: (Settings) -> Unit, onBack: () -> Unit) {
    var themeMode by rememberSaveable { mutableIntStateOf(s.themeMode) }
    var language by rememberSaveable { mutableStateOf(s.language) }
    var unit by rememberSaveable { mutableIntStateOf(s.unit) }
    var remindersEnabled by rememberSaveable { mutableStateOf(s.remindersEnabled) }
    var start by rememberSaveable { mutableIntStateOf(s.windowStartMin) }
    var end by rememberSaveable { mutableIntStateOf(s.windowEndMin) }
    var timeDialog by remember { mutableStateOf<Int?>(null) } // 0 kalkış, 1 yatış
    var restoredFromBackup by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    BackHandler(onBack = onBack)

    val draft = s.copy(
        themeMode = themeMode,
        language = language,
        unit = unit,
        remindersEnabled = remindersEnabled,
        windowStartMin = start,
        windowEndMin = end,
    )
    val latestDraft by rememberUpdatedState(draft)
    val latestSaved by rememberUpdatedState(s)
    val latestOnSave by rememberUpdatedState(onSave)
    val latestRestored by rememberUpdatedState(restoredFromBackup)

    LaunchedEffect(draft) {
        if (draft != s) {
            delay(500)
            onSave(draft)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (!latestRestored && latestDraft != latestSaved) {
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

        // 1. Gün Düzeni ve Hatırlatmalar
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

        // 2. Görünüm (Tema, Dil, Birim)
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
                                onSave(draft.copy(language = tag))
                                if (AppLocale.apply(ctx, tag)) {
                                    (ctx as? android.app.Activity)?.recreate()
                                }
                            }
                        },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            Text(stringResource(R.string.set_unit), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(UNIT_ML to R.string.unit_ml, UNIT_OZ to R.string.unit_oz).forEach { (u, label) ->
                    FilterChip(
                        selected = unit == u,
                        onClick = { unit = u },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
        }

        // 3. Veri ve Yedekleme (katlanabilir)
        CollapsibleSettingsGroup(title = stringResource(R.string.grp_data)) {
            BackupSection(onRestored = {
                restoredFromBackup = true
                onBack()
            })
        }

        // 4. Uygulama ve Hakkında (katlanabilir, tekil sürüm etiketi)
        CollapsibleSettingsGroup(title = stringResource(R.string.grp_app), badge = versionBadge) {
            UpdateSection(showCurrentVersion = false)
            HorizontalDivider(color = Ab.line)
            Text(stringResource(R.string.about_license), style = MaterialTheme.typography.bodySmall, color = Ab.muted)
            Text(stringResource(R.string.about_source), style = MaterialTheme.typography.bodySmall, color = Ab.muted)
        }

        Text(
            stringResource(R.string.settings_autosave),
            style = MaterialTheme.typography.bodySmall,
            color = Ab.muted,
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
internal fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
