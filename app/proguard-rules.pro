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
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# JetPack Navigation
-keepnames class androidx.navigation.fragment.NavHostFragment

# Serializables in the app
-keep public class * extends java.io.Serializable

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepclasseswithmembers class * {
    public <init>(android.os.Parcel);
}

# Enums in the app
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Android Logging
-assumenosideeffects class android.util.Log {
    native boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int e(...);
    public static int w(...);
    public static int wtf(...);
    public static java.lang.String getStackTraceString(java.lang.Throwable);
}

# Remove print statements during release build.
-assumenosideeffects class java.io.PrintStream {
    public void println(java.lang.String);
}

# Add rules for the data models to avoid Gson/Retrofit issues
-keep class com.tarek.asteroidradar.model.** { *; }
-keepclassmembers,allowobfuscation class * {
    <fields>;
}

# Keep classes that use Retrofit annotations
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Retrofit and OkHttp
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Gson classes
-keep class com.google.gson.** { *; }

# If using custom views or components that rely on reflection, add:
#-keepclassmembers class com.tarek.asteroidradar.view.** {
#    public <init>(android.content.Context, android.util.AttributeSet);
#}
# Replace `view` with the actual package name where your custom views are.

# Application classes that will be serialized/deserialized over Gson
-keep class com.tarek.asteroidradar.** { *; }

# Keep model classes used by Moshi
-keep class com.tarek.asteroidradar.domain.** { *; }
-keep class com.tarek.asteroidradar.network.** { *; }

# Keep Moshi classes
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier interface *

# Keep Kotlin classes and members used by Moshi
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}
# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontwarn com.squareup.okhttp3.internal.**
-dontwarn com.squareup.okhttp3.**
-dontwarn javax.annotation.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn android.support.v4.**
-dontwarn com.google.android.gms.internal.**
-dontwarn com.fasterxml.jackson.core.**
-dontwarn java.lang.management.**
-dontwarn com.google.android.gms.**
-keep class * {
    public private *;
}
-keep public class com.google.android.gms.*
