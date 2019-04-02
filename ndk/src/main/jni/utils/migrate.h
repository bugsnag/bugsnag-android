#ifndef BUGSNAG_MIGRATE_H
#define BUGSNAG_MIGRATE_H

#include "report.h"

#ifdef __cplusplus
extern "C" {
#endif

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

#ifdef __cplusplus
}
#endif
#endif
