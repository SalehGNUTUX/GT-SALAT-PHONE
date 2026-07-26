package io.github.salehgnutux.gtsalat.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.domain.AsmaFile
import io.github.salehgnutux.gtsalat.domain.AsmaName
import io.github.salehgnutux.gtsalat.domain.DuaCategory
import io.github.salehgnutux.gtsalat.domain.DuasFile
import io.github.salehgnutux.gtsalat.domain.Hadith
import io.github.salehgnutux.gtsalat.domain.HadithCollection
import io.github.salehgnutux.gtsalat.domain.HadithFile
import io.github.salehgnutux.gtsalat.domain.HikamCategory
import io.github.salehgnutux.gtsalat.domain.HikamFile
import io.github.salehgnutux.gtsalat.domain.Hikmah
import io.github.salehgnutux.gtsalat.domain.DailyAyah
import io.github.salehgnutux.gtsalat.domain.DailyAyatFile
import io.github.salehgnutux.gtsalat.domain.HisnCategory
import io.github.salehgnutux.gtsalat.domain.HisnFile
import io.github.salehgnutux.gtsalat.domain.TafsirFile
import io.github.salehgnutux.gtsalat.domain.TafsirSurah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * المحتوى الإسلاميّ المرجعيّ (أسماء الله، الأحاديث، الأدعية، الحِكَم) من assets/content،
 * المُستورَد من مشاريع GNUTUX (حصن المسلم/GT-SIRM). يُقرأ ويُخزَّن في الذاكرة مرّةً.
 */
@Singleton
class ContentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private var asmaCache: List<AsmaName>? = null
    private var hadithCache: List<HadithCollection>? = null
    private var duasCache: List<DuaCategory>? = null
    private var hikamCache: List<Hikmah>? = null
    private var hikamCatCache: List<HikamCategory>? = null
    private var hisnCache: List<HisnCategory>? = null
    private var tafsirCache: List<TafsirSurah>? = null

    private suspend fun read(file: String): String = withContext(Dispatchers.IO) {
        context.assets.open("content/$file").bufferedReader().use { it.readText() }
    }

    suspend fun asma(): List<AsmaName> = asmaCache ?: run {
        json.decodeFromString<AsmaFile>(read("asma.json")).items.also { asmaCache = it }
    }

    suspend fun hadithCollections(): List<HadithCollection> = hadithCache ?: run {
        json.decodeFromString<HadithFile>(read("hadith.json")).collections.also { hadithCache = it }
    }

    suspend fun duas(): List<DuaCategory> = duasCache ?: run {
        json.decodeFromString<DuasFile>(read("duas.json")).categories.also { duasCache = it }
    }

    suspend fun hikamCategories(): List<HikamCategory> = hikamCatCache ?: run {
        json.decodeFromString<HikamFile>(read("hikam.json")).categories.also { hikamCatCache = it }
    }

    suspend fun hisnCategories(): List<HisnCategory> = hisnCache ?: run {
        json.decodeFromString<HisnFile>(read("hisn.json")).categories.also { hisnCache = it }
    }

    suspend fun hisnCategory(id: Int): HisnCategory? = hisnCategories().firstOrNull { it.id == id }

    // ملفٌّ كبير (~4MB): تُجرى القراءة وفكّ الترميز على خيطٍ خلفيّ تفادياً لتجميد الواجهة.
    suspend fun tafsirSurahs(): List<TafsirSurah> = tafsirCache ?: withContext(Dispatchers.Default) {
        json.decodeFromString<TafsirFile>(read("tafsir.json")).surahs.also { tafsirCache = it }
    }

    suspend fun tafsirSurah(n: Int): TafsirSurah? = tafsirSurahs().firstOrNull { it.n == n }

    private var ayatCache: List<DailyAyah>? = null

    suspend fun dailyAyat(): List<DailyAyah> = ayatCache ?: run {
        json.decodeFromString<DailyAyatFile>(read("daily_ayat.json")).items.also { ayatCache = it }
    }

    /** آية اليوم (ثابتةٌ لليوم عبر seed، أو عشوائيّة عند التجديد). */
    suspend fun dailyAyah(seed: Int): DailyAyah? {
        val all = dailyAyat()
        return if (all.isEmpty()) null else all[(seed % all.size + all.size) % all.size]
    }

    private suspend fun allHikam(): List<Hikmah> = hikamCache ?: run {
        hikamCategories().flatMap { it.items }.also { hikamCache = it }
    }

    /** حكمة يوميّة ثابتة لليوم (نفسها طوال اليوم)، أو عشوائيّة عند التجديد. */
    suspend fun hikmah(seed: Int): Hikmah {
        val all = allHikam()
        return if (all.isEmpty()) Hikmah(0, "") else all[(seed % all.size + all.size) % all.size]
    }
}
