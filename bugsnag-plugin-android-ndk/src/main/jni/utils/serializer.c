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

static bool event_write(bsg_report_header *header, bugsnag_event *event,
                        int fd);
static bugsnag_event *event_read(int fd);
static bsg_report_header *report_header_read(int fd);

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

  return event_write(&env->report_header, &env->next_event, fd);
}

bugsnag_event *bsg_deserialize_event_from_file(char *filepath) {
  int fd = open(filepath, O_RDONLY);
  if (fd == -1) {
    return NULL;
  }

  return event_read(fd);
}

static bugsnag_event *report_read(int fd) {
  size_t event_size = sizeof(bugsnag_event);
  bugsnag_event *event = malloc(event_size);

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
static bugsnag_event *event_read(int fd) {
  bsg_report_header *header = report_header_read(fd);
  if (header == NULL) {
    return NULL;
  }

  int event_version = header->version;
  free(header);
  bugsnag_event *event = NULL;

  if (bsg_event_requires_migration(event_version)) {
    return bsg_event_read_and_migrate(event_version, fd);
  } else {
    return report_read(fd);
  }
}

static bsg_report_header *report_header_read(int fd) {
  bsg_report_header *header = malloc(sizeof(bsg_report_header));
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

static bool event_write(bsg_report_header *header, bugsnag_event *event,
                        int fd) {
  if (!bsg_report_header_write(header, fd)) {
    return false;
  }

  ssize_t len = write(fd, event, sizeof(bugsnag_event));
  return len == sizeof(bugsnag_event);
}

static const char *crumb_type_string(bugsnag_breadcrumb_type type) {
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

static const char *severity_string(bugsnag_severity type) {
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
  if (strlen(event->context) > 0) {
    json_object_set_string(event_obj, "context", event->context);
  } else {
    json_object_set_string(event_obj, "context", event->app.active_screen);
  }
}

static void serialize_grouping_hash(const bugsnag_event *event,
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
                         severity_string(event->severity));
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
    char *format = malloc(sizeof(char) * 256);
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
    char *format = malloc(sizeof(char) * 256);
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

void bsg_serialize_stackframe(bugsnag_stackframe *stackframe,
                              JSON_Array *stacktrace) {
  JSON_Value *frame_val = json_value_init_object();
  JSON_Object *frame = json_value_get_object(frame_val);
  json_object_set_number(frame, "frameAddress", (*stackframe).frame_address);
  json_object_set_number(frame, "symbolAddress", (*stackframe).symbol_address);
  json_object_set_number(frame, "loadAddress", (*stackframe).load_address);
  json_object_set_number(frame, "lineNumber", (*stackframe).line_number);
  if (strlen((*stackframe).filename) > 0) {
    json_object_set_string(frame, "file", (*stackframe).filename);
  }
  if (strlen((*stackframe).method) == 0) {
    char *frame_address = malloc(sizeof(char) * 32);
    sprintf(frame_address, "0x%lx", (unsigned long)(*stackframe).frame_address);
    json_object_set_string(frame, "method", frame_address);
    free(frame_address);
  } else {
    json_object_set_string(frame, "method", (*stackframe).method);
  }

  json_array_append_value(stacktrace, frame_val);
}

void bsg_serialize_error(bsg_error exc, JSON_Object *exception,
                         JSON_Array *stacktrace) {
  json_object_set_string(exception, "errorClass", exc.errorClass);
  json_object_set_string(exception, "message", exc.errorMessage);
  json_object_set_string(exception, "type", "c");
  for (int findex = 0; findex < exc.frame_count; findex++) {
    bugsnag_stackframe stackframe = exc.stacktrace[findex];
    bsg_serialize_stackframe(&stackframe, stacktrace);
  }
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
      json_object_set_string(crumb, "type", crumb_type_string(breadcrumb.type));
      bsg_serialize_breadcrumb_metadata(breadcrumb.metadata, crumb);
      current_index++;
      if (current_index == BUGSNAG_CRUMBS_MAX) {
        current_index = 0;
      }
    }
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
  JSON_Value *stack_val = json_value_init_array();
  JSON_Array *stacktrace = json_value_get_array(stack_val);
  json_object_set_value(event_obj, "exceptions", exceptions_val);
  json_object_set_value(event_obj, "breadcrumbs", crumbs_val);
  json_object_set_value(exception, "stacktrace", stack_val);
  json_array_append_value(exceptions, ex_val);
  char *serialized_string = NULL;
  {
    bsg_serialize_context(event, event_obj);
    serialize_grouping_hash(event, event_obj);
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

    serialized_string = json_serialize_to_string(event_val);
    json_value_free(event_val);
  }
  return serialized_string;
}
