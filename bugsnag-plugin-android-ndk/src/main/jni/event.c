#include "event.h"
#include "utils/string.h"
#include <string.h>

int bsg_find_next_free_metadata_index(bugsnag_event *event) {
  if (event->metadata.value_count < BUGSNAG_METADATA_MAX) {
    return event->metadata.value_count;
  } else {
    for (int i = 0; i < event->metadata.value_count; i++) {
      if (event->metadata.values[i].type == BSG_NONE_VALUE) {
        return i;
      }
    }
  }
  return -1;
}

int bugsnag_event_add_metadata_value(bugsnag_event *event, char *section,
                                     char *name) {
  int index = bsg_find_next_free_metadata_index(event);
  if (index < 0) {
    return index;
  }
  bsg_strncpy_safe(event->metadata.values[index].section, section,
                   sizeof(event->metadata.values[index].section));
  bsg_strncpy_safe(event->metadata.values[index].name, name,
                   sizeof(event->metadata.values[index].name));
  if (event->metadata.value_count < BUGSNAG_METADATA_MAX) {
    event->metadata.value_count = index + 1;
  }
  return index;
}
void bugsnag_event_add_metadata_double(bugsnag_event *event, char *section,
                                       char *name, double value) {
  int index = bugsnag_event_add_metadata_value(event, section, name);
  if (index >= 0) {
    event->metadata.values[index].type = BSG_NUMBER_VALUE;
    event->metadata.values[index].double_value = value;
  }
}

void bugsnag_event_add_metadata_string(bugsnag_event *event, char *section,
                                       char *name, char *value) {
  int index = bugsnag_event_add_metadata_value(event, section, name);
  if (index >= 0) {
    event->metadata.values[index].type = BSG_CHAR_VALUE;
    bsg_strncpy_safe(event->metadata.values[index].char_value, value,
                     sizeof(event->metadata.values[index].char_value));
  }
}

void bugsnag_event_add_metadata_bool(bugsnag_event *event, char *section,
                                     char *name, bool value) {
  int index = bugsnag_event_add_metadata_value(event, section, name);
  if (index >= 0) {
    event->metadata.values[index].type = BSG_BOOL_VALUE;
    event->metadata.values[index].bool_value = value;
  }
}

void bugsnag_event_remove_metadata(bugsnag_event *event, char *section,
                                   char *name) {
  for (int i = 0; i < event->metadata.value_count; ++i) {
    if (strcmp(event->metadata.values[i].section, section) == 0 &&
        strcmp(event->metadata.values[i].name, name) == 0) {
      memcpy(&event->metadata.values[i],
             &event->metadata.values[event->metadata.value_count - 1],
             sizeof(bsg_metadata_value));
      event->metadata.values[event->metadata.value_count - 1].type =
          BSG_NONE_VALUE;
      event->metadata.value_count--;
      break;
    }
  }
}

void bugsnag_event_remove_metadata_tab(bugsnag_event *event, char *section) {
  for (int i = 0; i < event->metadata.value_count; ++i) {
    if (strcmp(event->metadata.values[i].section, section) == 0) {
      event->metadata.values[i].type = BSG_NONE_VALUE;
    }
  }
}

void bugsnag_event_start_session(bugsnag_event *event, char *session_id,
                                 char *started_at, int handled_count, int unhandled_count) {
  bsg_strncpy_safe(event->session_id, session_id, sizeof(event->session_id));
  bsg_strncpy_safe(event->session_start, started_at,
                   sizeof(event->session_start));
  event->handled_events = handled_count;
  event->unhandled_events = unhandled_count;
}

char *bugsnag_event_get_context(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->context;
}

void bugsnag_event_set_context(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->context, value, sizeof(event->context));
}

void bugsnag_event_set_user_email(bugsnag_event *event, char *value) {
  bsg_strncpy_safe(event->user.email, value, sizeof(event->user.email));
}

void bugsnag_event_set_user_name(bugsnag_event *event, char *value) {
  bsg_strncpy_safe(event->user.name, value, sizeof(event->user.name));
}

void bugsnag_event_set_user_id(bugsnag_event *event, char *value) {
  bsg_strncpy_safe(event->user.id, value, sizeof(event->user.id));
}

void bugsnag_event_set_user(void *event_ptr, char* id, char* email, char* name) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bugsnag_event_set_user_id(event, id);
  bugsnag_event_set_user_email(event, email);
  bugsnag_event_set_user_name(event, name);
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
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->app.binaryArch;
}

void bugsnag_app_set_binary_arch(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->app.binaryArch, value, sizeof(event->app.binaryArch));
}

char *bugsnag_app_get_build_uuid(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->app.build_uuid;
}

void bugsnag_app_set_build_uuid(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->app.build_uuid, value, sizeof(event->app.build_uuid));
}

char *bugsnag_app_get_id(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->app.id;
}

void bugsnag_app_set_id(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->app.id, value, sizeof(event->app.id));
}

char *bugsnag_app_get_release_stage(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->app.release_stage;
}

void bugsnag_app_set_release_stage(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->app.release_stage, value, sizeof(event->app.release_stage));
}

char *bugsnag_app_get_type(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->app.type;
}

void bugsnag_app_set_type(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->app.type, value, sizeof(event->app.type));
}

char *bugsnag_app_get_version(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->app.version;
}

void bugsnag_app_set_version(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->app.version, value, sizeof(event->app.version));
}

int bugsnag_app_get_version_code(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->app.version_code;
}

void bugsnag_app_set_version_code(void *event_ptr, int value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  event->app.version_code = value;
}

time_t bugsnag_app_get_duration(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->app.duration;
}

void bugsnag_app_set_duration(void *event_ptr, time_t value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  event->app.duration = value;
}

time_t bugsnag_app_get_duration_in_foreground(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->app.duration_in_foreground;
}

void bugsnag_app_set_duration_in_foreground(void *event_ptr, time_t value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  event->app.duration_in_foreground = value;
}

bool bugsnag_app_get_in_foreground(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->app.in_foreground;
}

void bugsnag_app_set_in_foreground(void *event_ptr, bool value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  event->app.in_foreground = value;
}


/* Accessors for event.device */


bool bugsnag_device_get_jailbroken(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->device.jailbroken;
}

void bugsnag_device_set_jailbroken(void *event_ptr, bool value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  event->device.jailbroken = value;
}

char *bugsnag_device_get_id(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->device.id;
}

void bugsnag_device_set_id(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->device.id, value, sizeof(event->device.id));
}

char *bugsnag_device_get_locale(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->device.locale;
}

void bugsnag_device_set_locale(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->device.locale, value, sizeof(event->device.locale));
}

char *bugsnag_device_get_manufacturer(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->device.manufacturer;
}

void bugsnag_device_set_manufacturer(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->device.manufacturer, value, sizeof(event->device.manufacturer));
}

char *bugsnag_device_get_model(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->device.model;
}

void bugsnag_app_set_model(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->device.model, value, sizeof(event->device.model));
}

char *bugsnag_device_get_os_version(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->device.os_version;
}

void bugsnag_device_set_os_version(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->device.os_version, value, sizeof(event->device.os_version));
}

long bugsnag_device_get_total_memory(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->device.total_memory;
}

void bugsnag_device_set_total_memory(void *event_ptr, long value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  event->device.total_memory = value;
}

char *bugsnag_device_get_orientation(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->device.orientation;
}

void bugsnag_device_set_orientation(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->device.orientation, value, sizeof(event->device.orientation));
}

time_t bugsnag_device_get_time(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->device.time;
}

void bugsnag_device_set_time(void *event_ptr, time_t value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  event->device.time = value;
}

char *bugsnag_device_get_os_name(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->device.os_name;
}

void bugsnag_device_set_os_name(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->device.os_name, value, sizeof(event->device.os_name));
}

char *bugsnag_error_get_error_class(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->error.errorClass;
}

void bugsnag_error_set_error_class(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->error.errorClass, value, sizeof(event->error.errorClass));
}

char *bugsnag_error_get_error_message(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->error.errorMessage;
}

void bugsnag_error_set_error_message(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->error.errorMessage, value, sizeof(event->error.errorMessage));
}

char *bugsnag_error_get_error_type(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->error.type;
}

void bugsnag_error_set_error_type(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->error.type, value, sizeof(event->error.type));
}

bsg_severity_t bugsnag_event_get_severity(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->severity;
}

void bugsnag_event_set_severity(void *event_ptr, bsg_severity_t value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  event->severity = value;
}

bool bugsnag_event_is_unhandled(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->unhandled;
}

bsg_user_t bugsnag_event_get_user(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->user;
}
