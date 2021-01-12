#include "anr_handler.h"
#include <android/log.h>
#include <jni.h>
#include "unwind_func.h"

#ifdef __cplusplus
extern "C" {
#endif

extern unwind_func local_bsg_unwind_stack;

JNIEXPORT void JNICALL Java_com_bugsnag_android_AnrPlugin_enableAnrReporting(
    JNIEnv *env, jobject _this, jboolean callPreviousSigquitHandler) {
  bsg_handler_install_anr(env, _this, callPreviousSigquitHandler);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_AnrPlugin_disableAnrReporting(
    JNIEnv *env, jobject _this) {
  bsg_handler_uninstall_anr();
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_AnrPlugin_setUnwindFunction(
    JNIEnv *env, jobject thiz, jlong unwind_function) {
  local_bsg_unwind_stack = (unwind_func)unwind_function;
}

#ifdef __cplusplus
}
#endif
