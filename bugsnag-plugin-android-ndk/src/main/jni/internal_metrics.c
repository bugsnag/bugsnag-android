//
// Created by Karl Stenerud on 05.09.22.
//

#include "internal_metrics.h"
#include "utils/logger.h"

#include <string.h>

static bool internal_metrics_enabled = false;

// This MUST remain consistent with bsg_called_api in jni/event.h
const char *const bsg_called_api_names[] = {
    "app_get_binary_arch",
    "app_get_build_uuid",
    "app_get_duration",
    "app_get_duration_in_foreground",
    "app_get_id",
    "app_get_in_foreground",
    "app_get_is_launching",
    "app_get_release_stage",
    "app_get_type",
    "app_get_version",
    "app_get_version_code",
    "app_set_binary_arch",
    "app_set_build_uuid",
    "app_set_duration",
    "app_set_duration_in_foreground",
    "app_set_id",
    "app_set_in_foreground",
    "app_set_is_launching",
    "app_set_release_stage",
    "app_set_type",
    "app_set_version",
    "app_set_version_code",
    "device_get_id",
    "device_get_jailbroken",
    "device_get_locale",
    "device_get_manufacturer",
    "device_get_model",
    "device_get_orientation",
    "device_get_os_name",
    "device_get_os_version",
    "device_get_time",
    "device_get_total_memory",
    "device_set_id",
    "device_set_jailbroken",
    "device_set_locale",
    "device_set_manufacturer",
    "device_set_model",
    "device_set_orientation",
    "device_set_os_name",
    "device_set_os_version",
    "device_set_time",
    "device_set_total_memory",
    "error_get_error_class",
    "error_get_error_message",
    "error_get_error_type",
    "error_set_error_class",
    "error_set_error_message",
    "error_set_error_type",
    "event_add_metadata_bool",
    "event_add_metadata_double",
    "event_add_metadata_string",
    "event_clear_metadata",
    "event_clear_metadata_section",
    "event_get_api_key",
    "event_get_context",
    "event_get_grouping_hash",
    "event_get_metadata_bool",
    "event_get_metadata_double",
    "event_get_metadata_string",
    "event_get_severity",
    "event_get_stackframe",
    "event_get_stacktrace_size",
    "event_get_user",
    "event_has_metadata",
    "event_is_unhandled",
    "event_set_api_key",
    "event_set_context",
    "event_set_grouping_hash",
    "event_set_severity",
    "event_set_unhandled",
    "event_set_user",
};
const int bsg_called_apis_count =
    sizeof(bsg_called_api_names) / sizeof(*bsg_called_api_names);

void bsg_set_internal_metrics_enabled(bool enabled) {
  internal_metrics_enabled = enabled;
}

static int get_called_api_array_slot_index(bsg_called_api api) {
  return api / 64;
}

static uint64_t get_called_api_array_slot_bit(bsg_called_api api) {
  int bit_index = api & 63;
  return ((uint64_t)1) << bit_index;
}

void bsg_notify_api_called(bugsnag_event *event, bsg_called_api api) {
  if (!internal_metrics_enabled || event == NULL) {
    return;
  }

  static const int slot_count =
      sizeof(event->called_apis) / sizeof(*event->called_apis);
  int slot_index = get_called_api_array_slot_index(api);
  if (slot_index < slot_count) {
    event->called_apis[slot_index] |= get_called_api_array_slot_bit(api);
  }
}

bool bsg_was_api_called(const bugsnag_event *event, bsg_called_api api) {
  // No internal_metrics_enabled check because this function reads after an app
  // reload.

  static const int slot_count =
      sizeof(event->called_apis) / sizeof(*event->called_apis);
  int slot_index = get_called_api_array_slot_index(api);
  if (slot_index < slot_count) {
    return (event->called_apis[slot_index] &
            get_called_api_array_slot_bit(api)) != 0;
  }
  return false;
}

static void bsg_modify_callback_count(bugsnag_event *event, const char *api,
                                      int delta) {
  static const int total_callbacks =
      sizeof(event->set_callback_counts) / sizeof(*event->set_callback_counts);
  if (!api || strnlen(api, sizeof(event->set_callback_counts[0].name)) >=
                  sizeof(event->set_callback_counts[0].name)) {
    // API name is NULL or is too big to store.
    return;
  }

  int i = 0;
  for (; i < total_callbacks && event->set_callback_counts[i].name[0] != 0;
       i++) {
    set_callback_count *callback_counter = &event->set_callback_counts[i];
    if (strncmp(callback_counter->name, api, sizeof(callback_counter->name)) ==
        0) {
      callback_counter->count += delta;
      if (callback_counter->count < 0) {
        callback_counter->count = 0;
      }
      return;
    }
  }
  if (i < total_callbacks && delta > 0) {
    set_callback_count *callback_counter = &event->set_callback_counts[i];
    strncpy(callback_counter->name, api, sizeof(callback_counter->name));
    callback_counter->count = delta;
  }
}

void bsg_set_callback_count(bugsnag_event *event, const char *api,
                            int32_t count) {
  if (!internal_metrics_enabled || event == NULL || !api) {
    return;
  }

  static const int total_callbacks =
      sizeof(event->set_callback_counts) / sizeof(*event->set_callback_counts);
  if (strnlen(api, sizeof(event->set_callback_counts[0].name)) >=
      sizeof(event->set_callback_counts[0].name)) {
    // API name is too big to store.
    return;
  }

  int i = 0;
  for (; i < total_callbacks && event->set_callback_counts[i].name[0] != 0;
       i++) {
    if (strcmp(event->set_callback_counts[i].name, api) == 0) {
      event->set_callback_counts[i].count = count;
      if (event->set_callback_counts[i].count < 0) {
        event->set_callback_counts[i].count = 0;
      }
      return;
    }
  }
  if (i < total_callbacks && count > 0) {
    strncpy(event->set_callback_counts[i].name, api,
            sizeof(event->set_callback_counts[i].name));
    event->set_callback_counts[i].count = count;
  }
}

void bsg_notify_add_callback(bugsnag_event *event, const char *api) {
  if (!internal_metrics_enabled || event == NULL) {
    return;
  }

  bsg_modify_callback_count(event, api, 1);
}

void bsg_notify_remove_callback(bugsnag_event *event, const char *api) {
  if (!internal_metrics_enabled || event == NULL) {
    return;
  }

  bsg_modify_callback_count(event, api, -1);
}
