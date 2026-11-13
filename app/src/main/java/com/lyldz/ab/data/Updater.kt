package com.lyldz.ab.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val notes: String,
    val sizeBytes: Long,
    /** Aynı sürümün `<apk>.sha256` dosyası; eski sürümlerde yoktur (o zaman yalnızca imza denetlenir). */
    val sha256Url: String? = null,
)

sealed interface DownloadResult {
    data class Ok(val file: File) : DownloadResult
    data object Failed : DownloadResult
    data object HashMismatch : DownloadResult
    data object WrongSignature : DownloadResult
}

/**
 * GitHub sürümlerinden uygulama içi güncelleme. Yalnızca kullanıcı "Güncellemeyi denetle"ye
 * dokununca açık depoya (lyldz/ab) istek yapar; kimlik, konum ya da cihaz bilgisi gönderilmez.
 * İndirilen APK: adres yalnızca bu deponun sürüm adresi olabilir, varsa SHA-256 ile ve kurulu uygulamayla
 * aynı imza anahtarıyla doğrulanır; doğrulanmayan dosya kurulum ekranına verilmez.
 */
object Updater {
    private const val REPO = "lyldz/ab"
    private const val TRUSTED_PREFIX = "https://github.com/$REPO/releases/download/"
    const val USER_AGENT = "ab-android"

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

    /** İndirme adresi yalnızca bu deponun GitHub sürüm adresi olabilir (https). */
    fun isTrustedUrl(url: String): Boolean = url.startsWith(TRUSTED_PREFIX)

    /** Sürüm notundan yalnızca madde satırları: "- **Başlık:** metin" -> "• Başlık: metin". */
    fun cleanNotes(body: String): String = body.lines()
        .map { it.trim() }
        .filter { it.startsWith("- ") || it.startsWith("* ") }
        .joinToString("\n") { "• " + it.drop(2).replace("**", "").replace("`", "") }

    /** `sha256sum` çıktısından ("<64 hex>  dosya") özeti alır; biçim bozuksa null. */
    fun parseSha256(text: String): String? =
        text.trim().split(Regex("\\s+")).firstOrNull()?.lowercase()?.takeIf { it.matches(Regex("[0-9a-f]{64}")) }

    fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(32 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** GitHub "latest release" yanıtından sürüm, APK adresi ve notlar; güvenilir APK adresi yoksa null. */
    fun parseRelease(json: String): UpdateInfo? = runCatching {
        val o = JSONObject(json)
        val tag = o.optString("tag_name").takeIf { it.isNotEmpty() } ?: return null
        val assets = o.optJSONArray("assets") ?: return null
        var apk: JSONObject? = null
        var apkName = ""
        val urls = mutableMapOf<String, String>()
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.optString("name")
            val url = a.optString("browser_download_url")
            if (url.isEmpty() || !isTrustedUrl(url)) continue
            urls[name] = url
            if (apk == null && name.endsWith(".apk")) {
                apk = a
                apkName = name
            }
        }
        val found = apk ?: return null
        UpdateInfo(
            tag, urls.getValue(apkName), cleanNotes(o.optString("body")), found.optLong("size", 0L),
            sha256Url = urls["$apkName.sha256"],
        )
    }.getOrNull()

    fun currentVersion(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "0"

    private fun open(url: String, timeout: Int): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeout
            readTimeout = timeout
            setRequestProperty("User-Agent", USER_AGENT)
        }

    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = open("https://api.github.com/repos/$REPO/releases/latest", 8000)
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            try {
                if (conn.responseCode != 200) null else parseRelease(conn.inputStream.bufferedReader().use { it.readText() })
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun fetchText(url: String): String? = runCatching {
        val conn = open(url, 8000)
        try {
            if (conn.responseCode == 200) conn.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    /** İndirilen APK'nın imzası kurulu uygulamayla aynı mı (Android 9+; eskilerde sistem yükleyici denetler). */
    fun signatureMatches(ctx: Context, apk: File): Boolean {
        if (Build.VERSION.SDK_INT < 28) return true
        return runCatching {
            val pm = ctx.packageManager
            val flag = PackageManager.GET_SIGNING_CERTIFICATES
            val archive = pm.getPackageArchiveInfo(apk.path, flag)?.signingInfo?.apkContentsSigners ?: return true
            val own = pm.getPackageInfo(ctx.packageName, flag).signingInfo?.apkContentsSigners ?: return true
            archive.map { it.toCharsString() }.toSet() == own.map { it.toCharsString() }.toSet()
        }.getOrDefault(true)
    }

    /** APK'yı önbelleğe indirir ve doğrular; [onProgress] 0..1 (boyut bilinmiyorsa -1). */
    suspend fun download(ctx: Context, info: UpdateInfo, onProgress: (Float) -> Unit): DownloadResult = withContext(Dispatchers.IO) {
        if (!isTrustedUrl(info.downloadUrl)) return@withContext DownloadResult.Failed
        val expected = info.sha256Url?.takeIf(::isTrustedUrl)?.let { fetchText(it) }?.let(::parseSha256)
        val out = runCatching {
            val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "ab.apk")
            val conn = open(info.downloadUrl, 30_000)
            try {
                if (conn.responseCode != 200) return@runCatching null
                val total = conn.contentLengthLong
                var done = 0L
                var last = 0L
                conn.inputStream.use { input ->
                    file.outputStream().use { output ->
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
                    file.delete()
                    null
                } else {
                    file
                }
            } finally {
                conn.disconnect()
            }
        }.getOrNull() ?: return@withContext DownloadResult.Failed
        if (expected != null && sha256Of(out) != expected) {
            out.delete()
            return@withContext DownloadResult.HashMismatch
        }
        if (!signatureMatches(ctx, out)) {
            out.delete()
            return@withContext DownloadResult.WrongSignature
        }
        DownloadResult.Ok(out)
    }

    /** Sistemin paket yükleyicisini açar. */
    fun install(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
