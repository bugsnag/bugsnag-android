cppcheck --error-exitcode=2 --enable=warning,performance --check-level=exhaustive bugsnag-plugin-android-anr/src/main/jni && \
cppcheck --error-exitcode=2 --enable=warning,performance --check-level=exhaustive bugsnag-plugin-android-ndk/src/main/jni -i bugsnag-plugin-android-ndk/src/main/jni/deps -i bugsnag-plugin-android-ndk/src/main/jni/external
