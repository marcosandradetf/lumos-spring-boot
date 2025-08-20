# =========================================
# ProGuard / R8 Lumos OP
# =========================================

# Mantém assinaturas e anotações
-keepattributes Signature
-keepattributes Annotation

# Suprimir warnings inúteis
-dontwarn org.intellij.lang.annotations.Language
-dontwarn org.jetbrains.annotations.NotNull
-dontwarn org.jetbrains.annotations.Nullable

##################################################
# 🔹 Kotlin
##################################################
-keep class kotlin.Metadata { *; }
-keep class kotlin.jvm.internal.** { *; }
-keepclassmembers class * { @androidx.compose.runtime.Composable <methods>; }

##################################################
# 🔹 Room (Database, Entities, DAOs, TypeConverters)
##################################################
-keep class androidx.room.** { *; }
-keep class androidx.sqlite.db.** { *; }

-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Embedded class * { *; }
-keep @androidx.room.Relation class * { *; }
-keep @androidx.room.PrimaryKey class * { *; }
-keep @androidx.room.Dao class * { *; }

-keepclassmembers class * extends androidx.room.RoomDatabase { <fields>; <methods>; }
-keepclassmembers class * extends androidx.room.Dao { <methods>; }
-keepclassmembers class * { @androidx.room.TypeConverter <methods>; }

##################################################
# 🔹 Parcelable / Serializable
##################################################
-keepclassmembers class * implements android.os.Parcelable { static ** CREATOR; }
-keepclassmembers class * implements java.io.Serializable { *; }

##################################################
# 🔹 Gson
##################################################
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keepclassmembers class * { @com.google.gson.annotations.SerializedName <fields>; }

##################################################
# 🔹 Moshi
##################################################
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.Json class * { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * { @com.squareup.moshi.* <fields>; }

##################################################
# 🔹 WorkManager
##################################################
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

-dontwarn androidx.work.**

##################################################
# 🔹 Retrofit / OkHttp / Okio
##################################################
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class javax.annotation.** { *; }

##################################################
# 🔹 Firebase / Google Play Services
##################################################
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService { *; }

##################################################
# 🔹 Coil (Image Loading)
##################################################
-dontwarn coil.**

##################################################
# 🔹 Navigation (Safe Args / Compose)
##################################################
-keep class androidx.navigation.** { *; }
-keep class * implements androidx.navigation.NavArgs { *; }

##################################################
# 🔹 Outros
##################################################
-keep class com.lumos.** { *; } # mantém TUDO