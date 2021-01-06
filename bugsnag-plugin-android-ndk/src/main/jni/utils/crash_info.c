#include "crash_info.h"
#include <time.h>

#ifdef __cplusplus
extern "C" {
#endif

void bsg_populate_event_as(bsg_environment *env) {
  static time_t now;

  env->next_event.device.time = time(&now);
  // Convert to milliseconds:
  env->next_event.app.duration =
      env->next_event.app.duration_ms_offset + ((now - env->start_time) * 1000);
  if (env->next_event.app.in_foreground && env->foreground_start_time > 0) {
    env->next_event.app.duration_in_foreground =
        env->next_event.app.duration_in_foreground_ms_offset +
        ((now - env->foreground_start_time) * 1000);
  } else {
    env->next_event.app.duration_in_foreground = 0;
  }
}

void bsg_increment_unhandled_count(bugsnag_event *ptr) {
  if (ptr->unhandled) {
    ptr->unhandled_events++;
  } else {
    ptr->handled_events++;
  }
}

#ifdef __cplusplus
}
#endif
