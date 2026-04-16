# Add project-specific Proguard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Peter\AppData\Local\Android\sdk/tools/proguard/proguard-android-optimize.txt

# Keep Jetpack Compose specific classes
-keep class androidx.compose.runtime.* { *; }

# Keep Health Connect related classes
-keep class androidx.health.connect.client.** { *; }

# Keep WorkManager related classes
-keep class androidx.work.** { *; }
