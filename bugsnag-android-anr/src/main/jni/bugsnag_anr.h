/**
 * JNI interface between bugsnag-android-anr JVM and C++
 */
#ifndef BUGSNAG_ANR_H
#define BUGSNAG_ANR_H

#include <android/log.h>

#ifndef BUGSNAG_LOG
#define BUGSNAG_LOG(fmt, ...)                                                  \
  __android_log_print(ANDROID_LOG_WARN, "BugsnagANR", fmt, ##__VA_ARGS__)
#endif

#ifdef __cplusplus
extern "C" {
#endif

#ifdef __cplusplus
}
#endif
#endif // BUGSNAG_NDK_H
