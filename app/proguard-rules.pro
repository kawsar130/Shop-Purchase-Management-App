# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Attributes for stack traces and annotations
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# Firebase Crashlytics
-keep public class * extends java.lang.Exception

# Data & Domain Models (prevent Firestore & serialization reflection breaking)
-keep class com.shopkeeper.data.model.** { *; }
-keepclassmembers class com.shopkeeper.data.model.** {
    <fields>;
    <methods>;
    <init>(...);
}

# Room Database & Local Cache
-keep class com.shopkeeper.data.local.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Dagger Hilt & WorkManager Worker Injection
-keep class androidx.hilt.work.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclasseswithmembers class * {
    @androidx.hilt.work.HiltWorker <init>(...);
}

# Credential Manager & Google ID Option
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }

# Strip verbose and debug logging in production release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
