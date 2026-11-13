package com.lyldz.ab.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lyldz.ab.R
import com.lyldz.ab.core.Activity
import com.lyldz.ab.core.STOP_BEFORE_BED_MIN
import com.lyldz.ab.core.baseGoalMl
import com.lyldz.ab.core.baseIntervalMin
import com.lyldz.ab.core.dailyGoalMl
import com.lyldz.ab.core.tempExtraMl
import com.lyldz.ab.data.Settings
import com.lyldz.ab.data.WeatherApi
import com.lyldz.ab.data.currentCoarse
import com.lyldz.ab.ui.theme.Ab
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

private val needsNotifPermission = Build.VERSION.SDK_INT >= 33
private const val STEPS = 5
private const val LAST_END_MIN = 23 * 60 + 30 // su içmenin en geç sonu
private const val MIN_WINDOW_MIN = 60
private const val MAX_BED_MIN = LAST_END_MIN + STOP_BEFORE_BED_MIN // gece yarısından sonra 01:30

/**
 * Kurulum sihirbazı, 5 adım: karşılama, kilo ve hareket, gün düzeni ve bardak, izinler (konum + hava sonucu, pil notu),
 * sana özel hedef özeti. Yatış saati mutlak dakikadır (gece yarısından sonrası 1440 üstü); su içme penceresi
 * kalkış → yatıştan 2 saat önce. İzinler isteğe bağlıdır, hiçbiri engellemez.
 */
@Composable
fun OnboardingScreen(onDone: (Settings) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var weight by rememberSaveable { mutableStateOf("70") }
    var activityIdx by rememberSaveable { mutableIntStateOf(Activity.MEDIUM.ordinal) }
    var wake by rememberSaveable { mutableIntStateOf(6 * 60 + 30) }
    var bed by rememberSaveable { mutableIntStateOf(22 * 60) }
    var cup by rememberSaveable { mutableIntStateOf(200) }
    var notifOk by rememberSaveable {
        mutableStateOf(!needsNotifPermission || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    var locOk by rememberSaveable {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var lon by rememberSaveable { mutableStateOf<Double?>(null) }
    var city by rememberSaveable { mutableStateOf<String?>(null) }
    var temp by rememberSaveable { mutableStateOf<Double?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var cityDialog by remember { mutableStateOf(false) }
    var timeDialog by remember { mutableStateOf<Int?>(null) } // 0 kalkış, 1 yatış

    val kg = weight.toIntOrNull()
    val kgOk = kg != null && kg in 30..250
    val activity = Activity.entries[activityIdx]
    val end = (bed - STOP_BEFORE_BED_MIN).coerceIn(wake + MIN_WINDOW_MIN, LAST_END_MIN)

    fun loadTemp(la: Double, lo: Double, name: String?) {
        scope.launch {
            busy = true
            failed = false
            lat = la; lon = lo; city = name
            temp = WeatherApi.fetchMaxTemp(la, lo)
            failed = temp == null
            busy = false
        }
    }
    fun findMe() {
        scope.launch {
            busy = true
            failed = false
            val pos = currentCoarse(ctx)
            if (pos == null) {
                failed = true
                busy = false
            } else {
                loadTemp(pos.first, pos.second, null)
            }
        }
    }
    fun finish() = onDone(
        Settings(
            weightKg = kg ?: 70, activity = activity, cupMl = cup, windowStartMin = wake, windowEndMin = end,
            lat = lat, lon = lon, city = city,
            tempC = temp, tempDay = if (temp != null) LocalDate.now().toString() else null,
            onboarded = true,
        ),
    )

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = it }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        locOk = granted
        if (granted) findMe() else failed = true
    }

    BackHandler(enabled = step > 0) { step-- }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            OnboardingBottomBar(
                step = step,
                nextEnabled = step != 1 || kgOk,
                onBack = { if (step > 0) step-- },
                onNext = { if (step < STEPS - 1) step++ else finish() },
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            modifier = Modifier.padding(padding).statusBarsPadding().fillMaxSize(),
            label = "onboardingStep",
        ) { s ->
            when (s) {
                0 -> WelcomeStep()
                1 -> BiometricsStep(weight, kgOk, { weight = it }, activityIdx) { activityIdx = it }
                2 -> RoutineStep(
                    wake = wake, bed = bed, end = end, cup = cup,
                    onPickTime = { timeDialog = it }, onCup = { cup = it },
                )
                3 -> PermissionsStep(
                    notifOk = notifOk, locOk = locOk, temp = temp, city = city, busy = busy, failed = failed,
                    onNotif = {
                        if (needsNotifPermission) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else notifOk = true
                    },
                    onLoc = { locLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                    onCity = { cityDialog = true },
                )
                else -> SummaryStep(kg ?: 70, activity, temp, wake, end, cup)
            }
        }
    }

    if (cityDialog) {
        CityDialog(onDismiss = { cityDialog = false }, onPick = { c -> cityDialog = false; loadTemp(c.lat, c.lon, c.label) })
    }
    timeDialog?.let { which ->
        TimePickDialog(
            initialMin = (if (which == 0) wake else bed) % 1440,
            onDismiss = { timeDialog = null },
            onPick = { h, m ->
                timeDialog = null
                val picked = h * 60 + m
                if (which == 0) {
                    wake = picked.coerceAtMost(MAX_BED_MIN - 3 * 60)
                    bed = bed.coerceAtLeast(wake + 3 * 60)
                } else {
                    // yatış, kalkıştan en az 3 saat sonra olmalı; küçükse ertesi gece yarısından sonrası sayılır
                    val b = if (picked < wake + 3 * 60) picked + 1440 else picked
                    bed = b.coerceIn(wake + 3 * 60, MAX_BED_MIN)
                }
            },
        )
    }
}

@Composable
private fun OnboardingBottomBar(step: Int, nextEnabled: Boolean, onBack: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp)) {
        if (step > 0) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.Center) {
                repeat(STEPS - 1) { i ->
                    Box(
                        Modifier.padding(horizontal = 4.dp).size(if (i + 1 == step) 10.dp else 8.dp).clip(CircleShape)
                            .background(if (i + 1 <= step) MaterialTheme.colorScheme.primary else Ab.line),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) {
                TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 52.dp)) { Text(stringResource(R.string.onb_step_back)) }
                Spacer(Modifier.weight(1f))
            }
            Button(
                onClick = onNext, enabled = nextEnabled,
                modifier = (if (step > 0) Modifier else Modifier.fillMaxWidth()).heightIn(min = 52.dp),
            ) {
                Text(
                    stringResource(
                        when (step) {
                            0 -> R.string.onb_start_button
                            STEPS - 1 -> R.string.onb_go
                            else -> R.string.onb_step_continue
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painterResource(if (Ab.isDark) R.drawable.ic_wordmark_light else R.drawable.ic_wordmark),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.height(96.dp),
        )
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.onb_welcome_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.onb_tagline), style = MaterialTheme.typography.bodyLarge, color = Ab.muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StepColumn(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
}

@Composable
private fun BiometricsStep(weight: String, kgOk: Boolean, onWeight: (String) -> Unit, activityIdx: Int, onActivity: (Int) -> Unit) {
    StepColumn {
        Text(stringResource(R.string.onb_bio_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.onb_bio_sub), color = Ab.muted)
        OutlinedTextField(
            value = weight,
            onValueChange = { onWeight(it.filter(Char::isDigit).take(3)) },
            label = { Text(stringResource(R.string.onb_weight)) },
            isError = !kgOk,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(60, 70, 80, 90, 100).forEach { q ->
                FilterChip(selected = weight == q.toString(), onClick = { onWeight(q.toString()) }, label = { Text("$q") })
            }
        }
        Text(stringResource(R.string.onb_act_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Activity.entries.forEachIndexed { i, a ->
            val sel = activityIdx == i
            val (title, desc) = when (a) {
                Activity.NONE -> R.string.onb_act_none_t to R.string.onb_act_none_d
                Activity.MEDIUM -> R.string.onb_act_med_t to R.string.onb_act_med_d
                Activity.HIGH -> R.string.onb_act_high_t to R.string.onb_act_high_d
            }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onActivity(i) },
                colors = CardDefaults.cardColors(containerColor = if (sel) MaterialTheme.colorScheme.primaryContainer else Ab.card),
                border = if (sel) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, Ab.line),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(title), fontWeight = FontWeight.Bold)
                    Text(stringResource(desc, fmtMl(a.extraMl)), style = MaterialTheme.typography.bodySmall, color = Ab.muted)
                }
            }
        }
    }
}

@Composable
private fun RoutineStep(wake: Int, bed: Int, end: Int, cup: Int, onPickTime: (Int) -> Unit, onCup: (Int) -> Unit) {
    StepColumn {
        Text(stringResource(R.string.onb_day_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.onb_day_sub), color = Ab.muted)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeCard(stringResource(R.string.onb_from), wake, Modifier.weight(1f)) { onPickTime(0) }
            TimeCard(stringResource(R.string.onb_to), bed % 1440, Modifier.weight(1f)) { onPickTime(1) }
        }
        Text(stringResource(R.string.window_note, fmtTime(wake), fmtTime(end % 1440)), style = MaterialTheme.typography.bodySmall, color = Ab.muted)
        Text(stringResource(R.string.onb_cup_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(200, 250, 350, 500).forEach { ml ->
                FilterChip(selected = cup == ml, onClick = { onCup(ml) }, label = { Text("$ml ml") })
            }
        }
    }
}

@Composable
private fun TimeCard(label: String, minutes: Int, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Ab.card),
        border = BorderStroke(1.dp, Ab.line),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Ab.muted)
            Text(fmtTime(minutes), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickDialog(initialMin: Int, onDismiss: () -> Unit, onPick: (Int, Int) -> Unit) {
    val st = rememberTimePickerState(initialHour = initialMin / 60, initialMinute = initialMin % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = st) },
        confirmButton = { TextButton(onClick = { onPick(st.hour, st.minute) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun PermissionsStep(
    notifOk: Boolean, locOk: Boolean, temp: Double?, city: String?, busy: Boolean, failed: Boolean,
    onNotif: () -> Unit, onLoc: () -> Unit, onCity: () -> Unit,
) {
    StepColumn {
        Text(stringResource(R.string.onb_perm_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.onb_perm_sub), color = Ab.muted)

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Ab.card), border = BorderStroke(1.dp, Ab.line)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.onb_perm_notif_t), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.onb_perm_notif_d), style = MaterialTheme.typography.bodySmall, color = Ab.muted)
                }
                Button(onClick = onNotif, enabled = !notifOk) { Text(stringResource(if (notifOk) R.string.perm_status_on else R.string.onb_allow)) }
            }
        }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Ab.card), border = BorderStroke(1.dp, Ab.line)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.onb_perm_loc_t), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.onb_perm_loc_d), style = MaterialTheme.typography.bodySmall, color = Ab.muted)
                    }
                    Button(onClick = onLoc, enabled = !busy) { Text(stringResource(if (locOk && temp != null) R.string.perm_status_on else R.string.onb_allow)) }
                }
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.onb_loc_searching))
                    }
                } else if (temp != null) {
                    val extra = tempExtraMl(temp)
                    Text(
                        (city ?: stringResource(R.string.onb_place_loc)) + " · " + stringResource(R.string.onb_wx_found, temp.roundToInt().toString()) +
                            " · " + if (extra > 0) stringResource(R.string.onb_wx_extra, fmtMl(extra)) else stringResource(R.string.onb_wx_none),
                        fontWeight = FontWeight.SemiBold,
                    )
                } else if (failed) {
                    Text(stringResource(R.string.onb_wx_fail), color = MaterialTheme.colorScheme.tertiary)
                }
                TextButton(onClick = onCity, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.weather_search)) }
            }
        }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) { BatteryTipSection() }
        }
    }
}

@Composable
private fun SummaryStep(kg: Int, activity: Activity, temp: Double?, wake: Int, end: Int, cup: Int) {
    val base = baseGoalMl(kg)
    val tempExtra = if (temp != null) tempExtraMl(temp) else 0
    val total = dailyGoalMl(kg, activity, temp)
    val interval = baseIntervalMin(wake, end, cup, dailyGoalMl(kg, activity, null, useActivity = true, useWeather = false))
    StepColumn {
        Text(stringResource(R.string.onb_sum_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.onb_sum_goal), style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp)
                Text("${fmtMl(total)} ml", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(stringResource(R.string.onb_sum_cadence, interval, fmtMl(cup)), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Ab.card), border = BorderStroke(1.dp, Ab.line)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.onb_sum_break), fontWeight = FontWeight.Bold)
                SumRow(stringResource(R.string.gi_weight, kg.toString()), "${fmtMl(base)} ml")
                SumRow(stringResource(R.string.gi_activity, stringResource(activityLabel(activity))), "+${fmtMl(activity.extraMl)} ml")
                SumRow(
                    stringResource(R.string.gi_temp),
                    if (temp != null) stringResource(R.string.gi_temp_value, temp.roundToInt().toString(), fmtMl(tempExtra)) else stringResource(R.string.gi_no_temp),
                )
                if (temp == null) Text(stringResource(R.string.onb_sum_hot), style = MaterialTheme.typography.bodySmall, color = Ab.muted)
            }
        }
        Text(stringResource(R.string.onb_sum_foot), style = MaterialTheme.typography.bodySmall, color = Ab.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SumRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

fun activityLabel(a: Activity): Int = when (a) {
    Activity.NONE -> R.string.activity_none
    Activity.MEDIUM -> R.string.activity_medium
    Activity.HIGH -> R.string.activity_high
}

@Composable
private fun WelcomePage(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painterResource(if (Ab.isDark) R.drawable.ic_wordmark_light else R.drawable.ic_wordmark),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.width(280.dp),
        )
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.onb_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onb_tagline),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(R.string.onb_start))
        }
    }
}

@Composable
internal fun ActivityChips(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Activity.entries.forEachIndexed { i, a ->
            FilterChip(
                selected = i == selected,
                onClick = { onSelect(i) },
                label = { Text(stringResource(activityLabel(a))) },
            )
        }
    }
}

@Composable
internal fun Stepper(label: String, value: String, minusDesc: String, plusDesc: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$label  $value", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(
            onClick = onMinus,
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = minusDesc },
        ) { Text("\u2212") }
        OutlinedButton(
            onClick = onPlus,
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = plusDesc },
        ) { Text("+") }
    }
}
