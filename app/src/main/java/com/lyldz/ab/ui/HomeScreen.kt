package com.lyldz.ab.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.lyldz.ab.ui.theme.AbColors
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.core.PACE_TOLERANCE_ML
import com.lyldz.ab.core.nextPlan
import com.lyldz.ab.core.paceMl
import com.lyldz.ab.core.roundTo10
import com.lyldz.ab.core.tempExtraMl
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.data.Intake
import com.lyldz.ab.data.SOURCE_NOTIFICATION
import com.lyldz.ab.data.SOURCE_WIDGET
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    today: AbRepository.Today,
    nowMin: Int,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onDelete: (Intake) -> Unit,
    onRestore: (Intake) -> Unit,
    onEdit: (Intake, Int, Int) -> Unit,
    onSetCup: (Int) -> Unit,
    openCustom: Boolean = false,
    streak: Int = 0,
    onOpenSettings: () -> Unit = {},
) {
    val s = today.settings
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var custom by rememberSaveable { mutableStateOf(openCustom) }
    var editing by remember { mutableStateOf<Intake?>(null) }
    var cupDialog by remember { mutableStateOf(false) }

    fun showUndo(message: String, undo: () -> Unit) {
        scope.launch {
            snack.currentSnackbarData?.dismiss()
            val r = snack.showSnackbar(message, context.getString(R.string.undo), duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) undo()
        }
    }

    fun add(ml: Int) {
        onAdd(ml)
        showUndo(context.getString(R.string.added, fmtMl(ml)), onUndo)
    }

    val remaining = today.goalMl - today.drankMl
    val showPace = remaining > 0 && nowMin in s.windowStartMin..s.windowEndMin
    val pace = paceMl(today.goalMl, s.windowStartMin, s.windowEndMin, nowMin)
    val plan = nextPlan(nowMin, s.windowStartMin, s.windowEndMin, today.goalMl, today.drankMl, s.cupMl, today.anchorMin)

    Scaffold(snackbarHost = { SnackbarHost(snack) }, contentWindowInsets = WindowInsets.statusBars) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // üst çubuk: wordmark | selamlama | bildirim çanı
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_wordmark), contentDescription = stringResource(R.string.app_name), modifier = Modifier.height(34.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.width(1.dp).height(22.dp).background(AbColors.Mist))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (s.name.isBlank()) stringResource(R.string.greet_great) else stringResource(R.string.greet_hello, s.name),
                    color = AbColors.Slate, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                )
                val bellDesc = stringResource(R.string.bell_desc)
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color.White).border(1.dp, AbColors.Mist, CircleShape)
                        .clickable(role = Role.Button, onClick = onOpenSettings).semantics { contentDescription = bellDesc },
                    contentAlignment = Alignment.Center,
                ) {
                    BellIcon(AbColors.Navy)
                    Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(9.dp).clip(CircleShape).background(AbColors.Aqua1))
                }
            }

            val fraction = (today.drankMl.toFloat() / today.goalMl).coerceIn(0f, 1f)
            Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.gauge_title), color = AbColors.SlateLight,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                )
                Text(
                    when {
                        remaining <= 0 -> stringResource(R.string.home_plan_done)
                        plan == null -> stringResource(R.string.home_plan_none)
                        pace - today.drankMl >= PACE_TOLERANCE_ML -> stringResource(R.string.pace_behind, fmtMl(roundTo10(pace - today.drankMl)))
                        today.drankMl - pace >= PACE_TOLERANCE_ML -> stringResource(R.string.pace_ahead, fmtMl(roundTo10(today.drankMl - pace)))
                        else -> stringResource(R.string.pace_on)
                    },
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                )
            }

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WaterOrb(
                    fraction,
                    if (showPace) pace.toFloat() / today.goalMl else null,
                    stringResource(R.string.home_progress, fmtMl(today.drankMl), fmtMl(today.goalMl)),
                    Modifier.size(272.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Canvas(Modifier.size(width = 10.dp, height = 13.dp)) { drawPath(dropPath(size.width, size.height), AbColors.Teal) }
                        Text("${(fraction * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(fmtMl(today.drankMl), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = AbColors.Navy)
                        Text(" ml", color = AbColors.Slate, modifier = Modifier.padding(bottom = 7.dp))
                    }
                    Text(stringResource(R.string.goal_label, fmtMl(today.goalMl)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.left_label, fmtMl(remaining.coerceAtLeast(0))),
                        style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = AbColors.Slate,
                    )
                }
            }

            val nextAt = plan?.nextAtMin
            val nextNeed = if (plan != null) paceMl(today.goalMl, s.windowStartMin, s.windowEndMin, plan.nextAtMin) - today.drankMl else 0
            TimelineCard(today.entries, nextAt, nextNeed, streak)

            val pill = RoundedCornerShape(999.dp)
            val addDesc = stringResource(R.string.add_ml, fmtMl(s.cupMl))
            Box(
                Modifier.fillMaxWidth().height(60.dp)
                    .shadow(10.dp, pill, ambientColor = AbColors.Aqua1, spotColor = AbColors.Aqua1)
                    .clip(pill).background(Brush.horizontalGradient(listOf(AbColors.Aqua1, AbColors.Aqua2)))
                    .clickable(role = Role.Button) { add(s.cupMl) }
                    .semantics { contentDescription = addDesc },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${stringResource(R.string.add_water)} (${fmtMl(s.cupMl)} ml)",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AbColors.Navy,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickCard(stringResource(R.string.quick_glass), 250, Modifier.weight(1f)) { add(250) }
                QuickCard(stringResource(R.string.quick_mug), 350, Modifier.weight(1f)) { add(350) }
                QuickCard(stringResource(R.string.quick_bottle), 500, Modifier.weight(1f)) { add(500) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onUndo,
                    enabled = today.entries.isNotEmpty(), modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = AbColors.Slate),
                ) { Text(stringResource(R.string.undo)) }
                Row {
                    TextButton(onClick = { custom = true }, modifier = Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.textButtonColors(contentColor = AbColors.Slate)) {
                        Text(stringResource(R.string.add_custom))
                    }
                    TextButton(onClick = { cupDialog = true }, modifier = Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.textButtonColors(contentColor = AbColors.Slate)) {
                        Text(stringResource(R.string.cup_button, fmtMl(s.cupMl)))
                    }
                }
            }

            val temp = s.tempToday(LocalDate.now())
            if (s.useWeather && temp != null) {
                val extra = tempExtraMl(temp)
                val degrees = temp.roundToInt().toString()
                Text(
                    if (extra > 0 && s.goalOverrideMl == null) stringResource(R.string.home_weather, degrees, fmtMl(extra))
                    else stringResource(R.string.home_weather_none, degrees),
                    color = AbColors.Slate,
                )
            }

            Text(stringResource(R.string.records_title), style = MaterialTheme.typography.titleLarge)
            if (today.entries.isEmpty()) {
                Text(stringResource(R.string.records_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            today.entries.forEach { e ->
                EntryRow(
                    e,
                    onEdit = { editing = e },
                    onDelete = {
                        onDelete(e)
                        showUndo(context.getString(R.string.deleted, fmtMl(e.ml))) { onRestore(e) }
                    },
                )
            }
        }
    }

    if (cupDialog) CupDialog(s.cupMl, onDismiss = { cupDialog = false }, onPick = { cupDialog = false; onSetCup(it) })
    if (custom) CustomDialog(onDismiss = { custom = false }, onAdd = { custom = false; add(it) })
    editing?.let { e ->
        EditDialog(e, onDismiss = { editing = null }, onSave = { ml, minute -> editing = null; onEdit(e, ml, minute) })
    }
}

private fun minuteOfDay(atMs: Long): Int =
    Instant.ofEpochMilli(atMs).atZone(ZoneId.systemDefault()).let { it.hour * 60 + it.minute }

@Composable
private fun EntryRow(e: Intake, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val more = stringResource(R.string.more_options)
    val source = stringResource(
        when (e.source) {
            SOURCE_NOTIFICATION -> R.string.src_notification
            SOURCE_WIDGET -> R.string.src_widget
            else -> R.string.src_app
        },
    )
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${fmtTime(minuteOfDay(e.atMs))}  ·  ${fmtMl(e.ml)} ml", style = MaterialTheme.typography.titleMedium)
                Text(source, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Box {
                TextButton(
                    onClick = { menu = true },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = more },
                ) { Text("⋮") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.menu_edit)) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.menu_delete)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun EditDialog(e: Intake, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var text by remember { mutableStateOf(e.ml.toString()) }
    var minute by remember { mutableIntStateOf(minuteOfDay(e.atMs)) }
    val ml = text.toIntOrNull()?.takeIf { it in 1..5000 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(4) },
                    suffix = { Text("ml") },
                    isError = ml == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Stepper(
                    stringResource(R.string.edit_time), fmtTime(minute),
                    stringResource(R.string.earlier), stringResource(R.string.later),
                    { minute = (minute - 5).coerceIn(0, 1439) }, { minute = (minute + 5).coerceIn(0, 1439) },
                )
            }
        },
        confirmButton = { TextButton(onClick = { ml?.let { onSave(it, minute) } }, enabled = ml != null) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
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

private fun dropPath(w: Float, h: Float) = Path().apply {
    moveTo(w * .5f, h * .02f)
    cubicTo(w * .62f, h * .28f, w * .94f, h * .5f, w * .94f, h * .68f)
    cubicTo(w * .94f, h * .88f, w * .74f, h * .98f, w * .5f, h * .98f)
    cubicTo(w * .26f, h * .98f, w * .06f, h * .88f, w * .06f, h * .68f)
    cubicTo(w * .06f, h * .5f, w * .38f, h * .28f, w * .5f, h * .02f)
    close()
}

@Composable
private fun QuickCard(label: String, ml: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.shadow(6.dp, shape, ambientColor = AbColors.Navy.copy(alpha = 0.12f), spotColor = AbColors.Navy.copy(alpha = 0.12f))
            .clip(shape).background(Color.White).border(1.dp, AbColors.Mist, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AbColors.Slate)
        Text("+${fmtMl(ml)} ml", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AbColors.Navy)
    }
}

/** Bugünün kayıtları: çizgi üzerinde onay işaretli düğümler, son düğüm sıradaki hatırlatma; çoksa yatay kayar. */
@Composable
private fun TimelineCard(entries: List<Intake>, nextAtMin: Int?, nextNeedMl: Int, streak: Int) {
    val shape = RoundedCornerShape(24.dp)
    val context = LocalContext.current
    val sorted = entries.sortedBy { it.atMs }
    val scroll = rememberScrollState()
    LaunchedEffect(sorted.size) { scroll.animateScrollTo(scroll.maxValue) }
    Column(
        Modifier.fillMaxWidth().shadow(6.dp, shape, ambientColor = AbColors.Navy.copy(alpha = 0.08f), spotColor = AbColors.Navy.copy(alpha = 0.08f))
            .clip(shape).background(Color.White).border(1.dp, AbColors.Mist, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.tl_title), color = AbColors.Slate,
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            )
            Text(
                context.resources.getQuantityString(R.plurals.tl_count, sorted.size, sorted.size) +
                    if (streak > 0) "  ·  " + context.resources.getQuantityString(R.plurals.streak_days, streak, streak) else "",
                color = AbColors.SlateLight, style = MaterialTheme.typography.labelMedium,
            )
        }
        if (sorted.isEmpty() && nextAtMin == null) {
            Text(stringResource(R.string.history_empty), color = AbColors.SlateLight)
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.horizontalScroll(scroll).widthIn(min = maxWidth).drawBehind {
                        val y = 16.dp.toPx()
                        drawLine(AbColors.Mist, Offset(38.dp.toPx(), y), Offset(size.width - 38.dp.toPx(), y), 2.dp.toPx(), StrokeCap.Round)
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    sorted.forEach { e ->
                        TimelineNode(done = true, time = fmtTime(minuteOfDay(e.atMs)), detail = "${fmtMl(e.ml)} ml")
                    }
                    if (nextAtMin != null) {
                        TimelineNode(
                            done = false, time = stringResource(R.string.tl_next),
                            detail = fmtTime(nextAtMin) + if (nextNeedMl > 0) " · +${fmtMl(roundTo10(nextNeedMl))}" else "",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineNode(done: Boolean, time: String, detail: String) {
    Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Canvas(Modifier.size(32.dp)) {
            val r = size.minDimension / 2f
            if (done) {
                drawCircle(Brush.linearGradient(listOf(AbColors.Aqua1, AbColors.Aqua2)), r)
                val check = Path().apply {
                    moveTo(size.width * .30f, size.height * .52f)
                    lineTo(size.width * .44f, size.height * .66f)
                    lineTo(size.width * .70f, size.height * .38f)
                }
                drawPath(check, Color.White, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else {
                drawCircle(Color.White, r)
                drawCircle(
                    AbColors.SlateLight, r - 1.dp.toPx(), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
                )
                drawCircle(AbColors.SlateLight, 2.5.dp.toPx())
            }
        }
        Text(time, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (done) AbColors.Navy else AbColors.Slate)
        Text(detail, style = MaterialTheme.typography.labelSmall, color = AbColors.SlateLight, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BellIcon(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val k = size.width / 24f
        val body = Path().apply {
            moveTo(12 * k, 3 * k)
            cubicTo(8 * k, 3 * k, 6 * k, 6 * k, 6 * k, 10 * k)
            lineTo(6 * k, 14 * k); lineTo(4 * k, 17 * k); lineTo(20 * k, 17 * k); lineTo(18 * k, 14 * k)
            lineTo(18 * k, 10 * k)
            cubicTo(18 * k, 6 * k, 16 * k, 3 * k, 12 * k, 3 * k)
            close()
        }
        drawPath(body, color, style = Stroke(1.8f * k, join = StrokeJoin.Round))
        drawArc(color, 20f, 140f, false, Offset(9.5f * k, 17.5f * k), androidx.compose.ui.geometry.Size(5 * k, 4 * k), style = Stroke(1.8f * k, cap = StrokeCap.Round))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CupDialog(current: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    var custom by remember { mutableStateOf("") }
    val customMl = custom.toIntOrNull()?.takeIf { it in 50..1000 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cup_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(100, 150, 200, 250, 300, 400, 500).forEach { ml ->
                        val selected = ml == current
                        Column(
                            Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(ml) }).padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.size(56.dp).clip(CircleShape).background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                contentAlignment = Alignment.Center,
                            ) { CupIcon(ml) }
                            Text("${fmtMl(ml)} ml", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.cup_custom_hint)) },
                    suffix = { Text("ml") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = { TextButton(onClick = { customMl?.let(onPick) }, enabled = customMl != null) { Text(stringResource(R.string.cup_set)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
