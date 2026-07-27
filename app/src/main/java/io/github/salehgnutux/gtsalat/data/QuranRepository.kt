package io.github.salehgnutux.gtsalat.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.domain.QuranAyah
import io.github.salehgnutux.gtsalat.domain.QuranMeta
import io.github.salehgnutux.gtsalat.domain.Reciter
import io.github.salehgnutux.gtsalat.domain.Riwaya
import io.github.salehgnutux.gtsalat.domain.SurahMeta
import io.github.salehgnutux.gtsalat.domain.SurahReciter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مصدر بيانات القرآن: الفهرس والروايات والقرّاء من `quran_meta.json`،
 * ونصّ الآيات من `tafsir.json` (يُعاد استعماله عبر [ContentRepository]).
 */
@Singleton
class QuranRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val content: ContentRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient()
    private var metaCache: QuranMeta? = null
    private var onlineRecitersCache: List<SurahReciter>? = null

    private suspend fun meta(): QuranMeta = metaCache ?: withContext(Dispatchers.Default) {
        json.decodeFromString<QuranMeta>(
            context.assets.open("content/quran_meta.json").bufferedReader().use { it.readText() }
        ).also { metaCache = it }
    }

    suspend fun surahs(): List<SurahMeta> = meta().surahs
    suspend fun surah(n: Int): SurahMeta? = meta().surahs.firstOrNull { it.n == n }
    suspend fun riwayat(): List<Riwaya> = meta().riwayat
    suspend fun reciters(): List<Reciter> = meta().reciters
    suspend fun recitersByRiwaya(riwaya: String): List<Reciter> = meta().reciters.filter { it.riwaya == riwaya }
    suspend fun reciter(id: String): Reciter? = meta().reciters.firstOrNull { it.id == id }
    /** القرّاء المُضمَّنون (مختارون، يعملون دون إنترنت). */
    suspend fun surahReciters(): List<SurahReciter> = meta().surahReciters

    /**
     * **كلّ** قرّاء التلاوة الكاملة من مصدر mp3quran (يتيح إضافة قرّاء آخرين).
     * عند تعذّر الشبكة يسقط إلى القائمة المُضمَّنة. النتيجة مرتّبةٌ: ورش ثمّ حفص ثمّ البقيّة.
     */
    suspend fun surahRecitersOnline(): List<SurahReciter> {
        onlineRecitersCache?.let { return it }
        val fetched = withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url("https://mp3quran.net/api/v3/reciters?language=ar").build()
                http.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (body.isBlank()) emptyList()
                    else json.decodeFromString<Mp3Response>(body).reciters.flatMap { r ->
                        r.moshaf.filter { it.server.isNotBlank() }.map { m ->
                            SurahReciter("${r.id}-${m.id}", r.name, classifyRiwaya(m.name), m.server)
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }
        val order = mapOf("warsh" to 0, "hafs" to 1, "qaloon" to 2, "aldoori" to 3)
        val result = (if (fetched.isNotEmpty()) fetched else meta().surahReciters)
            .sortedWith(compareBy({ order[it.riwaya] ?: 9 }, { it.ar }))
        onlineRecitersCache = result
        return result
    }

    private fun classifyRiwaya(moshafName: String): String = when {
        moshafName.contains("ورش") -> "warsh"
        moshafName.contains("قالون") -> "qaloon"
        moshafName.contains("الدوري") -> "aldoori"
        else -> "hafs"
    }

    @Serializable private data class Mp3Response(val reciters: List<Mp3Reciter> = emptyList())
    @Serializable private data class Mp3Reciter(val id: Int = 0, val name: String = "", val moshaf: List<Mp3Moshaf> = emptyList())
    @Serializable private data class Mp3Moshaf(
        val id: Int = 0,
        val name: String = "",
        val server: String = "",
        @SerialName("surah_total") val surahTotal: Int = 0,
    )
    suspend fun sajda() = meta().sajda
    suspend fun juz() = meta().juz

    /** نصّ آيات سورةٍ (حفص/العثمانيّ) من tafsir.json. */
    suspend fun ayat(surah: Int): List<QuranAyah> =
        content.tafsirSurah(surah)?.ayahs?.map { QuranAyah(it.n, it.text) } ?: emptyList()

    /**
     * بحثٌ شاملٌ داخل نصّ القرآن (6236 آية) بالكلمات والعبارات، مع التطبيع العربيّ
     * (تجاهل التشكيل وتوحيد الحروف). يُنفَّذ على خيطٍ خلفيّ ويُحدَّد بسقفٍ لأداء الواجهة.
     */
    suspend fun searchAyat(query: String, limit: Int = 300): List<io.github.salehgnutux.gtsalat.domain.AyahHit> {
        val q = io.github.salehgnutux.gtsalat.domain.Quran.normalize(query)
        if (q.length < 2) return emptyList()
        return withContext(Dispatchers.Default) {
            val hits = ArrayList<io.github.salehgnutux.gtsalat.domain.AyahHit>()
            for (s in content.tafsirSurahs()) {
                for (a in s.ayahs) {
                    if (io.github.salehgnutux.gtsalat.domain.Quran.normalize(a.text).contains(q)) {
                        hits.add(io.github.salehgnutux.gtsalat.domain.AyahHit(s.n, s.name, a.n, a.text))
                        if (hits.size >= limit) return@withContext hits
                    }
                }
            }
            hits
        }
    }
}
