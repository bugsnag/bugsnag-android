#include "bugsnag_anr.h"
#include "anr_handler.h"

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_bugsnag_android_AnrPlugin_installAnrDetection(
    JNIEnv *env, jobject _this, jobject byteBuffer) {
    bsg_handler_install_anr((*env)->GetDirectBufferAddress(env, byteBuffer));
    BUGSNAG_LOG("Initialization complete!");
}

#ifdef __cplusplus
}
#endif
