#ifndef BUGSNAG_MIGRATE_H
#define BUGSNAG_MIGRATE_H

#include "event.h"

#ifdef __cplusplus
extern "C" {
#endif

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
    bsg_stackframe stacktrace[BUGSNAG_FRAMES_MAX];
} bsg_exception;

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
} bugsnag_report_v1;

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
} bugsnag_report_v2;

#ifdef __cplusplus
}
#endif
#endif
