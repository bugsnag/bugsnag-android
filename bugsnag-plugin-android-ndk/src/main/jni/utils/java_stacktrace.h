#ifndef BUGSNAG_ANDROID_JAVA_STACKTRACE_H
#define BUGSNAG_ANDROID_JAVA_STACKTRACE_H

#include "event.h"
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

void bsg_copy_java_stacktrace(JNIEnv *env, jobject stack_trace_,
                              bsg_error *error);

#ifdef __cplusplus
}
#endif
#endif // BUGSNAG_ANDROID_JAVA_STACKTRACE_H
