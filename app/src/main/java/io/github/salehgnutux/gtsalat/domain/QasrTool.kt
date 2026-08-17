package io.github.salehgnutux.gtsalat.domain

/**
 * أداةُ «هل يجوز لي القصر الآن؟» — منطقٌ نقيٌّ على المذهب المالكيّ، قواعده قابلةٌ للمراجعة.
 * القصر عند المالكيّة **سنّةٌ مؤكّدة** لا رخصةٌ فقط، بشروطه. الأرقام (حدّ المسافة/أيّام الإقامة)
 * مضبوطةٌ من المشهور المالكيّ؛ راجِع أهل العلم لحالتك الخاصّة.
 */
object QasrTool {

    /** حدّ مسافة القصر عند المالكيّة: بريدان ≈ ثمانيةٌ وأربعون ميلًا ≈ 83 كلم تقريبًا. */
    const val QASR_DISTANCE_KM = 83

    /** إجابات المستخدم على أسئلة الأداة (نعم/لا). */
    data class Input(
        val longDistance: Boolean,   // تبلغ وجهتك حدّ القصر (~83 كلم) فأكثر؟
        val leftTown: Boolean,       // فارقتَ عمران بلدك؟
        val permissible: Boolean,    // السفر مباحٌ (ليس لمعصية)؟
        val intendsStay4: Boolean,   // نويتَ الإقامة أربعة أيّامٍ فأكثر في وجهتك؟
    )

    enum class Hukm { QASR_SUNNAH, MUST_COMPLETE, NO_QASR }

    data class Result(
        val hukm: Hukm,
        val title: String,           // الحكم مختصرًا
        val shortenedPrayers: String, // الصلوات التي تُقصَر
        val canJoin: Boolean,        // هل يجوز الجمع؟
        val joinText: String,
        val reason: String,          // سبب النتيجة
        val source: String,          // المصدر المالكيّ
    )

    fun evaluate(i: Input): Result {
        val source = "المذهب المالكيّ (مختصر خليل وشروحه، وأحكام الطهارة والصلاة — نايف آل مبارك)"
        // شروطٌ أساسيّة لتحقّق حكم السفر
        if (!i.longDistance || !i.leftTown) {
            return Result(
                Hukm.NO_QASR, "لا قصر — لست في حكم المسافر",
                "تُصلّى الصلوات تامّةً كالمقيم.", false, "لا جمع.",
                if (!i.longDistance) "لأنّ وجهتك دون حدّ مسافة القصر (~$QASR_DISTANCE_KM كلم)."
                else "لأنّك لم تفارق عمران بلدك بعد؛ يبدأ حكم السفر بمفارقة العمران.",
                source,
            )
        }
        if (!i.permissible) {
            return Result(
                Hukm.NO_QASR, "لا رخصة في سفر المعصية",
                "تُصلّى تامّةً على المشهور.", false, "لا جمع.",
                "لأنّ رخص السفر (القصر والجمع) لا تُستباح بسفر المعصية على المشهور عند المالكيّة.",
                source,
            )
        }
        if (i.intendsStay4) {
            return Result(
                Hukm.MUST_COMPLETE, "تُتمّ الصلاة (نويتَ الإقامة)",
                "تُصلّى تامّةً؛ انقطع حكم السفر بنيّة الإقامة.", false, "لا جمع لأجل السفر.",
                "لأنّ من نوى الإقامة أربعة أيّامٍ فأكثر أتمّ ولم يقصر.",
                source,
            )
        }
        return Result(
            Hukm.QASR_SUNNAH, "يُسنّ لك القصر (سنّة مؤكّدة)",
            "تُقصَر الرباعيّة إلى ركعتين: الظهر والعصر والعشاء. أمّا الصبح فركعتان والمغرب ثلاثٌ فلا تُقصَران.",
            true, "ويجوز الجمع (الظهر مع العصر، والمغرب مع العشاء) بشروطه في السفر.",
            "لتحقّق شروط السفر: بلوغ المسافة، ومفارقة العمران، وإباحة السفر، وعدم نيّة الإقامة المانعة.",
            source,
        )
    }
}
