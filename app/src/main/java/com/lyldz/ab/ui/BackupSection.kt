package com.lyldz.ab.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lyldz.ab.AbApp
import com.lyldz.ab.R
import com.lyldz.ab.data.Backup
import com.lyldz.ab.data.BackupData
import com.lyldz.ab.data.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val MAX_BACKUP_BYTES = 20_000_000

/**
 * Ayarlar > Yedek: kayıtlar ve ayarlar tek JSON dosyasına yazılır ya da dosyadan geri yüklenir. Dosyayı sistemin
 * dosya seçicisi kaydeder/açar (izin gerekmez); hiçbir yere gönderilmez. Geri yükleme [onRestored] ile ana ekrana döner,
 * böylece Ayarlar'daki eski değerler yeni ayarların üstüne yazılmaz.
 */
@Composable
fun BackupSection(onRestored: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as AbApp
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<BackupData?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                val json = Backup.toJson(app.repo.exportData(), Updater.currentVersion(ctx), System.currentTimeMillis())
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
            }.isSuccess
            message = ctx.getString(if (ok) R.string.backup_exported else R.string.backup_export_failed)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val data = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                    if (bytes.size > MAX_BACKUP_BYTES) null else Backup.parse(String(bytes, Charsets.UTF_8))
                }.getOrNull()
            }
            if (data == null) message = ctx.getString(R.string.backup_invalid) else pending = data
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.backup_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.backup_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { exportLauncher.launch("ab-yedek-${LocalDate.now()}.json") },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.backup_export)) }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.backup_import))
            }
        }
    }

    pending?.let { d ->
        val zone = ZoneId.systemDefault()
        val days = d.intakes.map { Instant.ofEpochMilli(it.atMs).atZone(zone).toLocalDate() }.toSet().size
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.backup_confirm_title)) },
            text = { Text(stringResource(R.string.backup_confirm_text, d.intakes.size, days)) },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        val n = runCatching { app.importBackup(d) }.getOrNull()
                        if (n == null) message = ctx.getString(R.string.backup_import_failed) else {
                            message = ctx.getString(R.string.backup_imported, n)
                            onRestored()
                        }
                    }
                }) { Text(stringResource(R.string.backup_restore)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
