import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.salehgnutux.gtsalat"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.salehgnutux.gtsalat"
        minSdk = 26
        targetSdk = 36
        versionCode = 48
        versionName = "2.2"
        vectorDrawables { useSupportLibrary = true }
    }

    // المفتاح الدائم للحزم الموزَّعة مباشرةً: خارج المستودع، فضياعه يكسر التحديث لكلّ المستخدمين (انظر signing-fingerprints.txt).
    // بدونه يقع البناء إلى مفتاح debug للجهاز، فلا يصلح الناتج للتوزيع.
    val permanentProps = file(System.getProperty("user.home") + "/.gt-salat-keys/signing.properties").takeIf { it.exists() }
        ?.let { Properties().apply { it.inputStream().use(::load) } }
    if (permanentProps == null) logger.warn("تنبيه: المفتاح الدائم غير موجود، والحزم ستوقَّع بمفتاح debug لهذا الجهاز ولا تصلح للتوزيع.")
    // مفتاح رفع نكهة بلاي: يُقرأ من keystore.properties (متجاهَلٌ في git) بشرط وجوده.
    // signingConfigs قبل productFlavors (وإلّا ردّت findByName عدماً بصمت).
    val ksProps = rootProject.file("keystore.properties").takeIf { it.exists() }
        ?.let { Properties().apply { it.inputStream().use(::load) } }
    signingConfigs {
        if (permanentProps != null) create("permanent") {
            storeFile = file(permanentProps.getProperty("storeFile"))
            storePassword = permanentProps.getProperty("storePassword")
            keyAlias = permanentProps.getProperty("keyAlias")
            keyPassword = permanentProps.getProperty("keyPassword")
        }
        if (ksProps != null) create("playUpload") {
            storeFile = file(ksProps.getProperty("storeFile"))
            storePassword = ksProps.getProperty("storePassword")
            keyAlias = ksProps.getProperty("keyAlias")
            keyPassword = ksProps.getProperty("keyPassword")
        }
    }

    // نكهتان: foss (حرّة بالكامل، بلا Google) و full (بخدمات Google للموقع الأدقّ)
    flavorDimensions += "edition"
    productFlavors {
        create("foss") {
            dimension = "edition"
            applicationIdSuffix = ".foss"
            versionNameSuffix = "-foss"
            signingConfigs.findByName("permanent")?.let { signingConfig = it }
            buildConfigField("boolean", "USES_GMS", "false")
            resValue("string", "app_edition", "حرّة")
        }
        create("full") {
            dimension = "edition"
            buildConfigField("boolean", "USES_GMS", "true")
            resValue("string", "app_edition", "كاملة")
            // نسخة غوغل بلاي: توقَّع بمفتاح الرفع إن وُجد keystore.properties.
            (signingConfigs.findByName("playUpload") ?: signingConfigs.findByName("permanent"))?.let { signingConfig = it }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
            // الحزم الموزَّعة تُبنى اليوم من نوع debug، فتوقَّع بالمفتاح الدائم لا بمفتاح الجهاز.
            signingConfigs.findByName("permanent")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore + WorkManager + Coroutines
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // الشبكة + التسلسل (Aladhan API + المحتوى المحلّيّ)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // حساب المواقيت والقبلة محلّيّاً (بلا إنترنت)
    implementation(libs.adhan)

    // تحميل صور صفحات المصحف (مع كاش قرص) — Coil
    implementation(libs.coil.compose)

    // ودجت سطح الهاتف (Jetpack Glance)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // خدمات موقع Google — للنكهة الكاملة فقط
    "fullImplementation"(libs.play.services.location)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
}
