//
// Created by Karl Stenerud on 06.10.21.
//

#ifndef BUGSNAG_ANDROID_CRASHTIME_JOURNAL_H
#define BUGSNAG_ANDROID_CRASHTIME_JOURNAL_H

#include "bugsnag_ndk.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Store the contents of an event to the journal.
 *
 * @param event The event to store.
 * @return true if the event was successfully stored.
 */
bool bsg_ctj_store_event(const bugsnag_event *event);

// The following functions mirror the public API in event.h

bool bsg_ctj_set_api_key(const char *api_key);

bool bsg_ctj_set_event_context(const char *context);

bool bsg_ctj_set_event_user(const char *id, const char *email,
                            const char *name);

bool bsg_ctj_set_app_binary_arch(const char *arch);

bool bsg_ctj_set_app_build_uuid(const char *uuid);

bool bsg_ctj_set_app_id(const char *id);

bool bsg_ctj_set_app_release_stage(const char *value);

bool bsg_ctj_set_app_type(const char *value);

bool bsg_ctj_set_app_version(const char *value);

bool bsg_ctj_set_app_version_code(int value);

bool bsg_ctj_set_app_duration(time_t value);

bool bsg_ctj_set_app_duration_in_foreground(time_t value);

bool bsg_ctj_set_app_in_foreground(bool value);

bool bsg_ctj_set_app_is_launching(bool value);

bool bsg_ctj_set_device_jailbroken(bool value);

bool bsg_ctj_set_device_id(const char *value);

bool bsg_ctj_set_device_locale(const char *value);

bool bsg_ctj_set_device_manufacturer(const char *value);

bool bsg_ctj_set_device_model(const char *value);

bool bsg_ctj_set_device_os_version(const char *value);

bool bsg_ctj_set_device_total_memory(long value);

bool bsg_ctj_set_device_orientation(const char *value);

bool bsg_ctj_set_device_time_seconds(time_t value);

bool bsg_ctj_set_device_os_name(const char *value);

bool bsg_ctj_set_error_class(const char *value);

bool bsg_ctj_set_error_message(const char *value);

bool bsg_ctj_set_error_type(const char *value);

bool bsg_ctj_set_event_severity(bugsnag_severity value);

bool bsg_ctj_set_event_unhandled(bool value);

bool bsg_ctj_set_event_grouping_hash(const char *value);

bool bsg_ctj_set_metadata_double(const char *section, const char *name,
                                 double value);

bool bsg_ctj_set_metadata_string(const char *section, const char *name,
                                 const char *value);

bool bsg_ctj_set_metadata_bool(const char *section, const char *name,
                               bool value);

bool bsg_ctj_clear_metadata(const char *section, const char *name);

bool bsg_ctj_clear_metadata_section(const char *section);

bool bsg_ctj_record_current_time(void);

#ifdef __cplusplus
}
#endif

#endif // BUGSNAG_ANDROID_CRASHTIME_JOURNAL_H
