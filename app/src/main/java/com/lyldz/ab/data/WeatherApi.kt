package com.lyldz.ab.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Open-Meteo (hesapsız, anahtarsız). Sunucuya yalnızca şehir düzeyinde yuvarlanmış koordinat
 * (sıcaklık için) ya da yazılan şehir adı (arama için) gider; başka hiçbir veri gönderilmez.
 */
object WeatherApi {
    data class City(val name: String, val region: String?, val country: String?, val lat: Double, val lon: Double) {
        val label: String get() = listOfNotNull(name, region?.takeIf { it != name }, country).joinToString(", ")
    }

    fun parseMaxTemp(json: String): Double? = runCatching {
        val a = JSONObject(json).optJSONObject("daily")?.optJSONArray("temperature_2m_max")
        if (a != null && a.length() > 0 && !a.isNull(0)) a.getDouble(0) else null
    }.getOrNull()

    fun parseCities(json: String): List<City> = runCatching {
        val a = JSONObject(json).optJSONArray("results") ?: return emptyList()
        (0 until a.length()).mapNotNull { i ->
            val o = a.getJSONObject(i)
            if (!o.has("latitude") || !o.has("longitude") || !o.has("name")) return@mapNotNull null
            City(
                o.getString("name"),
                o.optString("admin1").ifEmpty { null },
                o.optString("country").ifEmpty { null },
                round1(o.getDouble("latitude")),
                round1(o.getDouble("longitude")),
            )
        }
    }.getOrDefault(emptyList())

    suspend fun fetchMaxTemp(lat: Double, lon: Double): Double? {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${coord(lat)}&longitude=${coord(lon)}" +
            "&daily=temperature_2m_max&timezone=auto&forecast_days=1"
        return get(url)?.let(::parseMaxTemp)
    }

    suspend fun searchCities(query: String): List<City> {
        val q = query.trim().take(60)
        if (q.length < 2) return emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${Uri.encode(q)}&count=5" +
            "&language=${Locale.getDefault().language}&format=json"
        return get(url)?.let(::parseCities) ?: emptyList()
    }

    private fun coord(v: Double) = String.format(Locale.ROOT, "%.1f", v)

    private suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL(url).openConnection() as HttpURLConnection
            try {
                c.setRequestProperty("User-Agent", Updater.USER_AGENT) // Open-Meteo uygulama adını ister
                c.connectTimeout = 10_000
                c.readTimeout = 10_000
                if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() } else null
            } finally {
                c.disconnect()
            }
        }.getOrNull()
    }
}

fun round1(v: Double): Double = Math.round(v * 10) / 10.0
