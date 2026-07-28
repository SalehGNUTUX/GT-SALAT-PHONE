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
        Source("GT-QURANREADER", "فهرس القرآن والروايات والقرّاء", "https://github.com/SalehGNUTUX/GT-QURANREADER"),
        Source("alquran.cloud", "النصّ العثمانيّ والتفسير الميسّر", "https://alquran.cloud"),
        Source("everyayah.com", "صوت القرآن آية-بآية (13 قارئاً)", "https://everyayah.com"),
        Source("mp3quran.net", "تلاوات السور الكاملة", "https://mp3quran.net"),
        Source("Quran-PNG", "صور صفحات مصحف المدينة (حفص)", "https://github.com/SalehGNUTUX/Quran-PNG"),
        Source("QuranHub/quran-pages-images", "صور مصحف ورش (مجمّع الملك فهد)", "https://github.com/QuranHub/quran-pages-images"),
        Source("alquran.cloud (روايات)", "نصّ القرآن بالروايات (ورش/قالون/الدوري)", "https://alquran.cloud"),
        Source("AlAdhan API", "المواقيت والتاريخ الهجريّ", "https://aladhan.com"),
        Source("Adhan (Batoul Apps)", "حساب المواقيت والقبلة محلّيّاً", "https://github.com/batoulapps/adhan-java"),
        Source("OpenStreetMap · Nominatim", "الموقع بلا خدمات Google", "https://www.openstreetmap.org"),
        Source("خطّ أميري", "الخطّ القرآنيّ للنصوص", "https://www.amirifont.org"),
        Source("خطّ Ubuntu Arabic", "خطّ الواجهة", "https://design.ubuntu.com/font"),
    )
}
