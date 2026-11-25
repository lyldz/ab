package com.lyldz.ab.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.InputChip
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
import com.lyldz.ab.core.isValidCupMl
import com.lyldz.ab.core.seriesName
import com.lyldz.ab.data.Settings
import com.lyldz.ab.data.Updater
import com.lyldz.ab.data.WeatherApi
import com.lyldz.ab.ui.theme.Ab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private const val SETTINGS_LAST_END_MIN = 23 * 60 + 30

/**
 * Uygulama ayarları (6 kategori): Gün düzeni ve hatırlatmalar, Bardak & Widget, Görünüm ve etkileşim,
 * katlanabilir Hedef ve istatistik gelişmiş ayarları, katlanabilir Veri/Yedekleme/Sıfırlama,
 * katlanabilir Uygulama ve hakkında.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    s: Settings,
    onSave: (Settings) -> Unit,
    onReset: () -> Unit = {},
    onBack: () -> Unit,
) {
    var themeMode by rememberSaveable { mutableIntStateOf(s.themeMode) }
    var language by rememberSaveable { mutableStateOf(s.language) }
    var unit by rememberSaveable { mutableIntStateOf(s.unit) }
    var remindersEnabled by rememberSaveable { mutableStateOf(s.remindersEnabled) }
    var statusNotificationEnabled by rememberSaveable { mutableStateOf(s.statusNotificationEnabled) }
    var weatherNotificationEnabled by rememberSaveable { mutableStateOf(s.weatherNotificationEnabled) }
    var preferExactAlarm by rememberSaveable { mutableStateOf(s.preferExactAlarm) }
    var customIntervalEnabled by rememberSaveable { mutableStateOf(s.customReminderIntervalMin != null) }
    var customIntervalMin by rememberSaveable { mutableIntStateOf(s.customReminderIntervalMin ?: 60) }
    var stopBeforeBedMin by rememberSaveable { mutableIntStateOf(s.stopBeforeBedMin) }
    var notifSecondActionMl by rememberSaveable { mutableIntStateOf(s.notifSecondActionMl) }
    var start by rememberSaveable { mutableIntStateOf(s.windowStartMin) }
    var end by rememberSaveable { mutableIntStateOf(s.windowEndMin) }

    var cupMl by rememberSaveable { mutableIntStateOf(s.cupMl) }
    var cupPresets by remember { mutableStateOf(s.cupPresets) }
    var newPresetText by remember { mutableStateOf("") }
    var widgetAmounts by remember { mutableStateOf(s.widgetQuickAmounts) }
    var newWidgetText by remember { mutableStateOf("") }
    var tileToastEnabled by rememberSaveable { mutableStateOf(s.tileToastEnabled) }

    var motionWater by rememberSaveable { mutableStateOf(s.motionWater) }
    var waterWaveAnimation by rememberSaveable { mutableStateOf(s.waterWaveAnimation) }
    var showOverflowEffect by rememberSaveable { mutableStateOf(s.showOverflowEffect) }
    var showHints by rememberSaveable { mutableStateOf(s.showHints) }
    var undoBannerMs by rememberSaveable { mutableIntStateOf(s.undoBannerMs) }

    var mlPerKg by rememberSaveable { mutableIntStateOf(s.mlPerKg) }
    var minGoalMl by rememberSaveable { mutableIntStateOf(s.minGoalMl) }
    var goalWarnMl by rememberSaveable { mutableIntStateOf(s.goalWarnMl) }
    var paceToleranceMl by rememberSaveable { mutableIntStateOf(s.paceToleranceMl) }
    var defaultStatsDays by rememberSaveable { mutableIntStateOf(s.defaultStatsDays) }
    var historyDays by rememberSaveable { mutableIntStateOf(s.historyDays) }
    var streakWindowDays by rememberSaveable { mutableIntStateOf(s.streakWindowDays) }

    var timeDialog by remember { mutableStateOf<Int?>(null) } // 0 kalkış, 1 yatış
    var resetDialog by remember { mutableStateOf(false) }
    var restoredFromBackup by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    BackHandler(onBack = onBack)

    // Dışarıdan sıfırlama/geri yükleme yapıldığında yerel alanları güncelle
    LaunchedEffect(s) {
        themeMode = s.themeMode
        language = s.language
        unit = s.unit
        remindersEnabled = s.remindersEnabled
        statusNotificationEnabled = s.statusNotificationEnabled
        weatherNotificationEnabled = s.weatherNotificationEnabled
        preferExactAlarm = s.preferExactAlarm
        customIntervalEnabled = s.customReminderIntervalMin != null
        customIntervalMin = s.customReminderIntervalMin ?: 60
        stopBeforeBedMin = s.stopBeforeBedMin
        notifSecondActionMl = s.notifSecondActionMl
        start = s.windowStartMin
        end = s.windowEndMin
        cupMl = s.cupMl
        cupPresets = s.cupPresets
        widgetAmounts = s.widgetQuickAmounts
        tileToastEnabled = s.tileToastEnabled
        motionWater = s.motionWater
        waterWaveAnimation = s.waterWaveAnimation
        showOverflowEffect = s.showOverflowEffect
        showHints = s.showHints
        undoBannerMs = s.undoBannerMs
        mlPerKg = s.mlPerKg
        minGoalMl = s.minGoalMl
        goalWarnMl = s.goalWarnMl
        paceToleranceMl = s.paceToleranceMl
        defaultStatsDays = s.defaultStatsDays
        historyDays = s.historyDays
        streakWindowDays = s.streakWindowDays
    }

    val draft = s.copy(
        themeMode = themeMode,
        language = language,
        unit = unit,
        remindersEnabled = remindersEnabled,
        statusNotificationEnabled = statusNotificationEnabled,
        weatherNotificationEnabled = weatherNotificationEnabled,
        preferExactAlarm = preferExactAlarm,
        customReminderIntervalMin = if (customIntervalEnabled) customIntervalMin else null,
        stopBeforeBedMin = stopBeforeBedMin,
        notifSecondActionMl = notifSecondActionMl,
        windowStartMin = start,
        windowEndMin = end,
        cupMl = cupMl,
        cupPresets = cupPresets,
        widgetQuickAmounts = widgetAmounts,
        tileToastEnabled = tileToastEnabled,
        motionWater = motionWater,
        waterWaveAnimation = waterWaveAnimation,
        showOverflowEffect = showOverflowEffect,
        showHints = showHints,
        undoBannerMs = undoBannerMs,
        mlPerKg = mlPerKg,
        minGoalMl = minGoalMl,
        goalWarnMl = goalWarnMl,
        paceToleranceMl = paceToleranceMl,
        defaultStatsDays = defaultStatsDays,
        historyDays = historyDays,
        streakWindowDays = streakWindowDays,
    )
    val latestDraft by rememberUpdatedState(draft)
    val latestSaved by rememberUpdatedState(s)
    val latestOnSave by rememberUpdatedState(onSave)
    val latestRestored by rememberUpdatedState(restoredFromBackup)
    var langSaved by remember { mutableStateOf(false) }

    LaunchedEffect(draft) {
        if (draft != s) {
            delay(150)
            onSave(draft)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (!latestRestored && !langSaved && latestDraft != latestSaved) {
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
                TimeCard(stringResource(R.string.onb_to), (end + stopBeforeBedMin) % 1440, Modifier.weight(1f)) { timeDialog = 1 }
            }
            Text(
                stringResource(R.string.window_note_dyn, fmtTime(start), fmtTime(end), stopBeforeBedMin),
                style = MaterialTheme.typography.bodySmall,
                color = Ab.muted,
            )
            SettingStepper(
                label = stringResource(R.string.set_stop_before_bed),
                description = stringResource(R.string.desc_stop_before_bed),
                value = stringResource(R.string.minutes_fmt, stopBeforeBedMin),
                onMinus = {
                    val bed = end + stopBeforeBedMin
                    val next = (stopBeforeBedMin - 15).coerceIn(0, 240)
                    stopBeforeBedMin = next
                    end = (bed - next).coerceIn(start + 60, SETTINGS_LAST_END_MIN)
                },
                onPlus = {
                    val bed = end + stopBeforeBedMin
                    val next = (stopBeforeBedMin + 15).coerceIn(0, 240)
                    stopBeforeBedMin = next
                    end = (bed - next).coerceIn(start + 60, SETTINGS_LAST_END_MIN)
                },
            )
            HorizontalDivider(color = Ab.line)
            SwitchRow(stringResource(R.string.set_reminders), remindersEnabled, stringResource(R.string.desc_reminders)) {
                remindersEnabled = it
            }
            SwitchRow(stringResource(R.string.set_status_notif), statusNotificationEnabled, stringResource(R.string.desc_status_notif)) {
                statusNotificationEnabled = it
            }
            SwitchRow(stringResource(R.string.set_weather_notif), weatherNotificationEnabled, stringResource(R.string.desc_weather_notif)) {
                weatherNotificationEnabled = it
            }
            SwitchRow(stringResource(R.string.set_exact_alarm), preferExactAlarm, stringResource(R.string.desc_exact_alarm)) {
                preferExactAlarm = it
            }
            SwitchRow(stringResource(R.string.set_custom_interval), customIntervalEnabled, stringResource(R.string.desc_custom_interval)) {
                customIntervalEnabled = it
            }
            if (customIntervalEnabled) {
                SettingStepper(
                    label = stringResource(R.string.set_custom_interval_val),
                    description = stringResource(R.string.desc_custom_interval_val),
                    value = stringResource(R.string.minutes_fmt, customIntervalMin),
                    onMinus = { customIntervalMin = (customIntervalMin - 5).coerceIn(15, 240) },
                    onPlus = { customIntervalMin = (customIntervalMin + 5).coerceIn(15, 240) },
                )
            }
            SettingStepper(
                label = stringResource(R.string.set_notif_second_ml),
                description = stringResource(R.string.desc_notif_second_ml),
                value = fmtVol(notifSecondActionMl, unit),
                onMinus = { notifSecondActionMl = (notifSecondActionMl - 50).coerceIn(50, 2000) },
                onPlus = { notifSecondActionMl = (notifSecondActionMl + 50).coerceIn(50, 2000) },
            )
            HorizontalDivider(color = Ab.line)
            PermissionsContent()
        }

        // 2. Bardak, Widget ve Hızlı Ayarlar
        SettingsGroup(stringResource(R.string.grp_vessels_widget)) {
            SettingStepper(
                label = stringResource(R.string.set_cup),
                description = stringResource(R.string.desc_cup),
                value = fmtVol(cupMl, unit),
                onMinus = { cupMl = (cupMl - 25).coerceIn(50, 2000) },
                onPlus = { cupMl = (cupMl + 25).coerceIn(50, 2000) },
            )
            HorizontalDivider(color = Ab.line)
            SettingLabel(stringResource(R.string.set_cup_presets), stringResource(R.string.desc_cup_presets))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cupPresets.forEach { ml ->
                    InputChip(
                        selected = ml == cupMl,
                        onClick = {
                            if (cupPresets.size > 1) cupPresets = cupPresets - ml
                        },
                        label = { Text("${fmtVol(ml, unit)} ×") },
                    )
                }
            }
            val parsedPreset = newPresetText.toIntOrNull()?.takeIf(::isValidCupMl)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newPresetText,
                    onValueChange = { newPresetText = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.set_add_preset_hint)) },
                    suffix = { Text(stringResource(R.string.unit_ml)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = {
                        parsedPreset?.let { ml ->
                            if (ml !in cupPresets && cupPresets.size < 8) {
                                cupPresets = (cupPresets + ml).distinct().sorted()
                                newPresetText = ""
                            }
                        }
                    },
                    enabled = parsedPreset != null && parsedPreset !in cupPresets && cupPresets.size < 8,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.ok_add)) }
            }
            HorizontalDivider(color = Ab.line)
            SettingLabel(stringResource(R.string.set_widget_amounts), stringResource(R.string.desc_widget_amounts))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                widgetAmounts.forEach { ml ->
                    InputChip(
                        selected = false,
                        onClick = {
                            if (widgetAmounts.size > 1) widgetAmounts = widgetAmounts - ml
                        },
                        label = { Text("${fmtVol(ml, unit)} ×") },
                    )
                }
            }
            val parsedWidget = newWidgetText.toIntOrNull()?.takeIf(::isValidCupMl)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newWidgetText,
                    onValueChange = { newWidgetText = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.set_add_preset_hint)) },
                    suffix = { Text(stringResource(R.string.unit_ml)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = {
                        parsedWidget?.let { ml ->
                            if (ml !in widgetAmounts && widgetAmounts.size < 3) {
                                widgetAmounts = (widgetAmounts + ml).distinct().sorted()
                                newWidgetText = ""
                            }
                        }
                    },
                    enabled = parsedWidget != null && parsedWidget !in widgetAmounts && widgetAmounts.size < 3,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.ok_add)) }
            }
            HorizontalDivider(color = Ab.line)
            SwitchRow(stringResource(R.string.set_tile_toast), tileToastEnabled, stringResource(R.string.desc_tile_toast)) {
                tileToastEnabled = it
            }
        }

        // 3. Görünüm ve Etkileşim
        SettingsGroup(stringResource(R.string.grp_look)) {
            SettingLabel(stringResource(R.string.set_theme), stringResource(R.string.desc_theme))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark).forEachIndexed { i, label ->
                    FilterChip(selected = themeMode == i, onClick = { themeMode = i }, label = { Text(stringResource(label)) })
                }
            }
            SettingLabel(stringResource(R.string.set_language), stringResource(R.string.desc_language))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("" to R.string.lang_system, "tr" to R.string.lang_tr, "en" to R.string.lang_en).forEach { (tag, label) ->
                    FilterChip(
                        selected = language == tag,
                        onClick = {
                            if (language != tag) {
                                language = tag
                                langSaved = true
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
            SettingLabel(stringResource(R.string.set_unit), stringResource(R.string.desc_unit))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(UNIT_ML to R.string.unit_ml, UNIT_OZ to R.string.unit_oz).forEach { (u, label) ->
                    FilterChip(
                        selected = unit == u,
                        onClick = { unit = u },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            HorizontalDivider(color = Ab.line)
            SwitchRow(stringResource(R.string.set_motion), motionWater, stringResource(R.string.desc_motion)) {
                motionWater = it
            }
            SwitchRow(stringResource(R.string.set_water_wave), waterWaveAnimation, stringResource(R.string.desc_water_wave)) {
                waterWaveAnimation = it
            }
            SwitchRow(stringResource(R.string.set_overflow_effect), showOverflowEffect, stringResource(R.string.desc_overflow_effect)) {
                showOverflowEffect = it
            }
            SwitchRow(stringResource(R.string.set_show_hints), showHints, stringResource(R.string.desc_show_hints)) {
                showHints = it
            }
            SettingStepper(
                label = stringResource(R.string.set_undo_duration),
                description = stringResource(R.string.desc_undo_duration),
                value = String.format(Locale.US, "%.1f s", undoBannerMs / 1000.0),
                onMinus = { undoBannerMs = (undoBannerMs - 200).coerceIn(1000, 10000) },
                onPlus = { undoBannerMs = (undoBannerMs + 200).coerceIn(1000, 10000) },
            )
        }

        // 4. Hedef ve İstatistik Gelişmiş Ayarları (katlanabilir)
        CollapsibleSettingsGroup(title = stringResource(R.string.grp_advanced)) {
            SettingStepper(
                label = stringResource(R.string.set_ml_per_kg),
                description = stringResource(R.string.desc_ml_per_kg),
                value = "$mlPerKg ml/kg",
                onMinus = { mlPerKg = (mlPerKg - 1).coerceIn(15, 50) },
                onPlus = { mlPerKg = (mlPerKg + 1).coerceIn(15, 50) },
            )
            SettingStepper(
                label = stringResource(R.string.set_min_goal),
                description = stringResource(R.string.desc_min_goal),
                value = fmtVol(minGoalMl, unit),
                onMinus = { minGoalMl = (minGoalMl - 100).coerceIn(500, 3000) },
                onPlus = { minGoalMl = (minGoalMl + 100).coerceIn(500, 3000) },
            )
            SettingStepper(
                label = stringResource(R.string.set_goal_warn_threshold),
                description = stringResource(R.string.desc_goal_warn_threshold),
                value = fmtVol(goalWarnMl, unit),
                onMinus = { goalWarnMl = (goalWarnMl - 250).coerceIn(2000, 8000) },
                onPlus = { goalWarnMl = (goalWarnMl + 250).coerceIn(2000, 8000) },
            )
            SettingStepper(
                label = stringResource(R.string.set_pace_tolerance),
                description = stringResource(R.string.desc_pace_tolerance),
                value = fmtVol(paceToleranceMl, unit),
                onMinus = { paceToleranceMl = (paceToleranceMl - 10).coerceIn(0, 500) },
                onPlus = { paceToleranceMl = (paceToleranceMl + 10).coerceIn(0, 500) },
            )
            HorizontalDivider(color = Ab.line)
            SettingLabel(stringResource(R.string.set_default_stats_days), stringResource(R.string.desc_default_stats_days))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = defaultStatsDays == 7, onClick = { defaultStatsDays = 7 }, label = { Text(stringResource(R.string.stats_week)) })
                FilterChip(selected = defaultStatsDays == 30, onClick = { defaultStatsDays = 30 }, label = { Text(stringResource(R.string.stats_month)) })
            }
            SettingStepper(
                label = stringResource(R.string.set_history_days),
                description = stringResource(R.string.desc_history_days),
                value = stringResource(R.string.days_fmt, historyDays),
                onMinus = { historyDays = (historyDays - 5).coerceIn(7, 365) },
                onPlus = { historyDays = (historyDays + 5).coerceIn(7, 365) },
            )
            SettingStepper(
                label = stringResource(R.string.set_streak_window),
                description = stringResource(R.string.desc_streak_window),
                value = stringResource(R.string.days_fmt, streakWindowDays),
                onMinus = { streakWindowDays = (streakWindowDays - 15).coerceIn(30, 365) },
                onPlus = { streakWindowDays = (streakWindowDays + 15).coerceIn(30, 365) },
            )
        }

        // 5. Veri, Yedekleme ve Sıfırlama (katlanabilir)
        CollapsibleSettingsGroup(title = stringResource(R.string.grp_data)) {
            BackupSection(onRestored = {
                restoredFromBackup = true
                onBack()
            })
            HorizontalDivider(color = Ab.line)
            SettingLabel(stringResource(R.string.reset_defaults_title), stringResource(R.string.reset_defaults_desc))
            OutlinedButton(
                onClick = { resetDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.reset_defaults_btn))
            }
        }

        // 6. Uygulama ve Hakkında (katlanabilir, tekil sürüm etiketi)
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

    if (resetDialog) {
        AlertDialog(
            onDismissRequest = { resetDialog = false },
            title = { Text(stringResource(R.string.reset_confirm_title)) },
            text = { Text(stringResource(R.string.reset_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        resetDialog = false
                        restoredFromBackup = true
                        onReset()
                        if (AppLocale.apply(ctx, "")) {
                            (ctx as? android.app.Activity)?.recreate()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.reset_defaults_btn)) }
            },
            dismissButton = {
                TextButton(onClick = { resetDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    timeDialog?.let { which ->
        val maxBed = SETTINGS_LAST_END_MIN + stopBeforeBedMin
        val bed = end + stopBeforeBedMin
        TimePickDialog(
            initialMin = (if (which == 0) start else bed) % 1440,
            onDismiss = { timeDialog = null },
            onPick = { h, m ->
                timeDialog = null
                val picked = h * 60 + m
                if (which == 0) {
                    start = picked.coerceIn(0, (end - 60).coerceAtLeast(0))
                } else {
                    val minBed = start + 60 + stopBeforeBedMin
                    val b = if (picked < minBed) picked + 1440 else picked
                    val clampedBed = b.coerceIn(minBed, maxBed)
                    end = (clampedBed - stopBeforeBedMin).coerceIn(start + 60, SETTINGS_LAST_END_MIN)
                }
            },
        )
    }
}

/** Ayar başlığı ve altında kısa açıklama metni. */
@Composable
internal fun SettingLabel(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(description, style = MaterialTheme.typography.bodySmall, color = Ab.muted)
    }
}

/** Açıklamalı sayısal ayar artır/azalt satırı. */
@Composable
internal fun SettingStepper(
    label: String,
    description: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Stepper(
            label = label,
            value = value,
            minusDesc = stringResource(R.string.less),
            plusDesc = stringResource(R.string.more),
            onMinus = onMinus,
            onPlus = onPlus,
        )
        Text(description, style = MaterialTheme.typography.bodySmall, color = Ab.muted)
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
internal fun SwitchRow(
    label: String,
    checked: Boolean,
    description: String? = null,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = Ab.muted)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
