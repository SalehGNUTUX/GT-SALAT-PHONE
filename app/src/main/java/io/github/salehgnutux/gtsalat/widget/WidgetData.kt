package io.github.salehgnutux.gtsalat.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.salehgnutux.gtsalat.data.PrayerRepository
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.domain.PrayerCalculator
import io.github.salehgnutux.gtsalat.domain.PrayerId
import io.github.salehgnutux.gtsalat.util.Format
import java.time.LocalDate

/** لقطةٌ جاهزةٌ للعرض في الودجت. */
data class WidgetSnapshot(
    val hasLocation: Boolean,
    val city: String,
    val hijri: String,
    val nextName: String,
    val nextTime: String,
    val remaining: String,
    val nextId: PrayerId?,
    /** الصلوات الخمس: (الاسم، الوقت، المعرّف). */
    val prayers: List<Triple<String, String, PrayerId>>,
)

/** نقطة دخول Hilt لجلب المستودعات داخل الودجت (خارج دورة حياة Compose). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun prayerRepository(): PrayerRepository
    fun settingsRepository(): SettingsRepository
}

private val EMPTY = WidgetSnapshot(false, "", "", "", "", "", null, emptyList())

/**
 * لقطة الودجت. المواقيت تُحسَب **محليّاً فوراً** (PrayerCalculator) فلا تعتمد على الكاش أو
 * الشبكة ولا تفرغ أبداً عند التحديث/تغيير الحجم؛ التاريخ الهجريّ من الكاش إن توفّر (أفضل جهد).
 * كلّ شيءٍ داخل runCatching فلا يظهر الودجت فارغاً عند أيّ خطأ.
 */
suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot = runCatching {
    val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
    val settings = ep.settingsRepository().current()
    val lat = settings.lat
    val lon = settings.lon
    if (!settings.hasLocation || lat == null || lon == null) return EMPTY

    val now = System.currentTimeMillis()
    val today = PrayerCalculator.computeDay(LocalDate.now(), lat, lon, settings.methodId, settings.madhab)
    val tomorrowFajr = PrayerCalculator
        .computeDay(LocalDate.now().plusDays(1), lat, lon, settings.methodId, settings.madhab)
        .time(PrayerId.FAJR)
    val next = PrayerCalculator.nextPrayer(today, tomorrowFajr, now)

    // التاريخ الهجريّ من الكاش إن توفّر بسرعة (لا يُوقِف العرض إن فشل)
    val hijri = runCatching { ep.prayerRepository().todayTimetable()?.hijri }.getOrNull().orEmpty()

    val prayers = today.prayers
        .filter { it.id.isPrayer }
        .map { Triple(it.id.arabic, Format.clock(it.epochMillis), it.id) }

    WidgetSnapshot(
        hasLocation = true,
        city = settings.city,
        hijri = hijri,
        nextName = next?.prayer?.id?.arabic ?: "",
        nextTime = next?.let { Format.clock(it.prayer.epochMillis) } ?: "",
        remaining = next?.let { Format.countdown(it.remainingMillis) } ?: "",
        nextId = next?.prayer?.id,
        prayers = prayers,
    )
}.getOrDefault(EMPTY)
