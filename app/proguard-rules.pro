# Add project specific ProGuard rules here.
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
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

# Keep Compose related classes
-keep class androidx.compose.** { *; }
-keep class com.example.LyricBox.ui.theme.** { *; }
-keep class com.example.LyricBox.** { *; }

# Keep audiotag library classes for JNI
-keep class com.lonx.audiotag.** { *; }

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep data classes used in JSON serialization
-keep class com.example.LyricBox.AudioFile { *; }

# Keep Composable functions
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Parcelable CREATOR
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep sealed class subclasses
-keep class * extends com.example.LyricBox.utils.UpdateResult { *; }

# Keep Kotlin metadata for reflection
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep class kotlin.Metadata { *; }
-keep class kotlin.** { *; }

# Keep OkHttp and Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# Remove all Log statements in release build
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}
