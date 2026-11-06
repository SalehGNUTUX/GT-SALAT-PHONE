package io.github.salehgnutux.gtsalat.domain

/** صورةٌ لخلفيّة الرئيسيّة، في assets/backgrounds/{id}.webp ومصغّرتها في thumb/ (تُولَّد بـtools/prepare_backgrounds.sh). */
data class HomeBackground(val id: String, val title: String, val place: String)

object HomeBackgrounds {
    const val DEFAULT_ID = "hassan"

    val all: List<HomeBackground> = listOf(
        HomeBackground(DEFAULT_ID, "مسجد حسّان", "الرباط · يتبع الوضع الداكن والفاتح"),
        HomeBackground("haram", "المسجد الحرام", "مكّة المكرّمة"),
        HomeBackground("nabawi", "المسجد النبويّ", "المدينة المنوّرة"),
        HomeBackground("aqsa", "قبّة الصخرة", "المسجد الأقصى المبارك"),
        HomeBackground("hassan2_sea", "مسجد الحسن الثاني", "الدار البيضاء"),
        HomeBackground("hassan2_sunset", "مسجد الحسن الثاني عند الغروب", "الدار البيضاء"),
        HomeBackground("hassan_day", "مسجد حسّان نهاراً", "الرباط"),
        HomeBackground("hassan_night", "مسجد حسّان ليلاً", "الرباط"),
        HomeBackground("qarawiyyin", "جامع القرويّين", "فاس"),
    )

    /** ملفّ الصورة الفعليّ: الافتراضيّ نهاريٌّ في الوضع الفاتح وليليٌّ في الداكن. */
    fun resolve(id: String, dark: Boolean): String =
        if (id == DEFAULT_ID) (if (dark) "hassan_night" else "hassan_day") else byId(id).id

    fun byId(id: String): HomeBackground = all.firstOrNull { it.id == id } ?: all.first()

    /** درجات ظهور الصورة، من الأهدأ إلى الأوضح. */
    const val LEVEL_CALM = 0
    const val LEVEL_BALANCED = 1
    const val LEVEL_VIVID = 2
}
