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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.lyldz.ab.ui.theme.Ab
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.core.Activity
import com.lyldz.ab.core.dailyGoalMl
import com.lyldz.ab.core.tempExtraMl
import com.lyldz.ab.data.WeatherApi
import java.time.LocalDate
import kotlin.math.roundToInt
import com.lyldz.ab.data.Settings
import com.lyldz.ab.data.currentCoarse
import kotlinx.coroutines.launch

private val needsNotifPermission = Build.VERSION.SDK_INT >= 33

/** Adım 0: karşılama, 1: bilgiler, 2: bildirim izni, 3: konum izni (isteğe bağlı). Her izin ayrı ekranda, tek tek istenir. */
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
        step = 3 // izin verilse de verilmese de devam
    }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    // konum + hava (adım 3): bulunan yer, bugünün en yüksek sıcaklığı ve hedefe eki gösterilir
    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var lon by rememberSaveable { mutableStateOf<Double?>(null) }
    var city by rememberSaveable { mutableStateOf<String?>(null) }
    var temp by rememberSaveable { mutableStateOf<Double?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var cityDialog by remember { mutableStateOf(false) }

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
        build().copy(
            lat = lat, lon = lon, city = city,
            tempC = temp, tempDay = if (temp != null) LocalDate.now().toString() else null,
        ),
    )
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) findMe() else failed = true
    }
    BackHandler(enabled = step >= 1) { step = if (step == 3 && !needsNotifPermission) 1 else step - 1 }

    if (step == 0) {
        WelcomePage(onStart = { step = 1 })
        return
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (step == 1) {
            Text(stringResource(R.string.onb_profile_title), style = MaterialTheme.typography.headlineLarge)

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
            ActivityChips(activityIdx) { activityIdx = it }

            Text(stringResource(R.string.onb_window), style = MaterialTheme.typography.titleMedium)
            val earlier = stringResource(R.string.earlier)
            val later = stringResource(R.string.later)
            Stepper(stringResource(R.string.onb_from), fmtTime(start), earlier, later,
                { start = (start - 30).coerceIn(0, end - 60) }, { start = (start + 30).coerceIn(0, end - 60) })
            Stepper(stringResource(R.string.onb_to), fmtTime(end), earlier, later,
                { end = (end - 30).coerceIn(start + 60, 23 * 60 + 30) }, { end = (end + 30).coerceIn(start + 60, 23 * 60 + 30) })

            Text(
                stringResource(R.string.onb_goal, fmtMl(dailyGoalMl(kg ?: 70, activity, null))),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(stringResource(R.string.onb_goal_note), color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(
                onClick = { step = if (needsNotifPermission) 2 else 3 },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.onb_next)) }
        } else if (step == 2) {
            Text(stringResource(R.string.perm_notif_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.onb_notif_why), style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.perm_allow)) }
            OutlinedButton(
                onClick = { step = 3 },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.perm_skip)) }
        } else {
            Text(stringResource(R.string.perm_loc_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.onb_loc_why), style = MaterialTheme.typography.bodyLarge)
            val t = temp
            if (t != null) {
                val extra = tempExtraMl(t)
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(city ?: stringResource(R.string.onb_place_loc), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.onb_wx_found, t.roundToInt().toString()), style = MaterialTheme.typography.headlineMedium)
                        Text(
                            if (extra > 0) stringResource(R.string.onb_wx_extra, fmtMl(extra)) else stringResource(R.string.onb_wx_none),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.onb_wx_goal, fmtMl(dailyGoalMl(kg ?: 70, activity, t))),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(onClick = { finish() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(stringResource(R.string.onb_finish))
                }
                OutlinedButton(onClick = { cityDialog = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(stringResource(R.string.weather_search))
                }
            } else {
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                        Text(stringResource(R.string.onb_loc_searching))
                    }
                } else if (failed) {
                    Text(stringResource(R.string.onb_wx_fail), color = MaterialTheme.colorScheme.tertiary)
                }
                Button(
                    onClick = { locLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text(stringResource(R.string.weather_use_location)) }
                OutlinedButton(
                    onClick = { cityDialog = true }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text(stringResource(R.string.weather_search)) }
                OutlinedButton(
                    onClick = { finish() },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text(stringResource(R.string.perm_skip)) }
            }
        }
    }

    if (cityDialog) {
        CityDialog(onDismiss = { cityDialog = false }, onPick = { c -> cityDialog = false; loadTemp(c.lat, c.lon, c.label) })
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
