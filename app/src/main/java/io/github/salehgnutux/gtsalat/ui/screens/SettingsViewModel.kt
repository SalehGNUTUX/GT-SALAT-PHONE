package io.github.salehgnutux.gtsalat.ui.screens

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.alarm.AdhanAlarmActivity
import io.github.salehgnutux.gtsalat.alarm.PrayerAlarmScheduler
import io.github.salehgnutux.gtsalat.audio.AdhanPreviewer
import io.github.salehgnutux.gtsalat.audio.AdhanService
import io.github.salehgnutux.gtsalat.data.PrayerRepository
import io.github.salehgnutux.gtsalat.util.Format
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
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository,
    private val repo: PrayerRepository,
    private val scheduler: PrayerAlarmScheduler,
    private val previewer: AdhanPreviewer,
    private val backup: io.github.salehgnutux.gtsalat.data.BackupManager,
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
    fun setEnableSunnahReminder(v: Boolean) = viewModelScope.launch { settingsRepo.setEnableSunnahReminder(v); reschedule() }
    fun setReminderHour(h: Int) = viewModelScope.launch { settingsRepo.setReminderHour(h); reschedule() }
    fun setEnableMorningAdhkar(v: Boolean) = viewModelScope.launch { settingsRepo.setEnableMorningAdhkar(v); reschedule() }
    fun setEnableEveningAdhkar(v: Boolean) = viewModelScope.launch { settingsRepo.setEnableEveningAdhkar(v); reschedule() }
    fun setMorningAdhkarHour(h: Int) = viewModelScope.launch { settingsRepo.setMorningAdhkarHour(h); reschedule() }
    fun setEveningAdhkarHour(h: Int) = viewModelScope.launch { settingsRepo.setEveningAdhkarHour(h); reschedule() }
    fun setUseApi(v: Boolean) = viewModelScope.launch { settingsRepo.setUseApi(v) }
    fun setDnd(v: Boolean) = viewModelScope.launch { settingsRepo.setDnd(v); reschedule() }
    fun setAutoSilence(v: Boolean) = viewModelScope.launch { settingsRepo.setAutoSilence(v) }
    fun setSilenceMinutes(m: Int) = viewModelScope.launch { settingsRepo.setSilenceMinutes(m) }
    fun setPersistentNotification(v: Boolean) = viewModelScope.launch { settingsRepo.setPersistentNotification(v); reschedule() }
    fun setTheme(t: ThemeMode) = viewModelScope.launch { settingsRepo.setTheme(t) }
    fun setClock24h(v: Boolean) = viewModelScope.launch { settingsRepo.setClock24h(v); refreshWidgets() }
    /** نمط ودجت الساعة/التقدّم (classic | center) — يُحدّث الودجتات فوراً. */
    fun setWidgetProgressStyle(style: String) = viewModelScope.launch { settingsRepo.setWidgetProgressStyle(style); refreshWidgets() }
    private suspend fun refreshWidgets() = runCatching { io.github.salehgnutux.gtsalat.widget.updateAllPrayerWidgets(context) }
    fun setCheckUpdates(v: Boolean) = viewModelScope.launch { settingsRepo.setCheckUpdates(v) }
    fun setEnableWird(v: Boolean) = viewModelScope.launch { settingsRepo.setEnableWird(v) }
    fun setHijriOffset(days: Int) = viewModelScope.launch { settingsRepo.setHijriOffset(days) }
    fun setSettingsSection(title: String) = viewModelScope.launch { settingsRepo.setSettingsOpenSection(title) }

    // ----- النسخ الاحتياطيّ / التصدير / الاستيراد الانتقائيّ -----

    /** أحجام ما هو متاحٌ على الجهاز (لحوار التصدير). */
    fun loadBackupSizes(cb: (io.github.salehgnutux.gtsalat.data.BackupSizes) -> Unit) =
        viewModelScope.launch { cb(backup.sizes()) }

    /** يصدّر الحزمة إلى ملفٍّ اختاره المستخدم (SAF) وفق ما اختاره. */
    fun exportBundle(uri: android.net.Uri, opts: io.github.salehgnutux.gtsalat.data.BackupOptions, done: (Boolean) -> Unit) =
        viewModelScope.launch { done(backup.export(uri, opts)) }

    /** يجهّز حزمةً في التخبئة ويعيد Uri للمشاركة عبر ورقة النظام (FileProvider). */
    fun shareBundle(opts: io.github.salehgnutux.gtsalat.data.BackupOptions, done: (android.net.Uri?) -> Unit) =
        viewModelScope.launch {
            val file = backup.exportToCache(opts)
            val uri = file?.let {
                runCatching {
                    androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
                }.getOrNull()
            }
            done(uri)
        }

    /** يفحص حزمةً قبل الاستيراد ليختار المستخدم ما يستعيد. */
    fun inspectBackup(uri: android.net.Uri, cb: (io.github.salehgnutux.gtsalat.data.BackupContents) -> Unit) =
        viewModelScope.launch { cb(backup.inspect(uri)) }

    /** يستورد ما اختاره المستخدم من الحزمة ثمّ يعيد جدولة التنبيهات إن لزم. */
    fun importBundle(uri: android.net.Uri, opts: io.github.salehgnutux.gtsalat.data.BackupOptions, done: (io.github.salehgnutux.gtsalat.data.BackupImport) -> Unit) =
        viewModelScope.launch {
            val res = backup.import(uri, opts)
            if (res.ok && (res.settings || res.prayers > 0)) reschedule()
            done(res)
        }
    fun setFullScreenAdhan(v: Boolean) = viewModelScope.launch { settingsRepo.setFullScreenAdhan(v) }
    fun setKeepAdhanScreen(v: Boolean) = viewModelScope.launch { settingsRepo.setKeepAdhanScreen(v) }

    /**
     * اختبارٌ فوريٌّ لنافذة ملء الشاشة (أذانٌ أو أذكار): يفتح النافذة ويشغّل الصوت،
     * وزرّ الإيقاف فيها يوقفه. لمعاينة السلوك دون انتظار وقت الصلاة.
     */
    fun testFullScreen(isDhikr: Boolean) {
        val prayer = if (isDhikr) "أذكارٌ (اختبار)" else "أذانٌ (اختبار)"
        runCatching {
            context.startActivity(
                AdhanAlarmActivity.intent(context, prayer, Format.clock(System.currentTimeMillis()), isDhikr),
            )
        }
        val svc = Intent(context, AdhanService::class.java).apply {
            putExtra(AdhanService.EXTRA_PRAYER_AR, prayer)
            putExtra(AdhanService.EXTRA_SOUND, if (isDhikr) AdhanService.SOUND_POST_DHIKR else AdhanService.SOUND_ADHAN)
            putExtra(AdhanService.EXTRA_ALERT_MODE, "FULL")
            putExtra(AdhanService.EXTRA_VOLUME, 100)
        }
        runCatching { ContextCompat.startForegroundService(context, svc) }
    }

    /** يجدول تنبيهاً اختباريّاً بعد دقيقة (لقياس وصول التنبيهات والشاشة مغلقة). */
    fun testNotification() = scheduler.scheduleTest()
    fun setDynamicColor(v: Boolean) = viewModelScope.launch { settingsRepo.setDynamicColor(v) }
    fun setSeedColor(argb: Int) = viewModelScope.launch { settingsRepo.setSeedColor(argb) }
    fun setGradient(dark: Boolean, top: Boolean, argb: Int) = viewModelScope.launch { settingsRepo.setGradient(dark, top, argb) }
    fun resetGradient(dark: Boolean) = viewModelScope.launch { settingsRepo.resetGradient(dark) }
    fun setMonthScheme(s: io.github.salehgnutux.gtsalat.domain.MonthScheme) = viewModelScope.launch { settingsRepo.setMonthScheme(s) }
    fun setTimetableCalendar(k: io.github.salehgnutux.gtsalat.domain.CalendarKind) = viewModelScope.launch { settingsRepo.setTimetableCalendar(k) }
    /** حالة إعادة اكتشاف الموقع لعرض رسالةٍ ذكيّة: null=خامل، ""=جارٍ، غيرها=نتيجة. */
    private val _locationStatus = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val locationStatus: StateFlow<String?> = _locationStatus

    fun redetectLocation() = viewModelScope.launch {
        _locationStatus.value = ""   // جارٍ
        val loc = runCatching { repo.detectAndSaveLocation() }.getOrNull()
        if (loc != null) {
            repo.prefetchMonths(3); reschedule()
            val place = listOf(loc.city, loc.country).filter { it.isNotBlank() }.joinToString("، ").ifBlank { "موقعك" }
            _locationStatus.value = "✓ حُدّث الموقع: $place"
        } else {
            _locationStatus.value = "تعذّر تحديد الموقع — فعّل صلاحيّة الموقع و GPS ثمّ أعِد المحاولة."
        }
    }

    fun clearLocationStatus() { _locationStatus.value = null }

    override fun onCleared() {
        previewer.stop()
        super.onCleared()
    }
}
