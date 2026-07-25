package io.github.salehgnutux.gtsalat.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.salehgnutux.gtsalat.data.PrayerRepository
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.domain.PrayerId
import io.github.salehgnutux.gtsalat.util.Format

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

/** يحسب لقطة الودجت من الكاش المحلّيّ (يعمل دون إنترنت والشاشة مغلقة). */
suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot {
    val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
    val settings = ep.settingsRepository().current()
    if (!settings.hasLocation) {
        return WidgetSnapshot(false, "", "", "", "", "", null, emptyList())
    }
    val repo = ep.prayerRepository()
    val today = ep.prayerRepository().todayTimetable()
    val next = repo.nextPrayer()
    val prayers = today?.prayers
        ?.filter { it.id.isPrayer }
        ?.map { Triple(it.id.arabic, Format.clock(it.epochMillis), it.id) }
        ?: emptyList()
    val remaining = next?.let { Format.countdown(it.remainingMillis) } ?: ""
    return WidgetSnapshot(
        hasLocation = true,
        city = settings.city,
        hijri = today?.hijri ?: "",
        nextName = next?.prayer?.id?.arabic ?: "",
        nextTime = next?.let { Format.clock(it.prayer.epochMillis) } ?: "",
        remaining = remaining,
        nextId = next?.prayer?.id,
        prayers = prayers,
    )
}
