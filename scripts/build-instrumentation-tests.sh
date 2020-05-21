echo Switching to NDK $NDK_VERSION

export ANDROID_NDK_HOME="${ANDROID_HOME}/android-ndk-${NDK_VERSION}"
export PATH="${ANDROID_NDK_HOME}:${PATH}"

# Build the Espresso Test App
./gradlew assembleAndroidTest --stacktrace
