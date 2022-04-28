#include "json_writer.h"

#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#include <parson/parson.h>

#include "../logger.h"

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
  default:
    return "";
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
  default:
    return "";
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
  char android_api_level[sizeof "1234"];
  snprintf(android_api_level, 4, "%d", device.api_level);
  json_object_dotset_string(event_obj, "device.runtimeVersions.androidApiLevel",
                            android_api_level);
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

#if defined(__i386__) || defined(__arm__)
#define TIMESTAMP_T long long
#define TIMESTAMP_DECODE atoll
#define TIMESTAMP_MILLIS_FORMAT "%s.%03lldZ"
#elif defined(__x86_64__) || defined(__aarch64__)
#define TIMESTAMP_T long
#define TIMESTAMP_DECODE atol
#define TIMESTAMP_MILLIS_FORMAT "%s.%03ldZ"
#endif
/**
 * Convert a string representing the number of milliseconds since the epoch
 * into the date format "yyyy-MM-ddTHH:mm:ss.SSSZ". Safe for all dates earlier
 * than 2038.
 *
 * @param source the timestamp string, should be something like: 1636710533109
 * @param dest   a buffer large enough to hold the 24 characters required in the
 *               date format
 *
 * @return true if the conversion succeeded
 */
static bool timestamp_to_iso8601_millis(const char *source, char *dest) {
  TIMESTAMP_T timestamp = TIMESTAMP_DECODE(source);
  if (timestamp) {
    time_t seconds = timestamp / 1000;
    TIMESTAMP_T milliseconds = timestamp - (seconds * 1000LL);
    if (milliseconds > 1000) { // round to nearest second
      seconds++;
      milliseconds -= 1000;
    }
    struct tm timer;
    // gmtime(3) can fail if "the year does not fit into an integer". Hopefully
    // nobody is running this code by then.
    if (gmtime_r(&seconds, &timer)) {
      char buffer[26];
      strftime(buffer, 26, "%Y-%m-%dT%H:%M:%S", &timer);
      sprintf(dest, TIMESTAMP_MILLIS_FORMAT, buffer, milliseconds);
      return true;
    } else {
      BUGSNAG_LOG("Hello, people of the far future! Please use your time "
                  "machine to file a bug in the year 2021.");
    }
  }
  return false;
}
#undef TIMESTAMP_T
#undef TIMESTAMP_DECODE
#undef TIMESTAMP_MILLIS_FORMAT

void bsg_serialize_breadcrumbs(const bugsnag_event *event, JSON_Array *crumbs) {
  if (event->crumb_count > 0) {
    int current_index = event->crumb_first_index;
    while (json_array_get_count(crumbs) < event->crumb_count) {
      JSON_Value *crumb_val = json_value_init_object();
      JSON_Object *crumb = json_value_get_object(crumb_val);
      json_array_append_value(crumbs, crumb_val);

      bugsnag_breadcrumb breadcrumb = event->breadcrumbs[current_index];
      json_object_set_string(crumb, "name", breadcrumb.name);
      // check whether to decode milliseconds into ISO8601 date format
      if (breadcrumb.timestamp[0] == 't') {
        char *unix_timestamp_str = breadcrumb.timestamp + 1;
        char buffer[32];
        if (timestamp_to_iso8601_millis(unix_timestamp_str, buffer)) {
          json_object_set_string(crumb, "timestamp", buffer);
        } else {
          // at least we tried.
          json_object_set_string(crumb, "timestamp", unix_timestamp_str);
        }
      } else {
        json_object_set_string(crumb, "timestamp", breadcrumb.timestamp);
      }
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

void bsg_serialize_feature_flags(const bugsnag_event *event,
                                 JSON_Array *feature_flags) {
  if (event->feature_flag_count <= 0) {
    return;
  }

  for (int index = 0; index < event->feature_flag_count; index++) {
    JSON_Value *feature_flag_val = json_value_init_object();
    JSON_Object *feature_flag = json_value_get_object(feature_flag_val);
    json_array_append_value(feature_flags, feature_flag_val);

    const bsg_feature_flag *flag = &event->feature_flags[index];
    json_object_set_string(feature_flag, "featureFlag", flag->name);

    if (flag->variant) {
      json_object_set_string(feature_flag, "variant", flag->variant);
    }
  }
}

char *bsg_event_to_json(bugsnag_event *event) {
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
  JSON_Value *feature_flags_val = json_value_init_array();
  JSON_Array *feature_flags = json_value_get_array(feature_flags_val);
  json_object_set_value(event_obj, "exceptions", exceptions_val);
  json_object_set_value(event_obj, "breadcrumbs", crumbs_val);
  json_object_set_value(event_obj, "threads", threads_val);
  json_object_set_value(exception, "stacktrace", stack_val);
  json_object_set_value(event_obj, "featureFlags", feature_flags_val);
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
    bsg_serialize_feature_flags(event, feature_flags);

    serialized_string = json_serialize_to_string(event_val);
    json_value_free(event_val);
  }
  return serialized_string;
}
