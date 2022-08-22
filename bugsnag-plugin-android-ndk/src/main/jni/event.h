#ifndef BUGSNAG_EVENT_H
#define BUGSNAG_EVENT_H

#include "../assets/include/event.h"
#include "bsg_unwind.h"
#include <stdbool.h>
#include <sys/types.h>
#ifndef BUGSNAG_METADATA_MAX
/**
 * Maximum number of values stored in metadata. Configures a default if not
 * defined.
 */
#define BUGSNAG_METADATA_MAX 128
#endif
#ifndef BUGSNAG_CRUMBS_MAX
/**
 *  Max number of breadcrumbs in an event. Configures a default if not defined.
 */
#define BUGSNAG_CRUMBS_MAX 50
#endif
#ifndef BUGSNAG_DEFAULT_EX_TYPE
/**
 * Type assigned to exceptions. Configures a default if not defined.
 */
#define BUGSNAG_DEFAULT_EX_TYPE "c"
#endif
#ifndef BUGSNAG_THREADS_MAX
/**
 * Maximum number of threads recorded for an event. Configures a default if not
 * defined.
 */
#define BUGSNAG_THREADS_MAX 255
#endif
/**
 * Version of the bugsnag_event struct. Serialized to report header.
 */
#define BUGSNAG_EVENT_VERSION 11

#ifdef __cplusplus
extern "C" {
#endif

/*********************************
 * (start) NDK-SPECIFIC BITS
 *********************************/

typedef struct {
  char id[64];
  char release_stage[64];
  char type[32];
  char version[32];
  char active_screen[64];
  int64_t version_code;
  char build_uuid[64];
  int64_t duration;
  int64_t duration_in_foreground;
  /**
   * The elapsed time in milliseconds between when the system clock starts and
   * when bugsnag-ndk install() is called
   */
  int64_t duration_ms_offset;
  /**
   * The elapsed time in the foreground in milliseconds between when the app
   * first enters the foreground and when bugsnag-ndk install() is called, if
   * the app is in the foreground when install() occurs and the app never enters
   * the background. This value is zero in all other cases.
   */
  time_t duration_in_foreground_ms_offset;
  bool in_foreground;
  bool is_launching;
  char binary_arch[32];
} bsg_app_info;

typedef struct {
  char value[32];
} bsg_cpu_abi;

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
  int64_t total_memory;
} bsg_device_info;

/**
 * Report versioning information, serialized to disk first in a report file,
 * including system info for potential debugging
 */
typedef struct {
  /**
   * The value of BUGSNAG_EVENT_VERSION
   */
  int version;
  /**
   * 0 if big endian
   */
  int big_endian;
  /**
   * The value of device.runtimeVersions.osBuild
   */
  char os_build[64];
} bsg_report_header;

/**
 * A single value in metadata
 */
typedef struct {
  /**
   * The key identifying this metadata entry
   */
  char name[64];
  /**
   * The metadata tab
   */
  char section[64];
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
  char char_value[64];
  /**
   * Value if type is BSG_DOUBLE_VALUE
   */
  double double_value;

  /**
   * Value if type is BSG_METADATA_OPAQUE_VALUE
   */
  void *opaque_value;

  /**
   * Length of the opaque_value cached here for performance
   */
  size_t opaque_value_size;
} bsg_metadata_value;

typedef struct {
  /** The number of values in use */
  int value_count;
  bsg_metadata_value values[BUGSNAG_METADATA_MAX];
} bugsnag_metadata;

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
  bugsnag_stackframe stacktrace[BUGSNAG_FRAMES_MAX];
} bsg_error;

typedef struct {
  char name[64];
  char timestamp[37];
  bugsnag_breadcrumb_type type;

  /**
   * Key/value pairs of related information for debugging
   */
  bugsnag_metadata metadata;
} bugsnag_breadcrumb;

typedef struct {
  char name[64];
  char version[16];
  char url[64];
} bsg_notifier;

typedef struct {
  pid_t id;
  char name[16];
  char state[13];
} bsg_thread;

typedef enum {
  SEND_THREADS_ALWAYS = 0,
  SEND_THREADS_UNHANDLED_ONLY = 1,
  SEND_THREADS_NEVER = 2
} bsg_thread_send_policy;

typedef struct {
  char *name;
  char *variant;
} bsg_feature_flag;

typedef struct {
  bsg_notifier notifier;
  bsg_app_info app;
  bsg_device_info device;
  bugsnag_user user;
  bsg_error error;
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

  /**
   * Flags to denote which native APIs have been called (see bsg_called_api).
   * This only records that at least one call was made per API; it doesn't tally
   * how many calls occurred.
   */
  uint64_t called_apis[2];
} bugsnag_event;

/**
 * APIs whose calls will be recorded.
 * The high bits (call / 64) represent the index into event->called_apis.
 * The low bits (call & 63) represent the bit index.
 *
 * The ordering of this enum must be consistent between the JNI and JVM side.
 * The ordering must not be changed. Add new APIs to the end, not in
 * alphabetical order. Naming of enums can be changed to denote deprecation
 * (e.g. BSG_API_APP_GET_ID_DEPRECATED), but their enum values must not be
 * re-used.
 */
typedef enum {
  BSG_API_ADD_ON_ERROR = 0,
  BSG_API_APP_GET_BINARY_ARCH,
  BSG_API_APP_GET_BUILD_UUID,
  BSG_API_APP_GET_DURATION,
  BSG_API_APP_GET_DURATION_IN_FOREGROUND,
  BSG_API_APP_GET_ID,
  BSG_API_APP_GET_IN_FOREGROUND,
  BSG_API_APP_GET_IS_LAUNCHING,
  BSG_API_APP_GET_RELEASE_STAGE,
  BSG_API_APP_GET_TYPE,
  BSG_API_APP_GET_VERSION,
  BSG_API_APP_GET_VERSION_CODE,
  BSG_API_APP_SET_BINARY_ARCH,
  BSG_API_APP_SET_BUILD_UUID,
  BSG_API_APP_SET_DURATION,
  BSG_API_APP_SET_DURATION_IN_FOREGROUND,
  BSG_API_APP_SET_ID,
  BSG_API_APP_SET_IN_FOREGROUND,
  BSG_API_APP_SET_IS_LAUNCHING,
  BSG_API_APP_SET_RELEASE_STAGE,
  BSG_API_APP_SET_TYPE,
  BSG_API_APP_SET_VERSION,
  BSG_API_APP_SET_VERSION_CODE,
  BSG_API_DEVICE_GET_ID,
  BSG_API_DEVICE_GET_JAILBROKEN,
  BSG_API_DEVICE_GET_LOCALE,
  BSG_API_DEVICE_GET_MANUFACTURER,
  BSG_API_DEVICE_GET_MODEL,
  BSG_API_DEVICE_GET_ORIENTATION,
  BSG_API_DEVICE_GET_OS_NAME,
  BSG_API_DEVICE_GET_OS_VERSION,
  BSG_API_DEVICE_GET_TIME,
  BSG_API_DEVICE_GET_TOTAL_MEMORY,
  BSG_API_DEVICE_SET_ID,
  BSG_API_DEVICE_SET_JAILBROKEN,
  BSG_API_DEVICE_SET_LOCALE,
  BSG_API_DEVICE_SET_MANUFACTURER,
  BSG_API_DEVICE_SET_MODEL,
  BSG_API_DEVICE_SET_ORIENTATION,
  BSG_API_DEVICE_SET_OS_NAME,
  BSG_API_DEVICE_SET_OS_VERSION,
  BSG_API_DEVICE_SET_TIME,
  BSG_API_DEVICE_SET_TOTAL_MEMORY,
  BSG_API_ERROR_GET_ERROR_CLASS,
  BSG_API_ERROR_GET_ERROR_MESSAGE,
  BSG_API_ERROR_GET_ERROR_TYPE,
  BSG_API_ERROR_SET_ERROR_CLASS,
  BSG_API_ERROR_SET_ERROR_MESSAGE,
  BSG_API_ERROR_SET_ERROR_TYPE,
  BSG_API_EVENT_ADD_METADATA_BOOL,
  BSG_API_EVENT_ADD_METADATA_DOUBLE,
  BSG_API_EVENT_ADD_METADATA_STRING,
  BSG_API_EVENT_CLEAR_METADATA,
  BSG_API_EVENT_CLEAR_METADATA_SECTION,
  BSG_API_EVENT_GET_API_KEY,
  BSG_API_EVENT_GET_CONTEXT,
  BSG_API_EVENT_GET_GROUPING_HASH,
  BSG_API_EVENT_GET_METADATA_BOOL,
  BSG_API_EVENT_GET_METADATA_DOUBLE,
  BSG_API_EVENT_GET_METADATA_STRING,
  BSG_API_EVENT_GET_SEVERITY,
  BSG_API_EVENT_GET_STACKFRAME,
  BSG_API_EVENT_GET_STACKTRACE_SIZE,
  BSG_API_EVENT_GET_USER,
  BSG_API_EVENT_HAS_METADATA,
  BSG_API_EVENT_IS_UNHANDLED,
  BSG_API_EVENT_SET_API_KEY,
  BSG_API_EVENT_SET_CONTEXT,
  BSG_API_EVENT_SET_GROUPING_HASH,
  BSG_API_EVENT_SET_SEVERITY,
  BSG_API_EVENT_SET_UNHANDLED,
  BSG_API_EVENT_SET_USER,
} bsg_called_api;

void bsg_notify_api_called(void *event_ptr, bsg_called_api api);

void bsg_event_add_breadcrumb(bugsnag_event *event, bugsnag_breadcrumb *crumb);
void bsg_event_start_session(bugsnag_event *event, const char *session_id,
                             const char *started_at, int handled_count,
                             int unhandled_count);
bool bsg_event_has_session(const bugsnag_event *event);

void bsg_add_metadata_value_double(bugsnag_metadata *metadata,
                                   const char *section, const char *name,
                                   double value);
void bsg_add_metadata_value_str(bugsnag_metadata *metadata, const char *section,
                                const char *name, const char *value);
void bsg_add_metadata_value_bool(bugsnag_metadata *metadata,
                                 const char *section, const char *name,
                                 bool value);
void bsg_add_metadata_value_opaque(bugsnag_metadata *metadata,
                                   const char *section, const char *name,
                                   const char *json);

/*********************************
 * (end) NDK-SPECIFIC BITS
 *********************************/

#ifdef __cplusplus
}
#endif
#endif
