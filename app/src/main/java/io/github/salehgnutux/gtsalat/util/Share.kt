package io.github.salehgnutux.gtsalat.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.github.salehgnutux.gtsalat.BuildConfig
import io.github.salehgnutux.gtsalat.domain.Credits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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

    /** نصٌّ تعريفيٌّ بالتطبيق للمشاركة: نبذةٌ + الإصدار + رابط الموقع + الوسوم. */
    fun appShareText(): String = buildString {
        append("GT-SALAT — تطبيقٌ إسلاميٌّ حرٌّ شاملٌ لأندرويد: مواقيت الصلاة والأذان، والقبلة، ")
        append("والأذكار وحصن المسلم، والقرآن الكريم (نصّاً وصوتاً ومصحفاً مصوَّراً) والتفسير، ")
        append("والرقية الشرعية، ودروسٌ مصوَّرةٌ للطهارة والصلاة.")
        append("\n\nيعمل دون إنترنت، وبلا إعلانات ولا تتبّع ولا خدمات Google.")
        append("\n\nالإصدار ").append(BuildConfig.VERSION_NAME.substringBefore("-"))
        append("\n").append(Credits.WEBSITE)
        append("\n\n").append(HASHTAGS)
    }

    /** الزرّ الأوّل: مشاركة رابط التطبيق بنصٍّ تعريفيّ. */
    fun shareLink(context: Context) {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, appShareText())
        }
        runCatching { context.startActivity(Intent.createChooser(i, "مشاركة رابط التطبيق").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /**
     * الزرّ الثاني: مشاركة ملفّ التثبيت (APK) نفسه عبر تطبيقات المراسلة/المشاركة المحلّيّة —
     * لمن لا إنترنت عنده. يُنسَخ APK التطبيق المثبَّت إلى الكاش ويُشارَك عبر FileProvider.
     */
    suspend fun shareApk(context: Context) {
        val uri = withContext(Dispatchers.IO) {
            runCatching {
                val src = File(context.applicationInfo.sourceDir)
                val dir = File(context.cacheDir, "shared_apk").apply { mkdirs() }
                val dest = File(dir, "GT-SALAT-${BuildConfig.VERSION_NAME}.apk")
                src.copyTo(dest, overwrite = true)
                FileProvider.getUriForFile(context, context.packageName + ".fileprovider", dest)
            }.getOrNull()
        } ?: return
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, appShareText())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(Intent.createChooser(i, "مشاركة ملفّ التثبيت").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
