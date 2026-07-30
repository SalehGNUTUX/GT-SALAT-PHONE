package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.AzkarRepository
import io.github.salehgnutux.gtsalat.data.ContentRepository
import io.github.salehgnutux.gtsalat.data.PrayerRepository
import io.github.salehgnutux.gtsalat.data.settings.AppSettings
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.domain.DayTimetable
import io.github.salehgnutux.gtsalat.domain.Hikmah
import io.github.salehgnutux.gtsalat.domain.NextPrayer
import io.github.salehgnutux.gtsalat.domain.PrayerCalculator
import io.github.salehgnutux.gtsalat.domain.PrayerId
import io.github.salehgnutux.gtsalat.domain.PrayerTime
import io.github.salehgnutux.gtsalat.util.Format
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

data class DashboardUi(
    val loading: Boolean = true,
    val hasLocation: Boolean = false,
    val city: String = "",
    val today: DayTimetable? = null,
    val hijri: String? = null,
    val gregorian: String = "",
    val clock: String = "",
    val next: NextPrayer? = null,
    val countdownText: String = "",
    val dhikr: String = "",
    val hikmah: Hikmah? = null,
    val ayah: io.github.salehgnutux.gtsalat.domain.DailyAyah? = null,
    val showAyah: Boolean = true,
)

/** حالةٌ خفيفةٌ تتغيّر كلّ ثانية (الساعة والعدّاد) — معزولةٌ عن [DashboardUi]
 *  حتّى لا يُعاد تركيب كامل بطاقات الرئيسيّة كلّ ثانية، بل نصوص الساعة/العدّاد فقط. */
data class DashboardTick(val clock: String = "", val countdownText: String = "", val progress: Float = 0f)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: PrayerRepository,
    private val settingsRepo: SettingsRepository,
    private val azkar: AzkarRepository,
    private val content: ContentRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(DashboardUi())
    val ui: StateFlow<DashboardUi> = _ui.asStateFlow()

    // تدفّقٌ منفصلٌ للساعة/العدّاد (يتغيّر كلّ ثانية) — تقرؤه بطاقة البطل فقط.
    private val _tick = MutableStateFlow(DashboardTick())
    val tick: StateFlow<DashboardTick> = _tick.asStateFlow()

    private var settings: AppSettings? = null
    private var today: DayTimetable? = null
    private var tomorrowFajr: PrayerTime? = null
    private var loadedDate: LocalDate? = null   // لكشف تغيّر اليوم وإعادة التحميل
    private var lastNextId: PrayerId? = null     // لإعادة بناء ui.next عند تبدّل الصلاة القادمة فقط

    private var azkarList: List<String> = emptyList()
    private var dhikr: String = ""
    private var hikmah: Hikmah? = null
    private var ayah: io.github.salehgnutux.gtsalat.domain.DailyAyah? = null

    init {
        viewModelScope.launch {
            val dayOfYear = LocalDate.now().dayOfYear
            azkarList = azkar.all()
            if (azkarList.isNotEmpty()) dhikr = azkarList[dayOfYear % azkarList.size]
            hikmah = content.hikmah(dayOfYear)
            ayah = content.dailyAyah(dayOfYear)
            rebuildUi()
        }
        viewModelScope.launch {
            settingsRepo.settings.collectLatest { s ->
                settings = s
                loadDay(s)
            }
        }
        viewModelScope.launch {
            while (true) {
                // إعادة تحميل بيانات اليوم عند تجاوز منتصف الليل (وإلّا بقيت مواقيت الأمس والعدّاد 00:00:00).
                val s = settings
                if (s != null && loadedDate != LocalDate.now()) loadDay(s)
                refreshTick()
                delay(1000)
            }
        }
    }

    /** ذكر جديد عشوائيّ (زرّ «ذكر جديد»). */
    fun refreshDhikr() {
        if (azkarList.isNotEmpty()) {
            dhikr = azkarList[Random.nextInt(azkarList.size)]
            rebuildUi()
        }
    }

    /** حكمة جديدة عشوائيّة. */
    fun refreshHikmah() {
        viewModelScope.launch {
            hikmah = content.hikmah(Random.nextInt(100000))
            rebuildUi()
        }
    }

    /** آية أخرى عشوائيّة. */
    fun refreshAyah() {
        viewModelScope.launch {
            ayah = content.dailyAyah(Random.nextInt(100000))
            rebuildUi()
        }
    }

    private suspend fun loadDay(s: AppSettings) {
        if (!s.hasLocation) {
            _ui.value = DashboardUi(loading = false, hasLocation = false)
            today = null
            return
        }
        loadedDate = LocalDate.now()
        today = repo.todayTimetable()
        tomorrowFajr = PrayerCalculator
            .computeDay(LocalDate.now().plusDays(1), s.lat!!, s.lon!!, s.methodId, s.madhab)
            .time(PrayerId.FAJR)
        rebuildUi()
    }

    private fun rebuildUi() {
        val s = settings ?: return
        val t = today
        if (!s.hasLocation || t == null) {
            _ui.value = _ui.value.copy(
                loading = false,
                hasLocation = s.hasLocation,
                gregorian = Format.gregorianArabic(scheme = io.github.salehgnutux.gtsalat.domain.GregorianMonths.effective(s.monthScheme, s.country)),
                clock = Format.clockNow(),
                dhikr = dhikr,
                hikmah = hikmah,
                ayah = ayah,
                showAyah = s.enableDailyAyah,
            )
            return
        }
        val now = System.currentTimeMillis()
        val next = PrayerCalculator.nextPrayer(t, tomorrowFajr, now)
        lastNextId = next?.prayer?.id
        _ui.value = DashboardUi(
            loading = false,
            hasLocation = true,
            city = s.city,
            today = t,
            hijri = t.hijri,
            gregorian = Format.gregorianArabic(scheme = io.github.salehgnutux.gtsalat.domain.GregorianMonths.effective(s.monthScheme, s.country)),
            clock = Format.clockNow(),
            next = next,
            countdownText = next?.let { Format.countdown(it.remainingMillis) } ?: "",
            dhikr = dhikr,
            hikmah = hikmah,
            ayah = ayah,
            showAyah = s.enableDailyAyah,
        )
    }

    /** تحديثٌ خفيفٌ كلّ ثانية: الساعة والعدّاد فقط في [_tick]؛ ولا يُعاد بناء [_ui]
     *  إلّا عند تبدّل الصلاة القادمة (لتجنّب إعادة تركيب كامل الرئيسيّة كلّ ثانية). */
    private fun refreshTick() {
        val s = settings ?: return
        val t = today
        val clock = Format.clockNow()
        if (!s.hasLocation || t == null) {
            _tick.value = DashboardTick(clock = clock, countdownText = "")
            return
        }
        val now = System.currentTimeMillis()
        val next = PrayerCalculator.nextPrayer(t, tomorrowFajr, now)
        _tick.value = DashboardTick(
            clock = clock,
            countdownText = next?.let { Format.countdown(it.remainingMillis) } ?: "",
            progress = progressToNext(t, now, next),
        )
        if (next?.prayer?.id != lastNextId) rebuildUi()
    }

    /** نسبة انقضاء المدّة بين الصلاة السابقة والقادمة (0..1) — لحلقة التقدّم في الرئيسيّة. */
    private fun progressToNext(t: DayTimetable, now: Long, next: NextPrayer?): Float {
        val nextE = next?.prayer?.epochMillis ?: return 0f
        val bounds = (t.prayers.filter { it.id.isPrayer }.map { it.epochMillis } + listOfNotNull(tomorrowFajr?.epochMillis)).sorted()
        val prev = bounds.lastOrNull { it <= now } ?: return 0f
        if (nextE <= prev) return 0f
        return ((now - prev).toFloat() / (nextE - prev)).coerceIn(0f, 1f)
    }
}
