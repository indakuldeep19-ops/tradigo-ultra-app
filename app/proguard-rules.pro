# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Razorpay
-keepclassmembers class com.razorpay.** { *; }
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**

# Keep Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Hilt
-keepclassmembers class * {
    @dagger.hilt.android.HiltAndroidApp <init>();
}

# Keep Compose
-keep class androidx.compose.** { *; }
