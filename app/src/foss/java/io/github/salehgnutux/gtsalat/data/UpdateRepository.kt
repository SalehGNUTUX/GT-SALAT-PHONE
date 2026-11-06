package io.github.salehgnutux.gtsalat.data

import io.github.salehgnutux.gtsalat.BuildConfig
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.domain.AppVersion
import io.github.salehgnutux.gtsalat.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/** معلومات نسخةٍ جديدةٍ متوفّرة. [apkUrl] رابط تنزيل الـAPK المطابق للنكهة (قد يكون فارغاً). */
data class UpdateAvailable(val version: String, val url: String, val apkUrl: String? = null)

/** حالةٌ عالميّةٌ للتحديث المتوفّر — تراقبها الرئيسيّة لعرض شريطٍ داخل التطبيق. */
object UpdateInfo {
    private val _available = MutableStateFlow<UpdateAvailable?>(null)
    val available: StateFlow<UpdateAvailable?> = _available.asStateFlow()
    fun set(u: UpdateAvailable?) { _available.value = u }
    fun dismiss() { _available.value = null }
}

/**
 * فحص توفّر نسخةٍ جديدة من صفحة إصدارات GitHub، وإشعارٌ داخليٌّ (شريط الرئيسيّة) ونظاميّ.
 * مفعّلٌ افتراضيّاً، ويمكن إلغاؤه من الإعدادات.
 */
@Singleton
class UpdateRepository @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val notifications: NotificationHelper,
) {
    private val http = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable private data class Release(
        val tag_name: String = "",
        val html_url: String = "",
        val draft: Boolean = false,
        val assets: List<Asset> = emptyList(),
    )
    @Serializable private data class Asset(val name: String = "", val browser_download_url: String = "")

    /** يفحص أحدث إصدار؛ إن كان أجدَّ من الحاليّ يضبط [UpdateInfo] ويُطلق إشعاراً. */
    suspend fun checkForUpdate() {
        val settings = settingsRepo.settings.first()
        if (!settings.checkUpdates) {
            UpdateInfo.set(null)
            return
        }
        // «/latest» يتجاهل الإصدارات الاختباريّة، فلا تصل إلّا لمن فعّل خيارها، من قائمة الإصدارات كلّها.
        val rel = withContext(Dispatchers.IO) {
            runCatching {
                if (settings.allowPrereleaseUpdates) {
                    val body = fetch("$RELEASES?per_page=30") ?: return@runCatching null
                    json.decodeFromString<List<Release>>(body)
                        .filter { !it.draft && it.tag_name.isNotBlank() }
                        .reduceOrNull { best, r -> if (AppVersion.isNewer(r.tag_name, best.tag_name)) r else best }
                } else {
                    fetch("$RELEASES/latest")?.let { json.decodeFromString<Release>(it) }
                }
            }.getOrNull()
        } ?: return
        val latest = rel.tag_name.removePrefix("v").trim()
        if (latest.isNotBlank() && AppVersion.isNewer(latest, BuildConfig.VERSION_NAME)) {
            val url = rel.html_url.ifBlank { "https://github.com/SalehGNUTUX/GT-SALAT-PHONE/releases/latest" }
            // اختيار الـAPK المطابق للنكهة: full ← "-full.apk"، foss ← "-foss.apk".
            val suffix = if (BuildConfig.USES_GMS) "-full.apk" else "-foss.apk"
            val apkUrl = rel.assets.firstOrNull { it.name.endsWith(suffix, ignoreCase = true) }?.browser_download_url
            UpdateInfo.set(UpdateAvailable(latest, url, apkUrl))
            notifications.showUpdate(latest, url)
        } else {
            UpdateInfo.set(null)
        }
    }

    private fun fetch(url: String): String? {
        val req = Request.Builder().url(url).header("Accept", "application/vnd.github+json").build()
        return http.newCall(req).execute().use { r -> if (r.isSuccessful) r.body?.string() else null }
    }

    private companion object {
        const val RELEASES = "https://api.github.com/repos/SalehGNUTUX/GT-SALAT-PHONE/releases"
    }
}
