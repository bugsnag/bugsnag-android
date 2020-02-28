#ifndef BUGSNAG_EVENT_H
#define BUGSNAG_EVENT_H

#include "../assets/include/event.h"
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
 *  Max number of breadcrumbs in an event. Configures a default if not defined.
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
 * Version of the bugsnag_event struct. Serialized to report header.
 */
#define BUGSNAG_EVENT_VERSION 3


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
    long total_memory;
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
    char name[32];
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
    char name[33];
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
} bugsnag_event;

void bugsnag_event_add_breadcrumb(bugsnag_event *event,
                                  bugsnag_breadcrumb *crumb);
void bugsnag_event_clear_breadcrumbs(bugsnag_event *event);
void bugsnag_event_start_session(bugsnag_event *event, char *session_id,
                                 char *started_at, int handled_count, int unhandled_count);
bool bugsnag_event_has_session(bugsnag_event *event);

void bsg_add_metadata_value_double(bugsnag_metadata *metadata, char *section,
                                   char *name, double value);
void bsg_add_metadata_value_str(bugsnag_metadata *metadata, char *section,
                                char *name, char *value);
void bsg_add_metadata_value_bool(bugsnag_metadata *metadata, char *section,
                                 char *name, bool value);

/*********************************
 * (end) NDK-SPECIFIC BITS
 *********************************/

#ifdef __cplusplus
}
#endif
#endif
