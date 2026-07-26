package io.github.salehgnutux.gtsalat.domain

/**
 * المصادر الحرّة والمفتوحة التي اعتمدنا عليها في إثراء التطبيق.
 * ★ يُحدَّث هذا الملفّ كلّما اعتمدنا مصدراً حرّاً/مفتوح المصدر جديداً.
 */
object Credits {
    data class Source(val name: String, val note: String, val url: String)

    const val DEVELOPER = "SalehGNUTUX"
    const val GITHUB = "https://github.com/SalehGNUTUX"
    const val REPO = "https://github.com/SalehGNUTUX/GT-SALAT-PHONE"
    const val WEBSITE = "https://salehgnutux.github.io/GT-SALAT-PHONE/"
    const val PROJECTS = "https://salehgnutux.github.io/gnutux/"

    val SOURCES = listOf(
        Source("GT_HISNMUSLIM", "حصن المسلم المصنّف (132 باباً)", "https://github.com/SalehGNUTUX/GT_HISNMUSLIM"),
        Source("GT-SIRM", "أحاديث · أدعية · حِكَم · أسماء الله", "https://github.com/SalehGNUTUX"),
        Source("GT-QURANREADER", "بنية القرآن والقرّاء (قادم)", "https://github.com/SalehGNUTUX/GT-QURANREADER"),
        Source("alquran.cloud", "النصّ العثمانيّ والتفسير الميسّر", "https://alquran.cloud"),
        Source("AlAdhan API", "المواقيت والتاريخ الهجريّ", "https://aladhan.com"),
        Source("Adhan (Batoul Apps)", "حساب المواقيت والقبلة محلّيّاً", "https://github.com/batoulapps/adhan-java"),
        Source("OpenStreetMap · Nominatim", "الموقع بلا خدمات Google", "https://www.openstreetmap.org"),
        Source("Quranpedia", "تلاواتٌ بالروايات (ورش عن نافع — قادم)", "https://quranpedia.net"),
        Source("خطّ أميري", "الخطّ القرآنيّ للنصوص", "https://www.amirifont.org"),
        Source("خطّ Ubuntu Arabic", "خطّ الواجهة", "https://design.ubuntu.com/font"),
    )
}
