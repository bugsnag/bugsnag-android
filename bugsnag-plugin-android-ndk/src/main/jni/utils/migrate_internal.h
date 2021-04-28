//
// Created by Karl Stenerud on 23.04.21.
//

#ifndef BUGSNAG_ANDROID_MIGRATE_INTERNAL_H
#define BUGSNAG_ANDROID_MIGRATE_INTERNAL_H

#include "string.h"

#include "migrate.h"
#include <event.h>
#include <fcntl.h>
#include <metadata.h>
#include <parson/parson.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#define V1_BUGSNAG_CRUMBS_MAX 30

/**
 * Definitions of structs that were used in previous versions of a notifier
 * release.
 *
 * Because these structs are serialized to disk directly, we need to retain the
 * original structs whenever a field is added or changed.
 *
 * The bsg_report_header indicates what version was serialized to disk. Knowing
 * this information, it is possible to migrate old payloads by first serializing
 * them into an old struct, and then mapping them into the latest version of
 * bugsnag_event.
 */

// Version 5

typedef struct {
  char name[64];  // CHANGED in v6
  char email[64]; // CHANGED in v6
  char id[64];    // CHANGED in v6
} bugsnag_user_v5;

typedef struct {
  int api_level;
  int cpu_abi_count;
  bsg_cpu_abi cpu_abi[8];
  char orientation[32];
  time_t time;
  char id[64]; // CHANGED in v6
  bool jailbroken;
  char locale[32];
  char manufacturer[64];
  char model[64];
  char os_build[64];
  char os_version[64];
  char os_name[64];
  long total_memory;
} bsg_device_info_v5;

typedef struct {
  /**
   * The key identifying this metadata entry
   */
  char name[32]; // CHANGED in v6
  /**
   * The metadata tab
   */
  char section[32];
  /**
   * The value type from bool, char, number
   */
  bugsnag_metadata_type type;

  /**
   * Value if type is BSG_BOOL_VALUE
   */
  bool bool_value;
  /**
   * Value if type is BSG_CHAR_VALUE
   */
  char char_value[64]; // CHANGED in v6
  /**
   * Value if type is BSG_DOUBLE_VALUE
   */
  double double_value;
} bsg_metadata_value_v5;

typedef struct {
  /** The number of values in use */
  int value_count;
  bsg_metadata_value_v5 values[BUGSNAG_METADATA_MAX]; // CHANGED in v6
} bugsnag_metadata_v5;

typedef struct {
  char name[64]; // CHANGED in v6
  char timestamp[37];
  bugsnag_breadcrumb_type type;

  /**
   * Key/value pairs of related information for debugging
   */
  bugsnag_metadata_v5 metadata; // CHANGED in v6
} bugsnag_breadcrumb_v5;

typedef struct {
  bsg_notifier notifier;
  bsg_app_info app;
  bsg_device_info_v5 device; // CHANGED in v6
  bugsnag_user_v5 user;      // CHANGED in v6
  bsg_error error;
  bugsnag_metadata_v5 metadata; // CHANGED in v6

  int crumb_count;
  // Breadcrumbs are a ring; the first index moves as the
  // structure is filled and replaced.
  int crumb_first_index;
  bugsnag_breadcrumb_v5 breadcrumbs[BUGSNAG_CRUMBS_MAX]; // CHANGED in v6

  char context[64]; // CHANGED in v6
  bugsnag_severity severity;

  char session_id[33];
  char session_start[33];
  int handled_events;
  int unhandled_events;
  char grouping_hash[64]; // CHANGED in v6
  bool unhandled;
  char api_key[64];
} bugsnag_event_v5;

typedef struct {
  char name[64];
  char version[16];
  char url[64];
} bsg_library;

/** a Bugsnag exception */
typedef struct {
  /** The exception name or stringified code */
  char name[64];
  /** A description of what went wrong */
  char message[256];
  /** The variety of exception which needs to be processed by the pipeline */
  char type[32];

  /**
   * The number of frames used in the stacktrace. Must be less than
   * BUGSNAG_FRAMES_MAX.
   */
  ssize_t frame_count;
  /**
   * An ordered list of stack frames from the oldest to the most recent
   */
  bugsnag_stackframe stacktrace[BUGSNAG_FRAMES_MAX];
} bsg_exception;

typedef struct {
  char key[64];
  char value[64];
} bsg_char_metadata_pair;

typedef struct {
  char name[33];
  char timestamp[37];
  bugsnag_breadcrumb_type type;

  /**
   * Key/value pairs of related information for debugging
   */
  bsg_char_metadata_pair metadata[8];
} bugsnag_breadcrumb_v1;

typedef struct {
  char name[64];
  char id[64];
  char package_name[64];
  char release_stage[64];
  char type[32];
  char version[32];
  char version_name[32];
  char active_screen[64];
  int version_code;
  char build_uuid[64];
  time_t duration;
  time_t duration_in_foreground;
  time_t duration_ms_offset;
  time_t duration_in_foreground_ms_offset;
  bool in_foreground;
  bool low_memory;
  size_t memory_usage;
  char binaryArch[32];
} bsg_app_info_v1;

typedef struct {
  char id[64];
  char release_stage[64];
  char type[32];
  char version[32];
  char active_screen[64];
  int version_code;
  char build_uuid[64];
  time_t duration;
  time_t duration_in_foreground;
  time_t duration_ms_offset;
  time_t duration_in_foreground_ms_offset;
  bool in_foreground;
  char binary_arch[32];
} bsg_app_info_v2;

typedef struct {
  int api_level;
  double battery_level;
  char brand[64];
  int cpu_abi_count;
  bsg_cpu_abi cpu_abi[8];
  int dpi;
  bool emulator;
  char orientation[32];
  time_t time;
  char id[64];
  bool jailbroken;
  char locale[32];
  char location_status[32];
  char manufacturer[64];
  char model[64];
  char network_access[64];
  char os_build[64];
  char os_version[64];
  float screen_density;
  char screen_resolution[32];
  long total_memory;
} bsg_device_info_v1;

typedef struct {
  bsg_library notifier;
  bsg_app_info_v1 app;
  bsg_device_info_v1 device;
  bugsnag_user_v5 user;
  bsg_exception exception;
  bugsnag_metadata_v5 metadata;

  int crumb_count;
  // Breadcrumbs are a ring; the first index moves as the
  // structure is filled and replaced.
  int crumb_first_index;
  bugsnag_breadcrumb_v1 breadcrumbs[V1_BUGSNAG_CRUMBS_MAX];

  char context[64];
  bugsnag_severity severity;

  char session_id[33];
  char session_start[33];
  int handled_events;
} bugsnag_report_v1;

typedef struct {
  bsg_library notifier;
  bsg_app_info_v1 app;
  bsg_device_info_v1 device;
  bugsnag_user_v5 user;
  bsg_exception exception;
  bugsnag_metadata_v5 metadata;

  int crumb_count;
  // Breadcrumbs are a ring; the first index moves as the
  // structure is filled and replaced.
  int crumb_first_index;
  bugsnag_breadcrumb_v1 breadcrumbs[V1_BUGSNAG_CRUMBS_MAX];

  char context[64];
  bugsnag_severity severity;

  char session_id[33];
  char session_start[33];
  int handled_events;
  int unhandled_events;
} bugsnag_report_v2;

typedef struct {
  bsg_notifier notifier;
  bsg_app_info_v2 app;
  bsg_device_info_v5 device;
  bugsnag_user_v5 user;
  bsg_error error;
  bugsnag_metadata_v5 metadata;

  int crumb_count;
  // Breadcrumbs are a ring; the first index moves as the
  // structure is filled and replaced.
  int crumb_first_index;
  bugsnag_breadcrumb breadcrumbs[BUGSNAG_CRUMBS_MAX];

  char context[64];
  bugsnag_severity severity;

  char session_id[33];
  char session_start[33];
  int handled_events;
  int unhandled_events;
  char grouping_hash[64];
  bool unhandled;
} bugsnag_report_v3;

typedef struct {
  bsg_notifier notifier;
  bsg_app_info_v2 app;
  bsg_device_info_v5 device;
  bugsnag_user_v5 user;
  bsg_error error;
  bugsnag_metadata_v5 metadata;

  int crumb_count;
  // Breadcrumbs are a ring; the first index moves as the
  // structure is filled and replaced.
  int crumb_first_index;
  bugsnag_breadcrumb breadcrumbs[BUGSNAG_CRUMBS_MAX];

  char context[64];
  bugsnag_severity severity;

  char session_id[33];
  char session_start[33];
  int handled_events;
  int unhandled_events;
  char grouping_hash[64];
  bool unhandled;
  char api_key[64];
} bugsnag_report_v4;

// Legacy Functions

int bsg_calculate_total_crumbs(int old_count);
int bsg_calculate_v1_start_index(int old_count);
int bsg_calculate_v1_crumb_index(int crumb_pos, int first_index);

void bsg_migrate_app_v2(bugsnag_report_v4 *report_v4, bugsnag_event_v5 *event);

#endif // BUGSNAG_ANDROID_MIGRATE_INTERNAL_H
