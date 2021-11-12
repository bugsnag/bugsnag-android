// Note: This code used to be used to fill out structs that would be persisted
// to disk and then read back on relaunch. It has now been re-purposed as a
// cache, and as a result there will likely be some leftover cruft here and
// there, as well as things that could be better optimised in future after
// enough time has passed to migrate older on-disk reports out in the field.

#include "event_cache.h"
#include "utils/string.h"
#include <memory.h>

static int find_next_free_metadata_index(bugsnag_metadata *const metadata) {
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

static int allocate_metadata_index(bugsnag_metadata *metadata,
                                   const char *section, const char *name) {
  int index = find_next_free_metadata_index(metadata);
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

// Public API

char *bsg_default_os_name() { return "android"; }

void bsg_cache_set_metadata_double(bugsnag_metadata *metadata,
                                   const char *section, const char *name,
                                   double value) {
  int index = allocate_metadata_index(metadata, section, name);
  if (index >= 0) {
    metadata->values[index].type = BSG_METADATA_NUMBER_VALUE;
    metadata->values[index].double_value = value;
  }
}

void bsg_cache_set_metadata_string(bugsnag_metadata *metadata,
                                   const char *section, const char *name,
                                   const char *value) {
  int index = allocate_metadata_index(metadata, section, name);
  if (index >= 0) {
    metadata->values[index].type = BSG_METADATA_CHAR_VALUE;
    bsg_strncpy_safe(metadata->values[index].char_value, value,
                     sizeof(metadata->values[index].char_value));
  }
}

void bsg_cache_set_metadata_bool(bugsnag_metadata *metadata,
                                 const char *section, const char *name,
                                 bool value) {
  int index = allocate_metadata_index(metadata, section, name);
  if (index >= 0) {
    metadata->values[index].type = BSG_METADATA_BOOL_VALUE;
    metadata->values[index].bool_value = value;
  }
}

void bsg_cache_clear_metadata(void *event_ptr, const char *section,
                              const char *name) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  for (int i = 0; i < event->metadata.value_count; ++i) {
    if (strncmp(event->metadata.values[i].section, section,
                sizeof(event->metadata.values[i].section)) == 0 &&
        strncmp(event->metadata.values[i].name, name,
                sizeof(event->metadata.values[i].name)) == 0) {
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

void bsg_cache_clear_metadata_section(void *event_ptr, const char *section) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  for (int i = 0; i < event->metadata.value_count; ++i) {
    if (strncmp(event->metadata.values[i].section, section,
                sizeof(event->metadata.values[i].section)) == 0) {
      event->metadata.values[i].type = BSG_METADATA_NONE_VALUE;
    }
  }
}

static bsg_metadata_value
get_metadata_value(void *event_ptr, const char *section, const char *name) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;

  for (int k = 0; k < event->metadata.value_count; ++k) {
    bsg_metadata_value val = event->metadata.values[k];
    if (strncmp(val.section, section, sizeof(val.section)) == 0 &&
        strncmp(val.name, name, sizeof(val.name)) == 0) {
      return val;
    }
  }
  bsg_metadata_value data;
  data.type = BSG_METADATA_NONE_VALUE;
  return data;
}

bugsnag_metadata_type
bsg_cache_has_metadata(void *event_ptr, const char *section, const char *name) {
  return get_metadata_value(event_ptr, section, name).type;
}

double bsg_cache_get_metadata_double(void *event_ptr, const char *section,
                                     const char *name) {
  bsg_metadata_value value = get_metadata_value(event_ptr, section, name);

  if (value.type == BSG_METADATA_NUMBER_VALUE) {
    return value.double_value;
  }
  return 0.0;
}

char *bsg_cache_get_metadata_string(void *event_ptr, const char *section,
                                    const char *name) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;

  for (int k = 0; k < event->metadata.value_count; ++k) {
    if (strncmp(event->metadata.values[k].section, section,
                sizeof(event->metadata.values[k].section)) == 0 &&
        strncmp(event->metadata.values[k].name, name,
                sizeof(event->metadata.values[k].name)) == 0) {
      return event->metadata.values[k].char_value;
    }
  }
  return NULL;
}

bool bsg_cache_get_metadata_bool(void *event_ptr, const char *section,
                                 const char *name) {
  bsg_metadata_value value = get_metadata_value(event_ptr, section, name);

  if (value.type == BSG_METADATA_BOOL_VALUE) {
    return value.bool_value;
  }
  return false;
}

char *bsg_cache_get_api_key(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->api_key;
}

void bsg_cache_set_api_key(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->api_key, value, sizeof(event->api_key));
}

char *bsg_cache_get_event_context(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->context;
}

void bsg_cache_set_event_context(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->context, value, sizeof(event->context));
}

void bsg_cache_set_event_user(void *event_ptr, const char *id,
                              const char *email, const char *name) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->user.id, id, sizeof(event->user.id));
  bsg_strncpy_safe(event->user.email, email, sizeof(event->user.email));
  bsg_strncpy_safe(event->user.name, name, sizeof(event->user.name));
}

void bsg_cache_add_breadcrumb(bugsnag_event *event, bugsnag_breadcrumb *crumb) {
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

char *bsg_cache_get_app_binary_arch(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.binary_arch;
}

void bsg_cache_set_app_binary_arch(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->app.binary_arch, value,
                   sizeof(event->app.binary_arch));
}

char *bsg_cache_get_app_build_uuid(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.build_uuid;
}

void bsg_cache_set_app_build_uuid(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->app.build_uuid, value, sizeof(event->app.build_uuid));
}

char *bsg_cache_get_app_id(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.id;
}

void bsg_cache_set_app_id(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->app.id, value, sizeof(event->app.id));
}

char *bsg_cache_get_app_release_stage(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.release_stage;
}

void bsg_cache_set_app_release_stage(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->app.release_stage, value,
                   sizeof(event->app.release_stage));
}

char *bsg_cache_get_app_type(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.type;
}

void bsg_cache_set_app_type(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->app.type, value, sizeof(event->app.type));
}

char *bsg_cache_get_app_version(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.version;
}

void bsg_cache_set_app_version(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->app.version, value, sizeof(event->app.version));
}

int bsg_cache_get_app_version_code(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.version_code;
}

void bsg_cache_set_app_version_code(void *event_ptr, int value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.version_code = value;
}

time_t bsg_cache_get_app_duration(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.duration;
}

void bsg_cache_set_app_duration(void *event_ptr, time_t value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.duration = value;
}

time_t bsg_cache_get_app_duration_in_foreground(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.duration_in_foreground;
}

void bsg_cache_set_app_duration_in_foreground(void *event_ptr, time_t value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.duration_in_foreground = value;
}

bool bsg_cache_get_app_in_foreground(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.in_foreground;
}

void bsg_cache_set_app_in_foreground(void *event_ptr, bool value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.in_foreground = value;
}

bool bsg_cache_get_app_is_launching(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.is_launching;
}

void bsg_cache_set_app_is_launching(void *event_ptr, bool value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.is_launching = value;
}

bool bsg_cache_get_device_jailbroken(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.jailbroken;
}

void bsg_cache_set_device_jailbroken(void *event_ptr, bool value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->device.jailbroken = value;
}

char *bsg_cache_get_device_id(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.id;
}

void bsg_cache_set_device_id(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->device.id, value, sizeof(event->device.id));
}

char *bsg_cache_get_device_locale(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.locale;
}

void bsg_cache_set_device_locale(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->device.locale, value, sizeof(event->device.locale));
}

char *bsg_cache_get_device_manufacturer(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.manufacturer;
}

void bsg_cache_set_device_manufacturer(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->device.manufacturer, value,
                   sizeof(event->device.manufacturer));
}

char *bsg_cache_get_device_model(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.model;
}

void bsg_cache_set_device_model(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->device.model, value, sizeof(event->device.model));
}

char *bsg_cache_get_device_os_version(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.os_version;
}

void bsg_cache_set_device_os_version(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->device.os_version, value,
                   sizeof(event->device.os_version));
}

long bsg_cache_get_device_total_memory(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.total_memory;
}

void bsg_cache_set_device_total_memory(void *event_ptr, long value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->device.total_memory = value;
}

char *bsg_cache_get_device_orientation(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.orientation;
}

void bsg_cache_set_device_orientation(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->device.orientation, value,
                   sizeof(event->device.orientation));
}

time_t bsg_cache_get_device_time(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.time;
}

void bsg_cache_set_device_time_seconds(void *event_ptr, time_t value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->device.time = value;
}

char *bsg_cache_get_device_os_name(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.os_name;
}

void bsg_cache_set_device_os_name(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->device.os_name, value, sizeof(event->device.os_name));
}

char *bsg_cache_get_error_class(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->error.errorClass;
}

void bsg_cache_set_error_class(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->error.errorClass, value,
                   sizeof(event->error.errorClass));
}

char *bsg_cache_get_error_message(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->error.errorMessage;
}

void bsg_cache_set_error_message(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->error.errorMessage, value,
                   sizeof(event->error.errorMessage));
}

char *bsg_cache_get_error_type(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->error.type;
}

void bsg_cache_set_error_type(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->error.type, value, sizeof(event->error.type));
}

bugsnag_severity bsg_cache_get_event_severity(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->severity;
}

void bsg_cache_set_event_severity(void *event_ptr, bugsnag_severity value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->severity = value;
}

bool bsg_cache_is_event_unhandled(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->unhandled;
}

void bsg_cache_set_event_unhandled(void *event_ptr, bool value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->unhandled = value;
}

bugsnag_user bsg_cache_get_event_user(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->user;
}

char *bsg_cache_get_event_grouping_hash(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->grouping_hash;
}

void bsg_cache_set_event_grouping_hash(void *event_ptr, const char *value) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy_safe(event->grouping_hash, value, sizeof(event->grouping_hash));
}

int bsg_cache_get_error_stacktrace_size(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->error.frame_count;
}

bugsnag_stackframe *bsg_cache_get_error_stackframe(void *event_ptr, int index) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  if (index >= 0 && index < event->error.frame_count) {
    return &event->error.stacktrace[index];
  } else {
    return NULL;
  }
}
