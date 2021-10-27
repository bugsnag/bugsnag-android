#include "serializer.h"
#include "string.h"

#include <event.h>
#include <fcntl.h>
#include <metadata.h>
#include <parson/parson.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <utils/migrate.h>

#ifdef __cplusplus
extern "C" {
#endif
bool bsg_event_write(bsg_report_header *header, bugsnag_event *event, int fd);

bugsnag_event *bsg_event_read(int fd);
bsg_report_header *bsg_report_header_read(int fd);
bugsnag_event *bsg_map_v6_to_report(bugsnag_report_v6 *report_v6);
bugsnag_event *bsg_map_v5_to_report(bugsnag_report_v5 *report_v5);
bugsnag_event *bsg_map_v4_to_report(bugsnag_report_v4 *report_v4);
bugsnag_event *bsg_map_v3_to_report(bugsnag_report_v3 *report_v3);
bugsnag_event *bsg_map_v2_to_report(bugsnag_report_v2 *report_v2);
bugsnag_event *bsg_map_v1_to_report(bugsnag_report_v1 *report_v1);

void migrate_app_v1(bugsnag_report_v2 *report_v2, bugsnag_report_v3 *event);
void migrate_app_v2(bugsnag_report_v4 *report_v4, bugsnag_event *event);
void migrate_device_v1(bugsnag_report_v2 *report_v2, bugsnag_report_v3 *event);
void migrate_breadcrumb_v1(bugsnag_report_v2 *report_v2,
                           bugsnag_report_v3 *event);
void migrate_breadcrumb_v2(bugsnag_report_v5 *report_v5, bugsnag_event *event);

#ifdef __cplusplus
}
#endif

/**
 * Serializes the LastRunInfo to the file. This persists information about
 * why the current launch crashed, for use on future launch.
 */
bool bsg_serialize_last_run_info_to_file(bsg_environment *env) {
  char *path = env->last_run_info_path;
  int fd = open(path, O_WRONLY | O_CREAT | O_TRUNC, 0644);
  if (fd == -1) {
    return false;
  }

  int size = bsg_strlen(env->next_last_run_info);
  ssize_t len = write(fd, env->next_last_run_info, size);
  return len == size;
}

bool bsg_serialize_event_to_file(bsg_environment *env) {
  int fd = open(env->next_event_path, O_WRONLY | O_CREAT, 0644);
  if (fd == -1) {
    return false;
  }

  return bsg_event_write(&env->report_header, &env->next_event, fd);
}

bugsnag_event *bsg_deserialize_event_from_file(char *filepath) {
  int fd = open(filepath, O_RDONLY);
  if (fd == -1) {
    return NULL;
  }

  return bsg_event_read(fd);
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

bugsnag_event *bsg_report_v7_read(int fd) {
  size_t event_size = sizeof(bugsnag_event);
  bugsnag_event *event = calloc(1, event_size);

  ssize_t len = read(fd, event, event_size);
  if (len != event_size) {
    free(event);
    return NULL;
  }
  return event;
}

/**
 * Reads persisted structs into memory from disk. The report version is
 * serialized in the file header, and old structs are maintained in migrate.h
 * for backwards compatibility. These are then migrated to the current
 * bugsnag_event struct.
 *
 * Note that calling the individual bsg_map_v functions will free the parameter
 * - this is to conserve memory when migrating particularly old payload
 * versions.
 */
bugsnag_event *bsg_event_read(int fd) {
  bsg_report_header *header = bsg_report_header_read(fd);
  if (header == NULL) {
    return NULL;
  }

  int event_version = header->version;
  free(header);
  bugsnag_event *event = NULL;

  if (event_version == 1) { // 'event->unhandled_events' was added in v2
    bugsnag_report_v1 *report_v1 = bsg_report_v1_read(fd);
    event = bsg_map_v1_to_report(report_v1);
  } else if (event_version == 2) {
    bugsnag_report_v2 *report_v2 = bsg_report_v2_read(fd);
    event = bsg_map_v2_to_report(report_v2);
  } else if (event_version == 3) {
    bugsnag_report_v3 *report_v3 = bsg_report_v3_read(fd);
    event = bsg_map_v3_to_report(report_v3);
  } else if (event_version == 4) {
    bugsnag_report_v4 *report_v4 = bsg_report_v4_read(fd);
    event = bsg_map_v4_to_report(report_v4);
  } else if (event_version == 5) {
    bugsnag_report_v5 *report_v5 = bsg_report_v5_read(fd);
    event = bsg_map_v5_to_report(report_v5);
  } else if (event_version == 6) {
    bugsnag_report_v6 *report_v6 = bsg_report_v6_read(fd);
    event = bsg_map_v6_to_report(report_v6);
  } else if (event_version == 7) {
    event = bsg_report_v7_read(fd);
  }
  return event;
}

bugsnag_event *bsg_map_v6_to_report(bugsnag_report_v6 *report_v6) {
  if (report_v6 == NULL) {
    return NULL;
  }
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));

  if (event != NULL) {
    memcpy(event, report_v6, sizeof(bugsnag_report_v6));
    free(report_v6);
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
    event->app = report_v5->app;
    event->device = report_v5->device;
    bsg_strcpy(event->context, report_v5->context);
    event->user = report_v5->user;
    event->error = report_v5->error;
    event->metadata = report_v5->metadata;
    event->severity = report_v5->severity;
    bsg_strncpy_safe(event->session_id, report_v5->session_id,
                     sizeof(event->session_id));
    bsg_strncpy_safe(event->session_start, report_v5->session_start,
                     sizeof(event->session_id));
    event->handled_events = report_v5->handled_events;
    event->unhandled_events = report_v5->unhandled_events;
    bsg_strncpy_safe(event->grouping_hash, report_v5->grouping_hash,
                     sizeof(event->session_id));
    event->unhandled = report_v5->unhandled;
    bsg_strncpy_safe(event->api_key, report_v5->api_key,
                     sizeof(event->api_key));

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
    event->device = report_v4->device;
    event->user = report_v4->user;
    event->error = report_v4->error;
    event->metadata = report_v4->metadata;
    event->crumb_count = report_v4->crumb_count;
    event->crumb_first_index = report_v4->crumb_first_index;
    memcpy(event->breadcrumbs, report_v4->breadcrumbs,
           sizeof(event->breadcrumbs));
    event->severity = report_v4->severity;
    bsg_strncpy_safe(event->session_id, report_v4->session_id,
                     sizeof(event->session_id));
    bsg_strncpy_safe(event->session_start, report_v4->session_start,
                     sizeof(event->session_id));
    event->handled_events = report_v4->handled_events;
    event->unhandled_events = report_v4->unhandled_events;
    bsg_strncpy_safe(event->grouping_hash, report_v4->grouping_hash,
                     sizeof(event->session_id));
    event->unhandled = report_v4->unhandled;
    bsg_strncpy_safe(event->api_key, report_v4->api_key,
                     sizeof(event->api_key));
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
           sizeof(event->breadcrumbs));
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
  bugsnag_event_add_metadata_string(event, "app", "packageName",
                                    report_v2->app.package_name);
  bugsnag_event_add_metadata_string(event, "app", "versionName",
                                    report_v2->app.version_name);
  bugsnag_event_add_metadata_string(event, "app", "name", report_v2->app.name);
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

void migrate_device_v1(bugsnag_report_v2 *report_v2, bugsnag_report_v3 *event) {
  bsg_strcpy(event->device.os_name,
             bsg_os_name()); // os_name was not a field in v2
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
  bugsnag_event_add_metadata_bool(event, "device", "emulator",
                                  report_v2->device.emulator);
  bugsnag_event_add_metadata_double(event, "device", "dpi",
                                    report_v2->device.dpi);
  bugsnag_event_add_metadata_double(event, "device", "screenDensity",
                                    report_v2->device.screen_density);
  bugsnag_event_add_metadata_double(event, "device", "batteryLevel",
                                    report_v2->device.battery_level);
  bugsnag_event_add_metadata_string(event, "device", "locationStatus",
                                    report_v2->device.location_status);
  bugsnag_event_add_metadata_string(event, "device", "brand",
                                    report_v2->device.brand);
  bugsnag_event_add_metadata_string(event, "device", "networkAccess",
                                    report_v2->device.network_access);
  bugsnag_event_add_metadata_string(event, "device", "screenResolution",
                                    report_v2->device.screen_resolution);
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

bsg_report_header *bsg_report_header_read(int fd) {
  bsg_report_header *header = calloc(1, sizeof(bsg_report_header));
  ssize_t len = read(fd, header, sizeof(bsg_report_header));
  if (len != sizeof(bsg_report_header)) {
    free(header);
    return NULL;
  }

  return header;
}

bool bsg_report_header_write(bsg_report_header *header, int fd) {
  ssize_t len = write(fd, header, sizeof(bsg_report_header));

  return len == sizeof(bsg_report_header);
}

bool bsg_event_write(bsg_report_header *header, bugsnag_event *event, int fd) {
  if (!bsg_report_header_write(header, fd)) {
    return false;
  }

  ssize_t len = write(fd, event, sizeof(bugsnag_event));
  return len == sizeof(bugsnag_event);
}

const char *bsg_crumb_type_string(bugsnag_breadcrumb_type type) {
  switch (type) {
  case BSG_CRUMB_ERROR:
    return "error";
  case BSG_CRUMB_LOG:
    return "log";
  case BSG_CRUMB_MANUAL:
    return "manual";
  case BSG_CRUMB_NAVIGATION:
    return "navigation";
  case BSG_CRUMB_PROCESS:
    return "process";
  case BSG_CRUMB_REQUEST:
    return "request";
  case BSG_CRUMB_STATE:
    return "state";
  case BSG_CRUMB_USER:
    return "user";
  }
}

const char *bsg_severity_string(bugsnag_severity type) {
  switch (type) {
  case BSG_SEVERITY_INFO:
    return "info";
  case BSG_SEVERITY_WARN:
    return "warn";
  case BSG_SEVERITY_ERR:
    return "error";
  }
}

void bsg_serialize_context(const bugsnag_event *event, JSON_Object *event_obj) {
  json_object_set_string(event_obj, "context", event->context);
}

void bsg_serialize_grouping_hash(const bugsnag_event *event,
                                 JSON_Object *event_obj) {
  if (strlen(event->grouping_hash) > 0) {
    json_object_set_string(event_obj, "groupingHash", event->grouping_hash);
  }
}

void bsg_serialize_severity_reason(const bugsnag_event *event,
                                   JSON_Object *event_obj) {
  // FUTURE(dm): severityReason/unhandled attributes are currently
  // over-optimized for signal handling. in the future we may want to handle
  // C++ exceptions, etc as well.
  json_object_set_string(event_obj, "severity",
                         bsg_severity_string(event->severity));
  bool unhandled = event->unhandled;
  json_object_dotset_boolean(event_obj, "unhandled", unhandled);

  // unhandled == false always means that the state has been overridden by the
  // user, as this codepath is only executed for unhandled native errors
  json_object_dotset_boolean(event_obj, "severityReason.unhandledOverridden",
                             !unhandled);
  json_object_dotset_string(event_obj, "severityReason.type", "signal");
  json_object_dotset_string(event_obj, "severityReason.attributes.signalType",
                            event->error.errorClass);
}

void bsg_serialize_app(const bsg_app_info app, JSON_Object *event_obj) {
  json_object_dotset_string(event_obj, "app.version", app.version);
  json_object_dotset_string(event_obj, "app.id", app.id);
  json_object_dotset_string(event_obj, "app.type", app.type);

  json_object_dotset_string(event_obj, "app.releaseStage", app.release_stage);
  json_object_dotset_number(event_obj, "app.versionCode", app.version_code);
  if (strlen(app.build_uuid) > 0) {
    json_object_dotset_string(event_obj, "app.buildUUID", app.build_uuid);
  }
  json_object_dotset_string(event_obj, "app.binaryArch", app.binary_arch);
  json_object_dotset_number(event_obj, "app.duration", app.duration);
  json_object_dotset_number(event_obj, "app.durationInForeground",
                            app.duration_in_foreground);
  json_object_dotset_boolean(event_obj, "app.inForeground", app.in_foreground);
  json_object_dotset_boolean(event_obj, "app.isLaunching", app.is_launching);
}

void bsg_serialize_app_metadata(const bsg_app_info app,
                                JSON_Object *event_obj) {
  json_object_dotset_string(event_obj, "metaData.app.activeScreen",
                            app.active_screen);
}

void bsg_serialize_device(const bsg_device_info device,
                          JSON_Object *event_obj) {
  json_object_dotset_string(event_obj, "device.osName", device.os_name);
  json_object_dotset_string(event_obj, "device.id", device.id);
  json_object_dotset_string(event_obj, "device.locale", device.locale);
  json_object_dotset_string(event_obj, "device.osVersion", device.os_version);
  json_object_dotset_string(event_obj, "device.manufacturer",
                            device.manufacturer);
  json_object_dotset_string(event_obj, "device.model", device.model);
  json_object_dotset_string(event_obj, "device.orientation",
                            device.orientation);
  json_object_dotset_number(event_obj, "device.runtimeVersions.androidApiLevel",
                            device.api_level);
  json_object_dotset_string(event_obj, "device.runtimeVersions.osBuild",
                            device.os_build);

  JSON_Value *abi_val = json_value_init_array();
  JSON_Array *cpu_abis = json_value_get_array(abi_val);
  json_object_dotset_value(event_obj, "device.cpuAbi", abi_val);
  for (int i = 0; i < device.cpu_abi_count; i++) {
    json_array_append_string(cpu_abis, device.cpu_abi[i].value);
  }

  json_object_dotset_number(event_obj, "device.totalMemory",
                            device.total_memory);
  json_object_dotset_boolean(event_obj, "device.jailbroken", device.jailbroken);

  char report_time[sizeof "2018-10-08T12:07:09Z"];
  if (device.time > 0) {
    strftime(report_time, sizeof report_time, "%FT%TZ", gmtime(&device.time));
    json_object_dotset_string(event_obj, "device.time", report_time);
  }
}

void bsg_serialize_device_metadata(const bsg_device_info device,
                                   JSON_Object *event_obj) {}

void bsg_serialize_custom_metadata(const bugsnag_metadata metadata,
                                   JSON_Object *event_obj) {
  for (int i = 0; i < metadata.value_count; i++) {
    char *format = calloc(1, sizeof(char) * 256);
    bsg_metadata_value value = metadata.values[i];

    switch (value.type) {
    case BSG_METADATA_BOOL_VALUE:
      sprintf(format, "metaData.%s.%s", value.section, value.name);
      json_object_dotset_boolean(event_obj, format, value.bool_value);
      break;
    case BSG_METADATA_CHAR_VALUE:
      sprintf(format, "metaData.%s.%s", value.section, value.name);
      json_object_dotset_string(event_obj, format, value.char_value);
      break;
    case BSG_METADATA_NUMBER_VALUE:
      sprintf(format, "metaData.%s.%s", value.section, value.name);
      json_object_dotset_number(event_obj, format, value.double_value);
      break;
    default:
      break;
    }
    free(format);
  }
}

void bsg_serialize_breadcrumb_metadata(const bugsnag_metadata metadata,
                                       JSON_Object *event_obj) {
  for (int i = 0; i < metadata.value_count; i++) {
    char *format = calloc(1, sizeof(char) * 256);
    bsg_metadata_value value = metadata.values[i];

    switch (value.type) {
    case BSG_METADATA_BOOL_VALUE:
      sprintf(format, "metaData.%s", value.name);
      json_object_dotset_boolean(event_obj, format, value.bool_value);
      break;
    case BSG_METADATA_CHAR_VALUE:
      sprintf(format, "metaData.%s", value.name);
      json_object_dotset_string(event_obj, format, value.char_value);
      break;
    case BSG_METADATA_NUMBER_VALUE:
      sprintf(format, "metaData.%s", value.name);
      json_object_dotset_number(event_obj, format, value.double_value);
      break;
    default:
      break;
    }
    free(format);
  }
}

void bsg_serialize_user(const bugsnag_user user, JSON_Object *event_obj) {
  if (strlen(user.name) > 0)
    json_object_dotset_string(event_obj, "user.name", user.name);
  if (strlen(user.email) > 0)
    json_object_dotset_string(event_obj, "user.email", user.email);
  if (strlen(user.id) > 0)
    json_object_dotset_string(event_obj, "user.id", user.id);
}

void bsg_serialize_session(bugsnag_event *event, JSON_Object *event_obj) {
  if (bugsnag_event_has_session(event)) {
    json_object_dotset_string(event_obj, "session.startedAt",
                              event->session_start);
    json_object_dotset_string(event_obj, "session.id", event->session_id);
    json_object_dotset_number(event_obj, "session.events.handled",
                              event->handled_events);
    json_object_dotset_number(event_obj, "session.events.unhandled",
                              event->unhandled_events);
  }
}

void bsg_serialize_error(bsg_error exc, JSON_Object *exception,
                         JSON_Array *stacktrace) {
  json_object_set_string(exception, "errorClass", exc.errorClass);
  json_object_set_string(exception, "message", exc.errorMessage);
  json_object_set_string(exception, "type", "c");
  // assuming that the initial frame is the program counter. This logic will
  // need to be revisited if (for example) we add more intelligent processing
  // for stack overflow-type errors, like discarding the top frames, which
  // would mean no stored frame is the program counter.
  if (exc.frame_count > 0) {
    bsg_serialize_stackframe(&(exc.stacktrace[0]), true, stacktrace);
  }
  for (int findex = 1; findex < exc.frame_count; findex++) {
    bugsnag_stackframe stackframe = exc.stacktrace[findex];
    bsg_serialize_stackframe(&stackframe, false, stacktrace);
  }
}

void bsg_serialize_stackframe(bugsnag_stackframe *stackframe, bool is_pc,
                              JSON_Array *stacktrace) {
  JSON_Value *frame_val = json_value_init_object();
  JSON_Object *frame = json_value_get_object(frame_val);
  json_object_set_number(frame, "frameAddress", (*stackframe).frame_address);
  json_object_set_number(frame, "symbolAddress", (*stackframe).symbol_address);
  json_object_set_number(frame, "loadAddress", (*stackframe).load_address);
  json_object_set_number(frame, "lineNumber", (*stackframe).line_number);
  if (is_pc) {
    // only necessary to set to true, false is the default value and omitting
    // the field keeps payload sizes smaller.
    json_object_set_boolean(frame, "isPC", true);
  }
  if (strlen((*stackframe).filename) > 0) {
    json_object_set_string(frame, "file", (*stackframe).filename);
  }
  if (strlen((*stackframe).method) == 0) {
    char *frame_address = calloc(1, sizeof(char) * 32);
    sprintf(frame_address, "0x%lx", (unsigned long)(*stackframe).frame_address);
    json_object_set_string(frame, "method", frame_address);
    free(frame_address);
  } else {
    json_object_set_string(frame, "method", (*stackframe).method);
  }

  json_array_append_value(stacktrace, frame_val);
}

void bsg_serialize_breadcrumbs(const bugsnag_event *event, JSON_Array *crumbs) {
  if (event->crumb_count > 0) {
    int current_index = event->crumb_first_index;
    while (json_array_get_count(crumbs) < event->crumb_count) {
      JSON_Value *crumb_val = json_value_init_object();
      JSON_Object *crumb = json_value_get_object(crumb_val);
      json_array_append_value(crumbs, crumb_val);

      bugsnag_breadcrumb breadcrumb = event->breadcrumbs[current_index];
      json_object_set_string(crumb, "name", breadcrumb.name);
      json_object_set_string(crumb, "timestamp", breadcrumb.timestamp);
      json_object_set_string(crumb, "type",
                             bsg_crumb_type_string(breadcrumb.type));
      bsg_serialize_breadcrumb_metadata(breadcrumb.metadata, crumb);
      current_index++;
      if (current_index == BUGSNAG_CRUMBS_MAX) {
        current_index = 0;
      }
    }
  }
}

void bsg_serialize_threads(const bugsnag_event *event, JSON_Array *threads) {
  if (event->thread_count <= 0) {
    return;
  }

  for (int index = 0; index < event->thread_count; index++) {
    JSON_Value *thread_val = json_value_init_object();
    JSON_Object *json_thread = json_value_get_object(thread_val);
    json_array_append_value(threads, thread_val);

    const bsg_thread *thread = &event->threads[index];
    json_object_set_number(json_thread, "id", (double)thread->id);
    json_object_set_string(json_thread, "name", thread->name);
    json_object_set_string(json_thread, "state", thread->state);
    json_object_set_string(json_thread, "type", "c");
  }
}

char *bsg_serialize_event_to_json_string(bugsnag_event *event) {
  JSON_Value *event_val = json_value_init_object();
  JSON_Object *event_obj = json_value_get_object(event_val);
  JSON_Value *crumbs_val = json_value_init_array();
  JSON_Array *crumbs = json_value_get_array(crumbs_val);
  JSON_Value *exceptions_val = json_value_init_array();
  JSON_Array *exceptions = json_value_get_array(exceptions_val);
  JSON_Value *ex_val = json_value_init_object();
  JSON_Object *exception = json_value_get_object(ex_val);
  JSON_Value *threads_val = json_value_init_array();
  JSON_Array *threads = json_value_get_array(threads_val);
  JSON_Value *stack_val = json_value_init_array();
  JSON_Array *stacktrace = json_value_get_array(stack_val);
  json_object_set_value(event_obj, "exceptions", exceptions_val);
  json_object_set_value(event_obj, "breadcrumbs", crumbs_val);
  json_object_set_value(event_obj, "threads", threads_val);
  json_object_set_value(exception, "stacktrace", stack_val);
  json_array_append_value(exceptions, ex_val);
  char *serialized_string = NULL;
  {
    bsg_serialize_context(event, event_obj);
    bsg_serialize_grouping_hash(event, event_obj);
    bsg_serialize_severity_reason(event, event_obj);
    bsg_serialize_app(event->app, event_obj);
    bsg_serialize_app_metadata(event->app, event_obj);
    bsg_serialize_device(event->device, event_obj);
    bsg_serialize_device_metadata(event->device, event_obj);
    bsg_serialize_custom_metadata(event->metadata, event_obj);
    bsg_serialize_user(event->user, event_obj);
    bsg_serialize_session(event, event_obj);
    bsg_serialize_error(event->error, exception, stacktrace);
    bsg_serialize_breadcrumbs(event, crumbs);
    bsg_serialize_threads(event, threads);

    serialized_string = json_serialize_to_string(event_val);
    json_value_free(event_val);
  }
  return serialized_string;
}
