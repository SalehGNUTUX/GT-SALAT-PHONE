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
./gradlew :app:compileFossDebugKotlin   # تحقّق سريع من الترجمة
```
الحزم في `app/build/outputs/apk/`. الـ SDK في `local.properties` (غير مُدرَج في git).
الأصول الثنائيّة (الخطوط/الأصوات/azkar) منقولة من `../GT-SALAT/resources` و`src/assets`.

---

## المعماريّة (الطبقات)

التدفّق: `ui (Compose+ViewModel)` → `data (Repository)` → `domain (حساب نقيّ)` / `local (Room)` / `remote (API)`.
والخلفيّة: `alarm` + `audio` + `notification` مستقلّة عن الواجهة، تُدار عبر Hilt.

```
app/src/main/java/io/github/salehgnutux/gtsalat/
├── domain/              ← نقيّة بلا Android (قابلة لإعادة الاستخدام لاحقاً)
│   ├── PrayerModels.kt      PrayerId, PrayerTime, DayTimetable, NextPrayer, AsrMadhab
│   ├── CalculationMethods.kt 22 طريقة + suggestByCountry + parametersOf (تحويل لـ adhan)
│   └── PrayerCalculator.kt   computeDay/Month + qiblaDirection + nextPrayer (adhan)
├── data/
│   ├── PrayerRepository.kt   ★ سلسلة السقوط: Room → API → حساب محلّيّ + prefetchMonths + detectAndSaveLocation
│   ├── local/               Room: TimetableEntity (مفتاح مركّب dateIso+methodId+locKey), TimetableDao, GtSalatDatabase
│   ├── remote/              AladhanApi (جدول شهريّ+هجريّ), GeoClients (NominatimGeocoder + IpLocationClient)
│   ├── location/            LocationModels (DetectedLocation + interface LocationProvider)
│   └── settings/            AppSettings (data class) + SettingsRepository (DataStore، Flow)
├── alarm/               ★ حلّ «الأذان في وقته والتطبيق مغلق»
│   ├── PrayerAlarmScheduler.kt  setExactAndAllowWhileIdle(RTC_WAKEUP) + fallback عند رفض الإذن. نمط ذاتيّ التسلسل.
│   ├── PrayerAlarmReceiver.kt   ACTION_ADHAN → أذان+إشعار+scheduleNext ; ACTION_PRENOTIFY → تنبيه اقتراب
│   ├── BootReceiver.kt          يعيد التسليح بعد BOOT_COMPLETED / MY_PACKAGE_REPLACED
│   ├── RescheduleWorker.kt      عامل دوريّ (6س): prefetch + scheduleNext (شبكة أمان)
│   └── WorkScheduler.kt         enqueueUniquePeriodicWork
├── audio/AdhanService.kt   خدمة مقدّمة (mediaPlayback): MediaPlayer بـ USAGE_ALARM + audio focus + wake + دعاء بعد الأذان + زرّ إيقاف
├── notification/NotificationHelper.kt  3 قنوات (adhan/prenotify/service). صوت الأذان عبر الخدمة لا القناة.
├── di/                  AppModule (OkHttp/Room/Dao) + LocationModule (@Binds حسب النكهة)
├── ui/
│   ├── RootViewModel + MainActivity + AppRoot (Scaffold + شريط سفليّ + بوّابة setupCompleted→Setup)
│   ├── theme/           Color (أخضر/ذهبيّ) + Type (Ubuntu Arabic + Amiri) + Theme (Material You + RTL)
│   └── screens/         Dashboard / Timetable / Settings / Setup (+ ViewModels)
├── util/Format.kt       clock/countdown/weekdayDate بأرقام لاتينيّة (0-9)
└── GtSalatApp.kt        @HiltAndroidApp + Configuration.Provider (HiltWorkerFactory) + ensureChannels + ensurePeriodic
```

### نمط الجدولة (الأهمّ — مستوحى من Five-Prayers + NoorUlHuda)
`scheduleNext()` يلغي القديم ويجدول إنذاراً دقيقاً **للصلاة القادمة فقط** (+ تنبيه اقتراب). عند إطلاق
`ACTION_ADHAN` يُعاد استدعاء `scheduleNext()` للتالية (self-rescheduling). إنذارات AlarmManager تُمحى عند
الإقلاع → `BootReceiver` يعيد التسليح. `RescheduleWorker` الدوريّ شبكة أمان + تحديث الكاش.
عند رفض `SCHEDULE_EXACT_ALARM` (Android 12+): سقوط إلى `setAndAllowWhileIdle` بدل الصمت.
إطلاق خدمة المقدّمة من مُستقبِل الإنذار الدقيق مسموح (استثناء الإنذار الدقيق من قيود FGS في الخلفيّة).

---

## نمط إضافة ميزة جديدة
1. نموذج نقيّ في `domain/`. 2. مصدر بيانات في `data/` (Room/remote/assets). 3. دالّة في `PrayerRepository` أو مستودع جديد.
4. ViewModel في `ui/screens/`. 5. شاشة Compose + إضافتها لـ`AppRoot` (وجهة/تبويب). 6. أذونات في المانيفست عند اللزوم.
عند إضافة حقل إعداد: أضِفه في `AppSettings` + مفتاح وقراءة/كتابة في `SettingsRepository`.

## مزالق مثبتة
- **RTL:** الجسم Rtl عبر `LocalLayoutDirection`؛ في `Row` استعمل أيقونات `Icons.AutoMirrored`.
- **الأرقام:** استعمل 0-9 اللاتينيّة في الواجهات (`Format` بـ `Locale.US`). التاريخ الهجريّ من API فقط.
- **الأصوات:** في `res/raw` بأسماء lowercase (adhan_full/adhan_short/dua_after_adhan/...).
- **الخطوط:** `res/font` بأسماء صالحة (amiri_quran, ubuntu_arabic).
- **الأيقونات:** minSdk 26 → أيقونة تكيّفيّة `mipmap-anydpi-v26` تكفي بلا PNG. لا تكتب محلّل SVG يدويّاً (استعمل Vector/Material Icons).
- **الكاش:** مفتاح Room المركّب (dateIso+methodId+locKey) يُبطِل الكاش تلقائيّاً عند تغيّر الطريقة أو الموقع.

---

## خريطة المراحل
- **م١ (الحاليّة):** مواقيت + جدولة أذان + إعداد + إعدادات + سِمة. ✅ مكتملة الهيكلة.
  أُضيف: أذان مخصّص + تجربة الأنواع + بطاقة موثوقيّة التنبيهات (إنذار دقيق + إعفاء بطاريّة).
- **م٢:** ✅ **قبلة** (بوصلة حيّة: `sensor/Compass.kt` + `QiblaScreen`/`QiblaViewModel`، تصحيح انحراف مغناطيسيّ) ·
  أذكار/حصن المسلم (azkar.txt في assets) + تسبيح + أسماء الله + كاتم تلقائيّ + ودجت (Glance).
- **م٣:** المصحف بالروايات (تنزيل) + القرّاء (تلاوة) + تظليل + ورد + إشارات.
- **م٤:** تفسير + قرآن مترجَم + تفسير مترجَم + لغات.
- **م٥:** إذاعات + رمضان + آية اليوم + مشاركة.

## المصادر الحرّة المدروسة (للإلهام لا النسخ)
Five-Prayers (النموذج الهجين للجدولة، سلسلة السقوط، الكاتم، USAGE_ALARM، Nominatim بلا Google) ·
NoorUlHuda (الأذان كخدمة mediaPlayback + audio focus + wake، ودجت Chronometer) ·
altaqwaa (أفكار المحتوى وبنية JSON؛ عيبه polling في المقدّمة — نتجنّبه).
