-keepattributes LineNumberTable,SourceFile
-keep class com.bugsnag.android.ndk.OpaqueValue {
    java.lang.String getJson();
    static java.lang.Object makeSafe(java.lang.Object);
}
-keep class com.bugsnag.android.ndk.NativeBridge { *; }
-keep class com.bugsnag.android.NdkPlugin { *; }
