package io.github.salehgnutux.gtsalat.data.remote

import io.github.salehgnutux.gtsalat.domain.DayTimetable
import io.github.salehgnutux.gtsalat.domain.PrayerId
import io.github.salehgnutux.gtsalat.domain.PrayerTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * جلب جدول شهرٍ من AlAdhan API (يتضمّن التاريخ الهجريّ). النتيجة تُخزَّن محلّيّاً
 * في Room لتعمل بعدها دون إنترنت. مسارٌ اختياريّ؛ يفشل بهدوء فيرجع null.
 */
@Singleton
class AladhanApi @Inject constructor(
    private val http: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun monthlyTimetable(
        year: Int,
        month: Int,
        lat: Double,
        lon: Double,
        methodId: Int,
    ): List<DayTimetable>? = withContext(Dispatchers.IO) {
        val url = "https://api.aladhan.com/v1/calendar/$year/$month" +
            "?latitude=$lat&longitude=$lon&method=$methodId"
        try {
            val req = Request.Builder().url(url).build()
            http.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                val body = res.body?.string() ?: return@withContext null
                val parsed = json.decodeFromString<AladhanResponse>(body)
                parsed.data.map { day ->
                    // منطقة الموقع نفسه (من meta) لا منطقة الجهاز — وإلّا اختلّت الأوقات عند المسافر.
                    val zone = day.meta?.timezone
                        ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
                        ?: ZoneId.systemDefault()
                    day.toDayTimetable(zone)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * إزاحة الموقع عن التوقيت العالميّ اليوم كما يعتمدها الخادم (طلبٌ خفيف ليومٍ واحد)، مع اسم منطقته.
     * null عند أيّ فشل.
     */
    suspend fun locationOffsetToday(lat: Double, lon: Double): Pair<Int, String>? = withContext(Dispatchers.IO) {
        runCatching {
            val today = LocalDate.now()
            val date = String.format(java.util.Locale.US, "%02d-%02d-%04d", today.dayOfMonth, today.monthValue, today.year)
            val url = "https://api.aladhan.com/v1/timings/$date?latitude=$lat&longitude=$lon&iso8601=true"
            http.newCall(Request.Builder().url(url).build()).execute().use { res ->
                if (!res.isSuccessful) return@use null
                val data = json.decodeFromString<AladhanSingleResponse>(res.body?.string() ?: return@use null).data
                val offset = java.time.OffsetDateTime.parse(data.timings?.get("Dhuhr") ?: return@use null).offset
                offset.totalSeconds / 60 to data.meta?.timezone.orEmpty()
            }
        }.getOrNull()
    }

    companion object {
        private val offsetPattern = Regex("""\((?:GMT|UTC)?([+-])(\d{1,2})(?::?(\d{2}))?\)""")

        /** «05:12 (+01)» ← +01:00؛ null إن غابت الإزاحة. */
        fun parseOffset(raw: String): java.time.ZoneOffset? {
            val m = offsetPattern.find(raw) ?: return null
            val sign = if (m.groupValues[1] == "-") -1 else 1
            val h = m.groupValues[2].toIntOrNull() ?: return null
            val min = m.groupValues[3].toIntOrNull() ?: 0
            return runCatching { java.time.ZoneOffset.ofHoursMinutes(sign * h, sign * min) }.getOrNull()
        }
    }

    private fun AladhanDay.toDayTimetable(zone: ZoneId): DayTimetable {
        // gregorian.date = "DD-MM-YYYY"
        val g = date.gregorian.date.split("-")
        val d = LocalDate.of(g[2].toInt(), g[1].toInt(), g[0].toInt())
        val hijriWeekday = date.hijri.weekday?.ar?.let { "$it " } ?: ""
        val hijri = "$hijriWeekday${date.hijri.day} ${date.hijri.month.ar} ${date.hijri.year} هـ".trim()

        fun t(id: PrayerId, raw: String): PrayerTime {
            val hhmm = raw.trim().split(" ")[0] // "05:12 (+01)" -> "05:12"
            val lt = LocalTime.parse(hhmm)
            // الإزاحة التي يرسلها الخادم مع الوقت أولى من قاعدة مناطق الجهاز، فقد تكون قديمة
            // (غيّر المغرب توقيته إلى GMT+0 في 20 شتنبر 2026، فأخطأت الهواتف غير المحدَّثة بساعة).
            val instant = parseOffset(raw)?.let { d.atTime(lt).toInstant(it) } ?: d.atTime(lt).atZone(zone).toInstant()
            val millis = instant.toEpochMilli()
            return PrayerTime(id, millis)
        }
        return DayTimetable(
            dateIso = d.toString(),
            hijri = hijri,
            prayers = listOf(
                t(PrayerId.FAJR, timings.Fajr),
                t(PrayerId.SUNRISE, timings.Sunrise),
                t(PrayerId.DHUHR, timings.Dhuhr),
                t(PrayerId.ASR, timings.Asr),
                t(PrayerId.MAGHRIB, timings.Maghrib),
                t(PrayerId.ISHA, timings.Isha),
            ),
        )
    }
}

@Serializable private data class AladhanResponse(val data: List<AladhanDay>)
@Serializable private data class AladhanSingleResponse(val data: AladhanDayMeta)
@Serializable private data class AladhanDayMeta(val timings: Map<String, String>? = null, val meta: AladhanMeta? = null)
@Serializable private data class AladhanDay(
    val timings: AladhanTimings,
    val date: AladhanDate,
    val meta: AladhanMeta? = null,
)
@Serializable private data class AladhanMeta(val timezone: String? = null)
@Serializable private data class AladhanTimings(
    @SerialName("Fajr") val Fajr: String,
    @SerialName("Sunrise") val Sunrise: String,
    @SerialName("Dhuhr") val Dhuhr: String,
    @SerialName("Asr") val Asr: String,
    @SerialName("Maghrib") val Maghrib: String,
    @SerialName("Isha") val Isha: String,
)
@Serializable private data class AladhanDate(val gregorian: AladhanGreg, val hijri: AladhanHijri)
@Serializable private data class AladhanGreg(val date: String)
@Serializable private data class AladhanHijri(
    val day: String,
    val month: AladhanMonth,
    val year: String,
    val weekday: AladhanWeekday? = null,
)
@Serializable private data class AladhanMonth(val ar: String)
@Serializable private data class AladhanWeekday(val ar: String? = null)
