package io.github.salehgnutux.gtsalat.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.domain.QuranAyah
import io.github.salehgnutux.gtsalat.domain.QuranMeta
import io.github.salehgnutux.gtsalat.domain.Reciter
import io.github.salehgnutux.gtsalat.domain.Riwaya
import io.github.salehgnutux.gtsalat.domain.SurahMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
    private var metaCache: QuranMeta? = null

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
    suspend fun surahReciters(): List<io.github.salehgnutux.gtsalat.domain.SurahReciter> = meta().surahReciters
    suspend fun sajda() = meta().sajda
    suspend fun juz() = meta().juz

    /** نصّ آيات سورةٍ (حفص/العثمانيّ) من tafsir.json. */
    suspend fun ayat(surah: Int): List<QuranAyah> =
        content.tafsirSurah(surah)?.ayahs?.map { QuranAyah(it.n, it.text) } ?: emptyList()
}
