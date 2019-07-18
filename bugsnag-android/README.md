# bugsnag-android

This module is a meta package which contains no code itself. Its primary purpose is to 
publish an AAR + POM that adds a dependency on _all_ the bugsnag-android-* artefacts. This allows 
users to include bugsnag-android in their applications using the following syntax:

```
implementation "com.bugsnag:bugsnag-android:$version"
```

Which is simpler than manually specifying every single dependency:

```
implementation "com.bugsnag:bugsnag-android-core:$version"
implementation "com.bugsnag:bugsnag-plugin-android-ndk:$version"
implementation "com.bugsnag:bugsnag-plugin-android-anr:$version"
```
