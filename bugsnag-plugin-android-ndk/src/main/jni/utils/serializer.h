#pragma once
#include "../bugsnag_ndk.h"
#include "build.h"

#ifdef __cplusplus
extern "C" {
#endif

char *bsg_serialize_event_to_json_string(bugsnag_event *event);

bool bsg_serialize_event_to_file(bsg_environment *env) __asyncsafe;

/**
 * Serializes the LastRunInfo to the file. This persists information about
 * why the current launch crashed, for use on future launch.
 */
bool bsg_serialize_last_run_info_to_file(bsg_environment *env) __asyncsafe;

#ifdef __cplusplus
}
#endif
