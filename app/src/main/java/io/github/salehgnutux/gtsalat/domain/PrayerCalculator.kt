package io.github.salehgnutux.gtsalat.domain

import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.Qibla
import com.batoulapps.adhan.data.DateComponents
import java.time.LocalDate

/**
 * حساب مواقيت الصلاة والقبلة محلّيّاً عبر مكتبة adhan — يعمل دون إنترنت.
 * نفس المكتبة (Batoul Apps) المعتمَدة في نسخة سطح المكتب.
 */
object PrayerCalculator {

    fun computeDay(
        date: LocalDate,
        lat: Double,
        lon: Double,
        methodId: Int,
        madhab: AsrMadhab,
    ): DayTimetable {
        val coords = Coordinates(lat, lon)
        val params = CalculationMethods.parametersOf(methodId).apply {
            this.madhab = if (madhab == AsrMadhab.HANAFI) Madhab.HANAFI else Madhab.SHAFI
            this.highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        }
        val dc = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val pt = PrayerTimes(coords, dc, params)

        val prayers = listOf(
            PrayerTime(PrayerId.FAJR, pt.fajr.time),
            PrayerTime(PrayerId.SUNRISE, pt.sunrise.time),
            PrayerTime(PrayerId.DHUHR, pt.dhuhr.time),
            PrayerTime(PrayerId.ASR, pt.asr.time),
            PrayerTime(PrayerId.MAGHRIB, pt.maghrib.time),
            PrayerTime(PrayerId.ISHA, pt.isha.time),
        )
        return DayTimetable(dateIso = date.toString(), prayers = prayers)
    }

    /** حساب جدول شهرٍ كامل محلّيّاً. */
    fun computeMonth(
        year: Int,
        month: Int,
        lat: Double,
        lon: Double,
        methodId: Int,
        madhab: AsrMadhab,
    ): List<DayTimetable> {
        val first = LocalDate.of(year, month, 1)
        val days = first.lengthOfMonth()
        return (0 until days).map { i ->
            computeDay(first.plusDays(i.toLong()), lat, lon, methodId, madhab)
        }
    }

    /** اتّجاه القبلة بالدرجات من الشمال (يُستعمل في البوصلة). */
    fun qiblaDirection(lat: Double, lon: Double): Double =
        Qibla(Coordinates(lat, lon)).direction

    /**
     * الصلاة القادمة الفعليّة (باستثناء الشروق). إن مضى العشاء يعيد فجر الغد.
     */
    fun nextPrayer(
        today: DayTimetable,
        tomorrowFajr: PrayerTime?,
        nowMillis: Long,
    ): NextPrayer? {
        val upcoming = today.prayers
            .filter { it.id.isPrayer && it.epochMillis > nowMillis }
            .minByOrNull { it.epochMillis }
        val prayer = upcoming ?: tomorrowFajr ?: return null
        return NextPrayer(prayer, prayer.epochMillis - nowMillis)
    }
}
