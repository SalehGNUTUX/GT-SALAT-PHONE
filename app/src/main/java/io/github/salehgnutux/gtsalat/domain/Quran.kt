package io.github.salehgnutux.gtsalat.domain

import kotlinx.serialization.Serializable

/**
 * بيانات القرآن المرجعيّة (114 سورة، 30 جزءاً، 15 سجدة، 4 روايات، 13 قارئاً).
 * مُستوردة من مشروع GNUTUX الحرّ **GT-QURANREADER** إلى `assets/content/quran_meta.json`،
 * والنصّ نفسه في `tafsir.json` (حفص/العثمانيّ). الصوت والصور تُجلب عند الطلب لتُخزَّن محليّاً.
 *
 * أنماط الروابط (كما في GT-QURANREADER):
 * - صوت آية-بآية: `https://everyayah.com/data/{folder}/{SSS}{AAA}.mp3`
 * - سورة كاملة:  `https://server8.mp3quran.net/{folder}/{SSS}.mp3`
 * - صور المصحف: `https://raw.githubusercontent.com/SalehGNUTUX/Quran-PNG/master/{PPP}.png` (+ بدائل)
 */
object Quran {
    const val TOTAL_SURAHS = 114
    const val TOTAL_PAGES = 604
    const val TOTAL_JUZ = 30

    private const val EVERYAYAH = "https://everyayah.com/data"
    private const val MP3QURAN = "https://server8.mp3quran.net"

    private fun p3(n: Int): String = n.toString().padStart(3, '0')

    /** صوت آيةٍ بعينها (للقراءة النصّيّة المتزامنة). */
    fun ayahAudioUrl(folder: String, surah: Int, ayah: Int): String =
        "$EVERYAYAH/$folder/${p3(surah)}${p3(ayah)}.mp3"

    /** رابط البسملة المنفصلة (تُشغَّل قبل الآية الأولى لكلّ سورةٍ عدا الفاتحة والتوبة). */
    fun basmalaUrl(folder: String): String = "$EVERYAYAH/$folder/001001.mp3"

    /** تلاوة سورةٍ كاملة من رابط خادمٍ كاملٍ (مثل `https://server11.mp3quran.net/qari/`). */
    fun surahAudioUrl(server: String, surah: Int): String =
        server.trimEnd('/') + "/${p3(surah)}.mp3"

    /** المصدر الأساسيّ لصورة صفحةٍ من المصحف حسب الرواية (حفص افتراضيّاً، وورش من مجمّع الملك فهد). */
    fun pageImageUrl(page: Int, riwaya: String = "hafs"): String = when (riwaya) {
        "warsh" -> "https://raw.githubusercontent.com/QuranHub/quran-pages-images/main/kfgqpc/warsh/$page.jpg"
        else -> "https://raw.githubusercontent.com/SalehGNUTUX/Quran-PNG/master/${p3(page)}.png"
    }

    /** مصادر بديلة تُجرَّب بالترتيب عند تعذّر الأساسيّ (لحفص فقط؛ ورش من مصدرٍ واحدٍ موثوق). */
    fun pageImageFallbacks(page: Int, riwaya: String = "hafs"): List<String> = when (riwaya) {
        "warsh" -> emptyList()
        else -> listOf(
            "https://quranpages.github.io/pages/page_${p3(page)}.png",
            "https://www.everyayah.com/data/images_png/${p3(page)}.png",
            "https://raw.githubusercontent.com/risan/quran-images/master/images/${p3(page)}.png",
        )
    }

    /**
     * تطبيعٌ عربيٌّ للبحث الشامل: إزالة التشكيل والتطويل والبسملة الزائدة، وتوحيد الألف والياء
     * والتاء المربوطة والهمزات — فيُطابق «الرحمن» ما رُسم «ٱلرَّحْمَٰن»، و«انا» ما رُسم «إنَّا».
     */
    private val diacritics = "[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640\\uFEFF]".toRegex()
    fun normalize(s: String): String = s
        .replace(diacritics, "")
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
        .replace('ى', 'ي').replace('ئ', 'ي').replace('ؤ', 'و').replace('ة', 'ه')
        .trim().lowercase()
}

/** نتيجة بحثٍ داخل الآيات: السورة ورقمها ونصّها. */
data class AyahHit(val surah: Int, val surahName: String, val ayah: Int, val text: String)

@Serializable
data class SurahMeta(
    val n: Int,
    val ar: String,
    val en: String = "",
    val verses: Int = 0,
    val place: String = "",          // مكية | مدنية
    val page: Int = 1,               // صفحة البداية في مصحف المدينة
    val aliases: List<String> = emptyList(),
)

@Serializable
data class JuzMeta(val n: Int, val page: Int, val surah: Int, val verse: Int)

@Serializable
data class SajdaMeta(val surah: Int, val ayah: Int, val page: Int, val type: String)

@Serializable
data class Riwaya(
    val id: String,
    val ar: String,
    val full: String,
    val apiSlug: String,
    val font: String,
)

@Serializable
data class Reciter(
    val id: String,
    val ar: String,
    val style: String = "",
    val riwaya: String,              // hafs | warsh | qaloon | aldoori
    val everyayah: String = "",      // مجلّد everyayah (آية-بآية) — قد يكون فارغاً
    val mp3quran: String = "",       // مجلّد mp3quran (سورة كاملة) — قد يكون فارغاً
) {
    val hasAyahAudio: Boolean get() = everyayah.isNotBlank()
    val hasSurahAudio: Boolean get() = mp3quran.isNotBlank()
}

/** قارئُ تلاوةٍ كاملةٍ (القرآن المسموع) برابط خادمٍ كامل من mp3quran. */
@Serializable
data class SurahReciter(
    val id: String,
    val ar: String,
    val riwaya: String,   // hafs | warsh
    val server: String,   // رابط الخادم الكامل (ينتهي بـ /)
)

@Serializable
data class QuranMeta(
    val totalPages: Int = 604,
    val surahs: List<SurahMeta> = emptyList(),
    val juz: List<JuzMeta> = emptyList(),
    val sajda: List<SajdaMeta> = emptyList(),
    val riwayat: List<Riwaya> = emptyList(),
    val reciters: List<Reciter> = emptyList(),
    val surahReciters: List<SurahReciter> = emptyList(),
)

/** آيةٌ نصّيّة (رقم + نصّ) — تُشتقّ من tafsir.json. */
data class QuranAyah(val n: Int, val text: String)
