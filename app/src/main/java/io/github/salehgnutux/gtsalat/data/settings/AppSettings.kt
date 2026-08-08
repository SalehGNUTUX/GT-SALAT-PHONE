package io.github.salehgnutux.gtsalat.data.settings

import io.github.salehgnutux.gtsalat.domain.AsrMadhab
import io.github.salehgnutux.gtsalat.domain.CalendarKind
import io.github.salehgnutux.gtsalat.domain.MonthScheme
import io.github.salehgnutux.gtsalat.domain.PrayerId

enum class AdhanType { FULL, SHORT, CUSTOM }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** نمط تنبيه دخول الوقت: أذانٌ كامل، أم رنّة تنبيهٍ قصيرة، أم صامتٌ (إشعارٌ بلا صوت). */
enum class AdhanAlertMode { FULL, TONE, SILENT }

/** كامل إعدادات التطبيق — تُخزَّن في DataStore وتُبثّ كـ Flow. */
data class AppSettings(
    val lat: Double? = null,
    val lon: Double? = null,
    val city: String = "",
    val country: String = "",
    val methodId: Int = 3,
    val madhab: AsrMadhab = AsrMadhab.SHAFI,
    val preNotifyMinutes: Int = 15,
    val adhanType: AdhanType = AdhanType.FULL,
    val customAdhanUri: String? = null,
    val customAdhanName: String = "",
    val enableSalatNotify: Boolean = true,
    val enableAdhanSound: Boolean = true,
    val enableDuaAfterAdhan: Boolean = false,
    val adhanAlertMode: AdhanAlertMode = AdhanAlertMode.FULL,
    val adhanVolume: Int = 100,                // مستوى صوت الأذان 0..100
    val perPrayerAlerts: Boolean = false,      // عند التفعيل تُخصَّص كلّ صلاة
    val prayerAlerts: List<AdhanAlertMode> = List(5) { AdhanAlertMode.FULL }, // فجر/ظهر/عصر/مغرب/عشاء
    val enablePreNotify: Boolean = true,
    val enablePreNotifySound: Boolean = true,
    val enablePostDhikr: Boolean = true,
    val postDhikrMinutes: Int = 20,
    val enableDailyAyah: Boolean = true,          // بطاقة آية اليوم في الرئيسيّة
    val enableRecitationReminder: Boolean = true, // تذكير وِرد التلاوة
    val enableWhiteDaysReminder: Boolean = true,  // تذكير الأيّام البيض (13/14/15 هجريّ)
    val enableSunnahReminder: Boolean = true,     // تذكير السُّنن الموسميّة (اثنين/خميس، جمعة، عاشوراء، عرفة…)
    val enableMorningAdhkar: Boolean = false,     // تذكير أذكار الصباح
    val enableEveningAdhkar: Boolean = false,     // تذكير أذكار المساء
    val morningAdhkarHour: Int = 6,               // ساعة تذكير أذكار الصباح
    val eveningAdhkarHour: Int = 17,              // ساعة تذكير أذكار المساء
    val reminderHour: Int = 8,                    // ساعة التذكيرات اليوميّة
    val useApiTimetables: Boolean = true,
    val doNotDisturb: Boolean = false,
    val autoSilence: Boolean = false,
    val silenceMinutes: Int = 15,
    val persistentNotification: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val seedColor: Int = 0,   // 0 = لون السِمة الافتراضيّ (أخضر)؛ غيره ARGB مخصّص
    // تدرّج الخلفيّة المخصّص لكلّ وضع (0 = تلقائيّ مشتقٌّ من السِمة)
    val gradTopLight: Int = 0,
    val gradBotLight: Int = 0,
    val gradTopDark: Int = 0,
    val gradBotDark: Int = 0,

    val monthScheme: MonthScheme = MonthScheme.AUTO,
    val timetableCalendar: CalendarKind = CalendarKind.HIJRI,
    val clock24h: Boolean = true,                 // عرض الوقت: 24 ساعة أو 12 (ص/م)
    val hijriOffset: Int = 0,                     // إزاحة التاريخ الهجريّ بالأيّام (تصحيح فرق المنطقة)
    val settingsOpenSection: String = "الموقع وطريقة الحساب",  // آخر قسم إعداداتٍ مفتوح ("" = الكلّ مطويّ)
    val checkUpdates: Boolean = true,             // إشعارٌ عند توفّر نسخةٍ جديدة
    val fullScreenAdhan: Boolean = true,          // نافذة أذانٍ ملء الشاشة فوق القفل
    val keepAdhanScreen: Boolean = false,         // إبقاء نافذة ملء الشاشة بعد انتهاء الصوت حتى يغلقها المستخدم
    val setupCompleted: Boolean = false,
    // موضع القراءة والاستماع الأخيرين (كلٌّ مستقلّ) + آخر قارئٍ مختار + آخر صفحة مصحف
    val lastReadSurah: Int = 0,      // 0 = لا يوجد
    val lastReadAyah: Int = 1,
    val lastListenSurah: Int = 0,
    val lastListenAyah: Int = 1,
    // متابعة القرآن المسموع (سورةٌ كاملة): آخر سورةٍ/قارئٍ/خادمٍ وموضعٍ بالميلّي — لاستئناف الاستماع.
    val lastAudioSurah: Int = 0,     // 0 = لا يوجد
    val lastAudioReciter: String = "",
    val lastAudioReciterName: String = "",
    val lastAudioServer: String = "",
    val lastAudioPosMs: Long = 0L,
    val lastReciterId: String = "",
    val lastMushafPage: Int = 1,
    val lastRiwaya: String = "hafs",              // آخر روايةٍ مختارة في القارئ النصّيّ (تُستعاد)
    val quranScrollSpeed: Int = 100,              // مهلة القراءة التلقائيّة (٪): 70 أسرع … 220 تدبُّر
    // إشارات القرآن المرجعيّة: مجموعة مفاتيح "سورة:آية" (مثل "2:255").
    val bookmarks: Set<String> = emptySet(),
    // وِرد التلاوة اليوميّ (يدويّ): بطاقةٌ اختياريّةٌ مطفأةٌ افتراضيّاً (التذكير في الإشعارات مستقلٌّ ومفعّل).
    val enableWird: Boolean = false,
    // طريقة عرض أقسام الأذكار: true = بطاقاتٌ سلايد (افتراضيّ، بأسلوب أسماء الله الحسنى)، false = قائمة.
    val adhkarCardView: Boolean = true,
    // نمط ودجت الساعة/التقدّم: classic (كلاسيكيّ، الساعة يميناً) أو center (مركزيّ).
    val widgetProgressStyle: String = "classic",
    // بطاقات «المزيد» المفضّلة (بمفاتيح المسارات) — تصعد للأعلى بترتيبها الأصليّ.
    val favoriteFeatures: Set<String> = emptySet(),
    // الوحدة والعدد + آخر يومٍ أُتمَّ فيه + سلسلة الأيّام المتتالية.
    val wirdGoalUnit: String = "juz",   // juz | hizb | pages | ayat
    val wirdGoalCount: Int = 1,
    val wirdLastDoneDate: String = "",  // yyyy-MM-dd لآخر إتمام
    val wirdStreak: Int = 0,
) {
    val hasLocation: Boolean get() = lat != null && lon != null

    /** نمط التنبيه الفعّال لصلاةٍ ما: مخصّصٌ لكلّ صلاة إن فُعّل الخيار، وإلّا الإعداد العامّ. */
    fun alertFor(id: PrayerId): AdhanAlertMode {
        if (!perPrayerAlerts) return adhanAlertMode
        val idx = when (id) {
            PrayerId.FAJR -> 0
            PrayerId.DHUHR -> 1
            PrayerId.ASR -> 2
            PrayerId.MAGHRIB -> 3
            PrayerId.ISHA -> 4
            else -> return adhanAlertMode
        }
        return prayerAlerts.getOrElse(idx) { AdhanAlertMode.FULL }
    }

    companion object {
        /** ترتيب الصلوات الخمس في قائمة prayerAlerts. */
        val ALERT_PRAYERS = listOf(PrayerId.FAJR, PrayerId.DHUHR, PrayerId.ASR, PrayerId.MAGHRIB, PrayerId.ISHA)
    }
}
