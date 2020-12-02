cppcheck --enable=warning,performance bugsnag-plugin-android-anr/src/main/jni && \
cppcheck --enable=warning,performance bugsnag-plugin-android-ndk/src/main/jni -i \
bugsnag-plugin-android-ndk/src/main/jni/deps -i bugsnag-plugin-android-ndk/src/main/jni/external
