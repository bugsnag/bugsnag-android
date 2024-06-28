-keepattributes LineNumberTable,SourceFile
-keep class com.bugsnag.android.NativeInterface { *; }
-keep class com.bugsnag.android.NativeStackframe { *; }
-keep class com.bugsnag.android.Breadcrumb { *; }
-keep class com.bugsnag.android.BreadcrumbState { *; }
-keep class com.bugsnag.android.BreadcrumbType { *; }
-keep class com.bugsnag.android.Severity { *; }
-keepclassmembers enum com.bugsnag.android.Telemetry {
    public static com.bugsnag.android.Telemetry[] values();
 }
-keepclassmembers enum com.bugsnag.android.ErrorType {
    public static com.bugsnag.android.Telemetry[] values();
 }