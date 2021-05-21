#ifndef BUGSNAG_ANR_HANDLER_H
#define BUGSNAG_ANR_HANDLER_H
#include "unwind_func.h"
#include <jni.h>
#include <stdbool.h>

/**
 * The Application Not Responding (ANR) handler captures SIGQUIT being raised,
 * interpreting it as the operating system sending an indication that the app
 * has not responded to user input for some time.
 *
 * References:
 * * https://android.googlesource.com/platform/dalvik2/+/refs/heads/master/vm/SignalCatcher.cpp
 * * https://android.googlesource.com/platform/frameworks/base/+/master/services/core/java/com/android/server/am/AppErrors.java
 */

#ifndef BUGSNAG_LOG
#define BUGSNAG_LOG(fmt, ...)                                                  \
  __android_log_print(ANDROID_LOG_WARN, "BugsnagANR", fmt, ##__VA_ARGS__)
#endif

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Monitor for ANRs, writing to a byte buffer when detected
 * @param env the JNIEnv used to notify when an ANR occurs
 * @param plugin the AnrPlugin object that shouild be used to notify bugsnag
 */
bool bsg_handler_install_anr(JNIEnv *env, jobject plugin);

void bsg_handler_uninstall_anr(void);

void bsg_set_unwind_function(unwind_func func);

#ifdef __cplusplus
}
#endif
#endif
