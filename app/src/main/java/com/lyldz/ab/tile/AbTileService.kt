package com.lyldz.ab.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.lyldz.ab.AbApp
import com.lyldz.ab.MainActivity
import com.lyldz.ab.R
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.data.SOURCE_TILE
import com.lyldz.ab.ui.AppLocale
import com.lyldz.ab.ui.fmtVol
import com.lyldz.ab.ui.fmtVolNum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android Hızlı Ayarlar kutucuğu (Quick Settings Tile): uygulamayı açmadan bildirim çekmecesinden
 * tek dokunuşla varsayılan bardak kadar su ekler ve alt başlıkta günlük ilerlemeyi gösterir.
 */
class AbTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStartListening() {
        super.onStartListening()
        val app = applicationContext as? AbApp ?: return
        scope.launch {
            runCatching { app.repo.todayNow() }.getOrNull()?.let { today ->
                withContext(Dispatchers.Main) { render(today) }
            }
        }
    }

    override fun onClick() {
        super.onClick()
        val app = applicationContext as? AbApp ?: return
        app.appScope.launch {
            val today = runCatching { app.repo.todayNow() }.getOrNull() ?: return@launch
            if (!today.settings.onboarded) {
                withContext(Dispatchers.Main) { openApp() }
                return@launch
            }
            val ml = today.settings.cupMl
            app.addWater(ml, SOURCE_TILE)
            val updated = runCatching { app.repo.todayNow() }.getOrNull()
            withContext(Dispatchers.Main) {
                val lctx = AppLocale.wrapContext(applicationContext)
                updated?.let(::render)
                if (today.settings.tileToastEnabled) {
                    Toast.makeText(
                        applicationContext,
                        lctx.getString(R.string.added_vol, fmtVol(ml, today.settings.unit)),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun render(today: AbRepository.Today) {
        val tile = qsTile ?: return
        val lctx = AppLocale.wrapContext(this)
        if (!today.settings.onboarded) {
            tile.label = lctx.getString(R.string.tile_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = lctx.getString(R.string.tile_setup)
            }
            tile.state = Tile.STATE_INACTIVE
        } else {
            val u = today.settings.unit
            tile.label = "+${fmtVol(today.settings.cupMl, u)}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "${fmtVolNum(today.drankMl, u)} / ${fmtVol(today.goalMl, u)}"
            }
            tile.state = if (today.drankMl >= today.goalMl) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated") // API 34+ PendingIntent dalını kullanır
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        fun requestRefresh(ctx: Context) {
            runCatching {
                requestListeningState(ctx, ComponentName(ctx, AbTileService::class.java))
            }
        }
    }
}
