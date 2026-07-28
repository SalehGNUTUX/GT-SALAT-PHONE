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
        targetSdk = 35
        versionCode = 12
        versionName = "0.9.3-beta"
        vectorDrawables { useSupportLibrary = true }
    }

    // نكهتان: foss (حرّة بالكامل، بلا Google) و full (بخدمات Google للموقع الأدقّ)
    flavorDimensions += "edition"
    productFlavors {
        create("foss") {
            dimension = "edition"
            applicationIdSuffix = ".foss"
            versionNameSuffix = "-foss"
            buildConfigField("boolean", "USES_GMS", "false")
            resValue("string", "app_edition", "حرّة")
        }
        create("full") {
            dimension = "edition"
            buildConfigField("boolean", "USES_GMS", "true")
            resValue("string", "app_edition", "كاملة")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
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
