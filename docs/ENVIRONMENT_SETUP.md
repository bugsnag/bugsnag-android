# Environment setup

This contains information on first-time setup required to develop bugsnag-android. Please raise a PR if any steps are missing as these dependencies commonly change.

## Pre-requisites

- Java 11 JDK, this can be installed with [sdkman](https://sdkman.io/)
    - If using Apple silicon, use Zulu 11.0.18 `sdk install java 11.0.18-zulu`
- A working [git](https://git-scm.com/) installation
- A ruby installation (can be skipped until running E2E tests)
- A docker installation (only for E2E tests/debugging CI images)

## Initial setup

1. Download the [latest stable version](https://developer.android.com/studio) of Android Studio.
2. Download [r23.1 of the NDK](https://developer.android.com/ndk/downloads/older_releases) (23.1.7779620)
3. Set the `$ANDROID_SDK_ROOT` environment variables to point to the SDK
4. Add the adb/android/emulator [platform tools](https://developer.android.com/studio/command-line/variables) to your `$PATH`
4. Clone the repository and its submodules: `git submodule update --init --recursive`
5. Open the project in Android Studio to trigger indexing and downloading of dependencies
