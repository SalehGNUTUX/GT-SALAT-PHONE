# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **GT-SALAT — نسخة الهاتف / أندرويد.** كل commit و README و CHANGELOG والنصوص داخل المستودع **بالعربيّة الفصحى**، وبإسم SalehGNUTUX فقط بلا `Co-Authored-By`.

---

## نبذة وقرارات مؤسِّسة

نسخة الهاتف من مشروع سطح المكتب `GT-SALAT` (Electron+React، في `../GT-SALAT`). تُبنى أصليّاً.

- **الاسم داخل التطبيق يبقى `GT-SALAT`**. لاحقة `PHONE` في اسم المستودع فقط.
- **الأولويّة أندرويد الآن**، ثمّ لينكس (PostmarketOS/Mobian) و iOS مستقبلاً (تُعزَل طبقة `domain` لإعادة الاستخدام).
- **الرؤية:** تطبيق إسلاميّ **شامل غنيّ**، لا مذكّر صلاة فقط. المبدأ: كلّ ما يُمكن محليّاً يعمل محليّاً؛ وما يحتاج إنترنت **يُنزَّل ليعمل محليّاً**.

### الحزمة التقنيّة (نُسخ مطابقة لمشروع GT-TAHAKOM المُختبَر محليّاً)
Kotlin 2.1.0 · AGP 8.9.2 · Gradle 8.11.1 · Compose BOM 2024.12.01 · Material 3 · Hilt 2.53.1 ·
Room 2.6.1 · DataStore 1.1.1 · WorkManager 2.9.1 · OkHttp 4.12 · kotlinx.serialization 1.7.3 ·
مكتبة الحساب `com.batoulapps.adhan:adhan:1.2.1` · **compileSdk 36 · targetSdk 36 · minSdk 26**.

### نكهتان (product flavors، بُعد `edition`)
- **`foss`** (applicationIdSuffix `.foss`): بلا Google — الموقع عبر `LocationManager` + Nominatim. للتنزيل المباشر وF-Droid، وفيها المحدِّث الذاتيّ.
- **`full`**: خدمات Google (`play-services-location` → Fused Location). للتنزيل المباشر الآن ولغوغل بلاي لاحقاً، وفيها المحدِّث الذاتيّ ما دام التطبيق خارج المتجر (بناء المتجر بـ`-PplayStore=true` يُخرجه)؛ يوقَّع بمفتاح `playUpload` إن وُجد `keystore.properties` (انظر `PLAY-CONSOLE.md` و`signing-fingerprints.txt`).
- التجريد: واجهة `LocationProvider` في `main`، وتطبيقان بنفس الاسم المؤهّل `PlatformLocationProvider`
  في `src/foss` و`src/full`، يربطهما `di/LocationModule` (@Binds).
- **المحدِّث في النكهتين منذ 2.3** ما دام التطبيق خارج بلاي: الشيفرة في `src/selfUpdate` (`UpdateRepository` و`UpdateBanner` و`UpdateSeam` و`UpdateInstaller` و`UpdateDownloadService` والمانيفست بإذن التثبيت وخدمة التنزيل)، تضيفها `sourceSets` في `app/build.gradle.kts` إلى foss وfull، بـ`kotlin.srcDir` لا `java.srcDir` وحده (وإلّا لم تُترجَم). **بناء المتجر** `-PplayStore=true` يستبدل بها في full `src/playStore` (بدائل خالية بنفس التواقيع ومانيفست بلا إذن)، فسياسة بلاي تمنع تحديث التطبيق نفسه. كلّ نكهةٍ تختار حزمتها من الإصدار (`-foss.apk`/`-full.apk` حسب `BuildConfig.USES_GMS`).

---

## الأوامر

```bash
./gradlew assembleFossDebug        # APK النكهة الحرّة
./gradlew assembleFullDebug        # APK نكهة Google
./gradlew :app:compileFossDebugKotlin   # تحقّق سريع من الترجمة (استعمله بعد كلّ دفعة)
```
الحزم في `app/build/outputs/apk/`. الـ SDK في `local.properties` (غير مُدرَج في git).
**مفتاح التوقيع:** الحزم الموزَّعة توقَّع بالمفتاح الدائم في `~/.gt-salat-keys/` (خارج المستودع)، ويقع البناء بدونه إلى مفتاح debug للجهاز مع تنبيه. **تحقّق من البصمة قبل أيّ توزيع:** `apksigner verify --print-certs <apk>` يجب أن يطابق `signing-fingerprints.txt`.
**متطلّبات الجهاز:** JDK كامل لا JRE (`openjdk-21-jdk-headless`، وإلّا فشلت KSP بـ`JAVA_COMPILER`) · Android SDK في `~/Android/Sdk` (`platforms;android-36` + `build-tools;35.0.0` + `platform-tools`).
لا اختبارات وحدة في المشروع؛ التحقّق عبر `compileFossDebugKotlin`. البناء الأوّل قد يبطؤ (تنزيل تبعيّات) — مهلة ≥ 9 دقائق.
الأصول الثنائيّة (الخطوط/الأصوات/azkar) منقولة من `../GT-SALAT/resources` و`src/assets`.
**نمط العمل:** بعد كلّ دفعة → `compileFossDebugKotlin` (تحقّق). التحزيم عند طلب المستخدم «حزّم» فقط.

### طقس الإصدار (نفّذه بالترتيب عند «حزّم/أصدر»)
> **قاعدة دائمة:** لا `push`/`tag`/`release` إلّا بإذنٍ صريح. نعمل على نسخ اختبارٍ محليّة أولاً (`GT-SALAT-vX.Y.Z-beta-test.apk` في الجذر، مُتجاهَلٌ في git)، وحفظٍ بـ commit محلّيّ عند الطلب، ثمّ نُصدر ونرفع بعد الموافقة.
1. رفع `versionCode` (+1) و`versionName` في `app/build.gradle.kts`.
2. مدخلٌ أعلى `CHANGELOG.md` (عربيّ)، وتحديث رقم النسخة في `README.md` و`docs/index.html` (سطر `.ver` + **روابط تنزيل الـAPK لكلا النكهتين** foss/full).
3. `./gradlew assembleFossDebug assembleFullDebug` ثمّ التحقّق من البصمة، ثمّ نسخ الناتجَين إلى الجذر باسمَي `GT-SALAT-vX.Y.Z-foss.apk` و`-full.apk` (الـAPK مُتجاهَلٌ في git). احذف APK الإصدار السابق ونسخة الاختبار من الجذر.
4. commit عربيّ + `git tag vX.Y.Z` + `git push origin main` + `git push origin vX.Y.Z`.
5. **`gh release create vX.Y.Z "GT-SALAT-vX.Y.Z-foss.apk" "GT-SALAT-vX.Y.Z-full.apk" --latest --title … --notes-file <ملفّ>`** — استعمل **`--notes-file` حصراً** (heredoc `<<'NOTES'`): نصوص العربيّة/الـbacktick في `--notes` تكسر bash (تُفسَّر كأوامر). للتجريبيّ استعمل `--prerelease` ووسم `-beta`.
- الوسم البعيد قد يتقدّم برفعٍ من واجهة GitHub → عند رفض الدفع: `git pull --rebase origin main` ثمّ أعد الدفع. لنقل وسمٍ موجود: `git tag -f` + `git push -f origin <tag>` ثمّ `gh release upload <tag> … --clobber`.
**محتوى `assets/content/*.json`** مُولَّد مرّةً بسكربت Node: أحاديث/أدعية/حِكَم/أسماء من `../GT-SQRM/GT-SIRM/GT-SIRM-WEB/*-data.js`،
حصن المسلم من `../GT_HISNMUSLIM-main/assets/data/`، والتفسير الميسّر `tafsir.json` (~4MB، 6236 آية) نُزّل ودُمج من `api.alquran.cloud` (ar.muyassar + quran-uthmani).
**محتوى الفقه (`purity_salah.json`) والرقية (`ruqyah.json`)** مكتوبٌ يدويّاً من مصادرَ مالكيّةٍ معتمدة (الموطّأ، الرسالة، ابن عاشر، مختصر خليل، وكتاب نايف آل مبارك)، ومذيَّلٌ بتنبيه المراجعة. **قاعدةٌ حاكمة: لا تأليفَ فقهيّ من عند النموذج ولا توثيق تصحيح الأحاديث؛ كلّ حكمٍ بمصدره، وآيات الرقية تُجلَب من مصحف التطبيق (`ContentRepository.ayatText`) لا تُكتَب يدويّاً.** الأداتان التفاعليّتان (طهرت الآن/القصر) تُبنيان فقط بعد تثبيت جداول قواعدهما فقهيّاً.

---

## المعماريّة (الطبقات)

التدفّق: `ui (Compose+ViewModel)` → `data (Repository)` → `domain (حساب نقيّ)` / `local (Room)` / `remote (API)`.
والخلفيّة: `alarm` + `audio` + `notification` مستقلّة عن الواجهة، تُدار عبر Hilt.

```
app/src/main/java/io/github/salehgnutux/gtsalat/
├── domain/              ← نقيّة بلا Android (قابلة لإعادة الاستخدام لاحقاً)
│   ├── PrayerModels.kt · CalculationMethods.kt (22 طريقة) · PrayerCalculator.kt (computeDay/Month + qibla + nextPrayer)
│   ├── Ramadan.kt          كشف رمضان + يوم الشهر + النطاق الميلاديّ (java.time HijrahDate/أمّ القرى) — للبطاقة والإمساكيّة
│   ├── MorningEveningAdhkar.kt (أذكار الصباح/المساء بعدد التكرار) — [أسماء الله انتقلت إلى assets/content/asma.json]
│   ├── IslamicContent.kt   نماذج @Serializable للمحتوى (أحاديث/أدعية/حِكَم/أسماء/حصن/تفسير)
│   ├── Quran.kt            نماذج القرآن + بُناة روابط (everyayah/mp3quran/صور) + `normalize` (تطبيع عربيّ للبحث) + AyahHit
│   ├── Credits.kt          ★ قائمة المصادر الحرّة — تُحدَّث كلّما اعتمدنا مصدراً (تظهر في الإعدادات + الموقع)
│   ├── GregorianMonths.kt  أسماء الأشهر الإقليميّة (MAGHREB/LEVANT/STANDARD) + MonthScheme + CalendarKind
│   ├── SunnahReminders.kt  تذكيرات السُّنن الموسميّة حسب الشهر/اليوم الهجريّ ويوم الأسبوع (رسالةٌ واحدةٌ باليوم بأولويّة)
│   ├── TaharaTool.kt · QasrTool.kt  منطق الأداتين التفاعليّتين (طهرت الآن/القصر)
│   ├── AdhkarCompletion.kt · WhatsNew.kt  رسائل إكمال الأذكار · محتوى «ما الجديد»
│   └── LearnModels.kt      نماذج «تعلّم الطهارة والصلاة» (LearnSection/Step/RulingGroup/Source) و«الرقية» (RuqyahSection/Segment)
├── data/
│   ├── PrayerRepository.kt   ★ سلسلة السقوط: Room → API → حساب محلّيّ + prefetchMonths + detectAndSaveLocation
│   ├── ContentRepository.kt  يقرأ assets/content/*.json (أحاديث/أدعية/حِكَم/أسماء/حصن/تفسير) + حكمة يوميّة
│   ├── QuranRepository.kt     quran_meta.json + نصّ من tafsir + بُناة روابط + searchAyat (بحثٌ شاملٌ عبر 6236 آية) + surahRecitersOnline
│   ├── QuranDownloader.kt     تنزيل المصحف/السور إلى filesDir للعمل دون إنترنت (StateFlow للتقدّم)
│   ├── AzkarRepository.kt     يقرأ azkar.txt (لذكر اليوم في الرئيسيّة)
│   ├── [src/foss] UpdateRepository.kt  ★ فحص أحدث إصدار GitHub دوريّاً + `UpdateInfo` (StateFlow عالميّ لشريط الرئيسيّة؛ في `src/full` بديلٌ خالٍ)
│   ├── [src/foss] UpdateInstaller.kt + UpdateDownloadService.kt  ★ تنزيل الـAPK في خدمة مقدّمةٍ باستئنافٍ + استدعاء مثبّت النظام عبر FileProvider
│   ├── BackupManager.kt       ★ نسخٌ احتياطيّ انتقائيّ ZIP (إعدادات/مواقيت/صوت/مصحف) + inspect (فحصٌ قبل استيراد انتقائيّ)
│   ├── local/ remote/ location/ settings/   (Room · AladhanApi+GeoClients · LocationProvider · AppSettings+SettingsRepository)
├── alarm/               ★ حلّ «الأذان في وقته والتطبيق مغلق»
│   ├── PrayerAlarmScheduler.kt  ★ الأذان بـ setAlarmClock (لا يؤجّله Doze) + scheduleTest + refreshStatus (إشعار دائم) + refreshWidgets
│   ├── PrayerAlarmReceiver.kt   ACTION_ADHAN/PRENOTIFY/RESTORE_SOUND/POST_DHIKR/TEST (كتم تلقائيّ + full-screen intent للأذان/الأذكار)
│   ├── AdhanAlarmActivity.kt    ★ نافذة أذانٍ ملء الشاشة فوق القفل (setShowWhenLocked/TurnScreenOn + وميض + زرّ إيقاف)
│   ├── BootReceiver.kt (BOOT/MY_PACKAGE_REPLACED/TIME_SET/TIMEZONE_CHANGED) · RescheduleWorker (6س) · WorkScheduler
├── audio/       AdhanService.kt (mediaPlayback أذان، حارسٌ زمنيّ MAX_SERVICE_MS) · QuranAudioService.kt (تلاوة القرآن، آية-بآية/سورة كاملة) + QuranPlayback + QuranAudio · RadioService · **RuqyahAudioService** (قائمة تشغيلٍ من الآيات everyayah للرقية المسموعة) + RuqyahPlayback + RuqyahAudio · AdhanPreviewer · RingerController (كتم) · **AdhkarAudioService** + AdhkarPlayback + AdhkarAudio (الأذكار المسموعة من `res/raw/adhkar_*.ogg`) · VoiceCounter (عدّ التسبيح صوتيّاً)
├── sensor/Compass.kt       بوصلة (TYPE_ROTATION_VECTOR) كـFlow — للقبلة
├── notification/NotificationHelper.kt  ★ 5 قنوات (adhan/prenotify/service/status/reminders). صوت الأذان عبر الخدمة لا القناة.
├── widget/      NextPrayerWidget + TodayTimesWidget + PrayerProgressWidget (ساعة + شريط تقدّم بين صلاتين، نمطان classic/center) — Glance خلفيّة شفّافة + WidgetData (Hilt EntryPoint، حساب محلّيّ) + عناصرٌ أصليّة حيّة (TextClock/Chronometer عبر AndroidRemoteViews+fillMaxSize)
├── di/          AppModule + LocationModule (@Binds حسب النكهة)
├── ui/
│   ├── MainActivity + RootViewModel + AppRoot (Box تدرّج + Scaffold + شريط سفليّ 5 تبويبات + المزيد nested-graph + بوّابة Setup + deep-link إشعارات عبر `EXTRA_ROUTE`)
│   ├── theme/    Color (أخضر/ذهبيّ) + Type (Ubuntu Arabic + Amiri) + Theme (Material You + RTL)
│   └── screens/  Dashboard(+حلقة تقدّم +RamadanCard) · Timetable · Settings(أكورديون محفوظ +نسخ احتياطيّ) · Setup(offline: GPS حيّ+قاعدة مواقع+يدويّ+استيراد+تخطّي) · Qibla · Tasbih(+مختلط) · More · Events(بحث) · ThemeToggle · DownloadsSettings ·
│                 Asma (Pager) · Hisn(بحث) · AdhkarSession · Content(حديث/أدعية/حِكَم) · Tafsir · UpdateBanner(شريط التحديث) · ImsakiahScreen ·
│                 QuranScreens (Hub + SurahIndex + TextReader(+إشارات) + QuranBookmarks + Wird + AudioRecitation + QuranMiniPlayer) · MushafScreen (+ViewModels) ·
│                 AdhkarAudioScreen + AdhkarPlayerBar · WhatsNewDialog · TaharaToolScreen · QasrToolScreen · CommonUi ·
│                 **LearnScreens** (تعلّم الطهارة والصلاة: Hub + SectionScreen بمستويين + LearnViewModel) · **RuqyahScreens** (الرقية: Hub + SectionScreen بوضعَي أقرأ/أستمع + مشغّل)
├── util/   Connectivity.kt · Format.kt (clock/countdown/gregorianArabic + hijriAdjusted/hijriForDate — أرقام مغربيّة 0-9) · Share.kt (تذييل نسخ/مشاركة برابطي الهاتف وسطح المكتب)
└── GtSalatApp.kt    @HiltAndroidApp + Configuration.Provider + ensureChannels/ensurePeriodic + scheduleNext عند الإقلاع
```

### نمط الجدولة (الأهمّ — مستوحى من Five-Prayers + NoorUlHuda)
الأذان والتنبيه المسبق يُجدولان بـ **`setAlarmClock`** (يُعامَل كإنذار منبّه، **لا يؤجّله Doze/توفير البطاريّة**، ويتطلّب من أندرويد 12 إذن الإنذار الدقيق المعلَن في المانيفست)؛ باقي الإنذارات (استعادة الرنين/ذكر بعد الصلاة/التذكيرات) بـ `setExactAndAllowWhileIdle` مع سقوطٍ إلى `setAndAllowWhileIdle` عند رفض الإذن.
`scheduleNext()` يحسب أوّلاً **الصلوات الـ12 القادمة** (`repo.upcomingPrayers` + مفاتيح مُفهرَسة `RC_ADHAN_BASE+i`/`RC_PRENOTIFY_BASE+i`) ثمّ يلغي القديم ويسلّحها، كشبكة أمانٍ إن جُمّد التطبيق فلم يُعِد الجدولة الذاتيّة. **ترتيب `ACTION_ADHAN` مقصود:** الإشعار والصوت **أوّلاً** (إذن بدء خدمة المقدّمة بعد الإنذار قصير الأمد)، ثمّ `scheduleNext(allowNetwork = false)` في `finally`. مسار المنبّه والإقلاع (`allowNetwork = false`) لا يمرّ بالشبكة. كلّ إنذارٍ يحمل موعده `EXTRA_SCHEDULED_AT`، وما تأخّر عن الحدّ (`MAX_LATE_*`: 3 دقائق للأذان والذكر و2 للاقتراب) لا يُسمَع: إعلامٌ صامتٌ للأذان وإسقاطٌ للبقيّة. `alarm/AlarmLog` يسجّل الموعد والوصول الفعليّ (يُعرَض في بطاقة الموثوقيّة). إنذارات AlarmManager تُمحى عند الإقلاع → `BootReceiver` يعيد التسليح؛ `RescheduleWorker` الدوريّ (6س) شبكة أمان + تحديث الكاش. إطلاق خدمة المقدّمة من مُستقبِل الإنذار مسموح (استثناء الإنذار من قيود FGS في الخلفيّة).
**تأخّر الإنذار حتى فتح الشاشة** مصدره عادةً إدارة بطاريّة المُصنّع (Xiaomi/Oppo/Transsion…) التي تجمّد التطبيق، ولا تعالجه الشيفرة وحدها؛ العلاج من المستخدم: إعفاء البطاريّة + التشغيل التلقائيّ (بطاقة الموثوقيّة). زرّ «اختبار التنبيه» (`scheduleTest`) وسجلّ التنبيهات يفرّقان بينه وبين خللٍ داخل التطبيق.

---

## نمط إضافة ميزة جديدة
1. نموذج نقيّ في `domain/`. 2. مصدر بيانات في `data/` (Room/remote/assets). 3. دالّة في `PrayerRepository` أو مستودع جديد.
4. ViewModel في `ui/screens/`. 5. شاشة Compose + إضافتها لـ`AppRoot` (وجهة/تبويب). 6. أذونات في المانيفست عند اللزوم.
عند إضافة حقل إعداد: أضِفه في `AppSettings` + مفتاح وقراءة/كتابة في `SettingsRepository`.

## مزالق مثبتة
- **RTL:** الجسم Rtl عبر `LocalLayoutDirection`؛ في `Row` استعمل أيقونات `Icons.AutoMirrored`.
- **الأرقام:** استعمل **الأرقام المغربيّة 0-9 حصراً** في كامل المشروع (واجهة + توثيق + commits)، لا المشرقيّة ١-٩. (`Format` بـ`Locale.US`.) لا تستعمل `"%d".format(x)` بلا لغة، فهو يتبع لغة الجهاز وقد يُخرج مشرقيّةً؛ بل `String.format(Locale.US, …)`. أمّا `java.time` بالنمط وقوالب `"$n"` فتُخرج 0-9 دائماً.
- **لغة البناء:** على نظامٍ بلغة `ar_EG` تعجز KSP عن الاتصال بخادم مترجم Kotlin، فتسقط إلى وضعٍ احتياطيّ يشوّه المسار العربيّ (`u0628u0631…` ثمّ `source file or directory not found`). الحلّ `-Duser.language=en` في **`kotlin.daemon.jvmargs`** تحديداً (`gradle.properties`)؛ ولا يُغني عنه `org.gradle.jvmargs` ولا `DEFAULT_JVM_OPTS` في `gradlew` (جُرِّبا). والأرجح أنّ السبب الأرقام المشرقيّة في اتّصال الخادم، ولم تُتتبَّع الآليّة بدقّة. الأرقام المشرقيّة في مخرجات Gradle مصدرها لغة النظام لا التطبيق.
- **منطقة الجهاز الزمنيّة:** المواقيت لحظاتٌ صحيحة تُعرَض بمنطقة **الجهاز**. أوقات AlAdhan تُفسَّر بالإزاحة المرسلة معها (`"05:22 (+00)"` ← `AladhanApi.parseOffset`) **لا** بقاعدة مناطق الجهاز: عاد المغرب إلى GMT+0 في 2026-09-20 فأخطأت الهواتف غير المحدَّثة بساعة. `migrateTimetableCache` يمسح صفوف `source = api` القديمة مرّةً (`TIMETABLE_CACHE_VERSION`). تنبيه الرئيسيّة (`domain/TimeZoneCheck`) يقارن إزاحة الهاتف بإزاحة الخادم اليوم (`locationOffsetToday`، مرّةً يوميّاً لكلّ موقع) لا بقاعدة الجهاز، وللعرض فقط. زحزحةٌ منتظمة في كلّ الصلوات تعني منطقةً لا خطأ حساب.
- **التاريخ الهجريّ:** من API افتراضاً (إزاحة `hijriOffset == 0`)؛ وعند أيّ إزاحةٍ (±3) يُحسب **محلّيّاً بأمّ القرى** عبر `Format.hijriAdjusted`/`hijriForDate` — تنعكس على الرئيسيّة **وكلّ أيّام جدول المواقيت** (لكلّ تاريخٍ على حدة). رمضان يُكشَف بـ`HijrahDate` مباشرةً (لا يتأثّر بالإزاحة).
- **الوضع الداكن + التدرّج:** `Scaffold` بخلفيّة `Color.Transparent` (لإظهار التدرّج) **يجب** أن يُمرَّر له `contentColor = onBackground` صراحةً، وإلّا سقط لون النصوص/الأيقونات الافتراضيّ إلى الأسود. أيّ شاشةٍ خارج الـScaffold تُلَفّ بـ`Surface`.
- **الودجت (Glance):** احسب البيانات **محليّاً فوراً** (`PrayerCalculator`) داخل `runCatching` — لا تعتمد كاش/شبكة وإلّا فرغ الودجت لحظيّاً؛ واستعمل `SizeMode.Responsive` فلا يُقصّ المحتوى عند تغيير الحجم.
- **الأصول الكبيرة:** فكّ ترميز JSON الكبير (tafsir ~4MB) على `Dispatchers.Default` لا الخيط الرئيسيّ.
- **تنقّل «المزيد»:** رسمٌ متداخل (`navigation(route=more_graph)`) بـ`saveState`/`restoreState` ليحفظ/يستعيد القسم؛ إعادة النقر على «المزيد» داخل قسمٍ فرعيّ تُظهر رسالةً (العودة/البقاء).
- **الإشعار الدائم:** على أندرويد 14+ يُزال بالسحب (قرار النظام)؛ يُعاد بثّه من `scheduleNext` وعند إقلاع التطبيق.
- **دورة حياة تنبيهات الأذان (تفادي الإشعارات العالقة/المتضاربة):** إشعارا الأذان (`ID_PRAYER`) والتنبيه المسبق (`ID_PRENOTIFY`) لهما **`setTimeoutAfter`** (يمسحهما النظام تلقائيّاً ولو جُمّد التطبيق فلم تُطلَق صلواتُ اليوم التالية). و**لا يمسحهما `scheduleNext`**، فقد يقلع التطبيق بسبب المنبّه نفسه فيمسح ما نُشر للتوّ. ويمسح إشعار خدمة الأذان (`ID_SERVICE`) **فقط إن كان `AdhanPlayback.active == false`**. وخدمة الأذان لها **حارسٌ زمنيّ** (`MAX_SERVICE_MS`) يوقفها قسراً فلا يبقى إشعارها «زومبي» إن جُمّدت العمليّة أثناء التشغيل.
- **الأصوات/الخطوط:** `res/raw` lowercase (adhan_full، adhan_maghribi_full/short: قناةٌ واحدة نحو -16 LUFS، أعلى من الأذان القديم -22 LUFS) · `res/font` (amiri_quran, ubuntu_arabic).
- **الأيقونات:** minSdk 26 → أيقونة تكيّفيّة `mipmap-anydpi-v26`. لا تكتب محلّل SVG يدويّاً (Vector/Material Icons).
- **الكاش:** مفتاح Room المركّب (dateIso+methodId+locKey) يُبطِل الكاش تلقائيّاً عند تغيّر الطريقة أو الموقع.
- **أداء التنقّل:** لا تضع قيمةً تتغيّر كلّ ثانية (ساعة/عدّاد) داخل كائن حالةٍ كبيرٍ تقرؤه بطاقاتٌ كثيرة، وإلّا أُعيد تركيبها جميعاً كلّ ثانية — افصلها في StateFlow خفيفٍ مستقلّ (`DashboardTick`). وثبّت أيّ `Brush`/شيدر بـ`remember` فلا يُعاد توليده أثناء انتقالات التنقّل.
- **صوت القرآن:** يُبثّ ويُخزَّن في كاش النظام (لا يُحزَّم). التظليل المتزامن يتطلّب قارئاً بترقيمٍ قياسيٍّ في everyayah (استُبعد عبد الباسط الورشيّ لعدم قياسيّته). البسملة ملفٌّ منفصلٌ (`001001.mp3`) تُشغَّل قبل الآية الأولى عدا الفاتحة والتوبة.
- **قرّاء التلاوة الكاملة:** لكلّ قارئٍ **رابط خادمٍ كامل** مختلف في mp3quran (`server{X}.mp3quran.net/…/`)، فالرابط = server + `SSS.mp3`. تُجلب القائمة كاملةً من `mp3quran.net/api/v3/reciters` وقتَ التشغيل (`QuranRepository.surahRecitersOnline`) مع سقوطٍ إلى `surahReciters` المُضمَّنة. لا تفترض خادماً واحداً للجميع. ولقارئٍ بأكثر من مصحف يُلحَق نوعه بالاسم (`moshafStyle`: مرتّل/مجوّد/معلّم، أو اسم المصحف كاملاً إن تكرّر النوع)، وإلّا ظهر الاسم مكرّراً. خوادم `serverN` تحوّل (301) إلى `cdn.mp3quran.net`، وOkHttp يتبع التحويل.
- **التنزيلات على نطاق التطبيق:** لا تُطلَق في `viewModelScope` (تُلغى بمغادرة القسم). `QuranDownloader.startSurah/startAyat/startMushaf` تعمل في نطاقه الخاصّ وتُبقيها `QuranDownloadService` (dataSync) بعد الخروج، والتقدّم في `progress` (مفتاح `surah:`/`ayat:`) و`finished`، فتقرؤه الشاشات عند العودة. الإلغاء بـ`runInterruptible` يقطع الملفّ الجاري ويحذف جزءه.
- **التنزيل دون إنترنت:** `data/QuranDownloader` يحفظ في `filesDir` بتقدّمٍ بالنسبة المئويّة (بثٌّ تدفّقيّ `downloadStreaming`): سور كاملة (`audio/{reciter}/SSS.mp3`) · صوت آيات النصّيّ (`audio_ayat/{reciter}/SSSAAA.mp3`، البسملة كآيةٍ 0) · صور المصحف حسب الرواية (`mushaf/` لحفص القديم، `mushaf_{riwaya}/` وإلّا). الشاشات/الخدمة تفحص المحلّيّ أوّلاً.
- **الحذف = تأجيلٌ لا فوريّ:** كلّ حذفٍ (سورة/صوت آيات/مصحف) **يُخفي أوّلاً** ويُظهر مهلة تراجع (Snackbar `Long` أو شريطٌ مضمَّن)، ولا يُحذف الملفّ فعليّاً إلّا بعد انقضائها — لا تحذف فوراً.
- **المصحف بالروايات (صور):** `Quran.pageImageUrl(page, riwaya)` — حفص من `SalehGNUTUX/Quran-PNG` (PPP.png)، ورش من `QuranHub/quran-pages-images` (`kfgqpc/warsh/{page}.jpg`، بلا بدائل). مبدّل الرواية في `MushafScreen` وفي مطويّة التنزيل.
- **نصّ القرآن بالروايات:** حفص من `tafsir.json` المُضمَّن؛ ورش وقالون والدوري من نصوص مجمّع الملك فهد المضمَّنة `assets/content/riwaya_{warsh,qaloon,doori}.json` (من `fawazahmed0/quran-api`، ترقيمٌ عثمانيّ يطابق حفصاً والصوت). **لا تعُد إلى alquran.cloud لها:** لا يملكها ويعيد نصّ حفصٍ بصمت (`quran-uthmani-quran-academy`)، فكان «ورش» حفصاً حتى 2.2. `ayatForRiwaya` يحذف ذاكرة `filesDir/riwaya_text` القديمة. اختيار الرواية في القارئ يختار قارئاً بنفس الرواية تلقائيّاً.
- **خطّ كلّ رواية** حقل `font` في `quran_meta.json` ← `quranFontFamily`/`quranTextStyle`: `Maghribi` (ورش وقالون) و`UthmanicHafs` (حفص، KFGQPC v2.2 لا يُعدَّل بشرط ترخيصه) و`AmiriQuran` (الدوري، يغطّي محارفه كلّها). الخطّ المغربيّ يرمّز العلامات في مواضع أخرى، فيحوّل `domain/QuranText.forFont` وقت العرض: ے←ى، ۥ←U+0658، ۦ←U+0659، ۪←U+06D9، ويحذف ۟. وعلامة الوقف ۖ تُرسَم «صه» صغيرةً مرفوعة (`appendQuran`) لأنّ رمز الخطّ لها يشبه الشدّة. جُرِّب كلّ بديلٍ برسم الخطّ؛ لا تضِف تحويلاً دون رسمه. الخطوط ثقيلة الوزن مصطنعة: لا `Bold` إلّا لأميري. التراخيص في `assets/content/fonts.LICENSE.txt`.
- **العرض المتّصل (`quranFlowText`، افتراضيّ):** `ui/screens/QuranFlowText.kt`. صفحةٌ لكلّ صفحة مصحفٍ مدنيّ (`pageStarts` في `quran_meta.json`، 604 بداية) نصّاً واحداً بتعليق `ayah` لكلّ آية: النقر يحدّد، والمطوّل يضع إشارة. يُعاد بناء الصفحة فقط إن تغيّر ما يخصّها. التمرير إلى الآية بسطرها داخل صفحتها (`pageLayouts`). البحث يعرض نتائجه بطاقاتٍ دائماً، والبسملة عنوانٌ مستقلّ تُحذف من أوّل الآية الأولى (`stripBismillah`).
- **قلب صور المصحف:** استعمل سِمة التطبيق الفعليّة (`colorScheme.surface.luminance() < 0.5`) **لا** `isSystemInDarkTheme()` — وإلّا بهت المصحف عند فرض وضعٍ مخالفٍ للنظام.
- **مشغّلٌ واحد:** التلاوة الجاريّة تُعرَض عبر `QuranMiniPlayer` العالميّ (فوق الشريط السفليّ) فقط — لا تُضِف شريطَ تشغيلٍ داخل الشاشات (يظهر مشغّلان). النقر على آيةٍ في القارئ **يحدّد** الموضع بلا تشغيل (tertiaryContainer)، والتشغيل من زرّ التشغيل يبدأ من المحدَّد. أيقونتا السابق/التالي **معكوستان** في RTL (السابق=SkipNext، التالي=SkipPrevious).
- **المتابعة تفاعليّة ومزدوجة:** موضعا القراءة والاستماع **منفصلان** (`lastRead*`/`lastListen*` في الإعدادات) وتفاعليّان (`combine(settings, surahs)`) — لا تحسبهما مرّةً في `init` وإلّا علِقا. المشغّل يشتقّ اسم السورة من الفهرس لا من `play.surahName` (يضيع في التتابع التلقائيّ).
- **مؤشّرات الرئيسيّة (LazyColumn):** ثوابت التمرير (`I_DHIKR=3`…) تربط الأزرارَ بفهارس عناصر ثابتة — **لا تُدرِج عنصراً علويّاً جديداً** (ولا عنصراً شرطيّاً) قبلها وإلّا انزاحت. البطاقات الجديدة (شريط التحديث، رمضان) تُوضَع **داخل** Column العنصر 0 لا كعنصرٍ مستقلّ.
- **إشعار صوت الأذان:** خدمة الأذان تنشر `adhanSoundNotification` على قناة `CH_ADHAN_SOUND` العالية، منبثقاً بزرّ الإيقاف وحاملاً الـfull-screen intent، وإشعار الوقت حينها صامت (`silent`) حتى لا يحجبه. ولا تستعمل خدمات الصوت الأخرى هذه القناة. وعند فشل بدء الخدمة يعود إشعار الوقت منبثقاً. نافذة الأذان تقرأ إعدادات المستخدم (الوضع والسمة والخلفيّة) لأنّها نشاطٌ مستقلّ.
- **إشعار full-screen intent:** نافذة القفل (`AdhanAlarmActivity`) تُطلَق عبر `setFullScreenIntent` على قناةٍ عالية الأولويّة فقط (لا القناة المنخفضة للخدمة)، بشرط `USE_FULL_SCREEN_INTENT`. للاختبار من داخل التطبيق (المقدّمة) أطلق النشاط مباشرةً (لا يُطلق النظام الـfull-screen intent والتطبيق مفتوح).
- **FileProvider:** سلطته `${applicationId}.fileprovider` (تختلف بالنكهة تلقائيّاً) + `res/xml/file_paths.xml` (`updates/` للتحديث، `backups/` cache للمشاركة). التثبيت يحتاج `REQUEST_INSTALL_PACKAGES` + سماح «مصادر مجهولة» (يُوجَّه إليه المستخدم). التحديث الذاتيّ يعمل ما دام نفس المفتاح؛ وأيّ تغييرٍ له يكسر التحديث للمثبَّت (حدث مرّةً عند v2.0 بضياع مفتاح debug القديم).
- **النسخ الاحتياطيّ (`BackupManager`):** حزمة ZIP انتقائيّة — تصدير/مشاركة يختار المُصدِّر محتواها، والاستيراد يُفحَص أولاً (`inspect`) ثمّ يختار المستورِد ما يستعيد. امنع الهروب من `filesDir` (رفض `../`). العمليّات على `Dispatchers.IO` مع **حوار انتظارٍ غير قابلٍ للإغلاق** (قد تطول مع الصوت/المصحف الكبير). المواقيت تُصدَّر JSON عبر `TimetableDao.all()`.
- **الإعدادات بشكلين (2.2):** بطاقاتٌ (`settingsCards`، افتراضيّ) أو مطويّات. لم تُعَد كتابة الأقسام: `SectionCard` يقرأ `LocalSettingsPage`، فلا يُرسَم في الشبكة، ويُرسَم مفتوحاً في صفحته. **كلّ `SectionCard` جديدٍ يحتاج مدخلاً في `SETTINGS_CATEGORIES` بمفتاحٍ يطابق عنوانه حرفيّاً** (أو `key =` إن تغيّر العنوان كـ«موثوقيّة التنبيهات ✓»)، وإلّا لم يظهر في شكل البطاقات. فتح قسمٍ من خارج الإعدادات بـ`UiEvents.requestSettingsPage` مع `setSettingsOpenSection` (الأوّل للبطاقات والثاني للمطويّات).
- **مواقيت الرئيسيّة (2.2):** صفٌّ عرضيٌّ (`homeTimesHorizontal`) أو قائمة، تُقلَب بلمسة. البطاقة تبقى العنصر 2 في القائمة بالشكلين، فلا تنزاح `I_DHIKR`…
- **حالة قابلة للتخزين في DataStore:** ما يجب تذكّره عبر الجلسات (آخر قسم إعداداتٍ مفتوح `settingsOpenSection`، الإشارات، الوِرد، إزاحة الهجريّ) حقولٌ في `AppSettings` تُقاد الواجهة منها مباشرةً (لا `remember` محلّيّ). الوِرد **مطفأٌ افتراضيّاً** (`enableWird=false`) وتذكيره في الإشعارات مستقلّ.
- **الموقع والعمل دون إنترنت:** **المواقيت تُحسَب محليّاً دائماً** (`PrayerCalculator`/adhan؛ سلسلة السقوط Room→API→محلّيّ)، فلا حاجة لقاعدة بيانات مواقيت — **العائق الوحيد offline هو تحديد الموقع**. حلوله (كلّها في `SetupScreen`): تثبيتٌ حيٌّ من GPS (`freshFix` في foss، إحداثيّات الأقمار دون إنترنت؛ اسم المدينة فقط يحتاج Nominatim) · قاعدة `places.json` المُضمَّنة · إدخال يدويّ · استيراد نسخة. `detectAndSaveLocation` يحفظ الإحداثيّات حتى لو فشل الترميز العكسيّ (city="").
- **خلفيّة الرئيسيّة:** الصور في `assets/backgrounds/{id}.webp` + `thumb/`، تُولَّد من `خلفيات/` بـ`tools/prepare_backgrounds.sh` (720×1600، امتدادٌ من شريط الحافّة فقط فلا تظهر أشباح). والقائمة في `domain/HomeBackgrounds`. **قاعدة المواقع** `places.json` بترخيص ODbL (`places.LICENSE.txt`): إحداثيّات المغرب المضافة من GeoNames والأسماء العربيّة من OpenStreetMap. أيّ إضافةٍ إليها تُنسب إلى مصدرها، ولا تُكتب إحداثيّةٌ من الذاكرة. صورة المستخدم (`CUSTOM_ID`) في `filesDir/backgrounds/` عبر `data/CustomBackgroundStore` (منتقي الصور بلا إذن)، ومعرّفها المعروض «custom:وقت التعديل» من `resolveBackgroundId` فيتجدّد الكاش، ويسقط إلى الافتراضيّ إن حُذفت. لا تُدرَج في النسخ الاحتياطيّ. تُرسَم في `AppRoot` خلف الـScaffold فتشمل شريط الحالة، وتظهر في الرئيسيّة وحدها. بطاقات الرئيسيّة تقرأ `LocalHomeBackdropOn` عبر `homeCardColor()`/`homeHeroAlpha()`. قيم الغشاوة والشفافيّة في `HomeBackground.kt` محسوبةٌ لتباينٍ لا يقلّ عن 6.7:1 للنصّ الأساسيّ؛ لا تخفّضها دون إعادة الحساب. **قبل النشر العامّ يلزم توثيق مصدر كلّ صورةٍ وترخيصها في `Credits.kt`.**
- **السمة الزجاجيّة:** `LocalGlassTheme` + مخطّطا ألوان في `Theme.kt` (تتجاهل الألوان الديناميكيّة). البطاقات تصير زجاجاً عبر `surfaceContainerHighest`/`surfaceVariant` الشفّافين، وباقي الحاويات (الحوارات والقوائم) معتمة عمداً. **لا ظلّ على عنصرٍ شفّاف** (يظهر من خلاله). `glassBlur` (0-100) يمزج النسخة المموَّهة فوق الحادّة بشفافيّةٍ بقدره، بلا تمويهٍ وقت التشغيل، ولا يؤثّر في حساب التباين لأنّه افترض أسوأ بكسل. في الرئيسيّة `Modifier.frostedGlass(shape)` يرسم من `assets/backgrounds/blur/{id}.webp` الجزءَ خلف العنصر (بلا تمويهٍ وقت التشغيل)، وفي غيرها `BlurredBackdrop`؛ ولا صورة في القارئ والمصحف.
- **اختصار الرئيسيّة:** `AppFeatures` مصدرٌ واحدٌ لأقسام «المزيد» وخيارات الاختصار (`homeShortcutRoute`، الافتراضيّ `radios`). `ShortcutPickerSheet` مشتركٌ بين الضغطة المطوّلة والإعدادات.
- **نافذة التعريف بالزجاج:** `ui/GlassIntro.kt` تُظهر بطاقةً بلا تعتيم مرّةً (`glassIntroShown`)، و`GlassPreview.override` يسبق `glassTheme` في `MainActivity` فيعاين المستخدم دون حفظ، ولا يُكتب إلّا بالتأكيد. تسبق الشرح الحركيّ.
- **الشرح الحركيّ:** `ui/tour/AppTour.kt`. أضف خطوةً في `TourSteps` واربط عنصرها بـ`Modifier.tourTarget(key)`، و`listItem` يمرّر الرئيسيّة إليه. يبدأ حين `tourShown == false` بعد رسائل «ما الجديد» والأذونات، ويحترم «تقليل الحركة».
- **تبويب الرئيسيّة بالرجوع لا بالتنقّل:** `popBackStack(dashboard, saveState = true)` لا `navigate(dashboard){popUpTo(saveState); restoreState}`: مع دخولٍ مباشر إلى قسمٍ (`nav.navigate(route)` من اختصارٍ/إشعار) يُحفَظ القسم تحت مفتاح الوجهة الأولى فيعيده `restoreState` بدلها. والقسم الداخل مباشرةً ليس تحته `more` في المكدّس.
- **ترتيب الإصدارات:** `domain/AppVersion` يقارن المراحل (alpha<beta<rc<المستقرّ) ويهمل لاحقة النكهة؛ لا تقارن الرقم وحده وإلّا فات من ثبّت ألفا صدور المستقرّة. الإصدارات الاختباريّة لا تأتي من `/releases/latest`، بل من قائمة الإصدارات عند تفعيل خيارها.
- **سجلّ المواقع السابقة:** يُكتب داخل `SettingsRepository.setLocation` نفسه في المعاملة ذاتها: الموقع **القديم** يُدفع إلى رأس `locationHistory` (5 حدّاً) ويُحذف منه الجديد، والمطابقة بـ`Places.same` (إحداثيّاتٌ بنحو 5 كم أوّلاً، لأنّ الاسم يتبدّل بالترجمة). لا تضِف إليه من مسارٍ آخر. الواجهة زرٌّ ونافذةٌ مصغّرة (`PreviousLocationsDialog`) تستبعد الموقع الحاليّ احتياطاً.
- **العفاسي مستبعَدٌ بطلب المستخدم:** أُزيل من `quran_meta.json` و`radios.json`، و`Quran.isExcludedReciter` يصفّي قائمة mp3quran الحيّة ومتابعة الاستماع المحفوظة. لا تُعِده في أيّ قسم (القارئ الافتراضيّ للنصّيّ والرقية الآن الحصري `Husary_128kbps`).
- **التحديث التلقائيّ للموقع محافظ:** `detectAndSaveLocation(auto = true)` في مسارَي الخلفيّة والإقلاع: لا موقع IP، ولا كتابة إن لم يتغيّر `locKey`، ولا `setMethod` إلّا بتغيّر البلد. وأيّ اختيارٍ يدويٍّ للموقع يطفئ `autoUpdateLocation`.
- **ألوان الودجت** في `values*/widget_colors.xml` وتتبع وضع النظام لا سِمة التطبيق (لا تعرف RemoteViews سِمته). حشوتها أكثف من بطاقات التطبيق إذ لا تمويه خلفها.
- **النسخ الاحتياطيّ يصدّر كلّ مفاتيح DataStore** (`exportJson`)، فكلّ حقل إعدادٍ جديدٍ مشمولٌ تلقائيّاً، ومنه موضعا القراءة والاستماع والإشارات.
- **خدمة الموقع المطفأة:** أيّ طلبٍ يدويٍّ لتحديد الموقع يمرّ عبر `ui/location/rememberLocationGate` (في `src/foss` نافذةٌ تنقل إلى صفحة النظام، وفي `src/full` نافذة النظام عبر `SettingsClient`). الرفض يلغي الإجراء، فلا يُستبدَل موقع IP بصمت. التحديث الخلفيّ لا يمرّ بها.
- **deep-link الإشعارات:** الإشعار يمرّر `MainActivity.EXTRA_ROUTE` (مثل `adhkar_session/morning`) بـ `PendingIntent` ذي **requestCode مميّز** (وإلّا أعاد النظام استعمال نيّةٍ قديمة)، و`AppRoot(deepLink)` ينتقل مرّةً عبر `LaunchedEffect` ثمّ يُمسَح.
- **صوت الرقية ≠ صوت القرآن:** `RuqyahAudioService` مشغّلُ **قائمةٍ** عابرةٍ للسور (يستقبل `IntArray` للسور والآيات + عناوين موازية) بخلاف `QuranAudioService` (سورةٌ واحدةٌ آية-بآية أو كاملة). لا تُعِد استعماله للقرآن ولا العكس. القارئ `Husary_128kbps` (everyayah)، والتظليل عبر `RuqyahPlayback.index` مقابل فهارس بداية المقاطع.
- **العرض البطاقيّ/الطوليّ بمفتاحين (منذ 2.2):** `adhkarSessionCards` (افتراضيّ **true**) للأذكار المعدودة: جلسات الصباح والمساء (`AdhkarSessionScreen`) وأبواب حصن المسلم (`HisnCategoryScreen`)، و`adhkarCardView` (افتراضيّ **false** فعلاً في `SettingsRepository`) لأقسام القراءة: المحتوى والتفسير والدروس والرقية. الزرّ `AdhkarViewToggleButton(session = true)` في الأولى. كان المفتاح واحداً، فاختيار القائمة في التفسير جعل الأذكار قوائم. النسخ/المشاركة عبر `ShareCopyRow` المشترك في العرضين. حالةٌ تخصّ الدرس كلَّه (مثل «شرح أكثر») تُرفَع **خارج** حلقة `HorizontalPager` وإلّا أُعيدت لكلّ بطاقة.
- **صور الدروس التعليميّة:** الصور المصدريّة AVIF **لا تُدعَم على minSdk 26** → حُوِّلت إلى **WebP** (`convert … -resize 600x600 -quality 78`) في `assets/learn/{section}/NN.webp`، وتُعرَض معرضاً عبر Coil بمسار `file:///android_asset/…`. عند إضافة أقسامٍ مصوَّرةٍ جديدة (الجنازة/العيدان… من زِب المستخدم) اتّبع النمط نفسه واضبط `imageDir`/`imageCount` في `purity_salah.json`. الصور مكتملةٌ بذاتها (فيها الخطوة والشرح) فلا تُضاف تسميةٌ فوقها.

---

## خريطة المراحل
- **م1 ✅ مكتملة:** مواقيت + جدولة أذان + إعداد + إعدادات + سِمة + خلفيّة متدرّجة.
  أذان مخصّص + تجربة الأنواع + بطاقة موثوقيّة التنبيهات + رئيسيّة متدرّجة (ساعة حيّة + تاريخان + الصلاة القادمة).
- **م2:** ✅ **قبلة** (بوصلة حيّة: `sensor/Compass.kt` + `QiblaScreen`/`QiblaViewModel`، تصحيح انحراف مغناطيسيّ، رمز كعبة) ·
  ✅ **أذكار** (`data/AzkarRepository` يقرأ azkar.txt + `AdhkarScreen`) ·
  ✅ **أذكار الصباح/المساء** (`domain/MorningEveningAdhkar` بعدد التكرار + `AdhkarSessionScreen` عدٌّ تنازليّ، مسار `adhkar_session/{type}`) ·
  ✅ **تسبيح** (`TasbihScreen`/`TasbihViewModel`) ·
  ✅ **محور «المزيد»** (`MoreScreen`، تبويبٌ خامس ببطاقات الأقسام) ·
  ✅ **أسماء الله الحسنى** (99 من `assets/content/asma.json` بمعانٍ وشواهد + `AsmaScreen` كبطاقات Pager سلايد) ·
  ✅ **الكاتم التلقائيّ** (`audio/RingerController` + جدولة استعادةٍ في المُستقبِل، يحتاج ACCESS_NOTIFICATION_POLICY) ·
  ✅ **محتوى مستورَد** (`data/ContentRepository` + `assets/content/*.json`): أحاديث (90) + أدعية (28) + حِكَم (32) + أسماء الله بالشواهد + **أحداث تاريخيّة** (events.json، 59 حدثاً + بحث + «حدث اليوم» عبر ICU) ·
  **المصادر:** `domain/Credits.kt` قائمةٌ صيانتها لازمةٌ **كلّما اعتمدنا مصدراً حرّاً** (تظهر في الإعدادات + الموقع). ·
  ✅ **حصن المسلم المصنّف** (`assets/content/hisn.json`، 132 باباً/267 ذكراً + `HisnScreen`/`HisnCategoryScreen` بعدٍّ تنازليّ؛ بديل القائمة المسطّحة azkar.txt التي بقيت لذكر اليوم فقط) ·
  ✅ **إغناء الرئيسيّة** (ذكر/حكمة اليوم بتجديد ونسخ + تاريخ هجريّ/ميلاديّ) + **خلفيّة متدرّجة** (`AppRoot`) ·
  ✅ **إشعار دائم** (القائمة المنسدلة، عدٌّ تنازليّ chronometer، `NotificationHelper.statusNotification`) ·
  ✅ **ودجت Glance** (`widget/`: `NextPrayerWidget` + `TodayTimesWidget` بخلفيّةٍ شفّافة + `WidgetData` عبر Hilt EntryPoint، تُحدَّث من `scheduleNext`).
  ✅ **بحث حصن المسلم** (تصفية عبر كلّ الأبواب) · ✅ **أكورديون الإعدادات** (قسمٌ واحدٌ مفتوح) ·
  ✅ **تقويم المواقيت** (`domain/GregorianMonths`: أشهر إقليميّة تلقائيّة/يدويّة + تاريخان لكلّ يوم + اختيار هجريّ/ميلاديّ + عودة تلقائيّة لصلوات اليوم). **م2 مكتملة.**
- **م3 (بدأت):** ✅ **التفسير الميسّر** (`tafsir.json` + `TafsirScreens`، 114 سورة/6236 آية عثمانيّ) ·
  ✅ **قسم القرآن الكريم** بثلاثة أقسام (`ui/screens/QuranScreens` + `MushafScreen`، بيانات `assets/content/quran_meta.json` مُولّدة من `../GT-QURANREADER`):
  ① **النصّيّ** (نصٌّ من tafsir.json + استماعٌ آية-بآية بتظليلٍ متزامنٍ وتمريرٍ تلقائيّ، everyayah) ·
  ② **المسموع** (سور كاملة، mp3quran، شريط تحكّم + تتابع تلقائيّ) ·
  ③ **المصوَّر** (604 صفحة **بالروايتين**: حفص `SalehGNUTUX/Quran-PNG` · ورش `QuranHub/quran-pages-images` عبر Coil، قلب ليليّ، عارضةٌ مضغوطة).
  المسموع هيكلته: قائمة قرّاء (`AudioRecitersScreen`) → صفحة قارئ (`ReciterSurahsScreen`, `quran_reciter/{id}`) + استعراض المُنزَّل عبر القرّاء (`DownloadedSurahsScreen`).
  الصوت عبر **`audio/QuranAudioService`** (خدمة مقدّمة mediaPlayback، تعمل الشاشة مغلقة) + **`QuranPlayback`** (حالة عالميّة StateFlow للتظليل) + **`QuranAudio`** (أوامر) + **`data/QuranRepository`/`QuranDownloader`** + **`domain/Quran`** (نماذج + بُناة روابط + `normalize`).
  الروايات: **ورش عن نافع** (المغرب، ومنهم يونس اسويلص `souilass`) · حفص · قالون · الدوري.
  ✅ **بحثٌ شاملٌ داخل الآيات** (`quran_read/{n}?ayah=`) · ✅ **متابعتان تفاعليّتان** (قراءة/استماع منفصلتان) · ✅ **اختيار الرواية في القارئ النصّيّ** (نصّ + تنزيل صوت آياته) · ✅ **تنزيلٌ بالنسبة المئويّة + حذفٌ بمهلة تراجع** · ✅ **زرّ تبديل السِمة** في الترويسات.
  ✅ **إشارات مرجعيّة** (`QuranBookmarks` + زرّ لكلّ آية، `bookmarks` في الإعدادات) · ✅ **وِرد التلاوة اليوميّ** (بطاقةٌ اختياريّة بهدفٍ وسلسلة أيّام، `WirdCard`). المتبقّي: قالون/الدوري بالصور (لا مصدر مؤكّد). **م3 مكتملة.**
- **م5:** ✅ **الإذاعات** (`RadiosScreen` + `RadioRepository` + `radios.json`، 36 إذاعة؛ `audio/{RadioService+WifiLock, RadioPlayback, RadioAudio}` + `RadioMiniPlayer` + `usesCleartextTraffic`) ·
  ✅ **رمضان** (`domain/Ramadan` + `RamadanCard` في الرئيسيّة بعدٍّ للإفطار/السحور + `ImsakiahScreen` جدولٌ شهريّ) · ✅ **آية اليوم + مشاركة** (بطاقات آية/ذكر/حكمة بزرّ `ACTION_SEND`). **م5 مكتملة.**
- **م4:** قرآن مترجَم + تفسير مترجَم + لغات.
- **بنية تحتيّة (v1.1–v1.3):** إشعار تحديثٍ داخليّ + تنزيل/تثبيت (`UpdateRepository`/`UpdateInstaller`/`UpdateBanner`) · نافذة أذانٍ ملء الشاشة فوق القفل (`AdhanAlarmActivity`) · نسخٌ احتياطيّ ومشاركةٌ انتقائيّة (`BackupManager`) · إزاحة التاريخ الهجريّ · زرّ 12/24 وحلقة تقدّمٍ في الرئيسيّة.
- **v1.4:** إيقاظ الشاشة للأذان + نافذة ملء الشاشة والتطبيق نشط + إبقاؤها بعد الصوت (`AdhanPlayback`/`keepAdhanScreen`) · إصلاح حلقة المتبقّي بين العشاء والفجر · تذكير أذكار الصباح/المساء · بطاقة تفعيل الإشعارات · تنزيل المصحف داخل قسمه · إرفاق رابط الموقع بالمشاركة. (البوصلة: خللٌ عتاديٌّ لا برمجيّ — انظر مزلق البوصلة.)
- **v1.5:** **إعدادٌ أوّليٌّ يعمل دون إنترنت** — تثبيتٌ حيٌّ من GPS (`PlatformLocationProvider.freshFix` في foss) + زرّ تفعيل خدمة الموقع + **قاعدة مواقع مُضمَّنة** (`assets/content/places.json` + `ContentRepository.places()` + `domain/Place`) باختيارٍ بحثيّ يقترح الطريقة + إدخال إحداثيّات يدويّ + استيراد نسخة + زرّ تخطّي (`SetupScreen`/`SetupViewModel`) · **إشعاراتٌ ذكيّة deep-link** (`MainActivity.EXTRA_ROUTE` + `AppRoot(deepLink)`) · **تسبيحٌ مختلط** (`TasbihUi.MIXED`) · **مشاركة/نسخ برابطي الهاتف وسطح المكتب** (`util/Share.kt` + `Credits.DESKTOP_WEBSITE`). المواقيت تُحسَب محليّاً (adhan) فلا حاجة لقاعدة بيانات مواقيت.
- **v1.6:** **عرضٌ بطاقيٌّ عامّ** لأقسام الأذكار/المحتوى (بأسلوب أسماء الله الحسنى، `HorizontalPager`) — مفتاحٌ واحدٌ عامّ `adhkarCardView` (افتراضيّ true) يتبدّل بزرّ `AdhkarViewToggleButton` بجانب زرّ السِمة المدمج في `SubScreenHeader` (لا تُضِف `ThemeToggleButton` ثانيةً)، يشمل: الأذكار/حصن/الأدعية/الأحاديث/الحِكَم/التفسير. الأذكار: انتقالٌ تلقائيٌّ عند إكمال العدّ + «تصفير» يعود للبطاقة 0 (حالة الـpager مرفوعة). · **بحثٌ داخل السورة** + **بحثٌ في الأحاديث**. · **مهلة قراءةٍ حسب طول الآية بسرعات** (`quranScrollSpeed`) + حفظ آخر رواية (`lastRiwaya`). · **ودجت الساعة وتقدّم الصلاة** (`PrayerProgressWidget`). · **`Quran.normalize` يحذف الألف** (وكلّ التشكيل وعلامات التلاوة والهمزة) فيطابق «جنات» «جَنّٰتٍ».
- **v1.7:** **ودجتات حيّة** — استبدال العناصر الزمنيّة بعناصر أندرويد أصليّة تنبض ذاتيّاً (`TextClock` + `Chronometer` تنازليّ) عبر `AndroidRemoteViews` **مع `GlanceModifier.fillMaxSize()`** (وإلّا انكمش المحتوى إذ يلتفّ حوله Glance). ودجت التقدّم بنمطين `classic`/`center` (`widgetProgressStyle`، الإعدادات←المظهر) بمُعرّفاتٍ مشتركة في `widget_progress_classic.xml`/`_center.xml`، والربط في `progressRemoteViews`. تاريخٌ ميلاديٌّ تحت الهجريّ. **اسم القارئ** يُحفَظ بجانب تنزيلاته (`QuranDownloader.saveReciterMeta/reciterMeta` → `audio/{id}/reciter.json`) فيظهر دون إنترنت (سلسلة: شبكة←مُضمَّن←قرص). `previewLayout` مضافٌ للثلاثة لكنّه لا يظهر على MIUI → **معاينة المُنتقي مؤجّلة لـv1.8** (الحلّ: `previewImage` PNG).
- **v1.8:** **مزامنة الودجت** — يقرأ المواقيت من `PrayerRepository.todayTimetable()` (نفس مصدر التطبيق: Room→API→محلّيّ) بمهلة `withTimeoutOrNull` وسقوطٍ محلّيّ، فتطابق الشاشة الرئيسيّة. **تقدُّمٌ تلقائيّ:** إنذارٌ مستقلٌّ عن الأذان (`PrayerAlarmReceiver.ACTION_REFRESH_WIDGETS` + `scheduler.scheduleWidgetRefresh`) يعيد رسم الودجت عند كلّ صلاة/منتصف ليل. **تحديثٌ فوريٌّ عند تبديل النمط:** `updateAllPrayerWidgets` يبثّ `ACTION_APPWIDGET_UPDATE` صراحةً للمُستقبِلات (لا يكفي `updateAll` الداخليّ على الأجهزة العنيدة) + `key(style)` حول `AndroidRemoteViews`. **نمطٌ ثالث `day`** (اسم اليوم كبيرٌ متوسّط + هجريٌّ بلا اسم اليوم، `widget_progress_day.xml`). تاريخٌ ميلاديٌّ بحرف «م». · **متابعة القرآن المسموع:** `QuranAudioService` يحفظ (سورة/قارئ/خادم/موضع بالميلّي) دوريّاً كلّ 8ث وعند pause/stop (`setLastAudio`)؛ بطاقة «متابعة الاستماع» في `AudioRecitersScreen` بحوارٍ (إتمام/من البداية)، والاستئناف عبر `QuranAudio.playSurah(..., positionMs)` → `EXTRA_POSITION_MS` → seek. · **تذكيرات السُّنن الهجريّة:** `domain/SunnahReminders` (اثنين/خميس، جمعة، عاشوراء، عرفة، عشر ذي الحجّة، ستّ شوّال…) عبر `DailyReminderReceiver` (يحسب الشهر/اليوم الهجريّ بـICU) + `enableSunnahReminder`. · **نجمة تفضيل «المزيد»:** `favoriteFeatures` (Set) ترفع البطاقات للأعلى بترتيبٍ أصليٍّ ثابت (`MoreViewModel`).
- **v1.9 (versionCode 28، صدر 2026-08-17):** إصلاحاتٌ + قسمان تعليميّان كبيران + أداتان تفاعليّتان + دليلٌ مصوَّر (صور WebP في `assets/learn/` معروضةٌ كمعرضٍ قابلٍ للتمرير عبر Coil) + تمريرٌ انسيابيٌّ في القرآن النصّيّ (`listState.scrollBy` في حلقةٍ بسرعة `quranScrollSpeed`) + زرّ إذاعةٍ في الرئيسيّة + وسومٌ في النسخ/المشاركة (`Share.HASHTAGS`) + إصلاح «إبقاء نافذة الأذان» (تبقى النافذة لكن تُترَك الشاشة تنطفئ — `AdhanAlarmActivity.allowScreenOff`).
  - **إصلاح الإشعارات العالقة/المتضاربة:** مهلة انتهاءٍ تلقائيّة (`setTimeoutAfter`) لإشعار الأذان والتنبيه المسبق · مسحُ العالق منها في `scheduleNext` (وخدمة الأذان إن `!AdhanPlayback.active`) · حارسٌ زمنيّ `MAX_SERVICE_MS` في `AdhanService`.
  - **قسم «تعلّم الطهارة والصلاة»** (مالكيّ، `LearnScreens` + `purity_salah.json`، 12 قسماً) و**«الرقية الشرعية»** (`RuqyahScreens` + `ruqyah.json` + `RuqyahAudioService`): بطاقتان في «المزيد» (`learn_hub`/`ruqyah_hub`). الرقية بوضعَي «أقرأ» (بطاقات/قائمة) و«أستمع» (قائمة تشغيلٍ آية-بآية everyayah=Alafasy). إذاعة الرقية (`roqiah`) في قسمَي الإذاعات والرقية.
  - **نظام العرض الموحّد:** أقسام الطهارة/الصلاة والرقية تستعمل توليفة **البطاقات/القائمة** عبر `adhkarCardView` (نفس `AdhkarViewToggleButton` العالميّ) + `ShareCopyRow` المشترك (من `AdhkarSessionScreen`) للنسخ/المشاركة في العرضين. حالة «شرح أكثر» **مشتركةٌ** بين خطوات الدرس (مرفوعةٌ خارج حلقة الـpager). أُضيف النسخ/المشاركة لبطاقات الأذكار/حصن أيضاً.
- **v1.10 (versionCode 29، صدر 2026-08-17):** دليلٌ مصوَّرٌ لبقيّة الصلوات — سجود السهو (23) والجنازة (14) والعيدين (25+25) والغائب (12) بصور WebP في `assets/learn/`. دعمُ **عدّة معارضَ معنونةٍ في القسم الواحد** عبر `LearnSection.galleries` (`LearnGalleryRef{title,dir,count}`) — الصلوات الخاصّة تعرض معارضَ الجنازة/العيدين/الغائب.
- **v1.11–v1.12:** تكبير صور الدروس والمصحف بملء الشاشة · دليلٌ مصوَّرٌ لتجهيز الميّت · مطويّة «مشاركة التطبيق» (رابط/APK).
- **v1.13–v1.14:** الأذكار المسموعة المضمَّنة (`AdhkarAudioService` + `AdhkarPlayerBar` العائم + قسم `AdhkarAudioScreen`) · تحديد الموقع يدويّاً في الإعدادات · مستوى صوت الأذان يحكم كلّ الأصوات · «ما الجديد» (`WhatsNewDialog`).
- **v1.15:** قسم «تجهيز الميّت» مستقلّ · الرقية مسجَّلةً بصوت سعد الغامدي · تهنئة إكمال الأذكار · أقرب مدينةٍ دون إنترنت · طريقة الخروج · فحص التحديثات كلّ 3 ساعات · توحيد الأرقام إلى 0-9.
- **v1.16:** تنزيل التحديث في خدمةٍ مقدّمةٍ باستئناف · تحديث الموقع تلقائيّاً + سجلّ آخر 5 مواقع · قاعدة نحو 317 مدينة · رسالة الأذونات بعد التثبيت.
- **v1.17–v1.17.2:** تسبيحٌ صوتيّ (`VoiceCounter`) ومختلطٌ بنوعين · زرّ تحديث الموقع · إصلاح تشوّه الساعة مع الاسم الطويل · اسم الموقع بلغته الأصليّة مع خيار ترجمته.
- **v1.18 (versionCode 39، صدر 2026-09-13):** بنية التوزيع: foss بمحدِّثٍ ذاتيّ وfull لبلاي بلا محدِّث · targetSdk 36 · تحديث الموقع في الخلفيّة · انتقالات تنقّلٍ تحترم «تقليل الحركة» (`NavAnim`) · شارات حالة الصلاحيّات · أذكار الصباح/المساء بـ`setAlarmClock` · إقرارات Play Console.
- **v2.0 (versionCode 45، صدر 2026-11-06؛ سبقته بيتا 1 إلى 4 بالأرقام 41 إلى 44):** شريطٌ علويٌّ ثابت في الرئيسيّة · إشعار صوتٍ منبثقٌ بزرّ الإيقاف · نافذة الأذان تتبع الخلفيّة · زرّ فحص التحديثات يدويّاً · مؤقّت الإذاعة · ودجتاتٌ زجاجيّة · خيار التحديثات الاختباريّة (`allowPrereleaseUpdates`) · السمة الزجاجيّة · اختصارٌ مخصَّص للرئيسيّة · شرحٌ مرئيّ · موثوقيّة إطلاق الأذان + منع الصوت خارج الموعد + `AlarmLog` · بوّابة خدمة الموقع · فتحٌ مباشرٌ من إشعارات الوِرد وآية اليوم وأذكار ما بعد الصلاة · خلفيّات مساجد للرئيسيّة (`HomeBackground.kt`) · أذانٌ بالصيغة المغربيّة (`AdhanType.MAGHRIBI_FULL/SHORT`).
- **v2.1 (versionCode 46، صدر 2026-11-07):** إزاحة الخادم بدل قاعدة مناطق الجهاز (عودة المغرب إلى GMT+0) + `migrateTimetableCache` · تنبيه المنطقة الزمنيّة · شدّة التغبيش (`glassBlur`) · نافذة التعريف بالزجاج (`GlassIntro`) · خلفيّةٌ من صور المستخدم (`CustomBackgroundStore`) · التنزيلات على نطاق التطبيق (`QuranDownloadService`) · إعادة اتّصال الإذاعة · 372 مدينةً مغربيّة (GeoNames + أسماء OSM، `places.json` بترخيص ODbL) · ترتيب «المزيد» · المصادر آخر الإعدادات.
- **v2.2 (versionCode 48، صدر 2026-11-11؛ سبقته v2.2-beta بالرقم 47):** مواقيت الرئيسيّة صفٌّ عرضيٌّ يُقلَب بلمسة (`homeTimesHorizontal`) · الإعدادات بطاقات (`settingsCards`، المطويّات خيار) · مفتاحٌ مستقلّ للأذكار المعدودة (`adhkarSessionCards`) · حلقة مؤقّت الإذاعة (`sleepTotal`) · «يوميّ» افتراضيّ للودجت · القارئ الطبلاوي وتمييز مصاحف القارئ الواحد (`moshafStyle`).
- **v2.3 (versionCode 50، صدر 2026-11-13؛ سبقته v2.3-beta بالرقم 49):** نصوص ورش وقالون والدوري الحقيقيّة مضمَّنة (`riwaya_*.json`) · الخطّ المغربيّ وخطّ المجمّع (`QuranText`/`quranTextStyle`) · القرآن النصّيّ متّصلٌ صفحةً صفحة (`quranFlowText`، `QuranFlowText.kt`) · تسجيلاتٌ جديدةٌ للأذكار · سجلّ المواقع السابقة في `setLocation` ونافذةٌ مصغّرة · إزالة العفاسي (`Quran.isExcludedReciter`) · إذاعة الطبلاوي.
- **الإصدار المستقرّ الحاليّ: v2.3** (versionCode 50). أوّل مستقرّ: v1.0 (versionCode 19).

## المصادر الحرّة المدروسة (للإلهام لا النسخ)
Five-Prayers (النموذج الهجين للجدولة، سلسلة السقوط، الكاتم، USAGE_ALARM، Nominatim بلا Google) ·
NoorUlHuda (الأذان كخدمة mediaPlayback + audio focus + wake، ودجت Chronometer) ·
altaqwaa (أفكار المحتوى وبنية JSON؛ عيبه polling في المقدّمة — نتجنّبه).
