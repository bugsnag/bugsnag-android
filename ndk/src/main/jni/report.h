#ifndef BUGSNAG_REPORT_H
#define BUGSNAG_REPORT_H

#include <stdbool.h>
#include <sys/types.h>
#ifndef BUGSNAG_METADATA_MAX
/**
 * Maximum number of values stored in metadata. Configures a default if not
 * defined.
 */
#define BUGSNAG_METADATA_MAX 128
#endif
#ifndef BUGSNAG_FRAMES_MAX
/**
 *  Number of frames in a stacktrace. Configures a default if not defined.
 */
#define BUGSNAG_FRAMES_MAX 192
#endif
#ifndef BUGSNAG_CRUMBS_MAX
/**
 *  Max number of breadcrumbs in a report. Configures a default if not defined.
 */
#define BUGSNAG_CRUMBS_MAX 30
#endif
#ifndef BUGSNAG_DEFAULT_EX_TYPE
/**
 * Type assigned to exceptions. Configures a default if not defined.
 */
#define BUGSNAG_DEFAULT_EX_TYPE "c"
#endif
/**
 * Version of the bugsnag_report struct. Serialized to report header.
 */
#define BUGSNAG_REPORT_VERSION 2

#define BUGSNAG_USER_INFO_LEN 64
#ifdef __cplusplus
extern "C" {
#endif

/*********************************
 * (start) NDK-SPECIFIC BITS
 *********************************/

typedef struct {
  uintptr_t frame_address;
  uintptr_t symbol_address;
  uintptr_t load_address;
  uintptr_t line_number;

  char filename[256];
  char method[256];
} bsg_stackframe;

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
  /**
   * The elapsed time in milliseconds between when the system clock starts and
   * when bugsnag-ndk install() is called
   */
  time_t duration_ms_offset;
  /**
   * The elapsed time in the foreground in milliseconds between when the app
   * first enters the foreground and when bugsnag-ndk install() is called, if
   * the app is in the foreground when install() occurs and the app never enters
   * the background. This value is zero in all other cases.
   */
  time_t duration_in_foreground_ms_offset;
  bool in_foreground;
  bool low_memory;
  size_t memory_usage;
  char binaryArch[32];
} bsg_app_info;

typedef struct {
  char value[32];
} bsg_cpu_abi;

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
} bsg_device_info;

/*********************************
 * (end) NDK-SPECIFIC BITS
 *********************************/

/**
 * Report versioning information, serialized to disk first in a report file,
 * including system info for potential debugging
 */
typedef struct {
  /**
   * The value of BUGSNAG_REPORT_VERSION
   */
  int version;
  /**
   * 0 if big endian
   */
  int big_endian;
  /**
   * The value of metaData.device.osBuild
   */
  char os_build[64];
} bsg_report_header;

typedef enum {
  /** An unhandled exception */
  BSG_SEVERITY_ERR,
  /** A handled exception */
  BSG_SEVERITY_WARN,
  /** Custom, notable error messages */
  BSG_SEVERITY_INFO,
} bsg_severity_t;

typedef enum {
  /**
   * Any breadcrumb recorded via \link bugsnag_leave_breadcrumb
   */
  BSG_CRUMB_MANUAL,
  /**
   * Call to bugsnag_notify*()
   * (Internal use only)
   */
  BSG_CRUMB_ERROR,
  /**
   * Log message
   */
  BSG_CRUMB_LOG,
  /**
   *  Navigation action, such as an overall change in the application view
   *  context
   */
  BSG_CRUMB_NAVIGATION,
  /**
   *  Background process, such performing a database query
   */
  BSG_CRUMB_PROCESS,
  /**
   *  Network request
   */
  BSG_CRUMB_REQUEST,
  /**
   *  Change in application state
   */
  BSG_CRUMB_STATE,
  /**
   *  User event, such as authentication or control events
   */
  BSG_CRUMB_USER,
} bsg_breadcrumb_t;

typedef enum {
  BSG_NONE_VALUE,
  BSG_BOOL_VALUE,
  BSG_CHAR_VALUE,
  BSG_NUMBER_VALUE,
} bsg_metadata_t;

/**
 * A single value in metadata
 */
typedef struct {
  /**
   * The key identifying this metadata entry
   */
  char name[32];
  /**
   * The metadata tab
   */
  char section[32];
  /**
   * The value type from bool, char, number
   */
  bsg_metadata_t type;

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
} bsg_metadata_value;

typedef struct {
  /** The number of values in use */
  int value_count;
  bsg_metadata_value values[BUGSNAG_METADATA_MAX];
} bugsnag_metadata;

typedef struct {
  char name[BUGSNAG_USER_INFO_LEN];
  char email[BUGSNAG_USER_INFO_LEN];
  char id[BUGSNAG_USER_INFO_LEN];
} bsg_user;

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
  bsg_stackframe stacktrace[BUGSNAG_FRAMES_MAX];
} bsg_exception;

typedef struct {
  char key[64];
  char value[64];
} bsg_char_metadata_pair;

typedef struct {
  char name[33];
  char timestamp[37];
  bsg_breadcrumb_t type;

  /**
   * Key/value pairs of related information for debugging
   */
  bsg_char_metadata_pair metadata[8];
} bugsnag_breadcrumb;

typedef struct {
  char name[64];
  char version[16];
  char url[64];
} bsg_library;

typedef struct {
  bsg_library notifier;
  bsg_app_info app;
  bsg_device_info device;
  bsg_user user;
  bsg_exception exception;
  bugsnag_metadata metadata;

  int crumb_count;
  // Breadcrumbs are a ring; the first index moves as the
  // structure is filled and replaced.
  int crumb_first_index;
  bugsnag_breadcrumb breadcrumbs[BUGSNAG_CRUMBS_MAX];

  char context[64];
  bsg_severity_t severity;

  char session_id[33];
  char session_start[33];
  int handled_events;
  int unhandled_events;
} bugsnag_report;

void bugsnag_report_add_metadata_double(bugsnag_report *report, char *section,
                                        char *name, double value);
void bugsnag_report_add_metadata_string(bugsnag_report *report, char *section,
                                        char *name, char *value);
void bugsnag_report_add_metadata_bool(bugsnag_report *report, char *section,
                                      char *name, bool value);
void bugsnag_report_add_breadcrumb(bugsnag_report *report,
                                   bugsnag_breadcrumb *crumb);
void bugsnag_report_clear_breadcrumbs(bugsnag_report *report);
void bugsnag_report_remove_metadata(bugsnag_report *report, char *section,
                                    char *name);
void bugsnag_report_remove_metadata_tab(bugsnag_report *report, char *section);
void bugsnag_report_set_context(bugsnag_report *report, char *value);
void bugsnag_report_set_orientation(bugsnag_report *report, int value);
void bugsnag_report_set_app_version(bugsnag_report *report, char *value);
void bugsnag_report_set_build_uuid(bugsnag_report *report, char *value);
void bugsnag_report_set_release_stage(bugsnag_report *report, char *value);
void bugsnag_report_set_user_email(bugsnag_report *report, char *value);
void bugsnag_report_set_user_id(bugsnag_report *report, char *value);
void bugsnag_report_set_user_name(bugsnag_report *report, char *value);
void bugsnag_report_start_session(bugsnag_report *report, char *session_id,
                                  char *started_at, int handled_count, int unhandled_count);
bool bugsnag_report_has_session(bugsnag_report *report);

#ifdef __cplusplus
}
#endif
#endif
