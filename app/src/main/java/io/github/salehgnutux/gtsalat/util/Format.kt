package io.github.salehgnutux.gtsalat.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Format {
    private val clockFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val clockSecFmt = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)
    private val dayFmt = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("ar"))

    fun clock(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(clockFmt)

    /** الساعة الحاليّة HH:mm:ss بأرقامٍ مغربيّة (0-9). */
    fun clockNow(): String = java.time.LocalTime.now().format(clockSecFmt)

    fun weekdayDate(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dayFmt)

    /** عدّاد تنازليّ HH:MM:SS بأرقام لاتينيّة (0-9). */
    fun countdown(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    /** زاوية بالدرجات بأرقام لاتينيّة، مثل «213°». */
    fun degrees(deg: Float): String = String.format(Locale.US, "%.0f°", deg)

    private val ar = Locale("ar")

    fun weekdayName(date: LocalDate): String = date.dayOfWeek.getDisplayName(TextStyle.FULL, ar)

    /** التاريخ الميلاديّ بالعربيّة وأرقامٍ مغربيّة (0-9) وفق مخطّط الأشهر الإقليميّ. */
    fun gregorianArabic(
        date: LocalDate = LocalDate.now(),
        scheme: io.github.salehgnutux.gtsalat.domain.MonthScheme = io.github.salehgnutux.gtsalat.domain.MonthScheme.STANDARD,
    ): String {
        val month = io.github.salehgnutux.gtsalat.domain.GregorianMonths.monthName(date.monthValue, scheme)
        return "${weekdayName(date)}، ${date.dayOfMonth} $month ${date.year}"
    }

    /** «يوليوز 2026» — الشهر والسنة فقط وفق المخطّط. */
    fun monthYear(year: Int, month: Int, scheme: io.github.salehgnutux.gtsalat.domain.MonthScheme): String =
        "${io.github.salehgnutux.gtsalat.domain.GregorianMonths.monthName(month, scheme)} $year"
}
