#ifndef BUGSNAG_MIGRATE_H
#define BUGSNAG_MIGRATE_H

#include "event.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * migrate.h contains definitions of structs that were used in previous versions of a notifier
 * release.
 *
 * Because these structs are serialized to disk directly, we need to retain the original structs
 * whenever a field is added or changed.
 *
 * The bsg_report_header indicates what version was serialized to disk. Knowing this information,
 * it is possible to migrate old payloads by first serializing them into an old struct, and then
 * mapping them into the latest version of bugsnag_event.
 */

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
    bsg_stackframe_t stacktrace[BUGSNAG_FRAMES_MAX];
} bsg_exception;

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
    bsg_user_t user;
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
} bugsnag_report_v1;

typedef struct {
    bsg_library notifier;
    bsg_app_info_v1 app;
    bsg_device_info_v1 device;
    bsg_user_t user;
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
} bugsnag_report_v2;

#ifdef __cplusplus
}
#endif
#endif
