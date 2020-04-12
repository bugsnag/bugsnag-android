echo Switching to NDK $NDK_VERSION
mv android-ndk-$NDK_VERSION $ANDROID_HOME/ndk-bundle

./gradlew assembleAndroidTest --stacktrace
