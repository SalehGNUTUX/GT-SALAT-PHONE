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

    // ---- صوت القرآن النصّيّ آية-بآية (audio_ayat/{reciter}/SSSAAA.mp3) ----
    private fun ayatDir(reciterId: String): File = File(context.filesDir, "audio_ayat/$reciterId").apply { mkdirs() }
    private fun ayahFile(reciterId: String, surah: Int, ayah: Int): File =
        File(ayatDir(reciterId), "${surah.toString().padStart(3, '0')}${ayah.toString().padStart(3, '0')}.mp3")
    fun localAyah(reciterId: String, surah: Int, ayah: Int): File? =
        ayahFile(reciterId, surah, ayah).takeIf { it.exists() && it.length() > 0 }

    /** هل نُزِّلت كلُّ آيات سورةٍ لقارئٍ (نصّيّ)؟ */
    fun ayatDownloaded(reciterId: String, surah: Int, verses: Int): Boolean =
        verses > 0 && (1..verses).all { localAyah(reciterId, surah, it) != null }

    /** حذف صوت آيات سورةٍ (نصّيّ). */
    fun deleteAyat(reciterId: String, surah: Int, verses: Int) {
        for (a in 1..verses) ayahFile(reciterId, surah, a).delete()
    }

    /** أرقام السور التي نُزِّل لها صوتُ آياتٍ (لأيّ قارئ) — لعرض المُنزَّل في القرآن النصّيّ. */
    fun downloadedAyatSurahs(): Set<Int> {
        val root = File(context.filesDir, "audio_ayat")
        val out = HashSet<Int>()
        root.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            dir.listFiles()?.forEach { f -> f.name.take(3).toIntOrNull()?.let { out.add(it) } }
        }
        return out
    }

    /** حذف سورةٍ منزَّلةٍ لقارئ. */
    fun deleteSurah(reciterId: String, surah: Int): Boolean = surahFile(reciterId, surah).delete()

    /** أرقام السور المُنزَّلة لقارئٍ معيّن. */
    fun downloadedSurahs(reciterId: String): Set<Int> =
        audioDir(reciterId).listFiles()
            ?.filter { it.length() > 0 }
            ?.mapNotNull { it.name.removeSuffix(".mp3").toIntOrNull() }
            ?.toSet() ?: emptySet()

    /** كلّ القرّاء الذين لهم سورٌ منزَّلة (مُعرّف القارئ → أرقام السور). */
    fun downloadedByReciter(): Map<String, Set<Int>> {
        val root = File(context.filesDir, "audio")
        return root.listFiles()?.filter { it.isDirectory }?.mapNotNull { dir ->
            val surahs = dir.listFiles()?.filter { it.length() > 0 }?.mapNotNull { it.name.removeSuffix(".mp3").toIntOrNull() }?.toSet()
            if (surahs.isNullOrEmpty()) null else dir.name to surahs
        }?.toMap() ?: emptyMap()
    }

    /** عدد صفحات المصحف المُنزَّلة. */
    fun mushafDownloadedCount(): Int = mushafDir().listFiles()?.count { it.length() > 0 } ?: 0

    /** حذف كلّ صفحات المصحف المُنزَّلة، وتصفير الحالة. */
    fun deleteMushaf() {
        mushafDir().listFiles()?.forEach { it.delete() }
        _mushaf.value = MushafDownloadState(running = false, done = 0)
    }

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

    /** تنزيل سورةٍ صوتيّاً لقارئٍ (رابط خادمٍ كامل) مع تقدّمٍ بالنسبة المئويّة. */
    suspend fun downloadSurah(reciterId: String, server: String, surah: Int, onProgress: (Int) -> Unit = {}): Boolean =
        withContext(Dispatchers.IO) {
            if (localSurah(reciterId, surah) != null) { onProgress(100); return@withContext true }
            downloadStreaming(Quran.surahAudioUrl(server, surah), surahFile(reciterId, surah), onProgress)
        }

    /**
     * تنزيل صوت آيات سورةٍ للقرآن النصّيّ (everyayah) مع بسملةٍ عند الحاجة، وتقدّمٍ بالنسبة.
     * @param folder مجلّد everyayah للقارئ.
     */
    suspend fun downloadSurahAyat(reciterId: String, folder: String, surah: Int, verses: Int, onProgress: (Int) -> Unit = {}): Boolean =
        withContext(Dispatchers.IO) {
            if (verses <= 0) return@withContext false
            val needBasmala = surah != 1 && surah != 9
            val total = verses + if (needBasmala) 1 else 0
            var done = 0
            var okAll = true
            if (needBasmala && localAyah(reciterId, surah, 0) == null) {
                // نخزّن البسملة كآيةٍ رقمها 0
                okAll = downloadStreaming(Quran.basmalaUrl(folder), ayahFile(reciterId, surah, 0)) {}
            }
            done++; onProgress((done * 100 / total))
            for (a in 1..verses) {
                if (localAyah(reciterId, surah, a) == null) {
                    if (!downloadStreaming(Quran.ayahAudioUrl(folder, surah, a), ayahFile(reciterId, surah, a)) {}) okAll = false
                }
                done++; onProgress((done * 100 / total).coerceIn(0, 100))
            }
            okAll
        }

    /** بثٌّ تدفّقيٌّ إلى ملفٍّ مع إبلاغٍ بالنسبة (يُستدعى onProgress عند تغيّر النسبة فقط). */
    private fun downloadStreaming(url: String, dest: File, onProgress: (Int) -> Unit): Boolean = runCatching {
        http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) return false
            val body = resp.body ?: return false
            val total = body.contentLength()
            val tmp = File(dest.parentFile, dest.name + ".part")
            var last = -1
            body.byteStream().use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(16 * 1024); var read: Int; var sum = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        out.write(buf, 0, read); sum += read
                        if (total > 0) {
                            val p = ((sum * 100) / total).toInt().coerceIn(0, 100)
                            if (p != last) { last = p; onProgress(p) }
                        }
                    }
                }
            }
            if (tmp.length() == 0L) { tmp.delete(); return false }
            tmp.renameTo(dest)
            true
        }
    }.getOrDefault(false)

    /** يجرّب الروابط بالترتيب، ويحفظ أوّل ناجحٍ إلى [dest] (للصور الصغيرة، بلا تقدّم). */
    private fun downloadFirst(urls: List<String>, dest: File): Boolean {
        for (url in urls) if (downloadStreaming(url, dest) {}) return true
        return false
    }
}
