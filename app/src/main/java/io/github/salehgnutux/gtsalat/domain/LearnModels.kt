package io.github.salehgnutux.gtsalat.domain

import kotlinx.serialization.Serializable

/**
 * نماذج قسمَي «تعلّم الطهارة والصلاة» (مذهب مالكيّ) و«الرقية الشرعية».
 * كلّ المحتوى في assets/content قابلٌ للتحرير فيُصحَّح النصّ أو المصدر دون لمس الكود.
 * حقل [draft] = مسودّةٌ تقنيّةٌ تحتاج مراجعةً فقهيّةً قبل اعتمادها.
 */

@Serializable
data class LearnSource(val title: String = "", val ref: String = "")

/** خطوةٌ مصوّرةٌ في درسٍ (مستويان: مبسّط [short] + «شرح أكثر» [full]). */
@Serializable
data class LearnStep(
    val n: Int = 0,
    val title: String = "",
    val short: String = "",
    val full: String = "",
    val said: String = "",        // ما يُقال في هذا الموضع (ذكرٌ/قراءة) — يُعرَض بزرّ «ما يُقال»
    val ruling: String = "",      // فرض/سنة/فضيلة/مكروه/ناقض… (نصٌّ حرّ)
    val image: String = "",       // اسم أصلٍ مستقبليّ (أيقونات الآن)
    val source: LearnSource? = null,
)

@Serializable
data class LearnRulingItem(val text: String = "", val ruling: String = "", val source: LearnSource? = null)

/** مجموعةُ أحكامٍ (فرائض/سنن/مكروهات/نواقض/أخطاء شائعة…). */
@Serializable
data class LearnRulingGroup(
    val title: String = "",
    val items: List<LearnRulingItem> = emptyList(),
    val note: String = "",
    val source: LearnSource? = null,
)

/** قسمٌ تعليميّ (الوضوء، الغسل، تعلّم الصلاة…): خطواتٌ + مجموعات أحكام. */
@Serializable
data class LearnSection(
    val id: String = "",
    val title: String = "",
    val icon: String = "",
    val intro: String = "",
    val imageDir: String = "",     // مجلّد صور الدليل المصوَّر في الأصول (مثل "learn/wudu")
    val imageCount: Int = 0,       // عدد صور المعرض (01.webp .. NN.webp)
    val draft: Boolean = false,    // مسودّة تحتاج مراجعةً فقهيّة
    val steps: List<LearnStep> = emptyList(),
    val rulings: List<LearnRulingGroup> = emptyList(),
    val note: String = "",
    val tool: String = "",     // أداةٌ تفاعليّةٌ مرتبطةٌ بالقسم (مثل "qasr")
)

@Serializable
data class LearnFile(val sections: List<LearnSection> = emptyList(), val disclaimer: String = "")

/* ================================ الرقية الشرعية ================================ */

/** مقطعُ رقيةٍ: قرآنٌ (سورة + آيات) أو دعاءٌ/ذكرٌ ثابت. */
@Serializable
data class RuqyahSegment(
    val label: String = "",
    val kind: String = "quran",    // quran | dua | dhikr
    val surah: Int = 0,
    val ayahFrom: Int = 0,
    val ayahTo: Int = 0,
    val text: String = "",         // للدعاء/الذكر (القرآن يُجلب من نصّ التطبيق)
    val note: String = "",
    val repeat: Int = 0,           // عددٌ ثابتٌ بدليل (0 = بلا تحديد)
    val source: LearnSource? = null,
)

/** قسمُ رقيةٍ (شاملة، العين، السحر…): مقاطعُ قرآنٍ وأدعية. */
@Serializable
data class RuqyahSection(
    val id: String = "",
    val title: String = "",
    val icon: String = "",
    val note: String = "",
    val link: String = "",     // مسارٌ يفتحه النقر على الرسالة (مثل قسم الأذكار)
    val segments: List<RuqyahSegment> = emptyList(),
)

@Serializable
data class RuqyahFile(val sections: List<RuqyahSection> = emptyList(), val disclaimer: String = "")
