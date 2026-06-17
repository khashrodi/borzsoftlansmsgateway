# BorzSoft SMS Gateway ProGuard Rules
-keep class com.borzsoft.smsgateway.** { *; }
-keep class fi.iki.elonen.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.zxing.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-dontwarn fi.iki.elonen.**
