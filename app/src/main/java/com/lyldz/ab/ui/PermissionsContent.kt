package com.lyldz.ab.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lyldz.ab.R

/**
 * "İzinler ve bildirimler" grubu: bildirim ve yaklaşık konum izninin şu anki durumu (ayardan dönünce yenilenir),
 * ilgili sistem ayar sayfasına kısayol ve pil rehberi. İzinler yalnızca sistem ayarlarından değiştirilir.
 */
@Composable
fun PermissionsContent() {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val notifOn = remember(tick) { NotificationManagerCompat.from(ctx).areNotificationsEnabled() }
    val locOn = remember(tick) {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    fun open(intent: Intent) {
        runCatching { ctx.startActivity(intent) }
    }
    val appDetails = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
    val notifSettings = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName)

    PermissionRow(stringResource(R.string.perm_notifications), notifOn) { open(notifSettings) }
    PermissionRow(stringResource(R.string.perm_location), locOn) { open(appDetails) }
    BatteryTipSection()
}

@Composable
private fun PermissionRow(label: String, on: Boolean, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(if (on) R.string.perm_status_on else R.string.perm_status_off),
                style = MaterialTheme.typography.bodySmall,
                color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            )
        }
        OutlinedButton(onClick = onOpen, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.perm_open)) }
    }
}
