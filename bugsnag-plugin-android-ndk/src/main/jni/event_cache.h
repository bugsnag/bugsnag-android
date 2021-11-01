// This code handles caching of data so that it can still be retrieved from a
// crashed C context. We can't fetch from the journal under async-safe
// conditions, so we keep any data that can be retrieved using public C APIs in
// here.

#ifndef BUGSNAG_ANDROID_EVENT_CACHE_H
#define BUGSNAG_ANDROID_EVENT_CACHE_H

#include "event.h"

char *bsg_default_os_name();

// The following functions mirror the public API in event.h

void bsg_cache_set_metadata_double(bugsnag_metadata *metadata,
                                   const char *section, const char *name,
                                   double value);

void bsg_cache_set_metadata_string(bugsnag_metadata *metadata,
                                   const char *section, const char *name,
                                   const char *value);

void bsg_cache_set_metadata_bool(bugsnag_metadata *metadata,
                                 const char *section, const char *name,
                                 bool value);

void bsg_cache_clear_metadata(void *event_ptr, const char *section,
                              const char *name);

void bsg_cache_clear_metadata_section(void *event_ptr, const char *section);

bugsnag_metadata_type
bsg_cache_has_metadata(void *event_ptr, const char *section, const char *name);

double bsg_cache_get_metadata_double(void *event_ptr, const char *section,
                                     const char *name);

char *bsg_cache_get_metadata_string(void *event_ptr, const char *section,
                                    const char *name);

bool bsg_cache_get_metadata_bool(void *event_ptr, const char *section,
                                 const char *name);

void bsg_cache_start_session(bugsnag_event *event, char *session_id,
                             char *started_at, int handled_count,
                             int unhandled_count);

char *bsg_cache_get_api_key(void *event_ptr);

void bsg_cache_set_api_key(void *event_ptr, const char *value);

char *bsg_cache_get_event_context(void *event_ptr);

void bsg_cache_set_event_context(void *event_ptr, const char *value);

void bsg_cache_set_event_user(void *event_ptr, const char *id,
                              const char *email, const char *name);

void bsg_cache_add_breadcrumb(bugsnag_event *event, bugsnag_breadcrumb *crumb);

bool bsg_cache_has_session(const bugsnag_event *event);

char *bsg_cache_get_app_binary_arch(void *event_ptr);

void bsg_cache_set_app_binary_arch(void *event_ptr, const char *value);

char *bsg_cache_get_app_build_uuid(void *event_ptr);

void bsg_cache_set_app_build_uuid(void *event_ptr, const char *value);

char *bsg_cache_get_app_id(void *event_ptr);

void bsg_cache_set_app_id(void *event_ptr, const char *value);

char *bsg_cache_get_app_release_stage(void *event_ptr);

void bsg_cache_set_app_release_stage(void *event_ptr, const char *value);

char *bsg_cache_get_app_type(void *event_ptr);

void bsg_cache_set_app_type(void *event_ptr, const char *value);

char *bsg_cache_get_app_version(void *event_ptr);

void bsg_cache_set_app_version(void *event_ptr, const char *value);

int bsg_cache_get_app_version_code(void *event_ptr);

void bsg_cache_set_app_version_code(void *event_ptr, int value);

time_t bsg_cache_get_app_duration(void *event_ptr);

void bsg_cache_set_app_duration(void *event_ptr, time_t value);

time_t bsg_cache_get_app_duration_in_foreground(void *event_ptr);

void bsg_cache_set_app_duration_in_foreground(void *event_ptr, time_t value);

bool bsg_cache_get_app_in_foreground(void *event_ptr);

void bsg_cache_set_app_in_foreground(void *event_ptr, bool value);

bool bsg_cache_get_app_is_launching(void *event_ptr);

void bsg_cache_set_app_is_launching(void *event_ptr, bool value);

bool bsg_cache_get_device_jailbroken(void *event_ptr);

void bsg_cache_set_device_jailbroken(void *event_ptr, bool value);

char *bsg_cache_get_device_id(void *event_ptr);

void bsg_cache_set_device_id(void *event_ptr, const char *value);

char *bsg_cache_get_device_locale(void *event_ptr);

void bsg_cache_set_device_locale(void *event_ptr, const char *value);

char *bsg_cache_get_device_manufacturer(void *event_ptr);

void bsg_cache_set_device_manufacturer(void *event_ptr, const char *value);

char *bsg_cache_get_device_model(void *event_ptr);

void bsg_cache_set_device_model(void *event_ptr, const char *value);

char *bsg_cache_get_device_os_version(void *event_ptr);

void bsg_cache_set_device_os_version(void *event_ptr, const char *value);

long bsg_cache_get_device_total_memory(void *event_ptr);

void bsg_cache_set_device_total_memory(void *event_ptr, long value);

char *bsg_cache_get_device_orientation(void *event_ptr);

void bsg_cache_set_device_orientation(void *event_ptr, const char *value);

time_t bsg_cache_get_device_time(void *event_ptr);

void bsg_cache_set_device_time_seconds(void *event_ptr, time_t value);

char *bsg_cache_get_device_os_name(void *event_ptr);

void bsg_cache_set_device_os_name(void *event_ptr, const char *value);

char *bsg_cache_get_error_class(void *event_ptr);

void bsg_cache_set_error_class(void *event_ptr, const char *value);

char *bsg_cache_get_error_message(void *event_ptr);

void bsg_cache_set_error_message(void *event_ptr, const char *value);

char *bsg_cache_get_error_type(void *event_ptr);

void bsg_cache_set_error_type(void *event_ptr, const char *value);

bugsnag_severity bsg_cache_get_event_severity(void *event_ptr);

void bsg_cache_set_event_severity(void *event_ptr, bugsnag_severity value);

bool bsg_cache_is_event_unhandled(void *event_ptr);

void bsg_cache_set_event_unhandled(void *event_ptr, bool value);

bugsnag_user bsg_cache_get_event_user(void *event_ptr);

char *bsg_cache_get_event_grouping_hash(void *event_ptr);

void bsg_cache_set_event_grouping_hash(void *event_ptr, const char *value);

int bsg_cache_get_error_stacktrace_size(void *event_ptr);

bugsnag_stackframe *bsg_cache_get_error_stackframe(void *event_ptr, int index);

#endif // BUGSNAG_ANDROID_EVENT_CACHE_H
