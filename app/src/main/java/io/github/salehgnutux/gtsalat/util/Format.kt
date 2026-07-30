package io.github.salehgnutux.gtsalat.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Format {
    /** نظام عرض الوقت: 24 ساعة (افتراضيّ) أو 12 ساعة (ص/م). يُضبَط من الإعدادات عند الإقلاع. */
    @Volatile var use24 = true

    private val fmt24 = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val fmt12 = DateTimeFormatter.ofPattern("hh:mm", Locale.US)
    private val fmtSec24 = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)
    private val fmtSec12 = DateTimeFormatter.ofPattern("hh:mm:ss", Locale.US)
    private val dayFmt = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("ar"))

    private fun meridiem(hour: Int) = if (hour < 12) " ص" else " م"

    fun clock(epochMillis: Long): String {
        val z = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        return if (use24) z.format(fmt24) else z.format(fmt12) + meridiem(z.hour)
    }

    /** الساعة الحاليّة (24 أو 12 ساعة) بأرقامٍ مغربيّة (0-9). */
    fun clockNow(): String {
        val t = java.time.LocalTime.now()
        return if (use24) t.format(fmtSec24) else t.format(fmtSec12) + meridiem(t.hour)
    }

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

    private val hijriMonths = arrayOf(
        "محرّم", "صفر", "ربيع الأوّل", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوّال", "ذو القعدة", "ذو الحجّة",
    )

    /** تاريخٌ هجريٌّ محلّيّ (أمّ القرى) مع إزاحةٍ بالأيّام لتصحيح فروق المناطق. بأرقامٍ 0-9. */
    fun hijriAdjusted(offsetDays: Int): String = hijriForDate(LocalDate.now(), offsetDays)

    /** تاريخٌ هجريٌّ محلّيّ لتاريخٍ ميلاديٍّ معيّن مع إزاحةٍ بالأيّام. */
    fun hijriForDate(date: LocalDate, offsetDays: Int): String = runCatching {
        val d = java.time.chrono.HijrahDate.from(date).plus(offsetDays.toLong(), java.time.temporal.ChronoUnit.DAYS)
        val day = d.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
        val month = d.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
        val year = d.get(java.time.temporal.ChronoField.YEAR)
        "$day ${hijriMonths[month - 1]} $year هـ"
    }.getOrDefault("")
}
