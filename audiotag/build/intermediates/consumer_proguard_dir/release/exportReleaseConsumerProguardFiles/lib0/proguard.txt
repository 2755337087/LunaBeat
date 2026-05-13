# Consumer rules for audiotag module

# Keep all audiotag classes for JNI
-keep class com.lonx.audiotag.** { *; }

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep model classes
-keep class com.lonx.audiotag.model.** { *; }
