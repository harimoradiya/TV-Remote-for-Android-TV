# ─── Encoding & Source Info ───────────────────────────────────────────────────
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,Signature,Exceptions

# ─── Your App Classes ─────────────────────────────────────────────────────────
-keep public class com.hari.androidtvremote.App { *; }
-keep class com.hari.androidtvremote.androidLib.** { *; }
-keep class com.connectsdk.** { *; }

# ─── Native Methods ───────────────────────────────────────────────────────────
-keepclasseswithmembernames class * {
    native <methods>;
}

# ─── Android Resources ────────────────────────────────────────────────────────
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ─── Enums ────────────────────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ─── Suppress Log in Release ──────────────────────────────────────────────────
-assumenoexternalsideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# ─── Google Ads ───────────────────────────────────────────────────────────────
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# ─── Suppress Missing Class Warnings (3rd party libs) ────────────────────────
-dontwarn com.amazon.**
-dontwarn javax.**
-dontwarn org.ietf.**
-dontwarn org.apache.**
-dontwarn sun.security.**
-dontwarn java.lang.management.**
-dontwarn okhttp3.internal.annotations.**