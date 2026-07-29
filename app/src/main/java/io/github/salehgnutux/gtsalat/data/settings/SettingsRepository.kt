package io.github.salehgnutux.gtsalat.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.domain.AsrMadhab
import io.github.salehgnutux.gtsalat.domain.CalendarKind
import io.github.salehgnutux.gtsalat.domain.MonthScheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gt_salat_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val LAT = doublePreferencesKey("lat")
        val LON = doublePreferencesKey("lon")
        val HAS_LOC = booleanPreferencesKey("has_loc")
        val CITY = stringPreferencesKey("city")
        val COUNTRY = stringPreferencesKey("country")
        val METHOD = intPreferencesKey("method_id")
        val MADHAB = stringPreferencesKey("madhab")
        val PRE_NOTIFY = intPreferencesKey("pre_notify_min")
        val ADHAN_TYPE = stringPreferencesKey("adhan_type")
        val CUSTOM_ADHAN_URI = stringPreferencesKey("custom_adhan_uri")
        val CUSTOM_ADHAN_NAME = stringPreferencesKey("custom_adhan_name")
        val EN_SALAT = booleanPreferencesKey("en_salat_notify")
        val EN_ADHAN = booleanPreferencesKey("en_adhan_sound")
        val EN_DUA = booleanPreferencesKey("en_dua_after")
        val ALERT_MODE = stringPreferencesKey("adhan_alert_mode")
        val ADHAN_VOLUME = intPreferencesKey("adhan_volume")
        val PER_PRAYER = booleanPreferencesKey("per_prayer_alerts")
        val PRAYER_ALERTS = stringPreferencesKey("prayer_alerts_csv")
        val EN_PRE = booleanPreferencesKey("en_pre_notify")
        val EN_PRE_SOUND = booleanPreferencesKey("en_pre_notify_sound")
        val EN_POST_DHIKR = booleanPreferencesKey("en_post_dhikr")
        val POST_DHIKR_MIN = intPreferencesKey("post_dhikr_min")
        val EN_DAILY_AYAH = booleanPreferencesKey("en_daily_ayah")
        val EN_RECITATION = booleanPreferencesKey("en_recitation")
        val EN_WHITEDAYS = booleanPreferencesKey("en_whitedays")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val USE_API = booleanPreferencesKey("use_api")
        val DND = booleanPreferencesKey("dnd")
        val AUTO_SILENCE = booleanPreferencesKey("auto_silence")
        val SILENCE_MIN = intPreferencesKey("silence_min")
        val SAVED_RINGER = intPreferencesKey("saved_ringer_mode")
        val PERSISTENT_NOTIF = booleanPreferencesKey("persistent_notif")
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val SEED_COLOR = intPreferencesKey("seed_color")
        val GRAD_TOP_L = intPreferencesKey("grad_top_light")
        val GRAD_BOT_L = intPreferencesKey("grad_bot_light")
        val GRAD_TOP_D = intPreferencesKey("grad_top_dark")
        val GRAD_BOT_D = intPreferencesKey("grad_bot_dark")
        val MONTH_SCHEME = stringPreferencesKey("month_scheme")
        val CAL_KIND = stringPreferencesKey("timetable_calendar")
        val CLOCK_24 = booleanPreferencesKey("clock_24h")
        val SETUP = booleanPreferencesKey("setup_completed")
        val LAST_READ_SURAH = intPreferencesKey("last_read_surah")
        val LAST_READ_AYAH = intPreferencesKey("last_read_ayah")
        val LAST_LISTEN_SURAH = intPreferencesKey("last_listen_surah")
        val LAST_LISTEN_AYAH = intPreferencesKey("last_listen_ayah")
        val LAST_RECITER = stringPreferencesKey("last_reciter_id")
        val LAST_MUSHAF_PAGE = intPreferencesKey("last_mushaf_page")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p -> p.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    private fun Preferences.toSettings(): AppSettings {
        val hasLoc = this[Keys.HAS_LOC] ?: false
        return AppSettings(
            lat = if (hasLoc) this[Keys.LAT] else null,
            lon = if (hasLoc) this[Keys.LON] else null,
            city = this[Keys.CITY] ?: "",
            country = this[Keys.COUNTRY] ?: "",
            methodId = this[Keys.METHOD] ?: 3,
            madhab = if (this[Keys.MADHAB] == "HANAFI") AsrMadhab.HANAFI else AsrMadhab.SHAFI,
            preNotifyMinutes = this[Keys.PRE_NOTIFY] ?: 15,
            adhanType = when (this[Keys.ADHAN_TYPE]) {
                "SHORT" -> AdhanType.SHORT
                "CUSTOM" -> AdhanType.CUSTOM
                else -> AdhanType.FULL
            },
            customAdhanUri = this[Keys.CUSTOM_ADHAN_URI],
            customAdhanName = this[Keys.CUSTOM_ADHAN_NAME] ?: "",
            enableSalatNotify = this[Keys.EN_SALAT] ?: true,
            enableAdhanSound = this[Keys.EN_ADHAN] ?: true,
            enableDuaAfterAdhan = this[Keys.EN_DUA] ?: false,
            adhanAlertMode = parseAlert(this[Keys.ALERT_MODE]),
            adhanVolume = (this[Keys.ADHAN_VOLUME] ?: 100).coerceIn(0, 100),
            perPrayerAlerts = this[Keys.PER_PRAYER] ?: false,
            prayerAlerts = (this[Keys.PRAYER_ALERTS] ?: "").split(",")
                .let { csv -> List(5) { i -> parseAlert(csv.getOrNull(i)) } },
            enablePreNotify = this[Keys.EN_PRE] ?: true,
            enablePreNotifySound = this[Keys.EN_PRE_SOUND] ?: true,
            enablePostDhikr = this[Keys.EN_POST_DHIKR] ?: true,
            postDhikrMinutes = this[Keys.POST_DHIKR_MIN] ?: 20,
            enableDailyAyah = this[Keys.EN_DAILY_AYAH] ?: true,
            enableRecitationReminder = this[Keys.EN_RECITATION] ?: true,
            enableWhiteDaysReminder = this[Keys.EN_WHITEDAYS] ?: true,
            reminderHour = (this[Keys.REMINDER_HOUR] ?: 8).coerceIn(0, 23),
            useApiTimetables = this[Keys.USE_API] ?: true,
            doNotDisturb = this[Keys.DND] ?: false,
            autoSilence = this[Keys.AUTO_SILENCE] ?: false,
            silenceMinutes = this[Keys.SILENCE_MIN] ?: 15,
            persistentNotification = this[Keys.PERSISTENT_NOTIF] ?: true,
            themeMode = when (this[Keys.THEME]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            },
            dynamicColor = this[Keys.DYNAMIC] ?: true,
            seedColor = this[Keys.SEED_COLOR] ?: 0,
            gradTopLight = this[Keys.GRAD_TOP_L] ?: 0,
            gradBotLight = this[Keys.GRAD_BOT_L] ?: 0,
            gradTopDark = this[Keys.GRAD_TOP_D] ?: 0,
            gradBotDark = this[Keys.GRAD_BOT_D] ?: 0,
            monthScheme = runCatching { MonthScheme.valueOf(this[Keys.MONTH_SCHEME] ?: "AUTO") }.getOrDefault(MonthScheme.AUTO),
            timetableCalendar = if (this[Keys.CAL_KIND] == "GREGORIAN") CalendarKind.GREGORIAN else CalendarKind.HIJRI,
            clock24h = this[Keys.CLOCK_24] ?: true,
            setupCompleted = this[Keys.SETUP] ?: false,
            lastReadSurah = this[Keys.LAST_READ_SURAH] ?: 0,
            lastReadAyah = this[Keys.LAST_READ_AYAH] ?: 1,
            lastListenSurah = this[Keys.LAST_LISTEN_SURAH] ?: 0,
            lastListenAyah = this[Keys.LAST_LISTEN_AYAH] ?: 1,
            lastReciterId = this[Keys.LAST_RECITER] ?: "",
            lastMushafPage = this[Keys.LAST_MUSHAF_PAGE] ?: 1,
        )
    }

    suspend fun setLastMushafPage(page: Int) = context.dataStore.edit { it[Keys.LAST_MUSHAF_PAGE] = page }

    /** حفظ موضع القراءة الأخير في القرآن (للمتابعة لاحقاً). */
    suspend fun setLastRead(surah: Int, ayah: Int) = context.dataStore.edit {
        it[Keys.LAST_READ_SURAH] = surah
        it[Keys.LAST_READ_AYAH] = ayah
    }

    /** حفظ موضع الاستماع الأخير (مستقلٌّ عن القراءة). */
    suspend fun setLastListen(surah: Int, ayah: Int) = context.dataStore.edit {
        it[Keys.LAST_LISTEN_SURAH] = surah
        it[Keys.LAST_LISTEN_AYAH] = ayah
    }
    suspend fun setLastReciter(id: String) = context.dataStore.edit { it[Keys.LAST_RECITER] = id }

    suspend fun setLocation(lat: Double, lon: Double, city: String, country: String) {
        context.dataStore.edit {
            it[Keys.LAT] = lat
            it[Keys.LON] = lon
            it[Keys.HAS_LOC] = true
            it[Keys.CITY] = city
            it[Keys.COUNTRY] = country
        }
    }

    suspend fun setMethod(id: Int) = context.dataStore.edit { it[Keys.METHOD] = id }
    suspend fun setMadhab(m: AsrMadhab) = context.dataStore.edit { it[Keys.MADHAB] = m.name }
    suspend fun setPreNotify(min: Int) = context.dataStore.edit { it[Keys.PRE_NOTIFY] = min }
    suspend fun setAdhanType(t: AdhanType) = context.dataStore.edit { it[Keys.ADHAN_TYPE] = t.name }
    suspend fun setCustomAdhan(uri: String, name: String) = context.dataStore.edit {
        it[Keys.CUSTOM_ADHAN_URI] = uri
        it[Keys.CUSTOM_ADHAN_NAME] = name
        it[Keys.ADHAN_TYPE] = AdhanType.CUSTOM.name
    }
    suspend fun setEnableSalat(v: Boolean) = context.dataStore.edit { it[Keys.EN_SALAT] = v }
    suspend fun setEnableAdhan(v: Boolean) = context.dataStore.edit { it[Keys.EN_ADHAN] = v }
    suspend fun setEnableDua(v: Boolean) = context.dataStore.edit { it[Keys.EN_DUA] = v }
    private fun parseAlert(s: String?): AdhanAlertMode = when (s) {
        "TONE" -> AdhanAlertMode.TONE
        "SILENT" -> AdhanAlertMode.SILENT
        else -> AdhanAlertMode.FULL
    }

    suspend fun setAdhanAlertMode(m: AdhanAlertMode) = context.dataStore.edit { it[Keys.ALERT_MODE] = m.name }
    suspend fun setAdhanVolume(v: Int) = context.dataStore.edit { it[Keys.ADHAN_VOLUME] = v.coerceIn(0, 100) }
    suspend fun setPerPrayerAlerts(v: Boolean) = context.dataStore.edit { it[Keys.PER_PRAYER] = v }
    suspend fun setPrayerAlert(index: Int, mode: AdhanAlertMode) = context.dataStore.edit { p ->
        val cur = (p[Keys.PRAYER_ALERTS] ?: "").split(",").let { csv -> MutableList(5) { i -> (csv.getOrNull(i) ?: "FULL") } }
        if (index in 0..4) cur[index] = mode.name
        p[Keys.PRAYER_ALERTS] = cur.joinToString(",")
    }
    suspend fun setEnablePreNotify(v: Boolean) = context.dataStore.edit { it[Keys.EN_PRE] = v }
    suspend fun setEnablePreNotifySound(v: Boolean) = context.dataStore.edit { it[Keys.EN_PRE_SOUND] = v }
    suspend fun setEnablePostDhikr(v: Boolean) = context.dataStore.edit { it[Keys.EN_POST_DHIKR] = v }
    suspend fun setEnableDailyAyah(v: Boolean) = context.dataStore.edit { it[Keys.EN_DAILY_AYAH] = v }
    suspend fun setEnableRecitationReminder(v: Boolean) = context.dataStore.edit { it[Keys.EN_RECITATION] = v }
    suspend fun setEnableWhiteDaysReminder(v: Boolean) = context.dataStore.edit { it[Keys.EN_WHITEDAYS] = v }
    suspend fun setReminderHour(h: Int) = context.dataStore.edit { it[Keys.REMINDER_HOUR] = h.coerceIn(0, 23) }
    suspend fun setUseApi(v: Boolean) = context.dataStore.edit { it[Keys.USE_API] = v }
    suspend fun setDnd(v: Boolean) = context.dataStore.edit { it[Keys.DND] = v }
    suspend fun setAutoSilence(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_SILENCE] = v }
    suspend fun setPersistentNotification(v: Boolean) = context.dataStore.edit { it[Keys.PERSISTENT_NOTIF] = v }
    suspend fun setSilenceMinutes(m: Int) = context.dataStore.edit { it[Keys.SILENCE_MIN] = m }
    suspend fun setSavedRingerMode(m: Int) = context.dataStore.edit { it[Keys.SAVED_RINGER] = m }
    suspend fun savedRingerMode(): Int? = context.dataStore.data.first()[Keys.SAVED_RINGER]
    suspend fun setClock24h(v: Boolean) = context.dataStore.edit { it[Keys.CLOCK_24] = v }
    suspend fun setTheme(t: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = t.name }
    suspend fun setDynamicColor(v: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC] = v }
    suspend fun setSeedColor(argb: Int) = context.dataStore.edit { it[Keys.SEED_COLOR] = argb }
    /** تعيين لون طرف التدرّج للوضع المطلوب (dark) أو الفاتح، أعلى (top) أو أسفل. */
    suspend fun setGradient(dark: Boolean, top: Boolean, argb: Int) = context.dataStore.edit {
        it[when {
            dark && top -> Keys.GRAD_TOP_D
            dark && !top -> Keys.GRAD_BOT_D
            !dark && top -> Keys.GRAD_TOP_L
            else -> Keys.GRAD_BOT_L
        }] = argb
    }
    suspend fun resetGradient(dark: Boolean) = context.dataStore.edit {
        if (dark) { it[Keys.GRAD_TOP_D] = 0; it[Keys.GRAD_BOT_D] = 0 }
        else { it[Keys.GRAD_TOP_L] = 0; it[Keys.GRAD_BOT_L] = 0 }
    }
    suspend fun setMonthScheme(s: MonthScheme) = context.dataStore.edit { it[Keys.MONTH_SCHEME] = s.name }
    suspend fun setTimetableCalendar(k: CalendarKind) = context.dataStore.edit { it[Keys.CAL_KIND] = k.name }
    suspend fun setSetupCompleted(v: Boolean) = context.dataStore.edit { it[Keys.SETUP] = v }
}
