#include "event.h"
#include "crashtime_journal.h"
#include "event_cache.h"
#include "utils/string.h"
#include <string.h>

void bugsnag_event_add_metadata_double(void *event_ptr, const char *section,
                                       const char *name, double value) {
  bsg_ctj_add_metadata_double(section, name, value);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_cache_add_metadata_value_double(&event->metadata, section, name, value);
}

void bugsnag_event_add_metadata_string(void *event_ptr, const char *section,
                                       const char *name, const char *value) {
  bsg_ctj_add_metadata_string(section, name, value);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_cache_add_metadata_value_string(&event->metadata, section, name, value);
}

void bugsnag_event_add_metadata_bool(void *event_ptr, const char *section,
                                     const char *name, bool value) {
  bsg_ctj_add_metadata_bool(section, name, value);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_cache_add_metadata_value_bool(&event->metadata, section, name, value);
}

void bugsnag_event_clear_metadata(void *event_ptr, const char *section,
                                  const char *name) {
  bsg_ctj_clear_metadata(section, name);
  bsg_cache_clear_metadata(event_ptr, section, name);
}

void bugsnag_event_clear_metadata_section(void *event_ptr,
                                          const char *section) {
  bsg_ctj_clear_metadata_section(section);
  bsg_cache_clear_metadata_section(event_ptr, section);
}

bsg_metadata_value bugsnag_get_metadata_value(void *event_ptr,
                                              const char *section,
                                              const char *name) {
  return bsg_cache_get_metadata_value(event_ptr, section, name);
}

bugsnag_metadata_type bugsnag_event_has_metadata(void *event_ptr,
                                                 const char *section,
                                                 const char *name) {
  return bsg_cache_has_metadata(event_ptr, section, name);
}

double bugsnag_event_get_metadata_double(void *event_ptr, const char *section,
                                         const char *name) {
  return bsg_cache_get_metadata_double(event_ptr, section, name);
}

char *bugsnag_event_get_metadata_string(void *event_ptr, const char *section,
                                        const char *name) {
  return bsg_cache_get_metadata_string(event_ptr, section, name);
}

bool bugsnag_event_get_metadata_bool(void *event_ptr, const char *section,
                                     const char *name) {
  return bsg_cache_get_metadata_bool(event_ptr, section, name);
}

char *bugsnag_event_get_api_key(void *event_ptr) {
  return bsg_cache_get_api_key(event_ptr);
}

void bugsnag_event_set_api_key(void *event_ptr, const char *value) {
  bsg_ctj_set_api_key(value);
  bsg_cache_set_api_key(event_ptr, value);
}

char *bugsnag_event_get_context(void *event_ptr) {
  return bsg_cache_get_context(event_ptr);
}

void bugsnag_event_set_context(void *event_ptr, const char *value) {
  bsg_ctj_set_context(value);
  bsg_cache_set_context(event_ptr, value);
}

void bugsnag_event_set_user(void *event_ptr, const char *id, const char *email,
                            const char *name) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->user.id, id, sizeof(event->user.id));
  bsg_strncpy_safe(event->user.email, email, sizeof(event->user.email));
  bsg_strncpy_safe(event->user.name, name, sizeof(event->user.name));
}

void bugsnag_event_add_breadcrumb(bugsnag_event *event,
                                  bugsnag_breadcrumb *crumb) {
  int crumb_index;
  if (event->crumb_count < BUGSNAG_CRUMBS_MAX) {
    crumb_index = event->crumb_count;
    event->crumb_count++;
  } else {
    crumb_index = event->crumb_first_index;
    event->crumb_first_index =
        (event->crumb_first_index + 1) % BUGSNAG_CRUMBS_MAX;
  }
  memcpy(&event->breadcrumbs[crumb_index], crumb, sizeof(bugsnag_breadcrumb));
}

void bugsnag_event_clear_breadcrumbs(bugsnag_event *event) {
  event->crumb_count = 0;
  event->crumb_first_index = 0;
}

bool bugsnag_event_has_session(const bugsnag_event *event) {
  return strlen(event->session_id) > 0;
}

/* Accessors for event.app */

char *bugsnag_app_get_binary_arch(void *event_ptr) {
  return bsg_cache_get_binary_arch(event_ptr);
}

void bugsnag_app_set_binary_arch(void *event_ptr, const char *value) {
  bsg_ctj_set_binary_arch(value);
  bsg_cache_set_binary_arch(event_ptr, value);
}

char *bugsnag_app_get_build_uuid(void *event_ptr) {
  return bsg_cache_get_build_uuid(event_ptr);
}

void bugsnag_app_set_build_uuid(void *event_ptr, const char *value) {
  bsg_ctj_set_build_uuid(value);
  bsg_cache_set_build_uuid(event_ptr, value);
}

char *bugsnag_app_get_id(void *event_ptr) {
  return bsg_cache_get_app_id(event_ptr);
}

void bugsnag_app_set_id(void *event_ptr, const char *value) {
  bsg_ctj_set_app_id(value);
  bsg_cache_set_app_id(event_ptr, value);
}

char *bugsnag_app_get_release_stage(void *event_ptr) {
  return bsg_cache_get_app_release_stage(event_ptr);
}

void bugsnag_app_set_release_stage(void *event_ptr, const char *value) {
  bsg_ctj_set_app_release_stage(value);
  bsg_cache_set_app_release_stage(event_ptr, value);
}

char *bugsnag_app_get_type(void *event_ptr) {
  return bsg_cache_get_app_type(event_ptr);
}

void bugsnag_app_set_type(void *event_ptr, const char *value) {
  bsg_ctj_set_app_type(value);
  bsg_cache_set_app_type(event_ptr, value);
}

char *bugsnag_app_get_version(void *event_ptr) {
  return bsg_cache_get_app_version(event_ptr);
}

void bugsnag_app_set_version(void *event_ptr, const char *value) {
  bsg_ctj_set_app_version(value);
  bsg_cache_set_app_version(event_ptr, value);
}

int bugsnag_app_get_version_code(void *event_ptr) {
  return bsg_cache_get_app_version_code(event_ptr);
}

void bugsnag_app_set_version_code(void *event_ptr, int value) {
  bsg_ctj_set_app_version_code(value);
  bsg_cache_set_app_version_code(event_ptr, value);
}

time_t bugsnag_app_get_duration(void *event_ptr) {
  return bsg_cache_get_app_duration(event_ptr);
}

void bugsnag_app_set_duration(void *event_ptr, time_t value) {
  bsg_ctj_set_app_duration(value);
  bsg_cache_set_app_duration(event_ptr, value);
}

time_t bugsnag_app_get_duration_in_foreground(void *event_ptr) {
  return bsg_cache_get_app_duration_in_foreground(event_ptr);
}

void bugsnag_app_set_duration_in_foreground(void *event_ptr, time_t value) {
  bsg_ctj_set_app_duration_in_foreground(value);
  bsg_cache_set_app_duration_in_foreground(event_ptr, value);
}

bool bugsnag_app_get_in_foreground(void *event_ptr) {
  return bsg_cache_get_app_in_foreground(event_ptr);
}

void bugsnag_app_set_in_foreground(void *event_ptr, bool value) {
  bsg_ctj_set_app_in_foreground(value);
  bsg_cache_set_app_in_foreground(event_ptr, value);
}

bool bugsnag_app_get_is_launching(void *event_ptr) {
  return bsg_cache_get_app_is_launching(event_ptr);
}

void bugsnag_app_set_is_launching(void *event_ptr, bool value) {
  bsg_ctj_set_app_is_launching(value);
  bsg_cache_set_app_is_launching(event_ptr, value);
}

/* Accessors for event.device */

bool bugsnag_device_get_jailbroken(void *event_ptr) {
  return bsg_cache_get_jailbroken(event_ptr);
}

void bugsnag_device_set_jailbroken(void *event_ptr, bool value) {
  bsg_ctj_set_jailbroken(value);
  bsg_cache_set_jailbroken(event_ptr, value);
}

char *bugsnag_device_get_id(void *event_ptr) {
  return bsg_cache_get_device_id(event_ptr);
}

void bugsnag_device_set_id(void *event_ptr, const char *value) {
  bsg_ctj_set_device_id(value);
  bsg_cache_set_device_id(event_ptr, value);
}

char *bugsnag_device_get_locale(void *event_ptr) {
  return bsg_cache_get_locale(event_ptr);
}

void bugsnag_device_set_locale(void *event_ptr, const char *value) {
  bsg_ctj_set_locale(value);
  bsg_cache_set_locale(event_ptr, value);
}

char *bugsnag_device_get_manufacturer(void *event_ptr) {
  return bsg_cache_get_manufacturer(event_ptr);
}

void bugsnag_device_set_manufacturer(void *event_ptr, const char *value) {
  bsg_ctj_set_manufacturer(value);
  bsg_cache_set_manufacturer(event_ptr, value);
}

char *bugsnag_device_get_model(void *event_ptr) {
  return bsg_cache_get_device_model(event_ptr);
}

void bugsnag_device_set_model(void *event_ptr, const char *value) {
  bsg_ctj_set_device_model(value);
  bsg_cache_set_device_model(event_ptr, value);
}

char *bugsnag_device_get_os_version(void *event_ptr) {
  return bsg_cache_get_os_version(event_ptr);
}

void bugsnag_device_set_os_version(void *event_ptr, const char *value) {
  bsg_ctj_set_os_version(value);
  bsg_cache_set_os_version(event_ptr, value);
}

long bugsnag_device_get_total_memory(void *event_ptr) {
  return bsg_cache_get_total_memory(event_ptr);
}

void bugsnag_device_set_total_memory(void *event_ptr, long value) {
  bsg_ctj_set_total_memory(value);
  bsg_cache_set_total_memory(event_ptr, value);
}

char *bugsnag_device_get_orientation(void *event_ptr) {
  return bsg_cache_get_orientation(event_ptr);
}

void bugsnag_device_set_orientation(void *event_ptr, const char *value) {
  bsg_ctj_set_orientation(value);
  bsg_cache_set_orientation(event_ptr, value);
}

time_t bugsnag_device_get_time(void *event_ptr) {
  return bsg_cache_get_device_time(event_ptr);
}

void bugsnag_device_set_time(void *event_ptr, time_t value) {
  bsg_ctj_set_device_time(value);
  bsg_cache_set_device_time(event_ptr, value);
}

char *bugsnag_device_get_os_name(void *event_ptr) {
  return bsg_cache_get_os_name(event_ptr);
}

void bugsnag_device_set_os_name(void *event_ptr, const char *value) {
  bsg_ctj_set_os_name(value);
  bsg_cache_set_os_name(event_ptr, value);
}

char *bugsnag_error_get_error_class(void *event_ptr) {
  return bsg_cache_get_error_class(event_ptr);
}

void bugsnag_error_set_error_class(void *event_ptr, const char *value) {
  bsg_ctj_set_error_class(value);
  bsg_cache_set_error_class(event_ptr, value);
}

char *bugsnag_error_get_error_message(void *event_ptr) {
  return bsg_cache_get_error_message(event_ptr);
}

void bugsnag_error_set_error_message(void *event_ptr, const char *value) {
  bsg_ctj_set_error_message(value);
  bsg_cache_set_error_message(event_ptr, value);
}

char *bugsnag_error_get_error_type(void *event_ptr) {
  return bsg_cache_get_error_type(event_ptr);
}

void bugsnag_error_set_error_type(void *event_ptr, const char *value) {
  bsg_ctj_set_error_type(value);
  bsg_cache_set_error_type(event_ptr, value);
}

bugsnag_severity bugsnag_event_get_severity(void *event_ptr) {
  return bsg_cache_get_event_severity(event_ptr);
}

void bugsnag_event_set_severity(void *event_ptr, bugsnag_severity value) {
  bsg_ctj_set_event_severity(value);
  bsg_cache_set_event_severity(event_ptr, value);
}

bool bugsnag_event_is_unhandled(void *event_ptr) {
  return bsg_cache_is_event_unhandled(event_ptr);
}

void bugsnag_event_set_unhandled(void *event_ptr, bool value) {
  bsg_ctj_set_event_unhandled(value);
  bsg_cache_set_event_unhandled(event_ptr, value);
}

bugsnag_user bugsnag_event_get_user(void *event_ptr) {
  return bsg_cache_get_user(event_ptr);
}

char *bugsnag_event_get_grouping_hash(void *event_ptr) {
  return bsg_cache_get_grouping_hash(event_ptr);
}

void bugsnag_event_set_grouping_hash(void *event_ptr, const char *value) {
  bsg_ctj_set_grouping_hash(value);
  bsg_cache_set_grouping_hash(event_ptr, value);
}

int bugsnag_event_get_stacktrace_size(void *event_ptr) {
  return bsg_cache_get_error_stacktrace_size(event_ptr);
}

bugsnag_stackframe *bugsnag_event_get_stackframe(void *event_ptr, int index) {
  return bsg_cache_get_error_stackframe(event_ptr, index);
}
