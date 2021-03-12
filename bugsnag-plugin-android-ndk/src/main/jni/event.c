#include "event.h"
#include "utils/string.h"
#include <string.h>

int bsg_find_next_free_metadata_index(bugsnag_metadata *const metadata) {
  if (metadata->value_count < BUGSNAG_METADATA_MAX) {
    return metadata->value_count;
  } else {
    for (int i = 0; i < metadata->value_count; i++) {
      if (metadata->values[i].type == BSG_METADATA_NONE_VALUE) {
        return i;
      }
    }
  }
  return -1;
}

int bsg_allocate_metadata_index(bugsnag_metadata *metadata, const char *section,
                                const char *name) {
  int index = bsg_find_next_free_metadata_index(metadata);
  if (index < 0) {
    return index;
  }
  bsg_strncpy_safe(metadata->values[index].section, section,
                   sizeof(metadata->values[index].section));
  bsg_strncpy_safe(metadata->values[index].name, name,
                   sizeof(metadata->values[index].name));
  if (metadata->value_count < BUGSNAG_METADATA_MAX) {
    metadata->value_count = index + 1;
  }
  return index;
}

void bsg_add_metadata_value_double(bugsnag_metadata *metadata,
                                   const char *section, const char *name,
                                   double value) {
  int index = bsg_allocate_metadata_index(metadata, section, name);
  if (index >= 0) {
    metadata->values[index].type = BSG_METADATA_NUMBER_VALUE;
    metadata->values[index].double_value = value;
  }
}

void bsg_add_metadata_value_str(bugsnag_metadata *metadata, const char *section,
                                const char *name, const char *value) {
  int index = bsg_allocate_metadata_index(metadata, section, name);
  if (index >= 0) {
    metadata->values[index].type = BSG_METADATA_CHAR_VALUE;
    bsg_strncpy_safe(metadata->values[index].char_value, value,
                     sizeof(metadata->values[index].char_value));
  }
}

void bsg_add_metadata_value_bool(bugsnag_metadata *metadata,
                                 const char *section, const char *name,
                                 bool value) {
  int index = bsg_allocate_metadata_index(metadata, section, name);
  if (index >= 0) {
    metadata->values[index].type = BSG_METADATA_BOOL_VALUE;
    metadata->values[index].bool_value = value;
  }
}

void bugsnag_event_add_metadata_double(void *event_ptr, char *section,
                                       char *name, double value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_add_metadata_value_double(&event->metadata, section, name, value);
}

void bugsnag_event_add_metadata_string(void *event_ptr, char *section,
                                       char *name, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_add_metadata_value_str(&event->metadata, section, name, value);
}

void bugsnag_event_add_metadata_bool(void *event_ptr, char *section, char *name,
                                     bool value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_add_metadata_value_bool(&event->metadata, section, name, value);
}

void bugsnag_event_clear_metadata(void *event_ptr, char *section, char *name) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  for (int i = 0; i < event->metadata.value_count; ++i) {
    if (strcmp(event->metadata.values[i].section, section) == 0 &&
        strcmp(event->metadata.values[i].name, name) == 0) {
      memcpy(&event->metadata.values[i],
             &event->metadata.values[event->metadata.value_count - 1],
             sizeof(bsg_metadata_value));
      event->metadata.values[event->metadata.value_count - 1].type =
          BSG_METADATA_NONE_VALUE;
      event->metadata.value_count--;
      break;
    }
  }
}

void bugsnag_event_clear_metadata_section(void *event_ptr, char *section) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  for (int i = 0; i < event->metadata.value_count; ++i) {
    if (strcmp(event->metadata.values[i].section, section) == 0) {
      event->metadata.values[i].type = BSG_METADATA_NONE_VALUE;
    }
  }
}

bsg_metadata_value bugsnag_get_metadata_value(void *event_ptr, char *section,
                                              char *name) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;

  for (int k = 0; k < event->metadata.value_count; ++k) {
    bsg_metadata_value val = event->metadata.values[k];
    if (strcmp(val.section, section) == 0 && strcmp(val.name, name) == 0) {
      return val;
    }
  }
  bsg_metadata_value data;
  data.type = BSG_METADATA_NONE_VALUE;
  return data;
}

bugsnag_metadata_type bugsnag_event_has_metadata(void *event_ptr, char *section,
                                                 char *name) {
  return bugsnag_get_metadata_value(event_ptr, section, name).type;
}

double bugsnag_event_get_metadata_double(void *event_ptr, char *section,
                                         char *name) {
  bsg_metadata_value value =
      bugsnag_get_metadata_value(event_ptr, section, name);

  if (value.type == BSG_METADATA_NUMBER_VALUE) {
    return value.double_value;
  }
  return 0.0;
}

char *bugsnag_event_get_metadata_string(void *event_ptr, char *section,
                                        char *name) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;

  for (int k = 0; k < event->metadata.value_count; ++k) {
    if (strcmp(event->metadata.values[k].section, section) == 0 &&
        strcmp(event->metadata.values[k].name, name) == 0) {
      return event->metadata.values[k].char_value;
    }
  }
  return NULL;
}

bool bugsnag_event_get_metadata_bool(void *event_ptr, char *section,
                                     char *name) {
  bsg_metadata_value value =
      bugsnag_get_metadata_value(event_ptr, section, name);

  if (value.type == BSG_METADATA_BOOL_VALUE) {
    return value.bool_value;
  }
  return false;
}

void bugsnag_event_start_session(bugsnag_event *event, char *session_id,
                                 char *started_at, int handled_count,
                                 int unhandled_count) {
  bsg_strncpy_safe(event->session_id, session_id, sizeof(event->session_id));
  bsg_strncpy_safe(event->session_start, started_at,
                   sizeof(event->session_start));
  event->handled_events = handled_count;
  event->unhandled_events = unhandled_count;
}

char *bugsnag_event_get_api_key(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->api_key;
}

void bugsnag_event_set_api_key(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->api_key, value, sizeof(event->api_key));
}

char *bugsnag_event_get_context(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->context;
}

void bugsnag_event_set_context(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->context, value, sizeof(event->context));
}

void bugsnag_event_set_user(void *event_ptr, char *id, char *email,
                            char *name) {
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

bool bugsnag_event_has_session(bugsnag_event *event) {
  return strlen(event->session_id) > 0;
}

/* Accessors for event.app */

char *bugsnag_app_get_binary_arch(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.binary_arch;
}

void bugsnag_app_set_binary_arch(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->app.binary_arch, value,
                   sizeof(event->app.binary_arch));
}

char *bugsnag_app_get_build_uuid(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.build_uuid;
}

void bugsnag_app_set_build_uuid(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->app.build_uuid, value, sizeof(event->app.build_uuid));
}

char *bugsnag_app_get_id(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.id;
}

void bugsnag_app_set_id(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->app.id, value, sizeof(event->app.id));
}

char *bugsnag_app_get_release_stage(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.release_stage;
}

void bugsnag_app_set_release_stage(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->app.release_stage, value,
                   sizeof(event->app.release_stage));
}

char *bugsnag_app_get_type(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.type;
}

void bugsnag_app_set_type(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->app.type, value, sizeof(event->app.type));
}

char *bugsnag_app_get_version(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.version;
}

void bugsnag_app_set_version(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->app.version, value, sizeof(event->app.version));
}

int bugsnag_app_get_version_code(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.version_code;
}

void bugsnag_app_set_version_code(void *event_ptr, int value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.version_code = value;
}

time_t bugsnag_app_get_duration(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.duration;
}

void bugsnag_app_set_duration(void *event_ptr, time_t value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.duration = value;
}

time_t bugsnag_app_get_duration_in_foreground(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.duration_in_foreground;
}

void bugsnag_app_set_duration_in_foreground(void *event_ptr, time_t value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.duration_in_foreground = value;
}

bool bugsnag_app_get_in_foreground(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.in_foreground;
}

void bugsnag_app_set_in_foreground(void *event_ptr, bool value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.in_foreground = value;
}

bool bugsnag_app_get_is_launching(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.is_launching;
}

void bugsnag_app_set_is_launching(void *event_ptr, bool value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.is_launching = value;
}

/* Accessors for event.device */

bool bugsnag_device_get_jailbroken(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.jailbroken;
}

void bugsnag_device_set_jailbroken(void *event_ptr, bool value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->device.jailbroken = value;
}

char *bugsnag_device_get_id(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.id;
}

void bugsnag_device_set_id(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->device.id, value, sizeof(event->device.id));
}

char *bugsnag_device_get_locale(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.locale;
}

void bugsnag_device_set_locale(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->device.locale, value, sizeof(event->device.locale));
}

char *bugsnag_device_get_manufacturer(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.manufacturer;
}

void bugsnag_device_set_manufacturer(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->device.manufacturer, value,
                   sizeof(event->device.manufacturer));
}

char *bugsnag_device_get_model(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.model;
}

void bugsnag_device_set_model(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->device.model, value, sizeof(event->device.model));
}

char *bugsnag_device_get_os_version(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.os_version;
}

void bugsnag_device_set_os_version(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->device.os_version, value,
                   sizeof(event->device.os_version));
}

long bugsnag_device_get_total_memory(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.total_memory;
}

void bugsnag_device_set_total_memory(void *event_ptr, long value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->device.total_memory = value;
}

char *bugsnag_device_get_orientation(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.orientation;
}

void bugsnag_device_set_orientation(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->device.orientation, value,
                   sizeof(event->device.orientation));
}

time_t bugsnag_device_get_time(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.time;
}

void bugsnag_device_set_time(void *event_ptr, time_t value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->device.time = value;
}

char *bugsnag_device_get_os_name(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.os_name;
}

void bugsnag_device_set_os_name(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->device.os_name, value, sizeof(event->device.os_name));
}

char *bugsnag_error_get_error_class(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->error.errorClass;
}

void bugsnag_error_set_error_class(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->error.errorClass, value,
                   sizeof(event->error.errorClass));
}

char *bugsnag_error_get_error_message(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->error.errorMessage;
}

void bugsnag_error_set_error_message(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->error.errorMessage, value,
                   sizeof(event->error.errorMessage));
}

char *bugsnag_error_get_error_type(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->error.type;
}

void bugsnag_error_set_error_type(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->error.type, value, sizeof(event->error.type));
}

bugsnag_severity bugsnag_event_get_severity(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->severity;
}

void bugsnag_event_set_severity(void *event_ptr, bugsnag_severity value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->severity = value;
}

bool bugsnag_event_is_unhandled(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->unhandled;
}

void bugsnag_event_set_unhandled(void *event_ptr, bool value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->unhandled = value;
}

bugsnag_user bugsnag_event_get_user(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->user;
}

char *bugsnag_event_get_grouping_hash(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->grouping_hash;
}

void bugsnag_event_set_grouping_hash(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->grouping_hash, value, sizeof(event->grouping_hash));
}

int bugsnag_event_get_stacktrace_size(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->error.frame_count;
}

bugsnag_stackframe *bugsnag_event_get_stackframe(void *event_ptr, int index) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  if (index >= 0 && index < event->error.frame_count) {
    return &event->error.stacktrace[index];
  } else {
    return NULL;
  }
}
