package com.lyldz.ab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lyldz.ab.ui.BottomBar
import com.lyldz.ab.ui.HistoryScreen
import com.lyldz.ab.ui.HomeScreen
import com.lyldz.ab.ui.MainViewModel
import com.lyldz.ab.ui.OnboardingScreen
import com.lyldz.ab.ui.SettingsScreen
import com.lyldz.ab.ui.StatsScreen
import com.lyldz.ab.ui.theme.AbTheme
import kotlinx.coroutines.delay
import java.time.LocalTime

/** Widget "Özel" düğmesi: ana ekranı özel miktar penceresi açık başlatır. */
const val EXTRA_CUSTOM = "open_custom"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AbRoot(openCustom = intent.getBooleanExtra(EXTRA_CUSTOM, false)) }
    }
}

private fun currentMin(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

@Composable
private fun AbRoot(openCustom: Boolean) {
    val app = LocalContext.current.applicationContext as AbApp
    val vm: MainViewModel = viewModel(factory = viewModelFactory { initializer { MainViewModel(app) } })
    val today by vm.today.collectAsStateWithLifecycle()
    val mode = today?.settings?.themeMode ?: 0
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val activity = LocalContext.current as ComponentActivity
    // Durum ve gezinme çubuğu simgeleri seçilen temaya göre (sistemden bağımsız) açık ya da koyu olsun
    DisposableEffect(dark) {
        val bar = if (dark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        activity.enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
        onDispose {}
    }
    LaunchedEffect(Unit) {
        app.reminders.refreshWeather() // günde bir kez; bugün alındıysa hiçbir şey yapmaz
        app.reminders.reschedule() // uygulama açılışında alarmı tazele
        app.reminders.refreshNotification(force = true) // durum bildirimi hep görünsün (sessiz)
    }
    AbTheme(mode) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            today?.let { AbContent(it, vm, openCustom) }
        }
    }
}

@Composable
private fun AbContent(t: com.lyldz.ab.data.AbRepository.Today, vm: MainViewModel, openCustom: Boolean) {
    if (!t.settings.onboarded) {
        OnboardingScreen(vm::finishOnboarding)
    } else {
        var nowMin by remember { mutableIntStateOf(currentMin()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(30_000)
                nowMin = currentMin()
            }
        }
        var tab by rememberSaveable { mutableIntStateOf(0) } // 0 bugün, 1 grafikler, 2 geçmiş, 3 ayarlar
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> HomeScreen(
                        t, nowMin, vm::add, vm::undo,
                        onDelete = vm::delete, onRestore = vm::restore, onEdit = vm::edit,
                        onSetCup = { vm.saveSettings(t.settings.copy(cupMl = it)) },
                        openCustom = openCustom,
                        streak = vm.streak.collectAsStateWithLifecycle().value,
                        onOpenSettings = { tab = 3 },
                    )
                    1 -> {
                        val stats by vm.stats.collectAsStateWithLifecycle()
                        val days by vm.periodDays.collectAsStateWithLifecycle()
                        StatsScreen(stats, days, vm::setPeriod, onBack = { tab = 0 })
                    }
                    2 -> HistoryScreen(vm.history.collectAsStateWithLifecycle().value, onBack = { tab = 0 })
                    else -> SettingsScreen(t.settings, onSave = { vm.saveSettings(it); tab = 0 }, onBack = { tab = 0 })
                }
            }
            BottomBar(tab) { tab = it }
        }
    }
}
