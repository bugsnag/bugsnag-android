/**
 * JNI interface between bugsnag-android-ndk Java and C++
 */
#ifndef BUGSNAG_NDK_H
#define BUGSNAG_NDK_H

#include <stdatomic.h>
#include <stdbool.h>

#include "event.h"
#include "include/bugsnag.h"
#include "utils/logger.h"
#include "utils/stack_unwinder.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
  /**
   * Records the version of the bugsnag NDK report being serialized to disk.
   */
  bsg_report_header report_header;
  /**
   * File path on disk where the next crash report will be written if needed.
   */
  char *event_path;
  /**
   * The pre-generated UUID of the next crash report.
   */
  char event_uuid[37];
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
  _Atomic bool handling_crash;
  /**
   * true if a handler has completed crash handling
   */
  bool crash_handled;

  bsg_on_error on_error;

  /**
   * Controls whether we should capture and serialize the state of all threads
   * at the time of an error.
   */
  bsg_thread_send_policy send_threads;

  /**
   * Static data set on app launch and never again modified.
   * Allocated dynamically, in JSON format.
   * On crash, this data gets dumped to a new file with the same name as the
   * event file + ".staticData.json".
   * On delivery, this data gets sent to the JVM alongside of the JSONified
   * event object.
   */
  const char *_Atomic static_json_data;

} bsg_environment;

/**
 * Invokes the user-supplied on_error callback, if it has been set. This allows
 * users to mutate the bugsnag_event payload before it is persisted to disk, and
 * to discard the report by returning false..
 *
 * @return true if the report should be delivered, false if it should be
 * discarded
 */
bool bsg_run_on_error();

/**
 * This must be called before handling a native crash to ensure that two crash
 * handlers don't run simultaneously. If this function returns falls, DO NOT
 * PROCEED WITH CRASH HANDLING!
 *
 * When done handling the crash, you must call bsg_finish_handling_crash()
 *
 * @return true if no other crash handler is already running.
 */
bool bsg_begin_handling_crash();

/**
 * Let the system know that you've finished handling the crash.
 */
void bsg_finish_handling_crash();

#ifdef __cplusplus
}
#endif
#endif // BUGSNAG_NDK_H
