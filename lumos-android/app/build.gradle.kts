plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
//    id("kotlin-kapt")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.lumos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.thryon.lumos"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    flavorDimensions += listOf("env")

    productFlavors {
        create("dev") {
            dimension = "env"
            buildConfigField("String", "API_URL", "\"http://192.168.3.2:8080\"")
        }

        create("prod") {
            dimension = "env"
            buildConfigField("String", "API_URL", "\"https://spring.thryon.com.br\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            buildConfigField("boolean", "ROOM_LOGGING_ENABLED", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    configurations {
        create("cleanedAnnotations")
        implementation {
            exclude(group = "org.jetbrains", module = "annotations")
        }
    }

//    signingConfigs {
//        create("release") {
//            storeFile = file("/home/marcos/projects/lumos/lumos-android/key-store-path/key.jks")
//            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
//            keyAlias = System.getenv("KEY_ALIAS") ?: ""
//            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
//        }
//    }
//
//    buildTypes {
//        getByName("release") {
//            isMinifyEnabled = false //  ProGuard
//            signingConfig = signingConfigs.getByName("release")
//        }
//    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.location)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.runtime.livedata)
//    implementation(libs.androidx.security.crypto.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation.compose)

    // Retrofit para chamadas de API
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    // Interceptor para autenticação
    implementation(libs.logging.interceptor)
    // Shared Preferences seguras
    implementation(libs.androidx.security.crypto)

//    implementation(libs.androidx.room.common)
//    implementation(libs.androidx.room.runtime)
//    implementation(libs.androidx.room.compiler)

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    // If this project only uses Java source, use the Java annotationProcessor
    // No additional plugins are necessary
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation(libs.androidx.material.icons.extended)



    implementation("org.java-websocket:Java-WebSocket:1.5.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    implementation("com.squareup.moshi:moshi-kotlin:1.14.0") // Para converter JSON
    implementation("androidx.core:core-ktx:1.9.0") // Para notificações
    implementation("io.reactivex.rxjava2:rxjava:2.2.21") // RxJava 2
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1") // RxAndroid 2
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6") // Biblioteca STOMP

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))

    // Add the dependencies for the Firebase Cloud Messaging and Analytics libraries
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")

    implementation("androidx.compose.material3:material3:1.3.0")
}

