package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.alarm.PrayerAlarmScheduler
import io.github.salehgnutux.gtsalat.data.PrayerRepository
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.domain.AsrMadhab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUi(
    val detecting: Boolean = false,
    val city: String = "",
    val country: String = "",
    val lat: Double? = null,
    val lon: Double? = null,
    val hasLocation: Boolean = false,
    val methodId: Int = 3,
    val madhab: AsrMadhab = AsrMadhab.SHAFI,
    val error: String? = null,
    val info: String? = null,
    val done: Boolean = false,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repo: PrayerRepository,
    private val settingsRepo: SettingsRepository,
    private val scheduler: PrayerAlarmScheduler,
    private val backup: io.github.salehgnutux.gtsalat.data.BackupManager,
    private val content: io.github.salehgnutux.gtsalat.data.ContentRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(SetupUi())
    val ui: StateFlow<SetupUi> = _ui.asStateFlow()

    /** المواقع المُضمَّنة (بلد/مدينة) لاختيارٍ يعمل دون GPS ولا إنترنت. */
    private val _places = MutableStateFlow<List<io.github.salehgnutux.gtsalat.domain.Place>>(emptyList())
    val places: StateFlow<List<io.github.salehgnutux.gtsalat.domain.Place>> = _places.asStateFlow()

    init {
        viewModelScope.launch {
            val s = settingsRepo.current()
            _ui.value = _ui.value.copy(
                city = s.city, country = s.country, lat = s.lat, lon = s.lon, hasLocation = s.hasLocation,
                methodId = s.methodId, madhab = s.madhab,
            )
        }
        viewModelScope.launch { _places.value = runCatching { content.places() }.getOrDefault(emptyList()) }
    }

    /** اختيار موقعٍ من القائمة المُضمَّنة: يحفظ الإحداثيّات ويقترح طريقة الحساب حسب البلد. */
    fun pickPlace(place: io.github.salehgnutux.gtsalat.domain.Place) {
        _ui.value = _ui.value.copy(error = null, info = null)
        viewModelScope.launch {
            settingsRepo.setLocation(place.lat, place.lon, place.city, place.country)
            val method = io.github.salehgnutux.gtsalat.domain.CalculationMethods.suggestByCountry(place.country)
            settingsRepo.setMethod(method)
            _ui.value = _ui.value.copy(
                hasLocation = true, lat = place.lat, lon = place.lon,
                city = place.city, country = place.country, methodId = method,
            )
        }
    }

    fun detectLocation() {
        _ui.value = _ui.value.copy(detecting = true, error = null, info = null)
        viewModelScope.launch {
            val loc = repo.detectAndSaveLocation()
            val s = settingsRepo.current()
            _ui.value = if (loc != null) {
                _ui.value.copy(
                    detecting = false, hasLocation = true,
                    city = loc.city, country = loc.country, lat = loc.lat, lon = loc.lon, methodId = s.methodId,
                )
            } else {
                _ui.value.copy(detecting = false, error = "تعذّر اكتشاف الموقع. دون إنترنت أدخِل الإحداثيّات يدويّاً أو استورِد نسخةً احتياطيّة.")
            }
        }
    }

    /** إدخال الموقع يدويّاً بخطّي الطول/العرض (يعمل دون إنترنت). */
    fun setManualLocation(lat: Double, lon: Double, city: String = "") {
        _ui.value = _ui.value.copy(error = null, info = null)
        viewModelScope.launch {
            settingsRepo.setLocation(lat, lon, city, "")
            _ui.value = _ui.value.copy(hasLocation = true, lat = lat, lon = lon, city = city, country = "")
        }
    }

    /** استيراد نسخةٍ احتياطيّة (فيها الموقع/الإعدادات) لتهيئة جهازٍ جديد دون إنترنت. */
    fun importBackup(uri: android.net.Uri, done: (Boolean) -> Unit) {
        _ui.value = _ui.value.copy(detecting = true, error = null, info = null)
        viewModelScope.launch {
            val c = backup.inspect(uri)
            val opts = io.github.salehgnutux.gtsalat.data.BackupOptions(
                settings = c.hasSettings, prayers = c.hasPrayers, audio = c.hasAudio, mushaf = c.hasMushaf,
            )
            val res = if (opts.any) backup.import(uri, opts) else io.github.salehgnutux.gtsalat.data.BackupImport(false)
            val s = settingsRepo.current()
            _ui.value = _ui.value.copy(
                detecting = false, hasLocation = s.hasLocation,
                city = s.city, country = s.country, lat = s.lat, lon = s.lon,
                methodId = s.methodId, madhab = s.madhab,
                info = if (res.ok && s.hasLocation) "تمّ الاستيراد وتحديد الموقع." else if (res.ok) "تمّ الاستيراد (بلا موقعٍ في النسخة)." else null,
                error = if (!res.ok) "تعذّر استيراد النسخة — تأكّد من صحّة الملفّ." else null,
            )
            done(res.ok)
        }
    }

    /** تخطّي الإعداد للدخول واستكشاف الأقسام غير المعتمِدة على الموقع. */
    fun skip() {
        viewModelScope.launch {
            settingsRepo.setSetupCompleted(true)
            scheduler.scheduleNext()
            _ui.value = _ui.value.copy(done = true)
        }
    }

    fun setMethod(id: Int) {
        _ui.value = _ui.value.copy(methodId = id)
        viewModelScope.launch { settingsRepo.setMethod(id) }
    }

    fun setMadhab(m: AsrMadhab) {
        _ui.value = _ui.value.copy(madhab = m)
        viewModelScope.launch { settingsRepo.setMadhab(m) }
    }

    fun finish() {
        viewModelScope.launch {
            settingsRepo.setSetupCompleted(true)
            repo.prefetchMonths(6)
            scheduler.scheduleNext()
            _ui.value = _ui.value.copy(done = true)
        }
    }
}
