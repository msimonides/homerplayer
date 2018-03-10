# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/marcin/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# EventBus
-keepclassmembers class ** {
    public void onEvent*(**);
}

# Guava
-dontwarn sun.misc.Unsafe
## https://github.com/google/guava/issues/2926#issuecomment-325455128
## https://stackoverflow.com/questions/9120338/proguard-configuration-for-guava-with-obfuscation-and-optimization
-dontwarn com.google.common.base.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn java.lang.ClassValue
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
# Added for guava 23.5-android
-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**

# Required to preserve the Flurry SDK
-keep class com.flurry.** { *; }
-dontwarn com.flurry.**
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepclasseswithmembers class * {
  public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Google Play Services library
-keep class * extends java.util.ListResourceBundle {
  protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
  public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
  @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
  public static final ** CREATOR;
}
#  -- end Flurry config --

-keepattributes SourceFile, LineNumberTable