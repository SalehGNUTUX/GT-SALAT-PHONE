package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.alarm.PrayerAlarmScheduler
import io.github.salehgnutux.gtsalat.audio.AdhanPreviewer
import io.github.salehgnutux.gtsalat.data.PrayerRepository
import io.github.salehgnutux.gtsalat.data.settings.AdhanType
import io.github.salehgnutux.gtsalat.data.settings.AppSettings
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.data.settings.ThemeMode
import io.github.salehgnutux.gtsalat.domain.AsrMadhab
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val repo: PrayerRepository,
    private val scheduler: PrayerAlarmScheduler,
    private val previewer: AdhanPreviewer,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepo.settings
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** النوع الجاري معاينته الآن (لتبديل زرّ التشغيل/الإيقاف)، أو null. */
    val previewing: StateFlow<AdhanType?> = previewer.playing
    val previewKey: StateFlow<String?> = previewer.previewKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val monthsRefresh = kotlinx.coroutines.flow.MutableStateFlow(0)
    val cachedMonths: StateFlow<Int> =
        kotlinx.coroutines.flow.combine(settingsRepo.settings, monthsRefresh) { _, _ -> repo.cachedMonthsCount() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** حذف الأشهر المنصرمة يدويّاً (تفاديّاً للتراكم). */
    fun pruneOldMonths() = viewModelScope.launch { repo.pruneOldMonths(); monthsRefresh.value++ }

    private fun reschedule() = viewModelScope.launch { scheduler.scheduleNext() }

    fun setMethod(id: Int) = viewModelScope.launch { settingsRepo.setMethod(id); repo.prefetchMonths(3); reschedule() }
    fun setMadhab(m: AsrMadhab) = viewModelScope.launch { settingsRepo.setMadhab(m); reschedule() }
    fun setPreNotify(min: Int) = viewModelScope.launch { settingsRepo.setPreNotify(min); reschedule() }
    fun setAdhanType(t: AdhanType) = viewModelScope.launch { settingsRepo.setAdhanType(t) }

    /** تجربة الأذان داخل الإعدادات (تشغيل/إيقاف). */
    fun previewAdhan(t: AdhanType) {
        previewer.toggle(t, settings.value?.customAdhanUri)
    }

    fun stopPreview() = previewer.stop()

    /** معاينة أصوات التنبيهات في الإعدادات. */
    fun previewTone() = previewer.previewTone("tone")
    fun previewPreNotifySound() = previewer.previewRes("prenotify", io.github.salehgnutux.gtsalat.R.raw.prayer_approaching)
    fun previewDua() = previewer.previewRes("dua", io.github.salehgnutux.gtsalat.R.raw.dua_after_adhan)
    fun previewPostDhikr() = previewer.previewRes("dhikr", io.github.salehgnutux.gtsalat.R.raw.post_prayer_dhikr)

    /** حفظ أذانٍ مخصّص مستورَد (URI دائم + اسمٌ للعرض)، ويصير النوع «مخصّص». */
    fun setCustomAdhan(uri: String, name: String) = viewModelScope.launch {
        settingsRepo.setCustomAdhan(uri, name)
    }
    fun setEnableSalat(v: Boolean) = viewModelScope.launch { settingsRepo.setEnableSalat(v); reschedule() }
    fun setEnableAdhan(v: Boolean) = viewModelScope.launch { settingsRepo.setEnableAdhan(v) }
    fun setEnableDua(v: Boolean) = viewModelScope.launch { settingsRepo.setEnableDua(v) }
    fun setAdhanAlertMode(m: io.github.salehgnutux.gtsalat.data.settings.AdhanAlertMode) = viewModelScope.launch { settingsRepo.setAdhanAlertMode(m) }
    fun setAdhanVolume(v: Int) = viewModelScope.launch { settingsRepo.setAdhanVolume(v) }
    fun setPerPrayerAlerts(v: Boolean) = viewModelScope.launch { settingsRepo.setPerPrayerAlerts(v) }
    fun setPrayerAlert(index: Int, m: io.github.salehgnutux.gtsalat.data.settings.AdhanAlertMode) = viewModelScope.launch { settingsRepo.setPrayerAlert(index, m) }
    fun setEnablePreNotify(v: Boolean) = viewModelScope.launch { settingsRepo.setEnablePreNotify(v); reschedule() }
    fun setEnablePreNotifySound(v: Boolean) = viewModelScope.launch { settingsRepo.setEnablePreNotifySound(v) }
    fun setEnablePostDhikr(v: Boolean) = viewModelScope.launch { settingsRepo.setEnablePostDhikr(v) }
    fun setEnableDailyAyah(v: Boolean) = viewModelScope.launch { settingsRepo.setEnableDailyAyah(v) }
    fun setEnableRecitationReminder(v: Boolean) = viewModelScope.launch { settingsRepo.setEnableRecitationReminder(v); reschedule() }
    fun setEnableWhiteDaysReminder(v: Boolean) = viewModelScope.launch { settingsRepo.setEnableWhiteDaysReminder(v); reschedule() }
    fun setReminderHour(h: Int) = viewModelScope.launch { settingsRepo.setReminderHour(h); reschedule() }
    fun setUseApi(v: Boolean) = viewModelScope.launch { settingsRepo.setUseApi(v) }
    fun setDnd(v: Boolean) = viewModelScope.launch { settingsRepo.setDnd(v); reschedule() }
    fun setAutoSilence(v: Boolean) = viewModelScope.launch { settingsRepo.setAutoSilence(v) }
    fun setSilenceMinutes(m: Int) = viewModelScope.launch { settingsRepo.setSilenceMinutes(m) }
    fun setPersistentNotification(v: Boolean) = viewModelScope.launch { settingsRepo.setPersistentNotification(v); reschedule() }
    fun setTheme(t: ThemeMode) = viewModelScope.launch { settingsRepo.setTheme(t) }
    fun setClock24h(v: Boolean) = viewModelScope.launch { settingsRepo.setClock24h(v) }

    /** يجدول تنبيهاً اختباريّاً بعد دقيقة (لقياس وصول التنبيهات والشاشة مغلقة). */
    fun testNotification() = scheduler.scheduleTest()
    fun setDynamicColor(v: Boolean) = viewModelScope.launch { settingsRepo.setDynamicColor(v) }
    fun setSeedColor(argb: Int) = viewModelScope.launch { settingsRepo.setSeedColor(argb) }
    fun setGradient(dark: Boolean, top: Boolean, argb: Int) = viewModelScope.launch { settingsRepo.setGradient(dark, top, argb) }
    fun resetGradient(dark: Boolean) = viewModelScope.launch { settingsRepo.resetGradient(dark) }
    fun setMonthScheme(s: io.github.salehgnutux.gtsalat.domain.MonthScheme) = viewModelScope.launch { settingsRepo.setMonthScheme(s) }
    fun setTimetableCalendar(k: io.github.salehgnutux.gtsalat.domain.CalendarKind) = viewModelScope.launch { settingsRepo.setTimetableCalendar(k) }
    fun redetectLocation() = viewModelScope.launch { repo.detectAndSaveLocation(); repo.prefetchMonths(3); reschedule() }

    override fun onCleared() {
        previewer.stop()
        super.onCleared()
    }
}
