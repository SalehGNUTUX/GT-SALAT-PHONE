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
    val clockNow: String = "",       // الساعة الحاليّة لحظة التحديث (لودجت الساعة)
    val prevName: String = "",       // اسم الصلاة السابقة (لشريط التقدّم)
    val progress: Float = 0f,        // نسبة انقضاء المدّة بين الصلاة السابقة والقادمة (0..1)
    val gregorian: String = "",      // التاريخ الميلاديّ المختصر (تحت الهجريّ)
    val remainingMillis: Long = 0L,  // المدّة المتبقّية للصلاة القادمة (لعدّاد Chronometer الحيّ)
    val use24: Boolean = true,       // نظام 24/12 ساعة (لساعة TextClock الحيّة)
    val progressStyle: String = "classic", // نمط ودجت التقدّم: classic | center | day
    val weekday: String = "",        // اسم اليوم (للنمط اليوميّ: كبيرٌ في الوسط)
)

/** نقطة دخول Hilt لجلب المستودعات داخل الودجت (خارج دورة حياة Compose). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun prayerRepository(): PrayerRepository
    fun settingsRepository(): SettingsRepository
}

private val EMPTY = WidgetSnapshot(false, "", "", "", "", "", null, emptyList(), "", "", 0f, "", 0L, true, "classic", "")

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
    // نفس مصدر التطبيق (Room→API→محلّيّ) لتطابق مواقيت الودجت مع الشاشة الرئيسيّة تماماً.
    // Room ملفٌّ يُقرأ دون حاجةٍ لتشغيل التطبيق؛ بمهلةٍ قصيرةٍ وسقوطٍ محلّيٍّ فلا يعلق الودجت أو يفرغ.
    val today = kotlinx.coroutines.withTimeoutOrNull(2500) {
        runCatching { ep.prayerRepository().todayTimetable() }.getOrNull()
    } ?: PrayerCalculator.computeDay(LocalDate.now(), lat, lon, settings.methodId, settings.madhab)
    val tomorrowFajr = PrayerCalculator
        .computeDay(LocalDate.now().plusDays(1), lat, lon, settings.methodId, settings.madhab)
        .time(PrayerId.FAJR)
    val yesterdayIsha = PrayerCalculator
        .computeDay(LocalDate.now().minusDays(1), lat, lon, settings.methodId, settings.madhab)
        .time(PrayerId.ISHA)
    val next = PrayerCalculator.nextPrayer(today, tomorrowFajr, now)

    // شريط التقدّم: نسبة انقضاء المدّة بين الصلاة السابقة والقادمة (عشاء الأمس حدٌّ بعد منتصف الليل).
    val todayPrayers = today.prayers.filter { it.id.isPrayer }
    val bounds = (todayPrayers.map { it.epochMillis } + listOfNotNull(yesterdayIsha?.epochMillis, tomorrowFajr?.epochMillis)).sorted()
    val prevBound = bounds.lastOrNull { it <= now }
    val nextE = next?.prayer?.epochMillis
    val progress = if (prevBound != null && nextE != null && nextE > prevBound)
        ((now - prevBound).toFloat() / (nextE - prevBound)).coerceIn(0f, 1f) else 0f
    val prevName = todayPrayers.lastOrNull { it.epochMillis <= now }?.id?.arabic
        ?: if (prevBound == yesterdayIsha?.epochMillis) PrayerId.ISHA.arabic else ""

    // التاريخ الهجريّ من نفس جدول اليوم المجلوب أعلاه (لا استدعاءً ثانياً)
    val hijri = today.hijri.orEmpty()
    // التاريخ الميلاديّ المختصر (يُحسَب محليّاً وفق مخطّط الأشهر الإقليميّ)
    val scheme = io.github.salehgnutux.gtsalat.domain.GregorianMonths.effective(settings.monthScheme, settings.country)
    val td = LocalDate.now()
    // «م» في آخره على غرار «هـ» في الهجريّ (تنسيقٌ متناسق).
    val gregorian = "${td.dayOfMonth} ${io.github.salehgnutux.gtsalat.domain.GregorianMonths.monthName(td.monthValue, scheme)} ${td.year} م"

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
        clockNow = Format.clock(now),
        prevName = prevName,
        progress = progress,
        gregorian = gregorian,
        remainingMillis = next?.remainingMillis ?: 0L,
        use24 = settings.clock24h,
        progressStyle = settings.widgetProgressStyle,
        weekday = Format.weekdayName(LocalDate.now()),
    )
}.getOrDefault(EMPTY)
