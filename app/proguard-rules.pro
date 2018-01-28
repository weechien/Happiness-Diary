# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Asus\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
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

-keepattributes Signature
-keep class android.support.v8.renderscript.** {*;}
-keep class android.support.v7.widget.SearchView { *; }
-keepattributes *Annotation*
-dontwarn io.branch.**
-dontwarn retrofit2.**
-ignorewarnings
-keep class * {
    public private *;
}
-keepclassmembers class com.goh.weechien.HappinessDiary.** {
  *;
  }
-keep class .R
-keep class **.R$* {
  <fields>;
}