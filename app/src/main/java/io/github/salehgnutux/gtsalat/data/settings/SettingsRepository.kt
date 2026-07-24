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
        val EN_PRE = booleanPreferencesKey("en_pre_notify")
        val USE_API = booleanPreferencesKey("use_api")
        val DND = booleanPreferencesKey("dnd")
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val SETUP = booleanPreferencesKey("setup_completed")
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
            enablePreNotify = this[Keys.EN_PRE] ?: true,
            useApiTimetables = this[Keys.USE_API] ?: true,
            doNotDisturb = this[Keys.DND] ?: false,
            themeMode = when (this[Keys.THEME]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            },
            dynamicColor = this[Keys.DYNAMIC] ?: true,
            setupCompleted = this[Keys.SETUP] ?: false,
        )
    }

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
    suspend fun setEnablePreNotify(v: Boolean) = context.dataStore.edit { it[Keys.EN_PRE] = v }
    suspend fun setUseApi(v: Boolean) = context.dataStore.edit { it[Keys.USE_API] = v }
    suspend fun setDnd(v: Boolean) = context.dataStore.edit { it[Keys.DND] = v }
    suspend fun setTheme(t: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = t.name }
    suspend fun setDynamicColor(v: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC] = v }
    suspend fun setSetupCompleted(v: Boolean) = context.dataStore.edit { it[Keys.SETUP] = v }
}
