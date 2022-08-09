#ifndef BUGSNAG_MIGRATE_H
#define BUGSNAG_MIGRATE_H

#include "event.h"

#ifndef V1_BUGSNAG_CRUMBS_MAX
#define V1_BUGSNAG_CRUMBS_MAX 30
#endif

#ifndef V2_BUGSNAG_CRUMBS_MAX
#define V2_BUGSNAG_CRUMBS_MAX 25
#endif

#ifdef __cplusplus
extern "C" {
#endif

/**
 * migrate.h contains definitions of structs that were used in previous versions
 * of a notifier release.
 *
 * Because these structs are serialized to disk directly, we need to retain the
 * original structs whenever a field is added or changed.
 *
 * The bsg_report_header indicates what version was serialized to disk. Knowing
 * this information, it is possible to migrate old payloads by first serializing
 * them into an old struct, and then mapping them into the latest version of
 * bugsnag_event.
 */

typedef struct {
  char name[64];
  char version[16];
  char url[64];
} bsg_library;

typedef struct {
  uintptr_t frame_address;
  uintptr_t symbol_address;
  uintptr_t load_address;
  uintptr_t line_number;

  char filename[256];
  char method[256];
} bugsnag_stackframe_v1;

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
  bugsnag_stackframe_v1 stacktrace[BUGSNAG_FRAMES_MAX];
} bsg_exception;

/** a Bugsnag exception */
typedef struct {
  /** The exception name or stringified code */
  char errorClass[64];
  /** A description of what went wrong */
  char errorMessage[256];
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
  bugsnag_stackframe_v1 stacktrace[BUGSNAG_FRAMES_MAX];
} bsg_error_v1;

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
  char name[32];
  char section[32];
  bugsnag_metadata_type type;
  bool bool_value;
  char char_value[64];
  double double_value;
} bsg_metadata_value_v1;

typedef struct {
  int value_count;
  bsg_metadata_value_v1 values[BUGSNAG_METADATA_MAX];
} bugsnag_metadata_v1;

typedef struct {
  char name[64];
  char timestamp[37];
  bugsnag_breadcrumb_type type;
  bugsnag_metadata_v1 metadata;
} bugsnag_breadcrumb_v2;

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
  bool is_launching;
  char binary_arch[32];
} bsg_app_info_v3;

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
  int api_level;
  int cpu_abi_count;
  bsg_cpu_abi cpu_abi[8];
  char orientation[32];
  time_t time;
  char id[64];
  bool jailbroken;
  char locale[32];
  char manufacturer[64];
  char model[64];
  char os_build[64];
  char os_version[64];
  char os_name[64];
  long total_memory;
} bsg_device_info_v2;

typedef struct {
  bsg_library notifier;
  bsg_app_info_v1 app;
  bsg_device_info_v1 device;
  bugsnag_user user;
  bsg_exception exception;
  bugsnag_metadata_v1 metadata;

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
  bugsnag_user user;
  bsg_exception exception;
  bugsnag_metadata_v1 metadata;

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
  bsg_device_info_v2 device;
  bugsnag_user user;
  bsg_error_v1 error;
  bugsnag_metadata_v1 metadata;

  int crumb_count;
  // Breadcrumbs are a ring; the first index moves as the
  // structure is filled and replaced.
  int crumb_first_index;
  bugsnag_breadcrumb_v2 breadcrumbs[V2_BUGSNAG_CRUMBS_MAX];

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
  bsg_device_info_v2 device;
  bugsnag_user user;
  bsg_error_v1 error;
  bugsnag_metadata_v1 metadata;

  int crumb_count;
  // Breadcrumbs are a ring; the first index moves as the
  // structure is filled and replaced.
  int crumb_first_index;
  bugsnag_breadcrumb_v2 breadcrumbs[V2_BUGSNAG_CRUMBS_MAX];

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

typedef struct {
  bsg_notifier notifier;
  bsg_app_info_v3 app;
  bsg_device_info_v2 device;
  bugsnag_user user;
  bsg_error_v1 error;
  bugsnag_metadata_v1 metadata;

  int crumb_count;
  // Breadcrumbs are a ring; the first index moves as the
  // structure is filled and replaced.
  int crumb_first_index;
  bugsnag_breadcrumb_v2 breadcrumbs[V2_BUGSNAG_CRUMBS_MAX];

  char context[64];
  bugsnag_severity severity;

  char session_id[33];
  char session_start[33];
  int handled_events;
  int unhandled_events;
  char grouping_hash[64];
  bool unhandled;
  char api_key[64];
} bugsnag_report_v5;

typedef struct {
  bsg_notifier notifier;
  bsg_app_info_v3 app;
  bsg_device_info_v2 device;
  bugsnag_user user;
  bsg_error_v1 error;
  bugsnag_metadata_v1 metadata;

  int crumb_count;
  // Breadcrumbs are a ring; the first index moves as the
  // structure is filled and replaced.
  int crumb_first_index;
  bugsnag_breadcrumb_v2 breadcrumbs[BUGSNAG_CRUMBS_MAX];

  char context[64];
  bugsnag_severity severity;

  char session_id[33];
  char session_start[33];
  int handled_events;
  int unhandled_events;
  char grouping_hash[64];
  bool unhandled;
  char api_key[64];
} bugsnag_report_v6;

typedef struct {
  bsg_notifier notifier;
  bsg_app_info_v3 app;
  bsg_device_info_v2 device;
  bugsnag_user user;
  bsg_error_v1 error;
  bugsnag_metadata_v1 metadata;

  int crumb_count;
  // Breadcrumbs are a ring; the first index moves as the
  // structure is filled and replaced.
  int crumb_first_index;
  bugsnag_breadcrumb_v2 breadcrumbs[BUGSNAG_CRUMBS_MAX];

  char context[64];
  bugsnag_severity severity;

  char session_id[33];
  char session_start[33];
  int handled_events;
  int unhandled_events;
  char grouping_hash[64];
  bool unhandled;
  char api_key[64];

  int thread_count;
  bsg_thread threads[BUGSNAG_THREADS_MAX];
} bugsnag_report_v7;

typedef struct {
  bsg_notifier notifier;
  bsg_app_info app;
  bsg_device_info device;
  bugsnag_user user;
  bsg_error_v1 error;
  bugsnag_metadata_v1 metadata;

  int crumb_count;
  // Breadcrumbs are a ring; the first index moves as the
  // structure is filled and replaced.
  int crumb_first_index;
  bugsnag_breadcrumb_v2 breadcrumbs[BUGSNAG_CRUMBS_MAX];

  char context[64];
  bugsnag_severity severity;

  char session_id[33];
  char session_start[33];
  int handled_events;
  int unhandled_events;
  char grouping_hash[64];
  bool unhandled;
  char api_key[64];

  int thread_count;
  bsg_thread threads[BUGSNAG_THREADS_MAX];

  size_t feature_flag_count;
  bsg_feature_flag *feature_flags;
} bugsnag_report_v8;

typedef struct {
  bsg_notifier notifier;
  bsg_app_info app;
  bsg_device_info device;
  bugsnag_user user;
  bsg_error_v1 error;
  bugsnag_metadata metadata;

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

  int thread_count;
  bsg_thread threads[BUGSNAG_THREADS_MAX];

  /**
   * The number of feature flags currently specified.
   */
  size_t feature_flag_count;

  /**
   * Pointer to the current feature flags. This is dynamically allocated and
   * serialized/deserialized separately to the rest of the struct.
   */
  bsg_feature_flag *feature_flags;
} bugsnag_report_v9;

#ifdef __cplusplus
}
#endif
#endif
