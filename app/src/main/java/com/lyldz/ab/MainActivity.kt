package com.lyldz.ab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lyldz.ab.ui.HomeScreen
import com.lyldz.ab.ui.MainViewModel
import com.lyldz.ab.ui.OnboardingScreen
import com.lyldz.ab.ui.SettingsScreen
import com.lyldz.ab.ui.theme.AbTheme
import kotlinx.coroutines.delay
import java.time.LocalTime

/** Widget "Özel" düğmesi: ana ekranı özel miktar penceresi açık başlatır. */
const val EXTRA_CUSTOM = "open_custom"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AbTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AbRoot(openCustom = intent.getBooleanExtra(EXTRA_CUSTOM, false))
                }
            }
        }
    }
}

private fun currentMin(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

@Composable
private fun AbRoot(openCustom: Boolean) {
    val app = LocalContext.current.applicationContext as AbApp
    val vm: MainViewModel = viewModel(factory = viewModelFactory { initializer { MainViewModel(app) } })
    val today by vm.today.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { app.reminders.reschedule() } // uygulama açılışında alarmı tazele
    val t = today ?: return

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
        var settingsOpen by rememberSaveable { mutableStateOf(false) }
        if (settingsOpen) {
            SettingsScreen(t.settings, onSave = { vm.saveSettings(it); settingsOpen = false }, onBack = { settingsOpen = false })
        } else {
            HomeScreen(
                t, nowMin, vm::add, vm::undo,
                onDelete = vm::delete, onRestore = vm::restore, onEdit = vm::edit,
                onOpenSettings = { settingsOpen = true }, openCustom = openCustom,
            )
        }
    }
}
