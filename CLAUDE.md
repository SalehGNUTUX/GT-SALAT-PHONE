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
مكتبة الحساب `com.batoulapps.adhan:adhan:1.2.1` · **compileSdk 36 · targetSdk 35 · minSdk 26**.

### نكهتان (product flavors، بُعد `edition`)
- **`foss`** (applicationIdSuffix `.foss`): بلا Google — الموقع عبر `LocationManager` + Nominatim. لـF-Droid.
- **`full`**: خدمات Google (`play-services-location` → Fused Location).
- التجريد: واجهة `LocationProvider` في `main`، وتطبيقان بنفس الاسم المؤهّل `PlatformLocationProvider`
  في `src/foss` و`src/full`، يربطهما `di/LocationModule` (@Binds).

---

## الأوامر

```bash
./gradlew assembleFossDebug        # APK النكهة الحرّة
./gradlew assembleFullDebug        # APK نكهة Google
./gradlew :app:compileFossDebugKotlin   # تحقّق سريع من الترجمة (استعمله بعد كلّ دفعة)
```
الحزم في `app/build/outputs/apk/`. الـ SDK في `local.properties` (غير مُدرَج في git).
لا اختبارات وحدة في المشروع؛ التحقّق عبر `compileFossDebugKotlin`. البناء الأوّل قد يبطؤ (تنزيل تبعيّات) — مهلة ≥ 9 دقائق.
الأصول الثنائيّة (الخطوط/الأصوات/azkar) منقولة من `../GT-SALAT/resources` و`src/assets`.
**نمط العمل:** بعد كلّ دفعة → `compileFossDebugKotlin` (تحقّق). التحزيم عند طلب المستخدم «حزّم» فقط.

### طقس إصدارٍ تجريبيّ (يتكرّر كثيراً — نفّذه بالترتيب عند «حزّم/أصدر»)
1. رفع `versionCode` (+1) و`versionName` في `app/build.gradle.kts`.
2. إضافة مدخلٍ أعلى `CHANGELOG.md` (عربيّ)، وتحديث رقم النسخة في `README.md` و`docs/index.html` (سطر `.ver` + روابط تنزيل الـAPK المباشرة).
3. `./gradlew assembleFossDebug` ثمّ نسخ الناتج إلى الجذر باسم `GT-SALAT-vX.Y.Z-beta-foss.apk` (الـAPK مُتجاهَلٌ في git؛ يُرفَع كأصل إصدار فقط). احذف APK الإصدار السابق من الجذر.
4. commit عربيّ + `git tag vX.Y.Z-beta` + `git push origin main --tags`.
5. `gh release create vX.Y.Z-beta "GT-SALAT-vX.Y.Z-beta-foss.apk" --prerelease --title … --notes …` (عربيّ).
- المستودع البعيد قد يتقدّم برفعٍ من واجهة GitHub → عند رفض الدفع: `git pull --rebase origin main` ثمّ أعد الدفع.
**محتوى `assets/content/*.json`** مُولَّد مرّةً بسكربت Node: أحاديث/أدعية/حِكَم/أسماء من `../GT-SQRM/GT-SIRM/GT-SIRM-WEB/*-data.js`،
حصن المسلم من `../GT_HISNMUSLIM-main/assets/data/`، والتفسير الميسّر `tafsir.json` (~4MB، 6236 آية) نُزّل ودُمج من `api.alquran.cloud` (ar.muyassar + quran-uthmani).

---

## المعماريّة (الطبقات)

التدفّق: `ui (Compose+ViewModel)` → `data (Repository)` → `domain (حساب نقيّ)` / `local (Room)` / `remote (API)`.
والخلفيّة: `alarm` + `audio` + `notification` مستقلّة عن الواجهة، تُدار عبر Hilt.

```
app/src/main/java/io/github/salehgnutux/gtsalat/
├── domain/              ← نقيّة بلا Android (قابلة لإعادة الاستخدام لاحقاً)
│   ├── PrayerModels.kt · CalculationMethods.kt (22 طريقة) · PrayerCalculator.kt (computeDay/Month + qibla + nextPrayer)
│   ├── MorningEveningAdhkar.kt (أذكار الصباح/المساء بعدد التكرار) — [أسماء الله انتقلت إلى assets/content/asma.json]
│   ├── IslamicContent.kt   نماذج @Serializable للمحتوى (أحاديث/أدعية/حِكَم/أسماء/حصن/تفسير)
│   ├── Quran.kt            نماذج القرآن + بُناة روابط (everyayah/mp3quran/صور) + `normalize` (تطبيع عربيّ للبحث) + AyahHit
│   ├── Credits.kt          ★ قائمة المصادر الحرّة — تُحدَّث كلّما اعتمدنا مصدراً (تظهر في الإعدادات + الموقع)
│   └── GregorianMonths.kt  أسماء الأشهر الإقليميّة (MAGHREB/LEVANT/STANDARD) + MonthScheme + CalendarKind
├── data/
│   ├── PrayerRepository.kt   ★ سلسلة السقوط: Room → API → حساب محلّيّ + prefetchMonths + detectAndSaveLocation
│   ├── ContentRepository.kt  يقرأ assets/content/*.json (أحاديث/أدعية/حِكَم/أسماء/حصن/تفسير) + حكمة يوميّة
│   ├── QuranRepository.kt     quran_meta.json + نصّ من tafsir + بُناة روابط + searchAyat (بحثٌ شاملٌ عبر 6236 آية) + surahRecitersOnline
│   ├── QuranDownloader.kt     تنزيل المصحف/السور إلى filesDir للعمل دون إنترنت (StateFlow للتقدّم)
│   ├── AzkarRepository.kt     يقرأ azkar.txt (لذكر اليوم في الرئيسيّة)
│   ├── local/ remote/ location/ settings/   (Room · AladhanApi+GeoClients · LocationProvider · AppSettings+SettingsRepository)
├── alarm/               ★ حلّ «الأذان في وقته والتطبيق مغلق»
│   ├── PrayerAlarmScheduler.kt  ★ الأذان بـ setAlarmClock (لا يؤجّله Doze) + scheduleTest + refreshStatus (إشعار دائم) + refreshWidgets
│   ├── PrayerAlarmReceiver.kt   ACTION_ADHAN/PRENOTIFY/RESTORE_SOUND/POST_DHIKR/TEST (كتم تلقائيّ)
│   ├── BootReceiver.kt (BOOT/MY_PACKAGE_REPLACED/TIME_SET/TIMEZONE_CHANGED) · RescheduleWorker (6س) · WorkScheduler
├── audio/       AdhanService.kt (mediaPlayback أذان) · QuranAudioService.kt (تلاوة القرآن، آية-بآية/سورة كاملة) + QuranPlayback (حالة StateFlow) + QuranAudio (أوامر) · AdhanPreviewer · RingerController (كتم)
├── sensor/Compass.kt       بوصلة (TYPE_ROTATION_VECTOR) كـFlow — للقبلة
├── notification/NotificationHelper.kt  ★ 5 قنوات (adhan/prenotify/service/status/reminders). صوت الأذان عبر الخدمة لا القناة.
├── widget/      NextPrayerWidget + TodayTimesWidget (Glance، خلفيّة شفّافة) + WidgetData (Hilt EntryPoint، حساب محلّيّ)
├── di/          AppModule + LocationModule (@Binds حسب النكهة)
├── ui/
│   ├── MainActivity + RootViewModel + AppRoot (Box تدرّج + Scaffold + شريط سفليّ 5 تبويبات + المزيد nested-graph + بوّابة Setup)
│   ├── theme/    Color (أخضر/ذهبيّ) + Type (Ubuntu Arabic + Amiri) + Theme (Material You + RTL)
│   └── screens/  Dashboard · Timetable · Settings · Setup · Qibla · Tasbih · More · Events(بحث) · ThemeToggle · DownloadsSettings ·
│                 Asma (Pager) · Hisn(بحث) · AdhkarSession · Content(حديث/أدعية/حِكَم) · Tafsir ·
│                 QuranScreens (Hub + SurahIndex + TextReader + AudioRecitation + QuranMiniPlayer) · MushafScreen (+ViewModels)
├── util/Format.kt   clock/countdown/clockNow/gregorianArabic/monthYear (أرقام مغربيّة 0-9 + أشهر إقليميّة)
└── GtSalatApp.kt    @HiltAndroidApp + Configuration.Provider + ensureChannels/ensurePeriodic + scheduleNext عند الإقلاع
```

### نمط الجدولة (الأهمّ — مستوحى من Five-Prayers + NoorUlHuda)
الأذان والتنبيه المسبق يُجدولان بـ **`setAlarmClock`** (يُعامَل كإنذار منبّه، **لا يؤجّله Doze/توفير البطاريّة** ولا يحتاج إذن `SCHEDULE_EXACT_ALARM`)؛ باقي الإنذارات (استعادة الرنين/ذكر بعد الصلاة/التذكيرات) بـ `setExactAndAllowWhileIdle` مع سقوطٍ إلى `setAndAllowWhileIdle` عند رفض الإذن.
`scheduleNext()` يلغي القديم ويُسلّح **الصلوات الـ٨ القادمة مقدّماً** (`repo.upcomingPrayers` + مفاتيح مُفهرَسة `RC_ADHAN_BASE+i`/`RC_PRENOTIFY_BASE+i`) كشبكة أمانٍ إن جُمّد التطبيق فلم يُعِد الجدولة الذاتيّة. عند إطلاق `ACTION_ADHAN` يُعاد استدعاء `scheduleNext()` (self-rescheduling، يُعيد التسليح من الآن). إنذارات AlarmManager تُمحى عند الإقلاع → `BootReceiver` يعيد التسليح؛ `RescheduleWorker` الدوريّ (6س) شبكة أمان + تحديث الكاش. إطلاق خدمة المقدّمة من مُستقبِل الإنذار مسموح (استثناء الإنذار من قيود FGS في الخلفيّة).
**تأخّر الإشعارات حتى فتح الشاشة** ليس خللاً في الكود بل **إدارة بطاريّة عدوانيّة** (Xiaomi/Oppo…) تجمّد التطبيق؛ العلاج من المستخدم: إعفاء البطاريّة + التشغيل التلقائيّ (بطاقة الموثوقيّة). زرّ «اختبار التنبيه» (`scheduleTest`) يشخّص ذلك.

---

## نمط إضافة ميزة جديدة
1. نموذج نقيّ في `domain/`. 2. مصدر بيانات في `data/` (Room/remote/assets). 3. دالّة في `PrayerRepository` أو مستودع جديد.
4. ViewModel في `ui/screens/`. 5. شاشة Compose + إضافتها لـ`AppRoot` (وجهة/تبويب). 6. أذونات في المانيفست عند اللزوم.
عند إضافة حقل إعداد: أضِفه في `AppSettings` + مفتاح وقراءة/كتابة في `SettingsRepository`.

## مزالق مثبتة
- **RTL:** الجسم Rtl عبر `LocalLayoutDirection`؛ في `Row` استعمل أيقونات `Icons.AutoMirrored`.
- **الأرقام:** استعمل **الأرقام المغربيّة 0-9 حصراً** في كامل المشروع (واجهة + توثيق + commits)، لا المشرقيّة ١-٩. (`Format` بـ`Locale.US`.) التاريخ الهجريّ من API فقط.
- **الوضع الداكن + التدرّج:** `Scaffold` بخلفيّة `Color.Transparent` (لإظهار التدرّج) **يجب** أن يُمرَّر له `contentColor = onBackground` صراحةً، وإلّا سقط لون النصوص/الأيقونات الافتراضيّ إلى الأسود. أيّ شاشةٍ خارج الـScaffold تُلَفّ بـ`Surface`.
- **الودجت (Glance):** احسب البيانات **محليّاً فوراً** (`PrayerCalculator`) داخل `runCatching` — لا تعتمد كاش/شبكة وإلّا فرغ الودجت لحظيّاً؛ واستعمل `SizeMode.Responsive` فلا يُقصّ المحتوى عند تغيير الحجم.
- **الأصول الكبيرة:** فكّ ترميز JSON الكبير (tafsir ~4MB) على `Dispatchers.Default` لا الخيط الرئيسيّ.
- **تنقّل «المزيد»:** رسمٌ متداخل (`navigation(route=more_graph)`) بـ`saveState`/`restoreState` ليحفظ/يستعيد القسم؛ إعادة النقر على «المزيد» داخل قسمٍ فرعيّ تُظهر رسالةً (العودة/البقاء).
- **الإشعار الدائم:** على أندرويد 14+ يُزال بالسحب (قرار النظام)؛ يُعاد بثّه من `scheduleNext` وعند إقلاع التطبيق.
- **الأصوات/الخطوط:** `res/raw` lowercase (adhan_full…) · `res/font` (amiri_quran, ubuntu_arabic).
- **الأيقونات:** minSdk 26 → أيقونة تكيّفيّة `mipmap-anydpi-v26`. لا تكتب محلّل SVG يدويّاً (Vector/Material Icons).
- **الكاش:** مفتاح Room المركّب (dateIso+methodId+locKey) يُبطِل الكاش تلقائيّاً عند تغيّر الطريقة أو الموقع.
- **أداء التنقّل:** لا تضع قيمةً تتغيّر كلّ ثانية (ساعة/عدّاد) داخل كائن حالةٍ كبيرٍ تقرؤه بطاقاتٌ كثيرة، وإلّا أُعيد تركيبها جميعاً كلّ ثانية — افصلها في StateFlow خفيفٍ مستقلّ (`DashboardTick`). وثبّت أيّ `Brush`/شيدر بـ`remember` فلا يُعاد توليده أثناء انتقالات التنقّل.
- **صوت القرآن:** يُبثّ ويُخزَّن في كاش النظام (لا يُحزَّم). التظليل المتزامن يتطلّب قارئاً بترقيمٍ قياسيٍّ في everyayah (استُبعد عبد الباسط الورشيّ لعدم قياسيّته). البسملة ملفٌّ منفصلٌ (`001001.mp3`) تُشغَّل قبل الآية الأولى عدا الفاتحة والتوبة.
- **قرّاء التلاوة الكاملة:** لكلّ قارئٍ **رابط خادمٍ كامل** مختلف في mp3quran (`server{X}.mp3quran.net/…/`)، فالرابط = server + `SSS.mp3`. تُجلب القائمة كاملةً من `mp3quran.net/api/v3/reciters` وقتَ التشغيل (`QuranRepository.surahRecitersOnline`) مع سقوطٍ إلى `surahReciters` المُضمَّنة. لا تفترض خادماً واحداً للجميع.
- **التنزيل دون إنترنت:** `data/QuranDownloader` يحفظ في `filesDir` بتقدّمٍ بالنسبة المئويّة (بثٌّ تدفّقيّ `downloadStreaming`): سور كاملة (`audio/{reciter}/SSS.mp3`) · صوت آيات النصّيّ (`audio_ayat/{reciter}/SSSAAA.mp3`، البسملة كآيةٍ 0) · صور المصحف حسب الرواية (`mushaf/` لحفص القديم، `mushaf_{riwaya}/` وإلّا). الشاشات/الخدمة تفحص المحلّيّ أوّلاً.
- **الحذف = تأجيلٌ لا فوريّ:** كلّ حذفٍ (سورة/صوت آيات/مصحف) **يُخفي أوّلاً** ويُظهر مهلة تراجع (Snackbar `Long` أو شريطٌ مضمَّن)، ولا يُحذف الملفّ فعليّاً إلّا بعد انقضائها — لا تحذف فوراً.
- **المصحف بالروايات (صور):** `Quran.pageImageUrl(page, riwaya)` — حفص من `SalehGNUTUX/Quran-PNG` (PPP.png)، ورش من `QuranHub/quran-pages-images` (`kfgqpc/warsh/{page}.jpg`، بلا بدائل). مبدّل الرواية في `MushafScreen` وفي مطويّة التنزيل.
- **نصّ القرآن بالروايات:** حفص من `tafsir.json` المُضمَّن (فوريّ)؛ غيره من `api.alquran.cloud/v1/surah/{n}/{apiSlug}` (`QuranRepository.ayatForRiwaya`) يُخزَّن في `filesDir/riwaya_text` ليعمل دون إنترنت. اختيار الرواية في القارئ يختار قارئاً بنفس الرواية تلقائيّاً.
- **قلب صور المصحف:** استعمل سِمة التطبيق الفعليّة (`colorScheme.surface.luminance() < 0.5`) **لا** `isSystemInDarkTheme()` — وإلّا بهت المصحف عند فرض وضعٍ مخالفٍ للنظام.
- **مشغّلٌ واحد:** التلاوة الجاريّة تُعرَض عبر `QuranMiniPlayer` العالميّ (فوق الشريط السفليّ) فقط — لا تُضِف شريطَ تشغيلٍ داخل الشاشات (يظهر مشغّلان). النقر على آيةٍ في القارئ **يحدّد** الموضع بلا تشغيل (tertiaryContainer)، والتشغيل من زرّ التشغيل يبدأ من المحدَّد. أيقونتا السابق/التالي **معكوستان** في RTL (السابق=SkipNext، التالي=SkipPrevious).
- **المتابعة تفاعليّة ومزدوجة:** موضعا القراءة والاستماع **منفصلان** (`lastRead*`/`lastListen*` في الإعدادات) وتفاعليّان (`combine(settings, surahs)`) — لا تحسبهما مرّةً في `init` وإلّا علِقا. المشغّل يشتقّ اسم السورة من الفهرس لا من `play.surahName` (يضيع في التتابع التلقائيّ).

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
  المتبقّي: وِرد + إشارات مرجعيّة + قالون/الدوري بالصور (لا مصدر مؤكّد). **م3 مكتملة.**
- **م5 (بدأت):** ✅ **الإذاعات** (`ui/screens/RadiosScreen` + `data/RadioRepository` + `assets/content/radios.json` من `../GT_QURANRADIO`، 36 إذاعة): بثٌّ حيٌّ في الخلفيّة عبر `audio/{RadioService (خدمة مقدّمة + WifiLock), RadioPlayback, RadioAudio}` · تعديل رابط · مخصّصة · مفضّلة (تصعد للرأس) · إعادة (فرديّ/شامل) · حذفٌ بتراجع · مشغّلٌ مصغّرٌ عالميّ (`RadioMiniPlayer`) + `usesCleartextTraffic` لروابط HTTP. المتبقّي: رمضان · آية اليوم · مشاركة.
- **م4:** قرآن مترجَم + تفسير مترجَم + لغات.
- **الإصدار المستقرّ الأوّل: v1.0** (versionCode 19).

## المصادر الحرّة المدروسة (للإلهام لا النسخ)
Five-Prayers (النموذج الهجين للجدولة، سلسلة السقوط، الكاتم، USAGE_ALARM، Nominatim بلا Google) ·
NoorUlHuda (الأذان كخدمة mediaPlayback + audio focus + wake، ودجت Chronometer) ·
altaqwaa (أفكار المحتوى وبنية JSON؛ عيبه polling في المقدّمة — نتجنّبه).
