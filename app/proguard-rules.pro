# Telefam R8 / ProGuard Obfuscation & Hardening Rules
-repackageclasses 'o'
-allowaccessmodification
-overloadaggressively
-renamesourcefileattribute T
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Strip verbose debug and console logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
-assumenosideeffects class kotlin.io.ConsoleKt {
    public static void println(...);
    public static void print(...);
}
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# Preserve JNI native bridge entry points
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class telefam.company.core.NativeTelefamBridge {
    *;
}

# Keep Moshi generated JSON adapters and DTOs
-keep class telefam.company.data.network.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# Keep Room database implementations
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
