package com.lyldz.ab.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val downloadUrl: String, val notes: String, val sizeBytes: Long)

/**
 * GitHub sürümlerinden uygulama içi güncelleme. Yalnızca kullanıcı "Güncellemeyi denetle"ye
 * dokununca açık depoya (lyldz/ab) tek bir GET yapar; kimlik, konum ya da cihaz bilgisi gönderilmez.
 */
object Updater {
    private const val REPO = "lyldz/ab"

    /** "v0.3.0" > "0.2.0" gibi sayısal sürüm karşılaştırması. */
    fun isNewer(remote: String, current: String): Boolean {
        fun parts(v: String) = v.removePrefix("v").trim().split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val r = parts(remote)
        val c = parts(current)
        for (i in 0 until maxOf(r.size, c.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /** Sürüm notundan yalnızca madde satırları: "- **Başlık:** metin" -> "• Başlık: metin". */
    fun cleanNotes(body: String): String = body.lines()
        .map { it.trim() }
        .filter { it.startsWith("- ") || it.startsWith("* ") }
        .joinToString("\n") { "• " + it.drop(2).replace("**", "").replace("`", "") }

    /** GitHub "latest release" yanıtından sürüm, APK adresi ve notlar; APK yoksa null. */
    fun parseRelease(json: String): UpdateInfo? = runCatching {
        val o = JSONObject(json)
        val tag = o.optString("tag_name").takeIf { it.isNotEmpty() } ?: return null
        val assets = o.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val url = a.optString("browser_download_url")
            if (a.optString("name").endsWith(".apk") && url.isNotEmpty()) {
                return UpdateInfo(tag, url, cleanNotes(o.optString("body")), a.optLong("size", 0L))
            }
        }
        null
    }.getOrNull()

    fun currentVersion(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "0"

    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "ab-android")
            try {
                if (conn.responseCode != 200) null else parseRelease(conn.inputStream.bufferedReader().use { it.readText() })
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    /** APK'yı önbelleğe indirir; [onProgress] 0..1 (boyut bilinmiyorsa -1). Başarısızlıkta null. */
    suspend fun download(ctx: Context, url: String, onProgress: (Float) -> Unit): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val out = File(dir, "ab.apk")
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.setRequestProperty("User-Agent", "ab-android")
            try {
                if (conn.responseCode != 200) return@runCatching null
                val total = conn.contentLengthLong
                var done = 0L
                var last = 0L
                conn.inputStream.use { input ->
                    out.outputStream().use { output ->
                        val buf = ByteArray(32 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            done += n
                            val now = System.currentTimeMillis()
                            if (now - last > 150) {
                                last = now
                                onProgress(if (total > 0) done.toFloat() / total else -1f)
                            }
                        }
                    }
                }
                if (total > 0 && done != total) {
                    out.delete()
                    null
                } else {
                    out
                }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    /** Sistemin paket yükleyicisini açar (yeni sürüm aynı anahtarla imzalıysa üstüne kurulur). */
    fun install(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
