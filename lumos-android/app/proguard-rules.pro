# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Suprimir warnings de anotações que só existem em tempo de compilação
-dontwarn org.intellij.lang.annotations.Language
-dontwarn org.jetbrains.annotations.NotNull
-dontwarn org.jetbrains.annotations.Nullable

##################################################
# 🔹 Regras gerais
##################################################
-keepattributes Signature
-keepattributes *Annotation*

##################################################
# 🔹 Retrofit + OkHttp
##################################################
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class javax.annotation.** { *; }

##################################################
# 🔹 Gson
##################################################
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }

# mantém campos anotados com @SerializedName
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# mantém suas models (requests/responses)
-keep class com.lumos.** { *; }
# Se houver DTOs/APIs sob o applicationId (com.thryon.lumos) além do namespace
-keep class com.thryon.lumos.** { *; }

##################################################
# 🔹 Moshi (caso use em algum lugar além do Gson)
##################################################
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.Json class * { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}

##################################################
# 🔹 Room (entities, daos, database)
##################################################
-keep class androidx.room.** { *; }
-keep class androidx.sqlite.db.** { *; }

# mantém suas entidades/daos
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Embedded class * { *; }
-keep @androidx.room.Relation class * { *; }
-keep @androidx.room.PrimaryKey class * { *; }

##################################################
# 🔹 WorkManager
##################################################
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
-keep class * extends androidx.work.ListenableWorker { *; }

##################################################
# 🔹 Coil (image loader)
##################################################
-dontwarn coil.**

##################################################
# 🔹 Firebase / Google Play Services
# já trazem consumer-proguard, só garantir que warnings não travem
##################################################
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# FCM (services referenciados só no AndroidManifest)
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService { *; }