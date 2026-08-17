package io.github.salehgnutux.gtsalat.util

import android.content.Context
import android.content.Intent
import io.github.salehgnutux.gtsalat.domain.Credits

/** توحيدُ نصّ النسخ/المشاركة لأيّ ذكرٍ/حكمةٍ/آية: المتن + المصدر + رابطا نسختَي الهاتف وسطح المكتب. */
object Share {

    /** وسومٌ للنشر (اسم البرنامج + وسومٌ عربيّة). الوسم ينتهي بالمسافة، فالمركّبة بشرطةٍ سفليّة. */
    private const val HASHTAGS =
        "#GT_SALAT #تطبيق #الصلاة #الأذكار #القرآن #دعاء #القبلة #مواقيت_الصلاة #حصن_المسلم #رقية_شرعية"

    private val FOOTER = buildString {
        append("\n\n— عبر تطبيق GT-SALAT")
        append("\nنسخة الهاتف: ").append(Credits.WEBSITE)
        append("\nنسخة سطح المكتب: ").append(Credits.DESKTOP_WEBSITE)
        append("\n\n").append(HASHTAGS)
    }

    /** يزيّن النصّ بالمصدر (اختياريّ) وتذييل الروابط — يُستعمَل للنسخ والمشاركة معاً. */
    fun decorate(body: String, caption: String? = null): String = buildString {
        append(body.trim())
        if (!caption.isNullOrBlank()) append("\n").append(caption.trim())
        append(FOOTER)
    }

    /** يفتح ورقة مشاركة النظام بنصٍّ مزيَّنٍ بالروابط. */
    fun send(context: Context, body: String, caption: String? = null) {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, decorate(body, caption))
        }
        runCatching { context.startActivity(Intent.createChooser(i, "مشاركة").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
