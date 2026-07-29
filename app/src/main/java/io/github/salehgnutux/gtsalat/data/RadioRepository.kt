package io.github.salehgnutux.gtsalat.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.domain.Radio
import io.github.salehgnutux.gtsalat.domain.RadioFile
import io.github.salehgnutux.gtsalat.domain.RadioItem
import io.github.salehgnutux.gtsalat.domain.UserRadios
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * إذاعات القرآن: قائمةٌ افتراضيّةٌ من `assets/content/radios.json` (مستوردة من GT_QURANRADIO)،
 * فوقها حالةُ المستخدم (روابط معدَّلة/مخصّصة/محذوفة) في `filesDir/radios_user.json`.
 */
@Singleton
class RadioRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var defaultsCache: List<Radio>? = null
    private fun userFile() = File(context.filesDir, "radios_user.json")

    suspend fun defaults(): List<Radio> = defaultsCache ?: withContext(Dispatchers.Default) {
        json.decodeFromString<RadioFile>(
            context.assets.open("content/radios.json").bufferedReader().use { it.readText() }
        ).radios.also { defaultsCache = it }
    }

    private fun loadUser(): UserRadios = runCatching {
        userFile().takeIf { it.exists() }?.readText()?.let { json.decodeFromString<UserRadios>(it) }
    }.getOrNull() ?: UserRadios()

    private fun saveUser(u: UserRadios) = runCatching { userFile().writeText(json.encodeToString(UserRadios.serializer(), u)) }

    /** القائمة الفعليّة: **المفضّلة أوّلاً** ثمّ الافتراضيّة (منقوصةً المحذوف، معدَّلةَ الروابط) فالمخصّصة. */
    suspend fun radios(): List<RadioItem> {
        val u = loadUser()
        val favSet = u.favorites.toSet()
        val out = ArrayList<RadioItem>()
        defaults().forEach { d ->
            if (d.name in u.deleted) return@forEach
            val url = u.overrides[d.name] ?: d.url
            out.add(RadioItem(d.name, d.desc, url, isCustom = false, isModified = u.overrides.containsKey(d.name), isFav = d.name in favSet))
        }
        u.customs.forEach { c -> out.add(RadioItem(c.name, c.desc, c.url, isCustom = true, isFav = c.name in favSet)) }
        // المفضّلة إلى الرأس (بترتيب إضافتها)، وغيرها بموضعها الأصليّ — فرزٌ مستقرّ.
        return out.sortedBy { if (it.isFav) u.favorites.indexOf(it.name) else Int.MAX_VALUE }
    }

    /** تبديل المفضّلة لإذاعة. */
    fun toggleFav(name: String) {
        val u = loadUser()
        val favs = if (name in u.favorites) u.favorites - name else u.favorites + name
        saveUser(u.copy(favorites = favs))
    }

    /** تعديل رابط إذاعةٍ (افتراضيّة أو مخصّصة). */
    fun setUrl(name: String, url: String) {
        val u = loadUser()
        val custom = u.customs.firstOrNull { it.name == name }
        if (custom != null) {
            saveUser(u.copy(customs = u.customs.map { if (it.name == name) it.copy(url = url) else it }))
        } else {
            saveUser(u.copy(overrides = u.overrides + (name to url)))
        }
    }

    /** إعادة رابط إذاعةٍ افتراضيّةٍ إلى الأصل. */
    fun resetUrl(name: String) {
        val u = loadUser()
        saveUser(u.copy(overrides = u.overrides - name))
    }

    /** إعادة كلّ الإذاعات الافتراضيّة إلى الأصل (تُلغى التعديلات والحذف؛ تبقى المخصّصة). */
    fun resetAll() {
        val u = loadUser()
        saveUser(u.copy(overrides = emptyMap(), deleted = emptyList()))
    }

    fun addCustom(name: String, desc: String, url: String) {
        val u = loadUser()
        if (u.customs.any { it.name == name }) return
        saveUser(u.copy(customs = u.customs + Radio(name, desc, url)))
    }

    /** حذف إذاعة: المخصّصة تُزال، والافتراضيّة تُخفى. */
    fun delete(name: String, isCustom: Boolean) {
        val u = loadUser()
        if (isCustom) saveUser(u.copy(customs = u.customs.filterNot { it.name == name }))
        else saveUser(u.copy(deleted = (u.deleted + name).distinct()))
    }

    /** التراجع عن الحذف. */
    fun restore(name: String, isCustom: Boolean, desc: String, url: String) {
        val u = loadUser()
        if (isCustom) { if (u.customs.none { it.name == name }) saveUser(u.copy(customs = u.customs + Radio(name, desc, url))) }
        else saveUser(u.copy(deleted = u.deleted - name))
    }
}
