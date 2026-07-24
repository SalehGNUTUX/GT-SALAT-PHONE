package io.github.salehgnutux.gtsalat.data.settings

import io.github.salehgnutux.gtsalat.domain.AsrMadhab

enum class AdhanType { FULL, SHORT, CUSTOM }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

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
    val enablePreNotify: Boolean = true,
    val useApiTimetables: Boolean = true,
    val doNotDisturb: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val setupCompleted: Boolean = false,
) {
    val hasLocation: Boolean get() = lat != null && lon != null
}
