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
                val zone = ZoneId.systemDefault()
                parsed.data.map { day -> day.toDayTimetable(zone) }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun AladhanDay.toDayTimetable(zone: ZoneId): DayTimetable {
        // gregorian.date = "DD-MM-YYYY"
        val g = date.gregorian.date.split("-")
        val d = LocalDate.of(g[2].toInt(), g[1].toInt(), g[0].toInt())
        val hijriWeekday = date.hijri.weekday?.ar?.let { "$it " } ?: ""
        val hijri = "$hijriWeekday${date.hijri.day} ${date.hijri.month.ar} ${date.hijri.year} هـ".trim()

        fun t(id: PrayerId, raw: String): PrayerTime {
            val hhmm = raw.trim().split(" ")[0] // "05:12 (GMT+1)" -> "05:12"
            val lt = LocalTime.parse(hhmm)
            val millis = d.atTime(lt).atZone(zone).toInstant().toEpochMilli()
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
@Serializable private data class AladhanDay(val timings: AladhanTimings, val date: AladhanDate)
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
