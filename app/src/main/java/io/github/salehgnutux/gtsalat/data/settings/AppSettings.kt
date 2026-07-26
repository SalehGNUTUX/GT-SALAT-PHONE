package io.github.salehgnutux.gtsalat.data.settings

import io.github.salehgnutux.gtsalat.domain.AsrMadhab
import io.github.salehgnutux.gtsalat.domain.CalendarKind
import io.github.salehgnutux.gtsalat.domain.MonthScheme

enum class AdhanType { FULL, SHORT, CUSTOM }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** نمط تنبيه دخول الوقت: أذانٌ كامل، أم رنّة تنبيهٍ قصيرة. */
enum class AdhanAlertMode { FULL, TONE }

/** كامل إعدادات التطبيق — تُخزَّن في DataStore وتُبثّ كـ Flow. */
data class AppSettings(
    val lat: Double? = null,
    val lon: Double? = null,
    val city: String = "",
    val country: String = "",
    val methodId: Int = 3,
    val madhab: AsrMadhab = AsrMadhab.SHAFI,
    val preNotifyMinutes: Int = 15,
    val adhanType: AdhanType = AdhanType.FULL,
    val customAdhanUri: String? = null,
    val customAdhanName: String = "",
    val enableSalatNotify: Boolean = true,
    val enableAdhanSound: Boolean = true,
    val enableDuaAfterAdhan: Boolean = false,
    val adhanAlertMode: AdhanAlertMode = AdhanAlertMode.FULL,
    val enablePreNotify: Boolean = true,
    val enablePreNotifySound: Boolean = true,
    val enablePostDhikr: Boolean = true,
    val postDhikrMinutes: Int = 20,
    val useApiTimetables: Boolean = true,
    val doNotDisturb: Boolean = false,
    val autoSilence: Boolean = false,
    val silenceMinutes: Int = 15,
    val persistentNotification: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val seedColor: Int = 0,   // 0 = لون السِمة الافتراضيّ (أخضر)؛ غيره ARGB مخصّص

    val monthScheme: MonthScheme = MonthScheme.AUTO,
    val timetableCalendar: CalendarKind = CalendarKind.HIJRI,
    val setupCompleted: Boolean = false,
) {
    val hasLocation: Boolean get() = lat != null && lon != null
}
