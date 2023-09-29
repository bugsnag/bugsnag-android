# bugsnag-android-appstart

Auto-start BugSnag using
the [Android App Startup Library](https://developer.android.com/topic/libraries/app-startup).

## Getting Started
1. [Create a Bugsnag account](https://www.bugsnag.com)
1. Add this module to your app dependencies:
```kotlin
dependencies {
    // ...
    implementation("com.bugsnag:bugsnag-android-appstart:6.+")
}
```
1. Add your API key to your `AndroidManifest.xml`:
```xml
<meta-data
    android:name="com.bugsnag.android.API_KEY"
    android:value="your-api-key" />
```