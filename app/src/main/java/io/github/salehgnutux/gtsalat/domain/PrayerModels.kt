package io.github.salehgnutux.gtsalat.domain

/**
 * معرّفات الصلوات (والشروق) بترتيبها اليوميّ.
 * الشروق ليس صلاةً لكنّه وقت يُعرَض في الجدول ويُحسب.
 */
enum class PrayerId(val arabic: String) {
    FAJR("الفجر"),
    SUNRISE("الشروق"),
    DHUHR("الظهر"),
    ASR("العصر"),
    MAGHRIB("المغرب"),
    ISHA("العشاء");

    /** الشروق ليس صلاةً فعليّة للإشعار/العدّ التنازليّ. */
    val isPrayer: Boolean get() = this != SUNRISE
}

/** وقتُ صلاةٍ واحد في يومٍ محدّد. */
data class PrayerTime(
    val id: PrayerId,
    val epochMillis: Long,
)

/** جدول يومٍ كامل: التاريخ الميلاديّ + الهجريّ (إن توفّر) + الأوقات الستّة. */
data class DayTimetable(
    val dateIso: String,          // yyyy-MM-dd
    val hijri: String? = null,
    val prayers: List<PrayerTime>,
) {
    fun time(id: PrayerId): PrayerTime? = prayers.firstOrNull { it.id == id }
}

/** معلومات الصلاة القادمة والوقت المتبقّي لها. */
data class NextPrayer(
    val prayer: PrayerTime,
    val remainingMillis: Long,
)

/** وصف طريقة حسابٍ لعرضها في الإعدادات. */
data class CalculationMethodInfo(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
)

/** المذهب المعتمَد في حساب وقت العصر. */
enum class AsrMadhab { SHAFI, HANAFI }
