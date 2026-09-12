package io.github.salehgnutux.gtsalat.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** حالة تنزيل/تثبيت التحديث داخل التطبيق. */
sealed interface UpdateDownload {
    data object Idle : UpdateDownload
    data class Downloading(val percent: Int) : UpdateDownload
    data class Ready(val file: File) : UpdateDownload          // نُزِّل، بانتظار موافقة تثبيت النظام
    data class NeedsPermission(val file: File) : UpdateDownload // ينقص سماح «تثبيت مصادر مجهولة»
    data class Failed(val message: String) : UpdateDownload
}

/**
 * ينزّل حزمة الـAPK للنسخة الجديدة إلى filesDir بتقدّمٍ بالنسبة المئويّة، ثمّ يستدعي
 * مثبّت النظام (شاشة التثبيت النظاميّة). لا تثبيتَ صامتاً — الموافقة النهائيّة للمستخدم دائماً.
 */
@Singleton
class UpdateInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // مهلاتٌ سخيّةٌ للاتّصال/القراءة/الكتابة فلا يُقطَع التنزيل الطويل بسهولة.
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val _state = MutableStateFlow<UpdateDownload>(UpdateDownload.Idle)
    val state: StateFlow<UpdateDownload> = _state.asStateFlow()

    private fun updatesDir() = File(context.filesDir, "updates").apply { mkdirs() }
    private fun partFile() = File(updatesDir(), "GT-SALAT-update.apk.part")
    private fun apkFile() = File(updatesDir(), "GT-SALAT-update.apk")

    /** يبدأ التنزيل عبر خدمةٍ مقدّمة (يُكمِل في الخلفية والتطبيق مغلق) — تُستدعى من الواجهة. */
    fun startDownload(url: String) {
        _state.value = UpdateDownload.Downloading(0)
        io.github.salehgnutux.gtsalat.data.UpdateDownloadService.start(context, url)
    }

    /**
     * تنزيلٌ **قابلٌ للاستئناف**: يُلحِق بالملفّ الجزئيّ (`.part`) عبر ترويسة `Range`، فإن انقطع النت
     * أو أُغلق التطبيق يُستأنف من حيث توقّف بلا بدءٍ من جديد. يعيد ملفّ الـAPK المكتمل أو null.
     */
    suspend fun downloadResumable(url: String, onProgress: (Int) -> Unit = {}): File? = withContext(Dispatchers.IO) {
        val part = partFile()
        val out = apkFile()
        runCatching {
            val existing = if (part.exists()) part.length() else 0L
            val reqB = Request.Builder().url(url)
            if (existing > 0) reqB.header("Range", "bytes=$existing-")
            http.newCall(reqB.build()).execute().use { r ->
                if (r.code != 200 && r.code != 206) return@runCatching null
                val body = r.body ?: return@runCatching null
                // إن طلبنا استئنافاً فردّ الخادمُ 200 (لا يدعم Range) نبدأ من جديد.
                val append = r.code == 206 && existing > 0
                val startAt = if (append) existing else 0L
                if (!append) runCatching { part.delete() }
                val total = if (append) existing + body.contentLength() else body.contentLength()
                java.io.RandomAccessFile(part, "rw").use { raf ->
                    raf.seek(startAt)
                    body.byteStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        var read = startAt
                        var n: Int
                        var lastPct = -1
                        while (input.read(buf).also { n = it } != -1) {
                            raf.write(buf, 0, n)
                            read += n
                            if (total > 0) {
                                val pct = ((read * 100) / total).toInt().coerceIn(0, 100)
                                if (pct != lastPct) {
                                    lastPct = pct
                                    _state.value = UpdateDownload.Downloading(pct)
                                    onProgress(pct)
                                }
                            }
                        }
                    }
                }
                if (out.exists()) out.delete()
                part.renameTo(out)
                out
            }
        }.getOrNull()
    }

    fun setDownloadFailed() {
        _state.value = UpdateDownload.Failed("تعذّر تنزيل التحديث — سيُستأنف عند إعادة المحاولة من حيث توقّف.")
    }

    /** يستدعي مثبّت النظام على [file]؛ إن نقص سماح المصادر المجهولة يوجّه المستخدم إليه. */
    fun install(file: File) {
        // أندرويد 8+: يجب أن يكون التطبيق مسموحاً له بتثبيت مصادر مجهولة.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            _state.value = UpdateDownload.NeedsPermission(file)
            val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(i) }
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { context.startActivity(i) }
        _state.value = UpdateDownload.Ready(file)
    }

    fun reset() { _state.value = UpdateDownload.Idle }
}
