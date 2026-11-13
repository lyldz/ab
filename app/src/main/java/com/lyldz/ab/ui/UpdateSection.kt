package com.lyldz.ab.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lyldz.ab.R
import com.lyldz.ab.data.DownloadResult
import com.lyldz.ab.data.UpdateInfo
import com.lyldz.ab.data.Updater
import kotlinx.coroutines.launch

private sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class Failed(val messageRes: Int) : UpdateState
}

/** Ayarlar'da "Güncelleme": yalnızca dokununca GitHub sürümlerine bakar, indirir ve kurulumu başlatır. */
@Composable
fun UpdateSection() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val current = remember { Updater.currentVersion(ctx) }
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var permHint by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.update_current, current), color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (val st = state) {
            UpdateState.Idle -> {}
            UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.update_checking))
            }
            UpdateState.UpToDate -> Text(stringResource(R.string.update_latest))
            is UpdateState.Failed -> Text(stringResource(st.messageRes), color = MaterialTheme.colorScheme.tertiary)
            is UpdateState.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.update_downloading))
                if (st.progress >= 0f) LinearProgressIndicator(progress = { st.progress }, Modifier.fillMaxWidth())
                else LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            is UpdateState.Available -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.update_available, st.info.version), style = MaterialTheme.typography.titleSmall)
                if (st.info.notes.isNotBlank()) Text(st.info.notes, style = MaterialTheme.typography.bodySmall)
                if (permHint) Text(stringResource(R.string.update_perm), color = MaterialTheme.colorScheme.tertiary)
                Button(
                    onClick = {
                        if (!ctx.packageManager.canRequestPackageInstalls()) {
                            permHint = true
                            ctx.startActivity(
                                Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        } else {
                            permHint = false
                            scope.launch {
                                state = UpdateState.Downloading(0f)
                                val res = Updater.download(ctx, st.info) { state = UpdateState.Downloading(it) }
                                when (res) {
                                    is DownloadResult.Ok -> {
                                        Updater.install(ctx, res.file)
                                        state = UpdateState.Available(st.info)
                                    }
                                    DownloadResult.HashMismatch -> state = UpdateState.Failed(R.string.update_hash_failed)
                                    DownloadResult.WrongSignature -> state = UpdateState.Failed(R.string.update_sig_failed)
                                    DownloadResult.Failed -> state = UpdateState.Failed(R.string.update_download_failed)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.update_install)) }
            }
        }
        OutlinedButton(
            onClick = {
                scope.launch {
                    state = UpdateState.Checking
                    val info = Updater.fetchLatest()
                    state = when {
                        info == null -> UpdateState.Failed(R.string.update_failed)
                        Updater.isNewer(info.version, current) -> UpdateState.Available(info)
                        else -> UpdateState.UpToDate
                    }
                }
            },
            enabled = state !is UpdateState.Checking && state !is UpdateState.Downloading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.update_check)) }
        Text(stringResource(R.string.update_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
