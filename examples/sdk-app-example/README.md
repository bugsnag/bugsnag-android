# Example Android integration with Bugsnag

Includes build variants for both Java and Kotlin

1. Open the repository root project in Android Studio
2. Insert your API key into the AndroidManifest.xml
3. Select build variant (either `javaExample` or `kotlinExample`)
4. Build the `ndk` module (select it from the menu)
5. Run the app! There are several examples of different kinds of errors which
   can be thrown.

_Note:_ If you are using NDK revision 17+, remove `armeabi` from the `abiFilters`
section of build.gradle.
