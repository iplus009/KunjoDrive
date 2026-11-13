# Crash-safe anti-tamper protection: never shrink or strip classes/resources (Rule 6)
-dontshrink
-dontoptimize

# Native C++ Security Shield JNI entrypoint
-keep class com.novo.drive.core.security.AndroidNativeCoreJni { *; }

# ML Kit On-Device Offline OCR
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Debug logging is a user-facing setting, so Log calls must survive R8.
# Never add -assumenosideeffects for android.util.Log here.
-keep class com.novo.drive.core.common.SafeLog { *; }
-keep class android.util.Log { *; }

# TDLib is reached through JNI, so its classes and native callbacks must stay.
-keep class org.drinkless.tdlib.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Room entities are constructed reflectively by generated code.
-keep class com.novo.drive.data.local.entity.** { *; }
-keep class com.novo.drive.data.local.dao.** { *; }

# kotlinx.serialization keeps generated serializers on the classes themselves.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class * implements kotlinx.serialization.KSerializer {
    <init>(...);
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    *** INSTANCE;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Navigation routes and domain models
-keep class com.novo.drive.presentation.navigation.** { *; }
-keep interface com.novo.drive.presentation.navigation.Route { *; }
-keep class com.novo.drive.domain.model.** { *; }

# Koin reflection and ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class org.koin.** { *; }

# Navigation & Compose
-keep class androidx.navigation.** { *; }

# Backdrop / AndroidLiquidGlass
-keep class com.kyant.backdrop.** { *; }
-dontwarn com.kyant.backdrop.**
