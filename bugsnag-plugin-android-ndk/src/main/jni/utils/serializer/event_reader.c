#include "event_reader.h"
#include "migrate.h"

#include "../../event.h"
#include "../string.h"

#include <fcntl.h>
#include <malloc.h>
#include <string.h>
#include <unistd.h>

const int BSG_MIGRATOR_CURRENT_VERSION = 8;

#ifdef __cplusplus
extern "C" {
#endif

// Symbols exported for unit test access
void migrate_app_v1(bugsnag_report_v2 *report_v2, bugsnag_report_v3 *event);
void migrate_app_v2(bugsnag_report_v4 *report_v4, bugsnag_event *event);
void migrate_device_v1(bugsnag_report_v2 *report_v2, bugsnag_report_v3 *event);
#ifdef __cplusplus
}
#endif

/**
 * the *_read() functions take a file descriptor and read a payload in the
 * {version} format. This allocates an instance of the return value type.
 */

bugsnag_report_v1 *bsg_report_v1_read(int fd);
bugsnag_report_v2 *bsg_report_v2_read(int fd);
bugsnag_report_v3 *bsg_report_v3_read(int fd);
bugsnag_report_v4 *bsg_report_v4_read(int fd);
bugsnag_report_v5 *bsg_report_v5_read(int fd);
bugsnag_report_v6 *bsg_report_v6_read(int fd);
bugsnag_report_v7 *bsg_report_v7_read(int fd);
bugsnag_event *bsg_report_v8_read(int fd);

/**
 * the map_*() functions convert a structure of an older format into the latest.
 * This frees the parameter provided to the function once conversion is
 * complete.
 */

bugsnag_event *bsg_map_v7_to_report(bugsnag_report_v7 *report_v7);
bugsnag_event *bsg_map_v6_to_report(bugsnag_report_v6 *report_v6);
bugsnag_event *bsg_map_v5_to_report(bugsnag_report_v5 *report_v5);
bugsnag_event *bsg_map_v4_to_report(bugsnag_report_v4 *report_v4);
bugsnag_event *bsg_map_v3_to_report(bugsnag_report_v3 *report_v3);
bugsnag_event *bsg_map_v2_to_report(bugsnag_report_v2 *report_v2);
bugsnag_event *bsg_map_v1_to_report(bugsnag_report_v1 *report_v1);

void migrate_breadcrumb_v1(bugsnag_report_v2 *report_v2,
                           bugsnag_report_v3 *event);
void migrate_breadcrumb_v2(bugsnag_report_v5 *report_v5, bugsnag_event *event);

void migrate_device_v2(bsg_device_info *output, bsg_device_info_v2 *input);
void migrate_app_v3(bsg_app_info *output, bsg_app_info_v3 *input);

void bsg_read_feature_flags(int fd, bsg_feature_flag **out_feature_flags,
                            size_t *out_feature_flag_count);

bsg_report_header *bsg_report_header_read(int fd) {
  bsg_report_header *header = calloc(1, sizeof(bsg_report_header));
  ssize_t len = read(fd, header, sizeof(bsg_report_header));
  if (len != sizeof(bsg_report_header)) {
    free(header);
    return NULL;
  }

  return header;
}

bugsnag_event *bsg_read_event(char *filepath) {
  int fildes = open(filepath, O_RDONLY);
  if (fildes == -1) {
    return NULL;
  }

  bsg_report_header *header = bsg_report_header_read(fildes);
  if (header == NULL) {
    return NULL;
  }

  int version = header->version;
  free(header);
  bugsnag_event *event = NULL;

  switch (version) {
  case 1:
    return bsg_map_v1_to_report(bsg_report_v1_read(fildes));
  case 2:
    return bsg_map_v2_to_report(bsg_report_v2_read(fildes));
  case 3:
    return bsg_map_v3_to_report(bsg_report_v3_read(fildes));
  case 4:
    return bsg_map_v4_to_report(bsg_report_v4_read(fildes));
  case 5:
    return bsg_map_v5_to_report(bsg_report_v5_read(fildes));
  case 6:
    return bsg_map_v6_to_report(bsg_report_v6_read(fildes));
  case 7:
    return bsg_map_v7_to_report(bsg_report_v7_read(fildes));
  case BSG_MIGRATOR_CURRENT_VERSION:
    return bsg_report_v8_read(fildes);
  default:
    return NULL;
  }
}

bugsnag_report_v1 *bsg_report_v1_read(int fd) {
  size_t event_size = sizeof(bugsnag_report_v1);
  bugsnag_report_v1 *event = calloc(1, event_size);

  ssize_t len = read(fd, event, event_size);
  if (len != event_size) {
    free(event);
    return NULL;
  }
  return event;
}

bugsnag_report_v2 *bsg_report_v2_read(int fd) {
  size_t event_size = sizeof(bugsnag_report_v2);
  bugsnag_report_v2 *event = calloc(1, event_size);

  ssize_t len = read(fd, event, event_size);
  if (len != event_size) {
    free(event);
    return NULL;
  }
  return event;
}

bugsnag_report_v3 *bsg_report_v3_read(int fd) {
  size_t event_size = sizeof(bugsnag_report_v3);
  bugsnag_report_v3 *event = calloc(1, event_size);

  ssize_t len = read(fd, event, event_size);
  if (len != event_size) {
    free(event);
    return NULL;
  }
  return event;
}

bugsnag_report_v4 *bsg_report_v4_read(int fd) {
  size_t event_size = sizeof(bugsnag_report_v4);
  bugsnag_report_v4 *event = calloc(1, event_size);

  ssize_t len = read(fd, event, event_size);
  if (len != event_size) {
    free(event);
    return NULL;
  }
  return event;
}

bugsnag_report_v5 *bsg_report_v5_read(int fd) {
  size_t event_size = sizeof(bugsnag_report_v5);
  bugsnag_report_v5 *event = calloc(1, event_size);

  ssize_t len = read(fd, event, event_size);
  if (len != event_size) {
    free(event);
    return NULL;
  }
  return event;
}

bugsnag_report_v6 *bsg_report_v6_read(int fd) {
  size_t event_size = sizeof(bugsnag_report_v6);
  bugsnag_report_v6 *event = calloc(1, event_size);

  ssize_t len = read(fd, event, event_size);
  if (len != event_size) {
    free(event);
    return NULL;
  }
  return event;
}

bugsnag_report_v7 *bsg_report_v7_read(int fd) {
  size_t event_size = sizeof(bugsnag_report_v7);
  bugsnag_report_v7 *event = calloc(1, event_size);

  ssize_t len = read(fd, event, event_size);
  if (len != event_size) {
    free(event);
    return NULL;
  }
  return event;
}

bugsnag_event *bsg_report_v8_read(int fd) {
  size_t event_size = sizeof(bugsnag_event);
  bugsnag_event *event = calloc(1, event_size);

  ssize_t len = read(fd, event, event_size);
  if (len != event_size) {
    free(event);
    return NULL;
  }

  // read the feature flags, if possible
  bsg_read_feature_flags(fd, &event->feature_flags, &event->feature_flag_count);

  return event;
}

bugsnag_event *bsg_map_v6_to_report(bugsnag_report_v6 *report_v6) {
  if (report_v6 == NULL) {
    return NULL;
  }
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));

  if (event != NULL) {
    event->notifier = report_v6->notifier;
    event->metadata = report_v6->metadata;
    migrate_app_v3(&event->app, &report_v6->app);
    migrate_device_v2(&event->device, &report_v6->device);
    event->user = report_v6->user;
    event->error = report_v6->error;
    event->crumb_count = report_v6->crumb_count;
    event->crumb_first_index = report_v6->crumb_first_index;
    memcpy(&event->breadcrumbs, report_v6->breadcrumbs,
           sizeof(report_v6->breadcrumbs));
    memcpy(&event->context, report_v6->context, sizeof(report_v6->context));
    event->severity = report_v6->severity;
    memcpy(&event->session_id, report_v6->session_id,
           sizeof(report_v6->session_id));
    memcpy(&event->session_start, report_v6->session_start,
           sizeof(report_v6->session_start));
    event->handled_events = report_v6->handled_events;
    event->unhandled_events = report_v6->unhandled_events;
    memcpy(&event->grouping_hash, report_v6->grouping_hash,
           sizeof(report_v6->grouping_hash));
    event->unhandled = report_v6->unhandled;
    memcpy(&event->api_key, report_v6->api_key, sizeof(report_v6->api_key));

    free(report_v6);
  }
  return event;
}

bugsnag_event *bsg_map_v7_to_report(bugsnag_report_v7 *report_v7) {
  if (report_v7 == NULL) {
    return NULL;
  }
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));

  if (event != NULL) {
    event->notifier = report_v7->notifier;
    event->metadata = report_v7->metadata;
    migrate_app_v3(&event->app, &report_v7->app);
    migrate_device_v2(&event->device, &report_v7->device);
    event->user = report_v7->user;
    event->error = report_v7->error;
    event->crumb_count = report_v7->crumb_count;
    event->crumb_first_index = report_v7->crumb_first_index;
    memcpy(&event->breadcrumbs, report_v7->breadcrumbs,
           sizeof(report_v7->breadcrumbs));
    memcpy(&event->context, report_v7->context, sizeof(report_v7->context));
    event->severity = report_v7->severity;
    memcpy(&event->session_id, report_v7->session_id,
           sizeof(report_v7->session_id));
    memcpy(&event->session_start, report_v7->session_start,
           sizeof(report_v7->session_start));
    event->handled_events = report_v7->handled_events;
    event->unhandled_events = report_v7->unhandled_events;
    memcpy(&event->grouping_hash, report_v7->grouping_hash,
           sizeof(report_v7->grouping_hash));
    event->unhandled = report_v7->unhandled;
    memcpy(&event->api_key, report_v7->api_key, sizeof(report_v7->api_key));
    event->thread_count = report_v7->thread_count;
    memcpy(&event->threads, report_v7->threads, sizeof(report_v7->threads));

    free(report_v7);
  }
  return event;
}

bugsnag_event *bsg_map_v5_to_report(bugsnag_report_v5 *report_v5) {
  if (report_v5 == NULL) {
    return NULL;
  }
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));

  if (event != NULL) {
    event->notifier = report_v5->notifier;
    event->metadata = report_v5->metadata;
    migrate_app_v3(&event->app, &report_v5->app);
    migrate_device_v2(&event->device, &report_v5->device);
    bsg_strcpy(event->context, report_v5->context);
    event->user = report_v5->user;
    event->error = report_v5->error;
    event->severity = report_v5->severity;
    bsg_strncpy_safe(event->session_id, report_v5->session_id,
                     sizeof(report_v5->session_id));
    bsg_strncpy_safe(event->session_start, report_v5->session_start,
                     sizeof(report_v5->session_start));
    event->handled_events = report_v5->handled_events;
    event->unhandled_events = report_v5->unhandled_events;
    bsg_strncpy_safe(event->grouping_hash, report_v5->grouping_hash,
                     sizeof(report_v5->grouping_hash));
    event->unhandled = report_v5->unhandled;
    bsg_strncpy_safe(event->api_key, report_v5->api_key,
                     sizeof(report_v5->api_key));

    migrate_breadcrumb_v2(report_v5, event);
    free(report_v5);
  }
  return event;
}

bugsnag_event *bsg_map_v4_to_report(bugsnag_report_v4 *report_v4) {
  if (report_v4 == NULL) {
    return NULL;
  }
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));

  if (event != NULL) {
    event->notifier = report_v4->notifier;
    event->metadata = report_v4->metadata;
    migrate_device_v2(&event->device, &report_v4->device);
    event->user = report_v4->user;
    event->error = report_v4->error;
    event->crumb_count = report_v4->crumb_count;
    event->crumb_first_index = report_v4->crumb_first_index;
    memcpy(event->breadcrumbs, report_v4->breadcrumbs,
           sizeof(report_v4->breadcrumbs));
    event->severity = report_v4->severity;
    bsg_strncpy_safe(event->context, report_v4->context,
                     sizeof(report_v4->context));
    bsg_strncpy_safe(event->session_id, report_v4->session_id,
                     sizeof(report_v4->session_id));
    bsg_strncpy_safe(event->session_start, report_v4->session_start,
                     sizeof(report_v4->session_start));
    event->handled_events = report_v4->handled_events;
    event->unhandled_events = report_v4->unhandled_events;
    bsg_strncpy_safe(event->grouping_hash, report_v4->grouping_hash,
                     sizeof(report_v4->grouping_hash));
    event->unhandled = report_v4->unhandled;
    bsg_strncpy_safe(event->api_key, report_v4->api_key,
                     sizeof(report_v4->api_key));
    migrate_app_v2(report_v4, event);
    free(report_v4);
  }
  return event;
}

bugsnag_event *bsg_map_v3_to_report(bugsnag_report_v3 *report_v3) {
  if (report_v3 == NULL) {
    return NULL;
  }
  bugsnag_report_v4 *event = calloc(1, sizeof(bugsnag_event));

  if (event != NULL) {
    event->notifier = report_v3->notifier;
    event->app = report_v3->app;
    event->device = report_v3->device;
    event->user = report_v3->user;
    event->error = report_v3->error;
    event->metadata = report_v3->metadata;
    event->crumb_count = report_v3->crumb_count;
    event->crumb_first_index = report_v3->crumb_first_index;
    memcpy(event->breadcrumbs, report_v3->breadcrumbs,
           sizeof(report_v3->breadcrumbs));
    event->severity = report_v3->severity;
    strcpy(event->session_id, report_v3->session_id);
    strcpy(event->session_start, report_v3->session_start);
    event->handled_events = report_v3->handled_events;
    event->unhandled_events = report_v3->unhandled_events;
    strcpy(event->grouping_hash, report_v3->grouping_hash);
    event->unhandled = report_v3->unhandled;

    // set a default value for the api key
    strcpy(event->api_key, "");
    free(report_v3);
  }
  return bsg_map_v4_to_report(event);
}

bugsnag_event *bsg_map_v2_to_report(bugsnag_report_v2 *report_v2) {
  if (report_v2 == NULL) {
    return NULL;
  }
  bugsnag_report_v3 *event = calloc(1, sizeof(bugsnag_report_v3));

  if (event != NULL) {
    // assign metadata first as old app/device fields are migrated there
    event->metadata = report_v2->metadata;
    migrate_app_v1(report_v2, event);
    migrate_device_v1(report_v2, event);
    event->user = report_v2->user;
    migrate_breadcrumb_v1(report_v2, event);

    strcpy(event->context, report_v2->context);
    event->severity = report_v2->severity;
    strcpy(event->session_id, report_v2->session_id);
    strcpy(event->session_start, report_v2->session_start);
    event->handled_events = report_v2->handled_events;
    event->unhandled_events = report_v2->unhandled_events;

    // migrate changed notifier fields
    strcpy(event->notifier.version, report_v2->notifier.version);
    strcpy(event->notifier.name, report_v2->notifier.name);
    strcpy(event->notifier.url, report_v2->notifier.url);

    // migrate changed error fields
    strcpy(event->error.errorClass, report_v2->exception.name);
    strcpy(event->error.errorMessage, report_v2->exception.message);
    strcpy(event->error.type, report_v2->exception.type);
    event->error.frame_count = report_v2->exception.frame_count;
    size_t error_size = sizeof(bugsnag_stackframe) * BUGSNAG_FRAMES_MAX;
    memcpy(&event->error.stacktrace, report_v2->exception.stacktrace,
           error_size);

    // Fatal C errors are always true by default, previously this was hardcoded
    // and not a field on the struct
    event->unhandled = true;
    free(report_v2);
  }
  return bsg_map_v3_to_report(event);
}

static void add_metadata_string(bugsnag_metadata *meta, char *section,
                                char *name, char *value) {
  if (meta->value_count < BUGSNAG_METADATA_MAX) {
    bsg_metadata_value *item = &meta->values[meta->value_count];
    strncpy(item->section, section, sizeof(item->section));
    strncpy(item->name, name, sizeof(item->name));
    strncpy(item->char_value, value, sizeof(item->char_value));
    item->type = BSG_METADATA_CHAR_VALUE;
    meta->value_count++;
  }
}

static void add_metadata_double(bugsnag_metadata *meta, char *section,
                                char *name, double value) {
  if (meta->value_count < BUGSNAG_METADATA_MAX) {
    bsg_metadata_value *item = &meta->values[meta->value_count];
    strncpy(item->section, section, sizeof(item->section));
    strncpy(item->name, name, sizeof(item->name));
    item->type = BSG_METADATA_NUMBER_VALUE;
    item->double_value = value;
    meta->value_count++;
  }
}

static void add_metadata_bool(bugsnag_metadata *meta, char *section, char *name,
                              bool value) {
  if (meta->value_count < BUGSNAG_METADATA_MAX) {
    bsg_metadata_value *item = &meta->values[meta->value_count];
    strncpy(item->section, section, sizeof(item->section));
    strncpy(item->name, name, sizeof(item->name));
    item->type = BSG_METADATA_BOOL_VALUE;
    item->bool_value = value;
    meta->value_count++;
  }
}

void migrate_device_v1(bugsnag_report_v2 *report_v2, bugsnag_report_v3 *event) {
  bsg_strcpy(event->device.os_name,
             "android"); // os_name was not a field in v2
  event->device.api_level = report_v2->device.api_level;
  event->device.cpu_abi_count = report_v2->device.cpu_abi_count;
  event->device.time = report_v2->device.time;
  event->device.jailbroken = report_v2->device.jailbroken;
  event->device.total_memory = report_v2->device.total_memory;

  for (int k = 0; k < report_v2->device.cpu_abi_count &&
                  k < sizeof(report_v2->device.cpu_abi);
       k++) {
    bsg_strcpy(event->device.cpu_abi[k].value,
               report_v2->device.cpu_abi[k].value);
    event->device.cpu_abi_count++;
  }

  bsg_strcpy(event->device.orientation, report_v2->device.orientation);
  bsg_strcpy(event->device.id, report_v2->device.id);
  bsg_strcpy(event->device.locale, report_v2->device.locale);
  bsg_strcpy(event->device.manufacturer, report_v2->device.manufacturer);
  bsg_strcpy(event->device.model, report_v2->device.model);
  bsg_strcpy(event->device.os_build, report_v2->device.os_build);
  bsg_strcpy(event->device.os_version, report_v2->device.os_version);

  // migrate legacy fields to metadata
  add_metadata_bool(&event->metadata, "device", "emulator",
                    report_v2->device.emulator);
  add_metadata_double(&event->metadata, "device", "dpi", report_v2->device.dpi);
  add_metadata_double(&event->metadata, "device", "screenDensity",
                      report_v2->device.screen_density);
  add_metadata_double(&event->metadata, "device", "batteryLevel",
                      report_v2->device.battery_level);
  add_metadata_string(&event->metadata, "device", "locationStatus",
                      report_v2->device.location_status);
  add_metadata_string(&event->metadata, "device", "brand",
                      report_v2->device.brand);
  add_metadata_string(&event->metadata, "device", "networkAccess",
                      report_v2->device.network_access);
  add_metadata_string(&event->metadata, "device", "screenResolution",
                      report_v2->device.screen_resolution);
}

void migrate_device_v2(bsg_device_info *output, bsg_device_info_v2 *input) {
  output->api_level = input->api_level;
  output->cpu_abi_count = input->cpu_abi_count;
  memcpy(&output->cpu_abi, input->cpu_abi, sizeof(input->cpu_abi));
  memcpy(&output->orientation, input->orientation, sizeof(input->orientation));
  output->time = input->time;
  memcpy(&output->id, input->id, sizeof(input->id));
  output->jailbroken = input->jailbroken;
  memcpy(&output->locale, input->locale, sizeof(input->locale));
  memcpy(&output->manufacturer, input->manufacturer,
         sizeof(input->manufacturer));
  memcpy(&output->model, input->model, sizeof(input->model));
  memcpy(&output->os_build, input->os_build, sizeof(input->os_build));
  memcpy(&output->os_version, input->os_version, sizeof(input->os_version));
  memcpy(&output->os_name, input->os_name, sizeof(input->os_name));
  output->total_memory = input->total_memory;
}

void bugsnag_report_v3_add_breadcrumb(bugsnag_report_v3 *event,
                                      bugsnag_breadcrumb *crumb) {
  int crumb_index;
  if (event->crumb_count < V2_BUGSNAG_CRUMBS_MAX) {
    crumb_index = event->crumb_count;
    event->crumb_count++;
  } else {
    crumb_index = event->crumb_first_index;
    event->crumb_first_index =
        (event->crumb_first_index + 1) % V2_BUGSNAG_CRUMBS_MAX;
  }
  memcpy(&event->breadcrumbs[crumb_index], crumb, sizeof(bugsnag_breadcrumb));
}

int bsg_calculate_total_crumbs(int old_count) {
  return old_count < BUGSNAG_CRUMBS_MAX ? old_count : BUGSNAG_CRUMBS_MAX;
}

int bsg_calculate_v1_start_index(int old_count) {
  return old_count < V2_BUGSNAG_CRUMBS_MAX ? 0
                                           : old_count % V2_BUGSNAG_CRUMBS_MAX;
}

int bsg_calculate_v1_crumb_index(int crumb_pos, int first_index) {
  return (crumb_pos + first_index) % V1_BUGSNAG_CRUMBS_MAX;
}

void migrate_breadcrumb_v1(bugsnag_report_v2 *report_v2,
                           bugsnag_report_v3 *event) {
  event->crumb_count = 0;
  event->crumb_first_index = 0;

  // previously breadcrumbs had 30 elements, now they have 25.
  // if more than 25 breadcrumbs were collected in the legacy report,
  // offset them accordingly by moving the start position by count -
  // BUGSNAG_CRUMBS_MAX.
  int new_crumb_total = bsg_calculate_total_crumbs(report_v2->crumb_count);
  int k = bsg_calculate_v1_start_index(report_v2->crumb_count);

  for (; k < new_crumb_total; k++) {
    int crumb_index =
        bsg_calculate_v1_crumb_index(k, report_v2->crumb_first_index);
    bugsnag_breadcrumb_v1 *old_crumb = &report_v2->breadcrumbs[crumb_index];
    bugsnag_breadcrumb *new_crumb = calloc(1, sizeof(bugsnag_breadcrumb));

    // copy old crumb fields to new
    new_crumb->type = old_crumb->type;
    bsg_strncpy_safe(new_crumb->name, old_crumb->name, sizeof(new_crumb->name));
    bsg_strncpy_safe(new_crumb->timestamp, old_crumb->timestamp,
                     sizeof(new_crumb->timestamp));

    for (int j = 0; j < 8; j++) {
      bsg_char_metadata_pair pair = old_crumb->metadata[j];

      if (strlen(pair.value) > 0 && strlen(pair.key) > 0) {
        bsg_add_metadata_value_str(&new_crumb->metadata, "metaData", pair.key,
                                   pair.value);
      }
    }

    // add crumb to event by copying memory
    bugsnag_report_v3_add_breadcrumb(event, new_crumb);
    free(new_crumb);
  }
}

void migrate_breadcrumb_v2(bugsnag_report_v5 *report_v5, bugsnag_event *event) {
  int old_first_index = report_v5->crumb_first_index;
  event->crumb_count = report_v5->crumb_count;
  event->crumb_first_index = 0; // sort crumbs while copying across

  // rationalize order of breadcrumbs while copying over to new struct
  for (int new_index = 0; new_index < event->crumb_count; new_index++) {
    int old_index = (new_index + old_first_index) % V2_BUGSNAG_CRUMBS_MAX;
    bugsnag_breadcrumb crumb = report_v5->breadcrumbs[old_index];
    memcpy(&event->breadcrumbs[new_index], &crumb, sizeof(bugsnag_breadcrumb));
  }
}

bugsnag_event *bsg_map_v1_to_report(bugsnag_report_v1 *report_v1) {
  if (report_v1 == NULL) {
    return NULL;
  }
  size_t report_size = sizeof(bugsnag_report_v2);
  bugsnag_report_v2 *event_v2 = calloc(1, report_size);

  if (event_v2 != NULL) {
    event_v2->notifier = report_v1->notifier;
    event_v2->app = report_v1->app;
    event_v2->device = report_v1->device;
    event_v2->user = report_v1->user;
    event_v2->exception = report_v1->exception;
    event_v2->metadata = report_v1->metadata;
    event_v2->crumb_count = report_v1->crumb_count;
    event_v2->crumb_first_index = report_v1->crumb_first_index;

    size_t breadcrumb_size =
        sizeof(bugsnag_breadcrumb_v1) * V1_BUGSNAG_CRUMBS_MAX;
    memcpy(&event_v2->breadcrumbs, report_v1->breadcrumbs, breadcrumb_size);

    strcpy(event_v2->context, report_v1->context);
    event_v2->severity = report_v1->severity;
    strcpy(event_v2->session_id, report_v1->session_id);
    strcpy(event_v2->session_start, report_v1->session_start);
    event_v2->handled_events = report_v1->handled_events;
    event_v2->unhandled_events = 1;

    free(report_v1);
  }
  return bsg_map_v2_to_report(event_v2);
}

void migrate_app_v1(bugsnag_report_v2 *report_v2, bugsnag_report_v3 *event) {
  bsg_strcpy(event->app.id, report_v2->app.id);
  bsg_strcpy(event->app.release_stage, report_v2->app.release_stage);
  bsg_strcpy(event->app.type, report_v2->app.type);
  bsg_strcpy(event->app.version, report_v2->app.version);
  bsg_strcpy(event->app.active_screen, report_v2->app.active_screen);
  bsg_strcpy(event->app.build_uuid, report_v2->app.build_uuid);
  bsg_strcpy(event->app.binary_arch, report_v2->app.binaryArch);
  event->app.version_code = report_v2->app.version_code;
  event->app.duration = report_v2->app.duration;
  event->app.duration_in_foreground = report_v2->app.duration_in_foreground;
  event->app.duration_ms_offset = report_v2->app.duration_ms_offset;
  event->app.duration_in_foreground_ms_offset =
      report_v2->app.duration_in_foreground_ms_offset;
  event->app.in_foreground = report_v2->app.in_foreground;

  // migrate legacy fields to metadata
  add_metadata_string(&event->metadata, "app", "packageName",
                      report_v2->app.package_name);
  add_metadata_string(&event->metadata, "app", "versionName",
                      report_v2->app.version_name);
  add_metadata_string(&event->metadata, "app", "name", report_v2->app.name);
}

void migrate_app_v2(bugsnag_report_v4 *report_v4, bugsnag_event *event) {
  bsg_strncpy_safe(event->app.id, report_v4->app.id, sizeof(event->app.id));
  bsg_strncpy_safe(event->app.release_stage, report_v4->app.release_stage,
                   sizeof(event->app.release_stage));
  bsg_strncpy_safe(event->app.type, report_v4->app.type,
                   sizeof(event->app.type));
  bsg_strncpy_safe(event->app.version, report_v4->app.version,
                   sizeof(event->app.version));
  bsg_strncpy_safe(event->app.active_screen, report_v4->app.active_screen,
                   sizeof(event->app.active_screen));
  bsg_strncpy_safe(event->app.build_uuid, report_v4->app.build_uuid,
                   sizeof(event->app.build_uuid));
  bsg_strncpy_safe(event->app.binary_arch, report_v4->app.binary_arch,
                   sizeof(event->app.binary_arch));
  event->app.version_code = report_v4->app.version_code;
  event->app.duration = report_v4->app.duration;
  event->app.duration_in_foreground = report_v4->app.duration_in_foreground;
  event->app.duration_ms_offset = report_v4->app.duration_ms_offset;
  event->app.duration_in_foreground_ms_offset =
      report_v4->app.duration_in_foreground_ms_offset;
  event->app.in_foreground = report_v4->app.in_foreground;

  // no info available, set to sensible default
  event->app.is_launching = false;
}

void migrate_app_v3(bsg_app_info *output, bsg_app_info_v3 *input) {
  memcpy(&output->id, input->id, sizeof(input->id));
  memcpy(&output->release_stage, input->release_stage,
         sizeof(input->release_stage));
  memcpy(&output->type, input->type, sizeof(input->type));
  memcpy(&output->version, input->version, sizeof(input->version));
  memcpy(&output->active_screen, input->active_screen,
         sizeof(input->active_screen));
  output->version_code = input->version_code;
  memcpy(&output->build_uuid, input->build_uuid, sizeof(input->build_uuid));
  output->duration = input->duration;
  output->duration_in_foreground = input->duration_in_foreground;
  output->duration_ms_offset = input->duration_ms_offset;
  output->duration_in_foreground_ms_offset =
      input->duration_in_foreground_ms_offset;
  output->in_foreground = input->in_foreground;
  output->is_launching = input->is_launching;
  memcpy(&output->binary_arch, input->binary_arch, sizeof(input->binary_arch));
}

static char *read_string(int fd) {
  ssize_t len;
  uint32_t string_length;
  len = read(fd, &string_length, sizeof(string_length));

  if (len != sizeof(string_length)) {
    return NULL;
  }

  // allocate enough space, zero filler, and with a trailing '\0' terminator
  char *string_buffer = calloc(1, (string_length + 1));
  if (!string_buffer) {
    return NULL;
  }

  len = read(fd, string_buffer, string_length);
  if (len != string_length) {
    free(string_buffer);
    return NULL;
  }

  return string_buffer;
}

int read_byte(int fd) {
  char value;
  if (read(fd, &value, 1) != 1) {
    return -1;
  }

  return value;
}

void bsg_read_feature_flags(int fd, bsg_feature_flag **out_feature_flags,
                            size_t *out_feature_flag_count) {

  ssize_t len;
  uint32_t feature_flag_count = 0;
  len = read(fd, &feature_flag_count, sizeof(feature_flag_count));
  if (len != sizeof(feature_flag_count)) {
    goto feature_flags_error;
  }

  bsg_feature_flag *flags =
      calloc(feature_flag_count, sizeof(bsg_feature_flag));
  for (uint32_t index = 0; index < feature_flag_count; index++) {
    char *name = read_string(fd);
    if (!name) {
      goto feature_flags_error;
    }

    int variant_exists = read_byte(fd);
    if (variant_exists < 0) {
      goto feature_flags_error;
    }

    char *variant = NULL;
    if (variant_exists) {
      variant = read_string(fd);
      if (!variant) {
        goto feature_flags_error;
      }
    }

    flags[index].name = name;
    flags[index].variant = variant;
  }

  *out_feature_flag_count = feature_flag_count;
  *out_feature_flags = flags;

  return;

feature_flags_error:
  // something wrong - we release all allocated memory
  for (uint32_t index = 0; index < feature_flag_count; index++) {
    if (flags[index].name) {
      free(flags[index].name);
    }

    if (flags[index].variant) {
      free(flags[index].variant);
    }
  }

  free(flags);

  // clear the out fields to indicate no feature-flags are availables
  *out_feature_flag_count = 0;
  *out_feature_flags = NULL;
}
