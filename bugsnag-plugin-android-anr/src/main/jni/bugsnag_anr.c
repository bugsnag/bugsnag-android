#include "anr_handler.h"
#include <android/log.h>

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_bugsnag_android_AnrPlugin_installAnrDetection(
    JNIEnv *env, jobject _this, jobject byteBuffer) {

    if (byteBuffer != NULL) {
        bsg_handler_install_anr((*env)->GetDirectBufferAddress(env, byteBuffer));
        BUGSNAG_LOG("Initialization complete!");
    } else {
        BUGSNAG_LOG("Failed to initialise ANR detection due to null buffer");
    }
}

#ifdef __cplusplus
}
#endif
