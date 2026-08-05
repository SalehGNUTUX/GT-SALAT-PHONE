package io.github.salehgnutux.gtsalat.domain

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters

/**
 * قائمة طرق الحساب (بمعرّفات AlAdhan نفسها 1..22) لتغطية مناطق الوطن العربيّ
 * والعالم الإسلاميّ والعالم. تُستعمل المعرّفات نفسها في نداء API وفي الحساب المحلّيّ.
 */
object CalculationMethods {

    val ALL: List<CalculationMethodInfo> = listOf(
        CalculationMethodInfo(1, "جامعة العلوم الإسلامية، كراتشي", "University of Islamic Sciences, Karachi"),
        CalculationMethodInfo(2, "الجمعية الإسلامية لأمريكا الشمالية (ISNA)", "Islamic Society of North America"),
        CalculationMethodInfo(3, "رابطة العالم الإسلامي", "Muslim World League"),
        CalculationMethodInfo(4, "جامعة أم القرى، مكة", "Umm Al-Qura University, Makkah"),
        CalculationMethodInfo(5, "الهيئة العامة المصرية للمساحة", "Egyptian General Authority of Survey"),
        CalculationMethodInfo(7, "معهد الجيوفيزياء، جامعة طهران", "University of Tehran"),
        CalculationMethodInfo(8, "منطقة الخليج", "Gulf Region"),
        CalculationMethodInfo(9, "الكويت", "Kuwait"),
        CalculationMethodInfo(10, "قطر", "Qatar"),
        CalculationMethodInfo(11, "سنغافورة", "Singapore"),
        CalculationMethodInfo(12, "اتحاد المنظمات الإسلامية بفرنسا", "UOIF, France"),
        CalculationMethodInfo(13, "رئاسة الشؤون الدينية، تركيا", "Diyanet, Turkey"),
        CalculationMethodInfo(14, "الإدارة الدينية لمسلمي روسيا", "Muslims of Russia"),
        CalculationMethodInfo(15, "لجنة رؤية الهلال العالمية", "Moonsighting Committee Worldwide"),
        CalculationMethodInfo(16, "دبي (تجريبي)", "Dubai"),
        CalculationMethodInfo(17, "الجهاز الماليزي (JAKIM)", "JAKIM, Malaysia"),
        CalculationMethodInfo(18, "تونس", "Tunisia"),
        CalculationMethodInfo(19, "الجزائر", "Algeria"),
        CalculationMethodInfo(20, "وزارة الشؤون الدينية، إندونيسيا", "KEMENAG, Indonesia"),
        CalculationMethodInfo(21, "المغرب", "Morocco"),
        CalculationMethodInfo(22, "الجالية الإسلامية، لشبونة", "Islamic Community of Lisbon"),
    )

    fun infoOf(id: Int): CalculationMethodInfo = ALL.firstOrNull { it.id == id } ?: ALL[2]

    /** اقتراح طريقة الحساب المناسبة بناءً على اسم البلد المكتشَف. */
    fun suggestByCountry(country: String?): Int {
        val lc = (country ?: "").lowercase()
        return when {
            Regex("morocco|maroc|المغرب").containsMatchIn(lc) -> 21
            Regex("algeria|algérie|الجزائر").containsMatchIn(lc) -> 19
            Regex("tunisia|tunisie|تونس").containsMatchIn(lc) -> 18
            Regex("egypt|egypte|مصر").containsMatchIn(lc) -> 5
            Regex("saudi|السعودية|مكة|المدينة").containsMatchIn(lc) -> 4
            Regex("kuwait|الكويت").containsMatchIn(lc) -> 9
            Regex("qatar|قطر").containsMatchIn(lc) -> 10
            Regex("emirates|uae|الإمارات|دبي").containsMatchIn(lc) -> 8
            Regex("bahrain|البحرين|oman|عمان|عُمان").containsMatchIn(lc) -> 8
            Regex("turkey|türkiye|تركيا").containsMatchIn(lc) -> 13
            Regex("pakistan|باكستان|bangladesh|بنغلاديش").containsMatchIn(lc) -> 1
            Regex("malaysia|ماليزيا").containsMatchIn(lc) -> 17
            Regex("indonesia|إندونيسيا").containsMatchIn(lc) -> 20
            Regex("france|فرنسا").containsMatchIn(lc) -> 12
            Regex("united states|canada|أمريكا|الولايات المتحدة|كندا").containsMatchIn(lc) -> 2
            Regex("singapore|سنغافورة").containsMatchIn(lc) -> 11
            Regex("russia|روسيا").containsMatchIn(lc) -> 14
            Regex("iran|إيران").containsMatchIn(lc) -> 7
            else -> 3
        }
    }

    /**
     * تحويل معرّف الطريقة إلى معاملات adhan للحساب المحلّيّ.
     * ما لا نظير مباشر له في المكتبة نُعيده إلى أقرب طريقة (رابطة العالم الإسلاميّ)
     * كما في نسخة سطح المكتب؛ ومسار API يستعمل المعرّف الدقيق حين يتوفّر الإنترنت.
     */
    fun parametersOf(id: Int): CalculationParameters = when (id) {
        1 -> CalculationMethod.KARACHI.parameters
        2 -> CalculationMethod.NORTH_AMERICA.parameters
        4 -> CalculationMethod.UMM_AL_QURA.parameters
        5 -> CalculationMethod.EGYPTIAN.parameters
        8, 16 -> CalculationMethod.DUBAI.parameters
        9 -> CalculationMethod.KUWAIT.parameters
        10 -> CalculationMethod.QATAR.parameters
        11 -> CalculationMethod.SINGAPORE.parameters
        12, 15 -> CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters
        else -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
    }
}
