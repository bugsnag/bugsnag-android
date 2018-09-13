#include "crash_info.h"
#include <time.h>

#ifdef __cplusplus
extern "C" {
#endif

void bsg_populate_report_as(bsg_environment *env) {
  static time_t now;

  env->next_report.device.time = time(&now);
  // Convert to milliseconds:
  env->next_report.app.duration = env->next_report.app.duration_ms_offset +
                                  ((now - env->start_time) * 1000);
  if (env->next_report.app.in_foreground && env->foreground_start_time > 0) {
    env->next_report.app.duration_in_foreground =
        env->next_report.app.duration_in_foreground_ms_offset +
        ((now - env->foreground_start_time) * 1000);
  } else {
    env->next_report.app.duration_in_foreground = 0;
  }
}

#ifdef __cplusplus
}
#endif
