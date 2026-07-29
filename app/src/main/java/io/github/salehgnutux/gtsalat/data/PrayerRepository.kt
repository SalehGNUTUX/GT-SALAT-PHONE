package io.github.salehgnutux.gtsalat.data

import io.github.salehgnutux.gtsalat.data.local.TimetableDao
import io.github.salehgnutux.gtsalat.data.local.TimetableEntity
import io.github.salehgnutux.gtsalat.data.location.DetectedLocation
import io.github.salehgnutux.gtsalat.data.location.LocationProvider
import io.github.salehgnutux.gtsalat.data.remote.AladhanApi
import io.github.salehgnutux.gtsalat.data.settings.AppSettings
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.domain.CalculationMethods
import io.github.salehgnutux.gtsalat.domain.DayTimetable
import io.github.salehgnutux.gtsalat.domain.NextPrayer
import io.github.salehgnutux.gtsalat.domain.PrayerCalculator
import io.github.salehgnutux.gtsalat.domain.PrayerId
import io.github.salehgnutux.gtsalat.domain.PrayerTime
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * المصدر الموحّد للمواقيت. سلسلة السقوط: Room (كاش طويل) → AlAdhan API → حساب محلّيّ.
 * كلّ ما يُجلب من API أو يُحسب محلّيّاً يُخزَّن في Room ليعمل بعدها دون إنترنت لأشهرٍ طويلة.
 */
@Singleton
class PrayerRepository @Inject constructor(
    private val dao: TimetableDao,
    private val api: AladhanApi,
    private val settingsRepo: SettingsRepository,
    private val locationProvider: LocationProvider,
) {
    private fun locKey(lat: Double, lon: Double): String =
        String.format(Locale.US, "%.2f_%.2f", lat, lon)

    suspend fun monthTimetable(year: Int, month: Int): List<DayTimetable> =
        monthTimetable(year, month, settingsRepo.current())

    /** جدول شهرٍ كامل، مع تخزينه محلّيّاً. يعيد قائمة فارغة إن لم يُضبَط الموقع بعد. */
    suspend fun monthTimetable(year: Int, month: Int, s: AppSettings): List<DayTimetable> {
        val lat = s.lat ?: return emptyList()
        val lon = s.lon ?: return emptyList()
        val key = locKey(lat, lon)
        val first = LocalDate.of(year, month, 1)
        val last = first.withDayOfMonth(first.lengthOfMonth())

        // 1) الكاش المحلّيّ إن كان الشهر مكتملاً
        val cached = dao.range(first.toString(), last.toString(), s.methodId, key)
        if (cached.size >= first.lengthOfMonth()) {
            return cached.map { it.toDomain() }
        }

        // 2) API (إن كان مسموحاً) مع الحفظ محلّيّاً
        if (s.useApiTimetables) {
            val fromApi = api.monthlyTimetable(year, month, lat, lon, s.methodId)
            if (!fromApi.isNullOrEmpty()) {
                dao.upsertAll(fromApi.map { it.toEntity(s.methodId, key, "api") })
                return fromApi
            }
        }

        // 3) حساب محلّيّ كامل + حفظ
        val local = PrayerCalculator.computeMonth(year, month, lat, lon, s.methodId, s.madhab)
        dao.upsertAll(local.map { it.toEntity(s.methodId, key, "local") })
        return local
    }

    suspend fun todayTimetable(): DayTimetable? {
        val s = settingsRepo.current()
        if (!s.hasLocation) return null
        val now = LocalDate.now()
        val month = monthTimetable(now.year, now.monthValue, s)
        return month.firstOrNull { it.dateIso == now.toString() }
            ?: PrayerCalculator.computeDay(now, s.lat!!, s.lon!!, s.methodId, s.madhab)
    }

    suspend fun nextPrayer(nowMillis: Long = System.currentTimeMillis()): NextPrayer? {
        val s = settingsRepo.current()
        if (!s.hasLocation) return null
        val today = todayTimetable() ?: return null
        val tomorrow = LocalDate.now().plusDays(1)
        val tomorrowFajr: PrayerTime? =
            PrayerCalculator.computeDay(tomorrow, s.lat!!, s.lon!!, s.methodId, s.madhab).time(PrayerId.FAJR)
        return PrayerCalculator.nextPrayer(today, tomorrowFajr, nowMillis)
    }

    /**
     * الصلوات الـ[count] القادمة (عبر اليوم والأيّام التالية) — لتسليح عدّة إنذاراتٍ مقدّماً
     * كشبكة أمانٍ إن جُمّد التطبيق فلم يُعِد جدولة التالية.
     */
    suspend fun upcomingPrayers(count: Int = 6, nowMillis: Long = System.currentTimeMillis()): List<PrayerTime> {
        val s = settingsRepo.current()
        if (!s.hasLocation) return emptyList()
        val out = ArrayList<PrayerTime>()
        var day = LocalDate.now()
        var guard = 0
        while (out.size < count && guard < 5) {
            val dt = if (day == LocalDate.now()) todayTimetable()
            else PrayerCalculator.computeDay(day, s.lat!!, s.lon!!, s.methodId, s.madhab)
            dt?.prayers?.filter { it.id.isPrayer && it.epochMillis > nowMillis }
                ?.sortedBy { it.epochMillis }
                ?.forEach { if (out.size < count) out.add(it) }
            day = day.plusDays(1); guard++
        }
        return out.take(count)
    }

    /** تخزين مسبق لعدّة أشهر قادمة ليعمل التطبيق دون إنترنت طويلاً (+ تنظيفٌ تلقائيّ للمنصرم). */
    suspend fun prefetchMonths(count: Int = 6) {
        val s = settingsRepo.current()
        if (!s.hasLocation) return
        pruneOldMonths()
        val start = LocalDate.now().withDayOfMonth(1)
        for (i in 0 until count) {
            val d = start.plusMonths(i.toLong())
            monthTimetable(d.year, d.monthValue, s)
        }
    }

    /** حذف أيّام الأشهر المنصرمة (قبل أوّل الشهر الحاليّ) تفاديّاً للتراكم. يعيد عدد الأشهر المتبقّية. */
    suspend fun pruneOldMonths(): Int {
        dao.deleteOlderThan(LocalDate.now().withDayOfMonth(1).toString())
        return cachedMonthsCount()
    }

    suspend fun cachedMonthsCount(): Int {
        val s = settingsRepo.current()
        val lat = s.lat ?: return 0
        val lon = s.lon ?: return 0
        return dao.cachedMonthsCount(s.methodId, locKey(lat, lon))
    }

    /** اكتشاف الموقع وحفظه (يقترح طريقة الحساب حسب البلد). يعيد الموقع المكتشَف. */
    suspend fun detectAndSaveLocation(): DetectedLocation? {
        val loc = locationProvider.detect() ?: return null
        settingsRepo.setLocation(loc.lat, loc.lon, loc.city, loc.country)
        if (loc.country.isNotBlank()) {
            settingsRepo.setMethod(CalculationMethods.suggestByCountry(loc.country))
        }
        return loc
    }

    private fun TimetableEntity.toDomain(): DayTimetable = DayTimetable(
        dateIso = dateIso,
        hijri = hijri,
        prayers = listOf(
            PrayerTime(PrayerId.FAJR, fajr),
            PrayerTime(PrayerId.SUNRISE, sunrise),
            PrayerTime(PrayerId.DHUHR, dhuhr),
            PrayerTime(PrayerId.ASR, asr),
            PrayerTime(PrayerId.MAGHRIB, maghrib),
            PrayerTime(PrayerId.ISHA, isha),
        ),
    )

    private fun DayTimetable.toEntity(methodId: Int, key: String, source: String): TimetableEntity {
        fun m(id: PrayerId) = time(id)!!.epochMillis
        return TimetableEntity(
            dateIso = dateIso,
            methodId = methodId,
            locKey = key,
            hijri = hijri,
            fajr = m(PrayerId.FAJR),
            sunrise = m(PrayerId.SUNRISE),
            dhuhr = m(PrayerId.DHUHR),
            asr = m(PrayerId.ASR),
            maghrib = m(PrayerId.MAGHRIB),
            isha = m(PrayerId.ISHA),
            source = source,
            savedAt = System.currentTimeMillis(),
        )
    }
}
