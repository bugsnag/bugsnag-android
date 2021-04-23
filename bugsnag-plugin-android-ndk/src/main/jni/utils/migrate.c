//
// Created by Karl Stenerud on 23.04.21.
//

#include "string.h"

#include "migrate.h"
#include "migrate_internal.h"
#include <event.h>
#include <fcntl.h>
#include <metadata.h>
#include <parson/parson.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

// ==================================================================
// Reading

static bugsnag_report_v1 *report_v1_read(int fd) {
  size_t event_size = sizeof(bugsnag_report_v1);
  bugsnag_report_v1 *event = malloc(event_size);

  ssize_t len = read(fd, event, event_size);
  if (len != event_size) {
    free(event);
    return NULL;
  }
  return event;
}

static bugsnag_report_v2 *report_v2_read(int fd) {
  size_t event_size = sizeof(bugsnag_report_v2);
  bugsnag_report_v2 *event = malloc(event_size);

  ssize_t len = read(fd, event, event_size);
  if (len != event_size) {
    free(event);
    return NULL;
  }
  return event;
}

static bugsnag_report_v3 *report_v3_read(int fd) {
  size_t event_size = sizeof(bugsnag_report_v3);
  bugsnag_report_v3 *event = malloc(event_size);

  ssize_t len = read(fd, event, event_size);
  if (len != event_size) {
    free(event);
    return NULL;
  }
  return event;
}

static bugsnag_report_v4 *report_v4_read(int fd) {
  size_t event_size = sizeof(bugsnag_report_v4);
  bugsnag_report_v4 *event = malloc(event_size);

  ssize_t len = read(fd, event, event_size);
  if (len != event_size) {
    free(event);
    return NULL;
  }
  return event;
}

// ==================================================================
// Migration

int bsg_calculate_total_crumbs(int old_count) {
  return old_count < BUGSNAG_CRUMBS_MAX ? old_count : BUGSNAG_CRUMBS_MAX;
}

int bsg_calculate_v1_start_index(int old_count) {
  return old_count < BUGSNAG_CRUMBS_MAX ? 0 : old_count % BUGSNAG_CRUMBS_MAX;
}

int bsg_calculate_v1_crumb_index(int crumb_pos, int first_index) {
  return (crumb_pos + first_index) % V1_BUGSNAG_CRUMBS_MAX;
}

void bsg_migrate_app_v1(bugsnag_report_v2 *report_v2,
                        bugsnag_report_v3 *event) {
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

void bsg_migrate_app_v2(bugsnag_report_v4 *report_v4, bugsnag_event *event) {
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

static void migrate_device_v1(bugsnag_report_v2 *report_v2,
                              bugsnag_report_v3 *event) {
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

static bugsnag_event *map_v4_to_report(bugsnag_report_v4 *report_v4) {
  if (report_v4 == NULL) {
    return NULL;
  }
  bugsnag_event *event = malloc(sizeof(bugsnag_event));

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
    bsg_migrate_app_v2(report_v4, event);
    free(report_v4);
  }
  return event;
}

static void report_v3_add_breadcrumb(bugsnag_report_v3 *event,
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

static void migrate_breadcrumb_v1(bugsnag_report_v2 *report_v2,
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
    bugsnag_breadcrumb *new_crumb = malloc(sizeof(bugsnag_breadcrumb));

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
    report_v3_add_breadcrumb(event, new_crumb);
    free(new_crumb);
  }
}

static bugsnag_event *map_v3_to_report(bugsnag_report_v3 *report_v3) {
  if (report_v3 == NULL) {
    return NULL;
  }
  bugsnag_report_v4 *event = malloc(sizeof(bugsnag_event));

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
  return map_v4_to_report(event);
}

static bugsnag_event *map_v2_to_report(bugsnag_report_v2 *report_v2) {
  if (report_v2 == NULL) {
    return NULL;
  }
  bugsnag_report_v3 *event = malloc(sizeof(bugsnag_report_v3));

  if (event != NULL) {
    // assign metadata first as old app/device fields are migrated there
    event->metadata = report_v2->metadata;
    bsg_migrate_app_v1(report_v2, event);
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
  return map_v3_to_report(event);
}

static bugsnag_event *map_v1_to_report(bugsnag_report_v1 *report_v1) {
  if (report_v1 == NULL) {
    return NULL;
  }
  size_t report_size = sizeof(bugsnag_report_v2);
  bugsnag_report_v2 *event_v2 = malloc(report_size);

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
  return map_v2_to_report(event_v2);
}

// ==================================================================
// Public API

bool bsg_event_requires_migration(int event_version) {
  return event_version != 5;
}

bugsnag_event *bsg_event_read_and_migrate(int event_version, int fd) {
  if (event_version == 1) { // 'event->unhandled_events' was added in v2
    bugsnag_report_v1 *report_v1 = report_v1_read(fd);
    return map_v1_to_report(report_v1);
  } else if (event_version == 2) {
    bugsnag_report_v2 *report_v2 = report_v2_read(fd);
    return map_v2_to_report(report_v2);
  } else if (event_version == 3) {
    bugsnag_report_v3 *report_v3 = report_v3_read(fd);
    return map_v3_to_report(report_v3);
  } else if (event_version == 4) {
    bugsnag_report_v4 *report_v4 = report_v4_read(fd);
    return map_v4_to_report(report_v4);
  } else {
    return NULL;
  }
}
