#include "anr_handler.h"
#include "unwind_func.h"
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

JNIEXPORT void JNICALL Java_com_bugsnag_android_AnrPlugin_setUnwindFunction(
    JNIEnv *env, jobject thiz, jlong unwind_function) {
  bsg_set_unwind_function((unwind_func)unwind_function);
}

#ifdef __cplusplus
}
#endif
