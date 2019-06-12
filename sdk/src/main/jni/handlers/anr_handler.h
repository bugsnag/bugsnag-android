#ifndef BUGSNAG_ANR_HANDLER_H
#define BUGSNAG_ANR_HANDLER_H
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

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Monitor for ANRs, writing to a byte buffer when detected
 * @param byte_buffer a pre-allocated reference to a direct byte buffer
 */
bool bsg_handler_install_anr(void *byte_buffer);

void bsg_handler_uninstall_anr(void);

#ifdef __cplusplus
}
#endif
#endif
