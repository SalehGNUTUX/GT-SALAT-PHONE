package io.github.salehgnutux.gtsalat.domain

import kotlinx.serialization.Serializable

/** نماذج المحتوى الإسلاميّ المُستورَد من مشاريع GNUTUX (حصن المسلم/GT-SIRM)، تُقرأ من assets/content. */

@Serializable
data class AsmaName(val index: Int, val arabic: String, val meaning: String, val ref: String = "")

@Serializable
data class AsmaFile(val items: List<AsmaName>)

@Serializable
data class Hadith(
    val n: Int,
    val chapter: String = "",
    val narrator: String = "",
    val text: String,
    val source: String = "",
    val grade: String = "",
)

@Serializable
data class HadithCollection(
    val id: String,
    val name: String,
    val author: String = "",
    val description: String = "",
    val hadiths: List<Hadith>,
)

@Serializable
data class HadithFile(val collections: List<HadithCollection>)

@Serializable
data class Dua(val n: Int, val text: String, val source: String = "", val context: String = "")

@Serializable
data class DuaCategory(val id: Int, val name: String, val icon: String = "", val items: List<Dua>)

@Serializable
data class DuasFile(val categories: List<DuaCategory>)

@Serializable
data class Hikmah(val n: Int, val text: String, val sayer: String = "", val source: String = "")

@Serializable
data class HikamCategory(val id: Int, val name: String, val items: List<Hikmah>)

@Serializable
data class HikamFile(val categories: List<HikamCategory>)

/** حصن المسلم المصنّف: باب فيه أذكار، لكلّ ذكر عدد تكراره ورابط صوته (أونلاين). */
@Serializable
data class HisnDhikr(val n: Int, val text: String, val count: Int = 1, val audio: String = "")

@Serializable
data class HisnCategory(
    val id: Int,
    val name: String,
    val icon: String = "",
    val audio: String = "",
    val count: Int = 0,
    val items: List<HisnDhikr> = emptyList(),
)

@Serializable
data class HisnFile(val categories: List<HisnCategory>)

/** التفسير الميسّر: آيةٌ بنصّها العثمانيّ وتفسيرها الموجز. */
@Serializable
data class TafsirAyah(val n: Int, val text: String, val tafsir: String = "")

@Serializable
data class TafsirSurah(
    val n: Int,
    val name: String,
    val en: String = "",
    val type: String = "",
    val count: Int = 0,
    val ayahs: List<TafsirAyah> = emptyList(),
)

@Serializable
data class TafsirFile(val surahs: List<TafsirSurah>)

/** آيةٌ منتقاة لـ«آية اليوم». */
@Serializable
data class DailyAyah(val surah: String, val n: Int, val text: String)

@Serializable
data class DailyAyatFile(val items: List<DailyAyah>)

/** حدثٌ تاريخيّ إسلاميّ. hMonth/hDay هجريّان (0 = غير محدّد) لمطابقة «حدث اليوم». */
@Serializable
data class HistoryEvent(
    val title: String,
    val year: String,
    val sort: Int = 0,
    val hMonth: Int = 0,
    val hDay: Int = 0,
    val text: String = "",
)

@Serializable
data class HistoryFile(val events: List<HistoryEvent>)

/** موقعٌ جغرافيٌّ مُضمَّن (بلد/مدينة + إحداثيّات) — لاختيار الموقع دون GPS ولا إنترنت. */
@Serializable
data class Place(val country: String, val city: String, val lat: Double, val lon: Double)

@Serializable
data class PlacesFile(val places: List<Place>)

/** أدوات المواقع المُضمَّنة — استدلالٌ على المدينة من الإحداثيّات دون إنترنت. */
object Places {
    /**
     * هل هما الموقع نفسه؟ بالإحداثيّات (نحو 1 كم) أو بالاسم والبلد معاً.
     * الإحداثيّات أوّلاً لأنّ الاسم يتبدّل بين «Salé» و«سلا» بخيار الترجمة. لا توسّع العتبة:
     * الرباط وسلا بينهما نحو 4 كم، وبعتبة 0.05° (نحو 5.5 كم، في v2.3) حلّت سلا محلّ الرباط.
     */
    fun same(a: Place, b: Place): Boolean =
        (kotlin.math.abs(a.lat - b.lat) < 0.01 && kotlin.math.abs(a.lon - b.lon) < 0.012) ||
            (a.city.isNotBlank() && a.city.equals(b.city, ignoreCase = true) && a.country.equals(b.country, ignoreCase = true))

    /**
     * أقربُ مدينةٍ من القائمة المُضمَّنة لإحداثيّاتٍ معطاة، بشرط أن تكون ضمن النطاق [maxKm]
     * (وإلّا فلا نُلصق اسمًا بعيدًا) — يُستعمَل للترميز العكسيّ دون إنترنت.
     */
    fun nearest(lat: Double, lon: Double, list: List<Place>, maxKm: Double = 200.0): Place? =
        list.minByOrNull { haversineKm(lat, lon, it.lat, it.lon) }
            ?.takeIf { haversineKm(lat, lon, it.lat, it.lon) <= maxKm }

    /** المسافة بين نقطتين على سطح الأرض بالكيلومترات (صيغة هافرسين). */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
