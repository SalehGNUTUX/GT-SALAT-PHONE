package io.github.salehgnutux.gtsalat.domain

import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

/**
 * أدواتُ رمضان (اعتماداً على التقويم الهجريّ Umm al-Qura في java.time).
 * قد يفرق يوماً عن رؤية الهلال المحلّيّة — يُستعمَل لعرض العدّاد والإمساكيّة تقريبيّاً.
 * الإمساك يُحسَب قبل الفجر بعشر دقائق (احتياطاً، عرفٌ شائع).
 */
object Ramadan {
    const val IMSAK_BEFORE_FAJR_MIN = 10L

    fun isRamadan(date: HijrahDate = HijrahDate.now()): Boolean =
        runCatching { date.get(ChronoField.MONTH_OF_YEAR) == 9 }.getOrDefault(false)

    fun dayOfRamadan(date: HijrahDate = HijrahDate.now()): Int =
        runCatching { date.get(ChronoField.DAY_OF_MONTH) }.getOrDefault(0)

    /** النطاق الميلاديّ لشهر رمضان (الحاليّ إن كنّا فيه، وإلّا القادم) لبناء الإمساكيّة. */
    fun ramadanGregorianRange(today: HijrahDate = HijrahDate.now()): Pair<LocalDate, LocalDate>? = runCatching {
        val month = today.get(ChronoField.MONTH_OF_YEAR)
        val year = today.get(ChronoField.YEAR) + if (month > 9) 1 else 0
        val first = HijrahChronology.INSTANCE.date(year, 9, 1)
        val len = first.lengthOfMonth()
        LocalDate.from(first) to LocalDate.from(first.plus((len - 1).toLong(), ChronoUnit.DAYS))
    }.getOrNull()

    /** كم يوماً حتى بداية رمضان القادم (0 إن كنّا فيه، سالبٌ غير ممكن). */
    fun daysUntilRamadan(today: LocalDate = LocalDate.now()): Int {
        if (isRamadan()) return 0
        val range = ramadanGregorianRange() ?: return -1
        return ChronoUnit.DAYS.between(today, range.first).toInt().coerceAtLeast(0)
    }
}
