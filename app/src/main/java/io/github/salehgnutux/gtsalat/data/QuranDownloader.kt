package io.github.salehgnutux.gtsalat.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.domain.Quran
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

/** حالة تنزيلٍ جارٍ (المصحف). */
data class MushafDownloadState(val running: Boolean = false, val done: Int = 0, val total: Int = 604)

/**
 * تنزيل محتوى القرآن للاستخدام **دون إنترنت**: صور صفحات المصحف والسور صوتيّاً.
 * تُخزَّن الملفّات في `filesDir` (مصحف: mushaf/PPP.png · صوت: audio/{reciter}/SSS.mp3)،
 * وتُقرأ محليّاً إن وُجدت قبل اللجوء للشبكة.
 */
@Singleton
class QuranDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val http = OkHttpClient()

    private val _mushaf = MutableStateFlow(MushafDownloadState())
    val mushaf: StateFlow<MushafDownloadState> = _mushaf.asStateFlow()

    private fun mushafDir(): File = File(context.filesDir, "mushaf").apply { mkdirs() }
    private fun audioDir(reciterId: String): File = File(context.filesDir, "audio/$reciterId").apply { mkdirs() }

    private fun pageFile(page: Int): File = File(mushafDir(), "${page.toString().padStart(3, '0')}.png")
    fun localPage(page: Int): File? = pageFile(page).takeIf { it.exists() && it.length() > 0 }

    private fun surahFile(reciterId: String, surah: Int): File =
        File(audioDir(reciterId), "${surah.toString().padStart(3, '0')}.mp3")
    fun localSurah(reciterId: String, surah: Int): File? =
        surahFile(reciterId, surah).takeIf { it.exists() && it.length() > 0 }

    /** عدد صفحات المصحف المُنزَّلة. */
    fun mushafDownloadedCount(): Int = mushafDir().listFiles()?.count { it.length() > 0 } ?: 0

    /** تنزيل كامل صور المصحف (604 صفحة) — يتخطّى الموجود، ويُحدّث [mushaf]. */
    suspend fun downloadMushaf() = withContext(Dispatchers.IO) {
        if (_mushaf.value.running) return@withContext
        _mushaf.value = MushafDownloadState(running = true, done = mushafDownloadedCount())
        try {
            for (page in 1..Quran.TOTAL_PAGES) {
                if (localPage(page) == null) {
                    val urls = listOf(Quran.pageImageUrl(page)) + Quran.pageImageFallbacks(page)
                    downloadFirst(urls, pageFile(page))
                }
                _mushaf.value = _mushaf.value.copy(done = page)
            }
        } finally {
            _mushaf.value = _mushaf.value.copy(running = false, done = mushafDownloadedCount())
        }
    }

    /** تنزيل سورةٍ صوتيّاً لقارئٍ (رابط خادمٍ كامل). يعيد true عند النجاح أو وجودها. */
    suspend fun downloadSurah(reciterId: String, server: String, surah: Int): Boolean = withContext(Dispatchers.IO) {
        if (localSurah(reciterId, surah) != null) return@withContext true
        downloadFirst(listOf(Quran.surahAudioUrl(server, surah)), surahFile(reciterId, surah))
    }

    /** يجرّب الروابط بالترتيب، ويحفظ أوّل ناجحٍ إلى [dest]. */
    private fun downloadFirst(urls: List<String>, dest: File): Boolean {
        for (url in urls) {
            val ok = runCatching {
                http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@use false
                    val bytes = resp.body?.bytes() ?: return@use false
                    if (bytes.isEmpty()) return@use false
                    val tmp = File(dest.parentFile, dest.name + ".part")
                    tmp.writeBytes(bytes)
                    tmp.renameTo(dest)
                    true
                }
            }.getOrDefault(false)
            if (ok) return true
        }
        return false
    }
}
