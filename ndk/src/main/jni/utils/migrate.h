#ifndef BUGSNAG_MIGRATE_H
#define BUGSNAG_MIGRATE_H

#include "report.h"

#ifdef __cplusplus
extern "C" {
#endif

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
    bsg_library notifier;
    bsg_app_info_v1 app;
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
} bugsnag_report_v1;

typedef struct {
    bsg_library notifier;
    bsg_app_info_v1 app;
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
} bugsnag_report_v2;

#ifdef __cplusplus
}
#endif
#endif
