package io.github.salehgnutux.gtsalat.domain

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** اختلاف منطقة الهاتف عن منطقة الموقع الآن. [diffMinutes] موجبٌ إن تقدّمت منطقة الموقع. */
data class TimeZoneMismatch(val device: String, val location: String, val diffMinutes: Int)

/**
 * المواقيت لحظاتٌ صحيحة تُعرَض بمنطقة الهاتف، فإن خالفت إزاحتُه إزاحةَ الموقع ظهرت مزاحةً عن ساعة المدينة.
 */
object TimeZoneCheck {
    /**
     * [locationOffsetMinutes]: إزاحة الموقع كما يعتمدها الخادم اليوم، لا قاعدة مناطق الجهاز التي قد تكون قديمة.
     * يقارن بإزاحة الهاتف الآن، فمنطقتان مختلفتا الاسم بالإزاحة نفسها لا تُعدّان اختلافاً.
     */
    fun mismatch(locationOffsetMinutes: Int, deviceZone: ZoneId, now: Instant = Instant.now()): TimeZoneMismatch? {
        val dev = deviceZone.rules.getOffset(now)
        val loc = runCatching { ZoneOffset.ofTotalSeconds(locationOffsetMinutes * 60) }.getOrNull() ?: return null
        if (loc == dev) return null
        return TimeZoneMismatch(label(dev), label(loc), (loc.totalSeconds - dev.totalSeconds) / 60)
    }

    /** «GMT+01:00» بأرقامٍ 0-9. */
    fun label(offset: ZoneOffset): String = if (offset.totalSeconds == 0) "GMT+00:00" else "GMT${offset.id}"
}
