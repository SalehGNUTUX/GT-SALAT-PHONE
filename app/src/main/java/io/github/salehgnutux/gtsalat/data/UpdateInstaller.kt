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
    private val http = OkHttpClient()
    private val _state = MutableStateFlow<UpdateDownload>(UpdateDownload.Idle)
    val state: StateFlow<UpdateDownload> = _state.asStateFlow()

    /** ينزّل الـAPK من [url] ثمّ يطلق التثبيت. */
    suspend fun downloadAndInstall(url: String) {
        _state.value = UpdateDownload.Downloading(0)
        val file = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, "updates").apply { mkdirs() }
                val out = File(dir, "GT-SALAT-update.apk")
                val req = Request.Builder().url(url).build()
                http.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) return@runCatching null
                    val body = r.body ?: return@runCatching null
                    val total = body.contentLength()
                    body.byteStream().use { input ->
                        out.outputStream().use { output ->
                            val buf = ByteArray(64 * 1024)
                            var read = 0L
                            var n: Int
                            while (input.read(buf).also { n = it } != -1) {
                                output.write(buf, 0, n)
                                read += n
                                if (total > 0) {
                                    _state.value = UpdateDownload.Downloading(((read * 100) / total).toInt().coerceIn(0, 100))
                                }
                            }
                        }
                    }
                    out
                }
            }.getOrNull()
        }
        if (file == null || !file.exists() || file.length() == 0L) {
            _state.value = UpdateDownload.Failed("تعذّر تنزيل التحديث — تحقّق من الاتّصال وأعد المحاولة.")
            return
        }
        _state.value = UpdateDownload.Ready(file)
        install(file)
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
