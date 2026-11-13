package io.github.salehgnutux.gtsalat.domain

/**
 * تهيئة نصّ القرآن للعرض بخطّ الرواية.
 *
 * نصوص ورش وقالون (مجمّع الملك فهد) مرمَّزةٌ لخطوط المجمّع، والخطّ المغربيّ AALMAGHRIBI
 * (مؤسّسة محمّد السادس) يرمّز بعض العلامات في مواضع أخرى. التحويل هنا وقت العرض فقط،
 * والنصّ المخزَّن يبقى كما نُشر (البحث والنسخ يعملان عليه).
 * جُرِّب كلّ بديلٍ برسم الخطّ نفسه؛ انظر `assets/content/fonts.LICENSE.txt`.
 */
object QuranText {
    /** علامة الوقف في نصوص المجمّع؛ تُرسَم في الخطّ المغربيّ «صه» صغيرةً منفصلة (رمز الخطّ لها يشبه الشدّة). */
    const val WAQF = 'ۖ'

    private val maghribiMap: Map<Char, String> = mapOf(
        'ے' to "ى",   // ياءٌ بلا نقط في آخر الكلمة (فِے) ← ى
        '۟' to "",         // الصفر المستدير: المصحف المغربيّ يترك الحرف الساكت بلا علامة
        'ۥ' to "٘",   // واو الصلة الصغيرة ← رمزها في الخطّ المغربيّ
        'ۦ' to "ٙ",   // ياء الصلة الصغيرة ← رمزها في الخطّ المغربيّ
        '۪' to "ۙ",   // نقطة الإمالة والتقليل تحت الحرف
        'ࣱ' to "ٌ",   // تنوينا الضمّ والكسر المتتابعان غير موجودَين في الخطّ
        'ࣲ' to "ٍ",
        ' ' to "", '⁠' to "", '﻿' to "",
    )

    /** النصّ المعروض لخطّ [font] (اسمه في `quran_meta.json`). علامة [WAQF] تبقى ليرسمها الواجهة. */
    fun forFont(text: String, font: String): String = when (font) {
        "Maghribi" -> buildString(text.length) { text.forEach { c -> append(maghribiMap[c] ?: c.toString()) } }
        else -> text.replace("﻿", "")
    }

    private val bismillahKey = Quran.normalize("بسم الله الرحمن الرحيم")

    /**
     * يحذف البسملة من أوّل الآية الأولى إن سبقتها (نصّ حفص المضمَّن يلحقها بها، ونصوص المجمّع لا).
     * في العرض المتّصل تُرسَم البسملة عنواناً مستقلّاً فوق السورة.
     */
    fun stripBismillah(text: String): String {
        val words = text.trim().split(Regex("\\s+"))
        if (words.size <= 4) return text
        return if (Quran.normalize(words.take(4).joinToString(" ")) == bismillahKey) words.drop(4).joinToString(" ") else text
    }

    /** رقم صفحة المصحف المدنيّ (1-604) للآية، من جدول بدايات الصفحات. */
    fun pageOf(starts: List<List<Int>>, surah: Int, ayah: Int): Int {
        var lo = 0; var hi = starts.size - 1; var ans = 0
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            val s = starts[mid]
            val before = s[0] < surah || (s[0] == surah && s[1] <= ayah)
            if (before) { ans = mid; lo = mid + 1 } else hi = mid - 1
        }
        return ans + 1
    }
}
