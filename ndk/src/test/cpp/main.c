#include <android/log.h>

#define GREATEST_FPRINTF(ignore, fmt, ...) __android_log_print(ANDROID_LOG_INFO, "BugsnagNDKTest", fmt, ##__VA_ARGS__)

#include <greatest/greatest.h>
#include <jni.h>

SUITE(string_utils);
SUITE(serialize_utils);
SUITE(breadcrumbs);

GREATEST_MAIN_DEFS();

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCXXTest_run(
    JNIEnv *_env, jobject _this) {
    int argc = 0;
    char *argv[] = {};
    GREATEST_MAIN_BEGIN();

    RUN_SUITE(string_utils);
    RUN_SUITE(serialize_utils);
    RUN_SUITE(breadcrumbs);

    GREATEST_MAIN_END();
}
