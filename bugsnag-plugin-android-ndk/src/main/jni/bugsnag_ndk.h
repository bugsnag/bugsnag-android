/**
 * JNI interface between bugsnag-android-ndk Java and C++
 */
#ifndef BUGSNAG_NDK_H
#define BUGSNAG_NDK_H

#include <android/log.h>
#include <stdbool.h>

#include "event.h"
#include "utils/stack_unwinder.h"
#include "../assets/include/bugsnag.h"

#ifndef BUGSNAG_LOG
#define BUGSNAG_LOG(fmt, ...)                                                  \
  __android_log_print(ANDROID_LOG_WARN, "BugsnagNDK", fmt, ##__VA_ARGS__)
#endif

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    /**
     * Unwinding style used for signal-safe handling
     */
    bsg_unwinder signal_unwind_style;
    /**
     * Preferred unwinding style
     */
    bsg_unwinder unwind_style;
    bsg_report_header report_header;
    /**
     * File path on disk where the next crash report will be written if needed.
     */
    char next_event_path[384];
    /**
     * Cache of static metadata and report info. Exception/time information is populated at crash time.
     */
    bugsnag_event next_event;
    /**
     * Time when installed
     */
    time_t start_time;
    /**
     * Time when last re-entering foreground
     */
    time_t foreground_start_time;
    /**
     * true if a crash is currently being handled. Disallows multiple crashes
     * from being processed simultaneously
     */
    bool handling_crash;
    /**
     * true if a handler has completed crash handling
     */
    bool crash_handled;

    on_error on_error;
} bsg_environment;

bsg_unwinder bsg_configured_unwind_style();

void bugsnag_add_on_error_env(JNIEnv *env, on_error on_error);
bool bsg_run_on_error_cbs(bsg_environment *const env);

#ifdef __cplusplus
}
#endif
#endif // BUGSNAG_NDK_H
