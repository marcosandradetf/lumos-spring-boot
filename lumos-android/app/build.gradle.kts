plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")

}

android {
    namespace = "com.lumos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.thryon.lumos"
        minSdk = 26
        targetSdk = 36
        versionCode = 31
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 1) Flavors controlam o appId
    flavorDimensions += listOf("env")
    productFlavors {
        create("dev") {
            dimension = "env"
            // instala lado a lado com o app da Play:
            applicationIdSuffix = ".dev"                 // -> com.thryon.lumos.dev
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Lumos (Dev)")
            buildConfigField("String", "API_URL", "\"http://192.168.3.2:8080\"")
        }
        create("prod") {
            dimension = "env"
            // sem sufixo: mesmo applicationId da produção
            resValue("string", "app_name", "Lumos")
            buildConfigField("String", "API_URL", "\"https://spring.thryon.com.br\"")
        }
    }

    // 2) Consolide buildTypes (um bloco só)
    signingConfigs {
        create("release") {
//            storeFile = file("C:/Users/marco/projects/lumos-keystore/com.thryon.lumos.jks")
//            storeFile = file("/Users/marcos/projects/lumos-keystore/com.thryon.lumos.jks")
//            storeFile = file("/home/marcosandrade/projects/lumos-keystore/com.thryon.lumos.jks")
            storeFile = file("/home/marcos/projects/lumos-keystore/com.thryon.lumos.jks")
            storePassword = "4dejulho_"
            keyAlias = "key0"
            keyPassword = "4dejulho_"
        }
        // debug: use o default (debug.keystore) — não precisa declarar
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
//            buildConfigField("boolean", "ROOM_LOGGING_ENABLED", "true")
//            isMinifyEnabled = true
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
        }

        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }

    buildFeatures { compose = true; buildConfig = true }

    configurations {
        create("cleanedAnnotations")
        implementation { exclude(group = "org.jetbrains", module = "annotations") }
    }
}


dependencies {
    // Core
    implementation(libs.androidx.core.ktx)

    // Lifecycle & Compose
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.runtime.livedata)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.compose)

    // Location
    implementation(libs.play.services.location)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Segurança
    implementation(libs.androidx.security.crypto)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Imagens
    implementation(libs.coil.compose)

    // Retrofit / Gson
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Logging Interceptor
    implementation(libs.logging.interceptor)


    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Moshi
    implementation(libs.moshi.kotlin)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation("com.google.firebase:firebase-crashlytics-ndk")

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    implementation("com.auth0.android:jwtdecode:2.0.2")

}
