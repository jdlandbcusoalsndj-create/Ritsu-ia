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

# Keep AI Engine classes
-keep class com.ritsu.aiassistant.core.ai.** { *; }

# Keep Voice Engine classes
-keep class com.ritsu.aiassistant.core.voice.** { *; }

# Keep Database classes
-keep class com.ritsu.aiassistant.data.** { *; }

# Keep Service classes
-keep class com.ritsu.aiassistant.services.** { *; }

# Keep Receiver classes
-keep class com.ritsu.aiassistant.receivers.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ONNX Runtime
-keep class ai.onnxruntime.** { *; }

# Accessibility Service
-keep class com.ritsu.aiassistant.services.RitsuAccessibilityService { *; }

# Notification Listener Service
-keep class com.ritsu.aiassistant.services.NotificationListenerService { *; }