package io.github.salehgnutux.gtsalat.domain

import kotlinx.serialization.Serializable

/** نماذج المحتوى الإسلاميّ المُستورَد من مشاريع GNUTUX (حصن المسلم/GT-SIRM)، تُقرأ من assets/content. */

@Serializable
data class AsmaName(val index: Int, val arabic: String, val meaning: String, val ref: String = "")

@Serializable
data class AsmaFile(val items: List<AsmaName>)

@Serializable
data class Hadith(
    val n: Int,
    val chapter: String = "",
    val narrator: String = "",
    val text: String,
    val source: String = "",
    val grade: String = "",
)

@Serializable
data class HadithCollection(
    val id: String,
    val name: String,
    val author: String = "",
    val description: String = "",
    val hadiths: List<Hadith>,
)

@Serializable
data class HadithFile(val collections: List<HadithCollection>)

@Serializable
data class Dua(val n: Int, val text: String, val source: String = "", val context: String = "")

@Serializable
data class DuaCategory(val id: Int, val name: String, val icon: String = "", val items: List<Dua>)

@Serializable
data class DuasFile(val categories: List<DuaCategory>)

@Serializable
data class Hikmah(val n: Int, val text: String, val sayer: String = "", val source: String = "")

@Serializable
data class HikamCategory(val id: Int, val name: String, val items: List<Hikmah>)

@Serializable
data class HikamFile(val categories: List<HikamCategory>)

/** حصن المسلم المصنّف: باب فيه أذكار، لكلّ ذكر عدد تكراره ورابط صوته (أونلاين). */
@Serializable
data class HisnDhikr(val n: Int, val text: String, val count: Int = 1, val audio: String = "")

@Serializable
data class HisnCategory(
    val id: Int,
    val name: String,
    val icon: String = "",
    val audio: String = "",
    val count: Int = 0,
    val items: List<HisnDhikr> = emptyList(),
)

@Serializable
data class HisnFile(val categories: List<HisnCategory>)
