Developing bugsnag-android
============

The following guide should get you setup for developing bugsnag-android.

# Initial setup

1. Download the [latest stable version](https://developer.android.com/studio) of Android Studio.
2. Download [r16b of the NDK](https://developer.android.com/ndk/downloads/older_releases)
3. Set the `$ANDROID_HOME` and `$ANDROID_NDK_HOME` environment variables to point to the SDKs, and optionally add the `adb/android/emulator` [platform tools](https://developer.android.com/studio/command-line/variables) to your `$PATH`
4. Clone the repository and its submodules: `git submodule update --init --recursive`
5. Open the project in Android Studio to trigger indexing and downloading of dependencies

# Project structure

The project is comprised of three [Gradle modules](https://docs.gradle.org/current/userguide/multi_project_builds.html):

- [bugsnag-android-core](../bugsnag-android-core/README.md), responsible for capturing JVM errors and delivery of payloads
- [bugsnag-plugin-android-ndk](../bugsnag-plugin-android-ndk/README.md), responsible for capturing NDK errors
- [bugsnag-plugin-android-anr](../bugsnag-plugin-android-anr/README.md), responsible for capturing ANRs

[Gradle tasks](https://docs.gradle.org/current/userguide/tutorial_using_tasks.html) can generally be run on the entire project, or on an individual module: 

```shell
./gradlew build // builds whole project
./gradlew bugsnag-plugin-android-anr:build // builds one module only
```

# Example app

The example app can be found in [examples/sdk-app-example](../examples/sdk-app-example). It contains example code which triggers crashes.

You should open `examples/sdk-app-example` as a separate Android Studio project and [run the app](https://developer.android.com/training/basics/firstapp/running-app). This will require a connected device or emulator.

The app **does not use the local development version of Bugsnag**. To test local development changes, you will need to build the library locally with a high version number, and update the `bugsnag-android` dependency in `examples/sdk-app-example/build.gradle` to use that version.

## Building the Library locally

The following command assembles bugsnag-android for local use:

```shell
./gradlew assembleRelease publishToMavenLocal -PVERSION_NAME=9.9.9
```

This installs bugsnag-android to a local maven repository at `~/.m2/repository/com/bugsnag/`. The example app should automatically use
this version when you next run the app.

# Building with custom ABIs

By default, the NDK module will be built with the following ABIs:

- arm64-v8a
- armeabi-v7a
- x86
- x86_64

To build the NDK module with specific ABIs, use the `ABI_FILTERS` project
option:

```shell
./gradlew assembleRelease -PABI_FILTERS=x86,arm64-v8a
```

For release purposes, the Makefile's build command includes the "armeabi" ABI for compatibility with devices using r16 and below of the NDK.

