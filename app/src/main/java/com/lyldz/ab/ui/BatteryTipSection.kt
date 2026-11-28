package com.lyldz.ab.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lyldz.ab.R

/** Cihaz pil optimizasyonu uygulamayı kısıtlıyor mu? (İzin gerektirmez.) */
internal fun isBatteryRestricted(ctx: Context): Boolean = runCatching {
    val pm = ctx.getSystemService(PowerManager::class.java) ?: return false
    !pm.isIgnoringBatteryOptimizations(ctx.packageName)
}.getOrDefault(false)

/**
 * "Hatırlatmalar gelmiyor mu?": pil optimizasyonu durumunu gösterir; pil ayar sayfasına ve uygulama ayarlarına
 * (MIUI'de otomatik başlatma orada) götürür. Pil muafiyeti isteme izni (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
 * kullanılmaz; yalnızca ayar sayfaları açılır.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatteryTipSection() {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val restricted = remember(tick) { isBatteryRestricted(ctx) }
    fun open(intent: Intent, fallback: Intent) {
        runCatching { ctx.startActivity(intent) }.onFailure { runCatching { ctx.startActivity(fallback) } }
    }
    val appDetails = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.battery_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(if (restricted) R.string.battery_status_restricted else R.string.battery_status_ok),
            style = MaterialTheme.typography.bodySmall,
            color = if (restricted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.battery_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { open(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), appDetails) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.battery_open)) }
            OutlinedButton(onClick = { open(appDetails, appDetails) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.battery_app))
            }
        }
    }
}
