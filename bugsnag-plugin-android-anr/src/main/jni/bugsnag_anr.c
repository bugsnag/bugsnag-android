#include "anr_handler.h"
#include <android/log.h>

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_bugsnag_android_AnrPlugin_enableAnrReporting(
        JNIEnv *env, jobject _this) {
    bsg_handler_install_anr(env, _this);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_AnrPlugin_disableAnrReporting(
        JNIEnv *env, jobject _this) {
    bsg_handler_uninstall_anr();
}

#ifdef __cplusplus
}
#endif
