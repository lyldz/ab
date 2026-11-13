package com.lyldz.ab.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R

/**
 * "Hatırlatmalar gelmiyor mu?": pil ayar sayfasına ve uygulama ayarlarına (MIUI'de otomatik başlatma orada)
 * götürür. Pil muafiyeti isteme izni (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) kullanılmaz; yalnızca ayar sayfaları açılır.
 */
@Composable
fun BatteryTipSection() {
    val ctx = LocalContext.current
    fun open(intent: Intent, fallback: Intent) {
        runCatching { ctx.startActivity(intent) }.onFailure { runCatching { ctx.startActivity(fallback) } }
    }
    val appDetails = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.battery_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.battery_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
