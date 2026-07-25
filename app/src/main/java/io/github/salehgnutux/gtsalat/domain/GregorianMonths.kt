package io.github.salehgnutux.gtsalat.domain

/** مخطّطات أسماء الأشهر الميلاديّة حسب المنطقة (يختلف نطقها/رسمها بين الأقاليم العربيّة). */
enum class MonthScheme { AUTO, STANDARD, MAGHREB, LEVANT }

/** أيّ تقويمٍ يُعتمد في عرض ترتيب المواقيت. */
enum class CalendarKind { HIJRI, GREGORIAN }

object GregorianMonths {
    // القياسيّ (مصر/الخليج ومعظم الإعلام): يناير..ديسمبر
    private val STANDARD = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
    )
    // المغرب العربيّ (المغرب خاصّةً): ماي/يوليوز/غشت/شتنبر/نونبر/دجنبر
    private val MAGHREB = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "ماي", "يونيو",
        "يوليوز", "غشت", "شتنبر", "أكتوبر", "نونبر", "دجنبر",
    )
    // بلاد الشام والعراق: كانون الثاني..كانون الأوّل
    private val LEVANT = listOf(
        "كانون الثاني", "شباط", "آذار", "نيسان", "أيّار", "حزيران",
        "تمّوز", "آب", "أيلول", "تشرين الأوّل", "تشرين الثاني", "كانون الأوّل",
    )

    fun names(scheme: MonthScheme): List<String> = when (scheme) {
        MonthScheme.MAGHREB -> MAGHREB
        MonthScheme.LEVANT -> LEVANT
        else -> STANDARD
    }

    /** اسم الشهر (1..12) وفق المخطّط. */
    fun monthName(month: Int, scheme: MonthScheme): String =
        names(scheme).getOrElse(month - 1) { "" }

    /** استنتاج المخطّط من اسم/رمز الدولة المكتشَفة. */
    fun schemeForCountry(country: String): MonthScheme {
        val c = country.trim().lowercase()
        val maghreb = listOf(
            "morocco", "المغرب", "algeria", "الجزائر", "tunisia", "تونس",
            "libya", "ليبيا", "mauritania", "موريتانيا", "ma", "dz", "tn", "ly", "mr",
        )
        val levant = listOf(
            "syria", "سوريا", "سورية", "lebanon", "لبنان", "iraq", "العراق",
            "jordan", "الأردن", "الاردن", "palestine", "فلسطين", "sy", "lb", "iq", "jo", "ps",
        )
        return when {
            maghreb.any { c.contains(it) } -> MonthScheme.MAGHREB
            levant.any { c.contains(it) } -> MonthScheme.LEVANT
            else -> MonthScheme.STANDARD
        }
    }

    /** المخطّط الفعّال: إن كان AUTO فمن الدولة، وإلّا المخطّط المختار يدويّاً. */
    fun effective(scheme: MonthScheme, country: String): MonthScheme =
        if (scheme == MonthScheme.AUTO) schemeForCountry(country) else scheme
}
