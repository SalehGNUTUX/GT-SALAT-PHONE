package io.github.salehgnutux.gtsalat.domain

import kotlinx.serialization.Serializable

/** إذاعةٌ قرآنيّة (اسم + وصف + رابط بثّ). المصدر: مشروع GNUTUX الحرّ GT_QURANRADIO. */
@Serializable
data class Radio(val name: String, val desc: String = "", val url: String)

@Serializable
data class RadioFile(val radios: List<Radio> = emptyList())

/** حالة المستخدم فوق الإذاعات الافتراضيّة: روابطٌ معدَّلة + إذاعاتٌ مخصّصة + محذوفة. */
@Serializable
data class UserRadios(
    val overrides: Map<String, String> = emptyMap(),   // الاسم → رابطٌ معدَّل
    val deleted: List<String> = emptyList(),            // أسماء الإذاعات الافتراضيّة المخفيّة
    val customs: List<Radio> = emptyList(),             // إذاعاتٌ أضافها المستخدم
    val favorites: List<String> = emptyList(),          // أسماء المفضّلة (تُرفَع لرأس القائمة)
)

/** إذاعةٌ في القائمة الفعليّة مع أعلامٍ للعرض. */
data class RadioItem(
    val name: String,
    val desc: String,
    val url: String,
    val isCustom: Boolean = false,      // أضافها المستخدم
    val isModified: Boolean = false,    // رابطها معدَّلٌ عن الافتراضيّ
    val isFav: Boolean = false,         // في المفضّلة
)
