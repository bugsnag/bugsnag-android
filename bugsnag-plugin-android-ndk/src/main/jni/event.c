#include "event.h"
#include "utils/string.h"
#include <string.h>
#include <utils/logger.h>
#include <utils/memory.h>

static void bsg_clear_metadata_value(bsg_metadata_value *value);
static void bsg_free_opaque_metadata(bugsnag_metadata *metadata);

/**
 * Compact the given metadata array by removing all duplicate entries and NONE
 * values. This will retain the order of the values. After this function
 * returns, all values with `index >= value_count` will have type
 * `BSG_METADATA_NONE`
 *
 * @return true if there is space for new values in the metadata
 */
static bool bsg_metadata_compact(bugsnag_metadata *const metadata) {
  int toRemove = 0;

  // first mark any duplicates as NONE, and count up all the empty (NONE) values
  // we find scan the array backwards to make retaining order simple
  for (int primaryIndex = metadata->value_count - 1; primaryIndex >= 0;
       primaryIndex--) {
    if (metadata->values[primaryIndex].type == BSG_METADATA_NONE_VALUE) {
      toRemove++;
      continue;
    }

    const char *section = metadata->values[primaryIndex].section;
    const char *name = metadata->values[primaryIndex].name;

    for (int searchIndex = primaryIndex - 1; searchIndex >= 0; searchIndex--) {
      bsg_metadata_value *value = &(metadata->values[searchIndex]);
      if (strcmp(value->section, section) == 0 &&
          strcmp(value->name, name) == 0) {

        // this will be picked up by our primaryIndex search on a future
        // iteration
        bsg_clear_metadata_value(value);
      }
    }
  }

  if (toRemove == 0) {
    return metadata->value_count < BUGSNAG_METADATA_MAX;
  }

  // now compact the array by closing all of the gaps where there are NONE
  // values
  for (int i = 0; i < metadata->value_count; i++) {
    if (metadata->values[i].type != BSG_METADATA_NONE_VALUE) {
      continue;
    }

    // scan "forwards" to find the extent of this gap
    int gapEnd = i + 1;
    while (gapEnd < metadata->value_count &&
           metadata->values[gapEnd].type == BSG_METADATA_NONE_VALUE)
      gapEnd++;

    memmove(&metadata->values[i], &metadata->values[gapEnd],
            sizeof(bsg_metadata_value) * (metadata->value_count - gapEnd));
  }

  // mark all of the "extra" slots a NONE for clarity
  for (int i = metadata->value_count - toRemove; i < metadata->value_count;
       i++) {
    metadata->values[i].type = BSG_METADATA_NONE_VALUE;
  }

  metadata->value_count -= toRemove;

  // if we got here there is space in the array
  return true;
}

static int find_next_free_metadata_index(bugsnag_metadata *const metadata) {
  if (metadata->value_count < BUGSNAG_METADATA_MAX ||
      bsg_metadata_compact(metadata)) {
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

static bool bsg_event_clear_metadata(bugsnag_metadata *metadata,
                                     const char *section, const char *name) {
  int clearedCount = 0;
  // remove *all* values for the given key, and then compact the entire dataset
  for (int i = 0; i < metadata->value_count; ++i) {
    bsg_metadata_value *value = &(metadata->values[i]);
    if (strcmp(value->section, section) == 0 &&
        strcmp(value->name, name) == 0) {

      bsg_clear_metadata_value(value);
      clearedCount++;
    }
  }

  if (clearedCount > 0) {
    bsg_metadata_compact(metadata);
    return true;
  }

  return false;
}

static int allocate_metadata_index(bugsnag_metadata *metadata,
                                   const char *section, const char *name) {
  int index = find_next_free_metadata_index(metadata);
  if (index < 0) {
    // we possibly have duplicates of this key we can remove before giving up
    if (bsg_event_clear_metadata(metadata, section, name)) {
      index = find_next_free_metadata_index(metadata);
    }

    if (index < 0) {
      return index;
    }
  }
  bsg_strncpy(metadata->values[index].section, section,
              sizeof(metadata->values[index].section));
  bsg_strncpy(metadata->values[index].name, name,
              sizeof(metadata->values[index].name));
  if (metadata->value_count < BUGSNAG_METADATA_MAX) {
    metadata->value_count = index + 1;
  }
  return index;
}

void bsg_clear_metadata_value(bsg_metadata_value *value) {
  if (value->type == BSG_METADATA_OPAQUE_VALUE &&
      value->opaque_value_size > 0) {
    bsg_free(value->opaque_value);

    value->opaque_value = NULL;
    value->opaque_value_size = 0;
  }

  value->type = BSG_METADATA_NONE_VALUE;
}

/**
 * Release any memory held by OPAQUE values in the given bugsnag_metadata. This
 * does *not* mark the metadata as cleared, or compact the metadata.
 */
void bsg_free_opaque_metadata(bugsnag_metadata *metadata) {
  for (int i = 0; i < metadata->value_count; ++i) {
    bsg_clear_metadata_value(&(metadata->values[i]));
  }
}

void bsg_add_metadata_value_double(bugsnag_metadata *metadata,
                                   const char *section, const char *name,
                                   double value) {
  int index = allocate_metadata_index(metadata, section, name);
  if (index >= 0) {
    metadata->values[index].type = BSG_METADATA_NUMBER_VALUE;
    metadata->values[index].double_value = value;
  }
}

void bsg_add_metadata_value_str(bugsnag_metadata *metadata, const char *section,
                                const char *name, const char *value) {
  int index = allocate_metadata_index(metadata, section, name);
  if (index >= 0) {
    metadata->values[index].type = BSG_METADATA_CHAR_VALUE;
    bsg_strncpy(metadata->values[index].char_value, value,
                sizeof(metadata->values[index].char_value));
  }
}

void bsg_add_metadata_value_bool(bugsnag_metadata *metadata,
                                 const char *section, const char *name,
                                 bool value) {
  int index = allocate_metadata_index(metadata, section, name);
  if (index >= 0) {
    metadata->values[index].type = BSG_METADATA_BOOL_VALUE;
    metadata->values[index].bool_value = value;
  }
}

void bsg_add_metadata_value_opaque(bugsnag_metadata *metadata,
                                   const char *section, const char *name,
                                   const char *json) {
  int index = allocate_metadata_index(metadata, section, name);
  if (index >= 0) {
    char *duplicate = strdup(json);

    if (duplicate == NULL) {
      return;
    }

    metadata->values[index].opaque_value = duplicate;
    metadata->values[index].type = BSG_METADATA_OPAQUE_VALUE;
    metadata->values[index].opaque_value_size = bsg_strlen(json);
  }
}

void bugsnag_event_add_metadata_double(void *event_ptr, const char *section,
                                       const char *name, double value) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_ADD_METADATA_DOUBLE);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_add_metadata_value_double(&event->metadata, section, name, value);
}

void bugsnag_event_add_metadata_string(void *event_ptr, const char *section,
                                       const char *name, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_ADD_METADATA_STRING);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_add_metadata_value_str(&event->metadata, section, name, value);
}

void bugsnag_event_add_metadata_bool(void *event_ptr, const char *section,
                                     const char *name, bool value) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_ADD_METADATA_BOOL);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_add_metadata_value_bool(&event->metadata, section, name, value);
}

void bugsnag_event_clear_metadata(void *event_ptr, const char *section,
                                  const char *name) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_CLEAR_METADATA);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_event_clear_metadata(&event->metadata, section, name);
}

void bugsnag_event_clear_metadata_section(void *event_ptr,
                                          const char *section) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_CLEAR_METADATA_SECTION);
  size_t clearedCount = 0;
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  for (int i = 0; i < event->metadata.value_count; ++i) {
    if (strcmp(event->metadata.values[i].section, section) == 0) {
      bsg_clear_metadata_value(&(event->metadata.values[i]));
      clearedCount++;
    }
  }

  if (clearedCount > 0) {
    bsg_metadata_compact(&(event->metadata));
  }
}

static bsg_metadata_value
get_metadata_value(void *event_ptr, const char *section, const char *name) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;

  for (int k = 0; k < event->metadata.value_count; ++k) {
    bsg_metadata_value val = event->metadata.values[k];
    if (strcmp(val.section, section) == 0 && strcmp(val.name, name) == 0) {
      return val;
    }
  }
  bsg_metadata_value data = {.type = BSG_METADATA_NONE_VALUE};
  return data;
}

bugsnag_metadata_type bugsnag_event_has_metadata(void *event_ptr,
                                                 const char *section,
                                                 const char *name) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_HAS_METADATA);
  return get_metadata_value(event_ptr, section, name).type;
}

double bugsnag_event_get_metadata_double(void *event_ptr, const char *section,
                                         const char *name) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_GET_METADATA_DOUBLE);
  bsg_metadata_value value = get_metadata_value(event_ptr, section, name);

  if (value.type == BSG_METADATA_NUMBER_VALUE) {
    return value.double_value;
  }
  return 0.0;
}

char *bugsnag_event_get_metadata_string(void *event_ptr, const char *section,
                                        const char *name) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_GET_METADATA_STRING);
  bugsnag_event *event = (bugsnag_event *)event_ptr;

  for (int k = 0; k < event->metadata.value_count; ++k) {
    if (strcmp(event->metadata.values[k].section, section) == 0 &&
        strcmp(event->metadata.values[k].name, name) == 0) {
      return event->metadata.values[k].char_value;
    }
  }
  return NULL;
}

bool bugsnag_event_get_metadata_bool(void *event_ptr, const char *section,
                                     const char *name) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_GET_METADATA_BOOL);
  bsg_metadata_value value = get_metadata_value(event_ptr, section, name);

  if (value.type == BSG_METADATA_BOOL_VALUE) {
    return value.bool_value;
  }
  return false;
}

void bsg_event_start_session(bugsnag_event *event, const char *session_id,
                             const char *started_at, int handled_count,
                             int unhandled_count) {
  bsg_strncpy(event->session_id, session_id, sizeof(event->session_id));
  bsg_strncpy(event->session_start, started_at, sizeof(event->session_start));
  event->handled_events = handled_count;
  event->unhandled_events = unhandled_count;
}

char *bugsnag_event_get_api_key(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_GET_API_KEY);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->api_key;
}

void bugsnag_event_set_api_key(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_SET_API_KEY);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->api_key, value, sizeof(event->api_key));
}

char *bugsnag_event_get_context(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_GET_CONTEXT);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->context;
}

void bugsnag_event_set_context(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_SET_CONTEXT);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->context, value, sizeof(event->context));
}

void bugsnag_event_set_user(void *event_ptr, const char *id, const char *email,
                            const char *name) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_SET_USER);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->user.id, id, sizeof(event->user.id));
  bsg_strncpy(event->user.email, email, sizeof(event->user.email));
  bsg_strncpy(event->user.name, name, sizeof(event->user.name));
}

void bsg_event_add_breadcrumb(bugsnag_event *event, bugsnag_breadcrumb *crumb) {
  int crumb_index;
  if (event->crumb_count < BUGSNAG_CRUMBS_MAX) {
    crumb_index = event->crumb_count;
    event->crumb_count++;
  } else {
    crumb_index = event->crumb_first_index;
    event->crumb_first_index =
        (event->crumb_first_index + 1) % BUGSNAG_CRUMBS_MAX;
  }

  bsg_free_opaque_metadata(&(event->breadcrumbs[crumb_index].metadata));
  memcpy(&event->breadcrumbs[crumb_index], crumb, sizeof(bugsnag_breadcrumb));
}

bool bsg_event_has_session(const bugsnag_event *event) {
  return bsg_strlen(event->session_id) > 0;
}

/* Accessors for event.app */

char *bugsnag_app_get_binary_arch(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_GET_BINARY_ARCH);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.binary_arch;
}

void bugsnag_app_set_binary_arch(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_SET_BINARY_ARCH);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->app.binary_arch, value, sizeof(event->app.binary_arch));
}

char *bugsnag_app_get_build_uuid(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_GET_BUILD_UUID);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.build_uuid;
}

void bugsnag_app_set_build_uuid(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_SET_BUILD_UUID);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->app.build_uuid, value, sizeof(event->app.build_uuid));
}

char *bugsnag_app_get_id(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_GET_ID);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.id;
}

void bugsnag_app_set_id(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_SET_ID);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->app.id, value, sizeof(event->app.id));
}

char *bugsnag_app_get_release_stage(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_GET_RELEASE_STAGE);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.release_stage;
}

void bugsnag_app_set_release_stage(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_SET_RELEASE_STAGE);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->app.release_stage, value,
              sizeof(event->app.release_stage));
}

char *bugsnag_app_get_type(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_GET_TYPE);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.type;
}

void bugsnag_app_set_type(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_SET_TYPE);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->app.type, value, sizeof(event->app.type));
}

char *bugsnag_app_get_version(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_GET_VERSION);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.version;
}

void bugsnag_app_set_version(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_SET_VERSION);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->app.version, value, sizeof(event->app.version));
}

int bugsnag_app_get_version_code(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_GET_VERSION_CODE);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.version_code;
}

void bugsnag_app_set_version_code(void *event_ptr, int value) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_SET_VERSION_CODE);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.version_code = value;
}

time_t bugsnag_app_get_duration(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_GET_DURATION);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.duration;
}

void bugsnag_app_set_duration(void *event_ptr, time_t value) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_SET_DURATION);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.duration = value;
}

time_t bugsnag_app_get_duration_in_foreground(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_GET_DURATION_IN_FOREGROUND);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.duration_in_foreground;
}

void bugsnag_app_set_duration_in_foreground(void *event_ptr, time_t value) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_SET_DURATION_IN_FOREGROUND);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.duration_in_foreground = value;
}

bool bugsnag_app_get_in_foreground(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_GET_IN_FOREGROUND);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.in_foreground;
}

void bugsnag_app_set_in_foreground(void *event_ptr, bool value) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_SET_IN_FOREGROUND);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.in_foreground = value;
}

bool bugsnag_app_get_is_launching(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_GET_IS_LAUNCHING);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->app.is_launching;
}

void bugsnag_app_set_is_launching(void *event_ptr, bool value) {
  bsg_notify_api_called(event_ptr, BSG_API_APP_SET_IS_LAUNCHING);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->app.is_launching = value;
}

/* Accessors for event.device */

bool bugsnag_device_get_jailbroken(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_GET_JAILBROKEN);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.jailbroken;
}

void bugsnag_device_set_jailbroken(void *event_ptr, bool value) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_SET_JAILBROKEN);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->device.jailbroken = value;
}

char *bugsnag_device_get_id(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_GET_ID);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.id;
}

void bugsnag_device_set_id(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_SET_ID);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->device.id, value, sizeof(event->device.id));
}

char *bugsnag_device_get_locale(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_GET_LOCALE);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.locale;
}

void bugsnag_device_set_locale(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_SET_LOCALE);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->device.locale, value, sizeof(event->device.locale));
}

char *bugsnag_device_get_manufacturer(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_GET_MANUFACTURER);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.manufacturer;
}

void bugsnag_device_set_manufacturer(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_SET_MANUFACTURER);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->device.manufacturer, value,
              sizeof(event->device.manufacturer));
}

char *bugsnag_device_get_model(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_GET_MODEL);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.model;
}

void bugsnag_device_set_model(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_SET_MODEL);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->device.model, value, sizeof(event->device.model));
}

char *bugsnag_device_get_os_version(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_GET_OS_VERSION);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.os_version;
}

void bugsnag_device_set_os_version(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_SET_OS_VERSION);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->device.os_version, value,
              sizeof(event->device.os_version));
}

long bugsnag_device_get_total_memory(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_GET_TOTAL_MEMORY);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.total_memory;
}

void bugsnag_device_set_total_memory(void *event_ptr, long value) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_SET_TOTAL_MEMORY);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->device.total_memory = value;
}

char *bugsnag_device_get_orientation(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_GET_ORIENTATION);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.orientation;
}

void bugsnag_device_set_orientation(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_SET_ORIENTATION);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->device.orientation, value,
              sizeof(event->device.orientation));
}

time_t bugsnag_device_get_time(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_GET_TIME);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.time;
}

void bugsnag_device_set_time(void *event_ptr, time_t value) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_SET_TIME);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->device.time = value;
}

char *bugsnag_device_get_os_name(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_GET_OS_NAME);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->device.os_name;
}

void bugsnag_device_set_os_name(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_DEVICE_SET_OS_NAME);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->device.os_name, value, sizeof(event->device.os_name));
}

char *bugsnag_error_get_error_class(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_ERROR_GET_ERROR_CLASS);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->error.errorClass;
}

void bugsnag_error_set_error_class(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_ERROR_SET_ERROR_CLASS);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->error.errorClass, value, sizeof(event->error.errorClass));
}

char *bugsnag_error_get_error_message(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_ERROR_GET_ERROR_MESSAGE);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->error.errorMessage;
}

void bugsnag_error_set_error_message(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_ERROR_SET_ERROR_MESSAGE);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->error.errorMessage, value,
              sizeof(event->error.errorMessage));
}

char *bugsnag_error_get_error_type(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_ERROR_GET_ERROR_TYPE);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->error.type;
}

void bugsnag_error_set_error_type(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_ERROR_SET_ERROR_TYPE);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->error.type, value, sizeof(event->error.type));
}

bugsnag_severity bugsnag_event_get_severity(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_GET_SEVERITY);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->severity;
}

void bugsnag_event_set_severity(void *event_ptr, bugsnag_severity value) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_SET_SEVERITY);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->severity = value;
}

bool bugsnag_event_is_unhandled(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_IS_UNHANDLED);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->unhandled;
}

void bugsnag_event_set_unhandled(void *event_ptr, bool value) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_SET_UNHANDLED);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  event->unhandled = value;
}

bugsnag_user bugsnag_event_get_user(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_GET_USER);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->user;
}

char *bugsnag_event_get_grouping_hash(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_GET_GROUPING_HASH);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->grouping_hash;
}

void bugsnag_event_set_grouping_hash(void *event_ptr, const char *value) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_SET_GROUPING_HASH);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  bsg_strncpy(event->grouping_hash, value, sizeof(event->grouping_hash));
}

int bugsnag_event_get_stacktrace_size(void *event_ptr) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_GET_STACKTRACE_SIZE);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  return event->error.frame_count;
}

bugsnag_stackframe *bugsnag_event_get_stackframe(void *event_ptr, int index) {
  bsg_notify_api_called(event_ptr, BSG_API_EVENT_GET_STACKFRAME);
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  if (index >= 0 && index < event->error.frame_count) {
    return &event->error.stacktrace[index];
  } else {
    return NULL;
  }
}

void bsg_notify_api_called(void *event_ptr, bsg_called_api api) {
  bugsnag_event *event = (bugsnag_event *)event_ptr;
  const int slot_count =
      sizeof(event->called_apis) / sizeof(*event->called_apis);
  int slot_index = api / 64;
  int bit_index = api & 63;
  if (slot_index < slot_count) {
    event->called_apis[slot_index] |= ((uint64_t)1) << bit_index;
  }
}
