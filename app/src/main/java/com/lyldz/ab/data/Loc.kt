package com.lyldz.ab.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Yaklaşık konum (Play Services yok, yalnızca Android'in kendi servisi). Önce bilinen son konum,
 * yoksa (Android 11+) kısa bir tek seferlik istek. Koordinat şehir düzeyine yuvarlanır.
 */
@SuppressLint("MissingPermission") // izin aşağıda kontrol ediliyor
suspend fun currentCoarse(ctx: Context): Pair<Double, Double>? {
    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
    val lm = ctx.getSystemService(LocationManager::class.java) ?: return null
    val last = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
    if (last != null) return round1(last.latitude) to round1(last.longitude)
    if (Build.VERSION.SDK_INT < 30 || !runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) return null
    return withTimeoutOrNull(8_000) {
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            runCatching {
                lm.getCurrentLocation(LocationManager.NETWORK_PROVIDER, signal, ContextCompat.getMainExecutor(ctx)) { loc ->
                    cont.resume(loc?.let { round1(it.latitude) to round1(it.longitude) })
                }
            }.onFailure { cont.resume(null) }
        }
    }
}
