/**
 * JNI interface between bugsnag-android-ndk Java and C++
 */
#ifndef BUGSNAG_NDK_H
#define BUGSNAG_NDK_H

#include <android/log.h>
#include <stdbool.h>

#include "../assets/include/bugsnag.h"
#include "event.h"
#include "utils/stack_unwinder.h"

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
  /**
   * Records the version of the bugsnag NDK report being serialized to disk.
   */
  bsg_report_header report_header;
  /**
   * File path on disk where the next crash report will be written if needed.
   */
  char next_event_path[384];
  /**
   * File path on disk where the last run info will be written if needed.
   */
  char last_run_info_path[384];
  /**
   * The next value to be written to last run info if a crash occurs
   */
  char next_last_run_info[256];
  /**
   * The value of consecutiveLaunchCrashes, used when a crash occurs
   */
  int consecutive_launch_crashes;
  /**
   * Cache of static metadata and event info. Exception/time information is
   * populated at crash time.
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

  bsg_on_error on_error;
} bsg_environment;

bsg_unwinder bsg_configured_unwind_style();

/**
 * Invokes the user-supplied on_error callback, if it has been set. This allows
 * users to mutate the bugsnag_event payload before it is persisted to disk, and
 * to discard the report by returning false..
 *
 * @return true if the report should be delivered, false if it should be
 * discarded
 */
bool bsg_run_on_error();

#ifdef __cplusplus
}
#endif
#endif // BUGSNAG_NDK_H
