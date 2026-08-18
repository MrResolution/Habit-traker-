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

# ── Room ──────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# ── Firebase / Firestore ──────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.example.data.FirestoreUser { *; }
-keep class com.example.data.LeaderboardEntry { *; }
-keep class com.example.data.CloudBackup { *; }
-keep class com.example.data.Habit { *; }
-keep class com.example.data.HabitLog { *; }
-keep class com.example.data.StreakMilestone { *; }
-keep class com.example.data.User { *; }
-keep class com.example.data.ScoreBreakdown { *; }
-keep class com.example.data.UserLevelInfo { *; }
-keepattributes *Annotation*
-keepattributes Signature

# ── Glance Widgets ────────────────────────────────────
-keep class com.example.widget.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver

# ── Moshi ─────────────────────────────────────────────
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}
-keep @com.squareup.moshi.JsonQualifier @interface *

# ── OkHttp / Retrofit ────────────────────────────────
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ── Kotlin / Coroutines ──────────────────────────────
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# ── FCM / Receivers ──────────────────────────────────
-keep class com.example.receiver.** { *; }

# ── Keep line numbers for crash reports ──────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
