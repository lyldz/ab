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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.lyldz.ab.ui.theme.Ab
import com.lyldz.ab.ui.theme.AbColors
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.core.baseGoalMl
import com.lyldz.ab.core.nextPlan
import com.lyldz.ab.core.paceMl
import com.lyldz.ab.core.roundTo10
import com.lyldz.ab.core.tempExtraMl
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.data.Intake
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

private const val MAX_HINTS = 3
private var hintShownThisRun = false

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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
    onSetPresets: (List<Int>) -> Unit = {},
    openCustom: Boolean = false,
    onCustomConsumed: () -> Unit = {},
    streak: Int = 0,
    onOpenSettings: () -> Unit = {},
    onOpenProfile: () -> Unit = onOpenSettings,
    hintsShown: Int = MAX_HINTS,
    onHintCount: (Int) -> Unit = {},
) {
    val s = today.settings
    val context = LocalContext.current
    var undoBanner by remember { mutableStateOf<Triple<Long, String, () -> Unit>?>(null) }
    var custom by rememberSaveable { mutableStateOf(openCustom) }
    LaunchedEffect(undoBanner?.first) {
        if (undoBanner != null) {
            delay(s.undoBannerMs.toLong())
            undoBanner = null
        }
    }
    LaunchedEffect(openCustom) {
        if (openCustom) {
            custom = true
            onCustomConsumed()
        }
    }
    var editing by remember { mutableStateOf<Intake?>(null) }
    var cupDialog by remember { mutableStateOf(false) }
    var goalInfo by remember { mutableStateOf(false) }
    var hintVisible by remember { mutableStateOf(false) }
    LaunchedEffect(s.showHints) {
        if (!s.showHints) {
            hintVisible = false
            return@LaunchedEffect
        }
        // uygulama başına bir kez, ilk maxHints açılışta; sekme değiştirince tekrar çıkmaz
        if (!hintShownThisRun && hintsShown < s.maxHints) {
            hintShownThisRun = true
            onHintCount(hintsShown + 1)
            delay(700)
            hintVisible = true
            delay(5000)
            hintVisible = false
        }
    }

    fun showUndo(message: String, undo: () -> Unit) {
        undoBanner = Triple(System.nanoTime(), message, undo)
    }

    fun add(ml: Int) {
        onAdd(ml)
        showUndo(context.getString(R.string.added_vol, fmtVol(ml, s.unit)), onUndo)
    }

    val remaining = today.goalMl - today.drankMl
    val pace = paceMl(today.goalMl, s.windowStartMin, s.windowEndMin, nowMin)
    val plan = if (s.remindersEnabled) {
        nextPlan(nowMin, s.windowStartMin, s.windowEndMin, today.goalMl, today.drankMl, s.reminderIntervalMin(), today.anchorMin, s.minReminderMl, s.maxReminderMl)
    } else {
        null
    }

    val temp = s.tempToday(LocalDate.now())
    val hasWeather = s.useWeather && temp != null
    Scaffold(contentWindowInsets = WindowInsets.statusBars) { padding ->
        // tek sayfa: gösterge, kalan boşluğa göre büyür/küçülür; çok küçük ekranda içerik kayar
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val reserved = if (hasWeather) 398.dp else 380.dp
            val orb = (maxHeight - reserved).coerceIn(148.dp, 360.dp).coerceAtMost(maxWidth - 24.dp)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // üst çubuk: wordmark, (isim varsa) selamlama, profil ve hedef düğmesi
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painterResource(if (Ab.isDark) R.drawable.ic_wordmark_light else R.drawable.ic_wordmark),
                        contentDescription = stringResource(R.string.app_name), modifier = Modifier.height(38.dp),
                    )
                    Text(
                        if (s.name.isBlank()) "" else stringResource(R.string.greet_hello, s.name),
                        color = Ab.muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    )
                    val profileDesc = stringResource(R.string.profile_desc)
                    val initial = s.name.trim().firstOrNull()?.toString()?.uppercase(Locale("tr"))
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(Ab.card).border(1.dp, Ab.line, CircleShape)
                            .clickable(role = Role.Button, onClick = onOpenProfile).semantics { contentDescription = profileDesc },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (initial != null) {
                            Text(
                                initial,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Ab.strong,
                            )
                        } else {
                            ProfileIcon(Ab.strong)
                        }
                    }
                }

                // hedef (i: nasıl hesaplandı), altında bugünün sıcaklığı, altında tempo
                val rawFraction = (today.drankMl.toFloat() / today.goalMl.coerceAtLeast(1)).coerceAtLeast(0f)
                val infoDesc = stringResource(R.string.info_desc)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(999.dp))
                            .clickable(role = Role.Button) { goalInfo = true }
                            .semantics { contentDescription = infoDesc }.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.goal_label_vol, fmtVol(today.goalMl, s.unit)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        InfoBadge()
                    }
                    if (s.useWeather && temp != null) {
                        val extra = tempExtraMl(temp)
                        val degrees = temp.roundToInt().toString()
                        Text(
                            if (extra > 0 && s.goalOverrideMl == null) stringResource(R.string.home_weather_vol, degrees, fmtVol(extra, s.unit))
                            else stringResource(R.string.home_weather_none, degrees),
                            style = MaterialTheme.typography.bodySmall, color = Ab.muted,
                        )
                    }
                    val tol = s.paceToleranceMl
                    Text(
                        when {
                            remaining <= 0 -> stringResource(R.string.home_plan_done)
                            plan == null -> stringResource(R.string.home_plan_none)
                            pace - today.drankMl >= tol && pace > today.drankMl -> stringResource(R.string.pace_behind_vol, fmtVol(roundTo10(pace - today.drankMl), s.unit))
                            today.drankMl - pace >= tol && today.drankMl > pace -> stringResource(R.string.pace_ahead_vol, fmtVol(roundTo10(today.drankMl - pace), s.unit))
                            else -> stringResource(R.string.pace_on)
                        },
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                    )
                }

                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    WaterOrb(
                        rawFraction,
                        stringResource(R.string.home_progress, fmtMl(today.drankMl), fmtMl(today.goalMl)),
                        Modifier.size(orb),
                        motion = s.motionWater,
                        waveAnimation = s.waterWaveAnimation,
                        showOverflow = s.showOverflowEffect,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            Modifier.clip(RoundedCornerShape(999.dp)).background(Ab.card).padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Canvas(Modifier.size(width = 10.dp, height = 13.dp)) { drawPath(dropPath(size.width, size.height), AbColors.Teal) }
                            Text("${(rawFraction * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(fmtVolNum(today.drankMl, s.unit), fontSize = (orb.value * 0.15f).coerceIn(26f, 40f).sp, fontWeight = FontWeight.ExtraBold, color = Ab.strong)
                            Text(" ${unitLabel(s.unit)}", color = Ab.muted, modifier = Modifier.padding(bottom = 5.dp))
                        }
                        Text(
                            if (remaining < 0) stringResource(R.string.over_label_vol, fmtVol(-remaining, s.unit))
                            else stringResource(R.string.left_label_vol, fmtVol(remaining, s.unit)),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (remaining < 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (remaining < 0) (if (Ab.isDark) AbColors.Aqua1 else MaterialTheme.colorScheme.primary) else Ab.muted,
                        )
                    }
                }

                val nextAt = plan?.nextAtMin
                val nextNeed = if (plan != null) paceMl(today.goalMl, s.windowStartMin, s.windowEndMin, plan.nextAtMin) - today.drankMl else 0
                TimelineCard(today.entries, nextAt, nextNeed, streak, s.unit) { editing = it }

                val pill = RoundedCornerShape(999.dp)
                val addDesc = stringResource(R.string.add_ml, fmtMl(s.cupMl))
                val pickCupDesc = stringResource(R.string.pick_cup_desc)
                Box(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().height(54.dp)
                            .shadow(10.dp, pill, ambientColor = AbColors.Aqua1, spotColor = AbColors.Aqua1)
                            .clip(pill).background(Brush.horizontalGradient(listOf(AbColors.Aqua1, AbColors.Aqua2))),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.weight(1f).height(54.dp)
                                // basınca bardak boyutu kadar ekler, uzun basınca ölçü seçtirir
                                .combinedClickable(
                                    role = Role.Button,
                                    onLongClick = { hintVisible = false; onHintCount(s.maxHints.coerceAtLeast(MAX_HINTS)); cupDialog = true },
                                ) { add(s.cupMl) }
                                .semantics { contentDescription = addDesc }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${stringResource(R.string.add_water)} (${fmtVol(s.cupMl, s.unit)})",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AbColors.Navy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(Modifier.width(1.dp).height(28.dp).background(AbColors.Navy.copy(alpha = 0.18f)))
                        Box(
                            Modifier.widthIn(min = 56.dp).height(54.dp)
                                .clickable(role = Role.Button) {
                                    hintVisible = false
                                    onHintCount(s.maxHints.coerceAtLeast(MAX_HINTS))
                                    cupDialog = true
                                }
                                .semantics { contentDescription = pickCupDesc }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CupPickerIcon(AbColors.Navy)
                        }
                    }
                    // uzun basmayı keşfettirmek için geçici ok işaretli ipucu (ilk birkaç açılışta, bir kez uzun basınca kalkar)
                    HintHost(hintVisible, stringResource(R.string.hint_long_press), Modifier.align(Alignment.TopCenter).offset(y = (-54).dp))
                }
            }
            // düğmeleri kapatmayan, 1.8 sn'de kaybolan ince üst geri-al hapı
            val currentUndo = undoBanner
            AnimatedVisibility(
                visible = currentUndo != null,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            ) {
                currentUndo?.let { (_, msg, action) ->
                    val pillShape = RoundedCornerShape(999.dp)
                    Row(
                        Modifier.shadow(8.dp, pillShape, ambientColor = AbColors.Aqua1, spotColor = AbColors.Aqua1)
                            .clip(pillShape)
                            .background(Ab.card.copy(alpha = 0.96f))
                            .border(1.dp, AbColors.Aqua1.copy(alpha = 0.65f), pillShape)
                            .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(msg, style = MaterialTheme.typography.labelLarge, color = Ab.strong)
                        Box(
                            Modifier.heightIn(min = 44.dp).clip(pillShape)
                                .clickable(role = Role.Button) {
                                    undoBanner = null
                                    action()
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(R.string.undo),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (goalInfo) GoalInfoDialog(today, temp, onDismiss = { goalInfo = false }, onProfile = { goalInfo = false; onOpenProfile() })
    if (cupDialog) {
        CupDialog(
            current = s.cupMl,
            presets = s.cupPresets,
            unit = s.unit,
            onDismiss = { cupDialog = false },
            onPick = { cupDialog = false; onSetCup(it); add(it) },
            onSavePresets = onSetPresets,
        )
    }
    if (custom) CustomDialog(onDismiss = { custom = false }, onAdd = { custom = false; add(it) })
    editing?.let { e ->
        EditDialog(
            e, onDismiss = { editing = null },
            onSave = { ml, minute -> editing = null; onEdit(e, ml, minute) },
            onDelete = {
                editing = null
                onDelete(e)
                showUndo(context.getString(R.string.deleted_vol, fmtVol(e.ml, s.unit))) { onRestore(e) }
            },
        )
    }
}

private fun minuteOfDay(atMs: Long): Int =
    Instant.ofEpochMilli(atMs).atZone(ZoneId.systemDefault()).let { it.hour * 60 + it.minute }

@Composable
private fun EditDialog(e: Intake, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit, onDelete: () -> Unit) {
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
                    suffix = { Text(stringResource(R.string.unit_ml)) },
                    isError = ml == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Stepper(
                    stringResource(R.string.edit_time), fmtTime(minute),
                    stringResource(R.string.earlier), stringResource(R.string.later),
                    { minute = (minute - 5).coerceIn(0, 1439) }, { minute = (minute + 5).coerceIn(0, 1439) },
                )
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.menu_delete))
                }
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
                suffix = { Text(stringResource(R.string.unit_ml)) },
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

/** İpucunu belirip kaybolan animasyonla gösterir (üst düzey çağrı: dış Column kapsamıyla karışmasın). */
@Composable
private fun HintHost(visible: Boolean, text: String, modifier: Modifier) {
    AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut(), modifier = modifier) {
        HintBubble(text)
    }
}

/** Aşağı bakan oklu ipucu balonu. */
@Composable
private fun HintBubble(text: String) {
    val bg = MaterialTheme.colorScheme.inverseSurface
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(text, color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Canvas(Modifier.size(width = 18.dp, height = 9.dp)) {
            drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2f, size.height); close() }, bg)
        }
    }
}

/** Bugünün kayıtları: çizgi üzerinde onay işaretli düğümler, son düğüm sıradaki hatırlatma; çoksa yatay kayar. */
@Composable
private fun TimelineCard(entries: List<Intake>, nextAtMin: Int?, nextNeedMl: Int, streak: Int, unit: Int = UNIT_ML, onEntry: (Intake) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val context = LocalContext.current
    val sorted = entries.sortedBy { it.atMs }
    val scroll = rememberScrollState()
    val lineC = Ab.line
    LaunchedEffect(sorted.size) { scroll.animateScrollTo(scroll.maxValue) }
    Column(
        Modifier.fillMaxWidth().shadow(6.dp, shape, ambientColor = AbColors.Navy.copy(alpha = 0.08f), spotColor = AbColors.Navy.copy(alpha = 0.08f))
            .clip(shape).background(Ab.card).border(1.dp, Ab.line, shape).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.tl_title), color = Ab.muted,
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            )
            Text(
                context.resources.getQuantityString(R.plurals.tl_count, sorted.size, sorted.size) +
                    if (streak > 0) "  ·  " + context.resources.getQuantityString(R.plurals.streak_days, streak, streak) else "",
                color = Ab.faint, style = MaterialTheme.typography.labelMedium,
            )
        }
        if (sorted.isEmpty() && nextAtMin == null) {
            Text(stringResource(R.string.history_empty), color = Ab.faint)
        } else {
            val itemCount = sorted.size + if (nextAtMin != null) 1 else 0
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.horizontalScroll(scroll).widthIn(min = maxWidth).drawBehind {
                        if (itemCount >= 2) {
                            val y = 16.dp.toPx()
                            drawLine(lineC, Offset(38.dp.toPx(), y), Offset(size.width - 38.dp.toPx(), y), 2.dp.toPx(), StrokeCap.Round)
                        }
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    sorted.forEach { e ->
                        TimelineNode(done = true, time = fmtTime(minuteOfDay(e.atMs)), detail = fmtVol(e.ml, unit), onClick = { onEntry(e) })
                    }
                    if (nextAtMin != null) {
                        TimelineNode(
                            done = false, time = stringResource(R.string.tl_next),
                            detail = fmtTime(nextAtMin) + if (nextNeedMl > 0) " · +${fmtVol(roundTo10(nextNeedMl), unit)}" else "",
                        )
                    }
                }
            }
        }
    }
}

/** Bölünmüş ana düğmenin sağ tarafındaki minimalist bardak + aşağı ok simgesi. */
@Composable
private fun CupPickerIcon(color: Color) {
    Canvas(Modifier.size(width = 26.dp, height = 18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        // sol tarafta minimalist bardak
        val glass = Path().apply {
            moveTo(w * 0.08f, h * 0.16f)
            lineTo(w * 0.50f, h * 0.16f)
            lineTo(w * 0.43f, h * 0.86f)
            lineTo(w * 0.15f, h * 0.86f)
            close()
        }
        drawPath(glass, color, style = stroke)
        // sağ tarafta aşağı açılır ok (chevron)
        val chevron = Path().apply {
            moveTo(w * 0.64f, h * 0.40f)
            lineTo(w * 0.78f, h * 0.62f)
            lineTo(w * 0.92f, h * 0.40f)
        }
        drawPath(chevron, color, style = stroke)
    }
}

@Composable
private fun TimelineNode(done: Boolean, time: String, detail: String, onClick: (() -> Unit)? = null) {
    val faint = Ab.faint
    val cardC = Ab.card
    val entryDesc = if (onClick != null) stringResource(R.string.tl_entry_desc, time, detail) else null
    Column(
        Modifier.width(76.dp).heightIn(min = 48.dp).let {
            if (onClick != null) {
                it.clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onClick)
                    .semantics(mergeDescendants = true) { entryDesc?.let { d -> contentDescription = d } }
            } else {
                it
            }
        }.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
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
                drawCircle(cardC, r)
                drawCircle(
                    faint, r - 1.dp.toPx(), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
                )
                drawCircle(faint, 2.5.dp.toPx())
            }
        }
        Text(time, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (done) Ab.strong else Ab.muted)
        Text(detail, style = MaterialTheme.typography.labelSmall, color = Ab.faint, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ProfileIcon(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val k = size.width / 24f
        val stroke = Stroke(1.9f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(color, radius = 3.8f * k, center = Offset(12 * k, 8.2f * k), style = stroke)
        val shoulders = Path().apply {
            moveTo(4.8f * k, 19.6f * k)
            cubicTo(4.8f * k, 15.6f * k, 8.0f * k, 14.0f * k, 12.0f * k, 14.0f * k)
            cubicTo(16.0f * k, 14.0f * k, 19.2f * k, 15.6f * k, 19.2f * k, 19.6f * k)
        }
        drawPath(shoulders, color, style = stroke)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun CupDialog(
    current: Int,
    presets: List<Int>,
    unit: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
    onSavePresets: (List<Int>) -> Unit,
) {
    var custom by remember { mutableStateOf("") }
    var list by remember(presets) { mutableStateOf(presets) }
    val customMl = custom.toIntOrNull()?.takeIf { it in 50..2000 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cup_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    list.forEach { ml ->
                        val selected = ml == current
                        val cupDesc = stringResource(R.string.add_ml, fmtMl(ml))
                        Column(
                            Modifier.heightIn(min = 48.dp)
                                .combinedClickable(
                                    role = Role.RadioButton,
                                    onLongClick = {
                                        if (list.size > 2) {
                                            val next = list - ml
                                            list = next
                                            onSavePresets(next)
                                        }
                                    },
                                ) { onPick(ml) }
                                .semantics(mergeDescendants = true) { contentDescription = cupDesc }
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.size(56.dp).clip(CircleShape).background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                contentAlignment = Alignment.Center,
                            ) { CupIcon(ml) }
                            Text(fmtVol(ml, unit), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (list.size > 2) {
                    Text(stringResource(R.string.cup_remove_hint), style = MaterialTheme.typography.labelSmall, color = Ab.faint)
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.cup_custom_hint)) },
                    suffix = { Text(stringResource(R.string.unit_ml)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (customMl != null && customMl !in list && list.size < 8) {
                    TextButton(
                        onClick = {
                            val next = (list + customMl).distinct().sorted()
                            list = next
                            onSavePresets(next)
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.cup_save_preset)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { customMl?.let(onPick) }, enabled = customMl != null) { Text(stringResource(R.string.cup_set)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Hedefin yanındaki "i" simgesi. */
@Composable
private fun InfoBadge() {
    val c = MaterialTheme.colorScheme.primary
    Box(
        Modifier.size(22.dp).border(1.5.dp, c, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text("i", color = c, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
}

/** Hedefin parçaları: kilo, hareket, bugünün sıcaklığı ve konum. */
@Composable
private fun GoalInfoDialog(today: AbRepository.Today, temp: Double?, onDismiss: () -> Unit, onProfile: () -> Unit) {
    val s = today.settings
    val tempExtra = if (s.useWeather && temp != null) tempExtraMl(temp) else 0
    val off = stringResource(R.string.gi_off)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_info_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow(stringResource(R.string.gi_weight, s.weightKg.toString()), fmtVol(baseGoalMl(s.weightKg, s.mlPerKg, s.minGoalMl), s.unit))
                Text(stringResource(R.string.gi_formula_dyn, s.mlPerKg, fmtMl(s.minGoalMl)), style = MaterialTheme.typography.bodySmall, color = Ab.muted)
                InfoRow(
                    stringResource(R.string.gi_activity, stringResource(activityLabel(s.activity))),
                    if (s.useActivity) "+${fmtVol(s.activity.extraMl, s.unit)}" else off,
                )
                InfoRow(
                    stringResource(R.string.gi_temp),
                    when {
                        !s.useWeather -> off
                        temp == null -> stringResource(R.string.gi_no_temp)
                        else -> stringResource(R.string.gi_temp_value_vol, temp.roundToInt().toString(), fmtVol(tempExtra, s.unit))
                    },
                )
                InfoRow(
                    stringResource(R.string.gi_location),
                    s.city ?: if (s.lat != null) stringResource(R.string.weather_city_location) else stringResource(R.string.weather_city_none),
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(Ab.line))
                if (s.goalOverrideMl != null) Text(stringResource(R.string.gi_manual), color = MaterialTheme.colorScheme.tertiary)
                InfoRow(stringResource(R.string.gi_total), fmtVol(today.goalMl, s.unit), bold = true)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onProfile) { Text(stringResource(R.string.profile_desc)) } },
    )
}

@Composable
private fun InfoRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}
