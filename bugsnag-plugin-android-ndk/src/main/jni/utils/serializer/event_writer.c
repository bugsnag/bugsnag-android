#include "event_writer.h"

#include <fcntl.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#include "BSG_KSCrashStringConversion.h"
#include "BSG_KSJSONCodec.h"
#include "buffered_writer.h"
#include "event.h"
#include "internal_metrics.h"
#include "utils/string.h"

#define CHECKED(e)                                                             \
  if ((e) != BSG_KSJSON_OK) {                                                  \
    goto error;                                                                \
  }

#define JSON_CONSTANT_ELEMENT(name, value)                                     \
  bsg_ksjsonaddStringElement(json, name, value,                                \
                             sizeof(value) - 1 /* remove terminator */)
#define JSON_LIMITED_STRING_ELEMENT(name, value)                               \
  bsg_ksjsonaddStringElement(json, (name), (value),                            \
                             strnlen((value), sizeof((value))))

#define STR_CONST_CAT(dst, src) bsg_strncpy((dst), (src), sizeof(src))

static bool bsg_write_metadata(BSG_KSJSONEncodeContext *json,
                               bugsnag_metadata *metadata);
static bool bsg_write_severity_reason(BSG_KSJSONEncodeContext *json,
                                      bugsnag_event *event);
static bool bsg_write_user(BSG_KSJSONEncodeContext *json, bugsnag_user *user);
static bool bsg_write_error(BSG_KSJSONEncodeContext *json, bsg_error *error);
static bool bsg_write_app(BSG_KSJSONEncodeContext *json, bsg_app_info *app);
static bool bsg_write_device(BSG_KSJSONEncodeContext *json,
                             bsg_device_info *device);
static bool bsg_write_breadcrumbs(BSG_KSJSONEncodeContext *json,
                                  bugsnag_breadcrumb *breadcrumbs,
                                  int first_crumb_index, int crumb_count,
                                  int max_crumb_count);
static bool bsg_write_threads(BSG_KSJSONEncodeContext *json,
                              bsg_thread *threads, int thread_count);
static bool bsg_write_feature_flags(BSG_KSJSONEncodeContext *json,
                                    bsg_feature_flag *flags, int flag_count);
static bool bsg_write_session(BSG_KSJSONEncodeContext *json,
                              bugsnag_event *event);
static bool bsg_write_usage(BSG_KSJSONEncodeContext *json,
                            bsg_environment *env);

static inline bool string_is_not_empty(const char *restrict s) {
  return (*(s) != 0);
}

static inline bool string_is_empty(const char *restrict s) {
  return (*(s) == 0);
}

static int bsg_write(const char *data, size_t length, void *userData) {
  bsg_buffered_writer *writer = userData;
  return writer->write(writer, data, length) ? BSG_KSJSON_OK
                                             : BSG_KSJSON_ERROR_CANNOT_ADD_DATA;
}

/*
 * Build the event filename in the same format as defined in EventFilenameInfo:
 * "[timestamp]_[apiKey]_[errorTypes]_[UUID]_[startupcrash|not-jvm].json"
 */
static size_t build_filename(bsg_environment *env, char *out) {
  time_t now;
  time(&now);

  int length = strnlen(env->event_path, 4096);
  memcpy(out, env->event_path, length);
  out[length++] = '/';

  // the timestamp is encoded as unix time in millis
  length += bsg_uint64_to_string(now * 1000uL, &out[length]);

  // append the api_key to the filename
  out[length++] = '_';
  length += bsg_strncpy(out + length, env->next_event.api_key,
                        sizeof(env->next_event.api_key));

  // append the errorType (c)
  out[length++] = '_';
  out[length++] = 'c';
  out[length++] = '_';

  // use the pre-generated UUID in `env` - avoiding needing any source of
  // randomness on the signal-handler path
  length += bsg_strncpy(out + length, env->event_uuid, sizeof(env->event_uuid));

  if (env->next_event.app.is_launching) {
    length += STR_CONST_CAT(out + length, "_startupcrash");
  } else {
    length += STR_CONST_CAT(out + length, "_not-jvm");
  }

  length += STR_CONST_CAT(out + length, ".json");

  return length;
}

bool bsg_event_write(bsg_environment *env) {
  char filename[sizeof(env->event_path) + 256];
  filename[0] = '\0';
  build_filename(env, filename);

  return bsg_write_event_file(env, filename);
}

bool bsg_write_event_file(bsg_environment *env, const char *filename) {
  BSG_KSJSONEncodeContext jsonContext;
  bsg_buffered_writer writer;
  bugsnag_event *event = &env->next_event;
  BSG_KSJSONEncodeContext *json = &jsonContext;

  if (!bsg_buffered_writer_open(&writer, filename)) {
    return false;
  }

  bsg_ksjsonbeginEncode(json, false, &bsg_write, &writer);

  CHECKED(bsg_ksjsonbeginObject(json, NULL));
  {
    CHECKED(bsg_ksjsonaddStringElement(
        json, "context", event->context,
        strnlen(event->context, sizeof(event->context))));

    CHECKED(bsg_ksjsonbeginObject(json, "metaData"));
    {
      if (!bsg_write_metadata(json, &event->metadata)) {
        goto error;
      }
    }
    CHECKED(bsg_ksjsonendContainer(json));

    if (!bsg_write_severity_reason(json, event)) {
      goto error;
    }

    // Write the exception / error info
    if (!bsg_write_error(json, &event->error)) {
      goto error;
    }

    // Write user info
    if (!bsg_write_user(json, &event->user)) {
      goto error;
    }

    // Write diagnostics
    if (!bsg_write_app(json, &event->app)) {
      goto error;
    }
    if (!bsg_write_device(json, &event->device)) {
      goto error;
    }
    if (!bsg_write_breadcrumbs(json, event->breadcrumbs,
                               event->crumb_first_index, event->crumb_count,
                               event->max_crumb_count)) {
      goto error;
    }
    if (string_is_not_empty(event->grouping_hash)) {
      CHECKED(
          JSON_LIMITED_STRING_ELEMENT("groupingHash", event->grouping_hash));
    }
    if (!bsg_write_usage(json, env)) {
      goto error;
    }
    if (!bsg_write_threads(json, event->threads, event->thread_count)) {
      goto error;
    }
    if (!bsg_write_feature_flags(json, event->feature_flags,
                                 event->feature_flag_count)) {
      goto error;
    }
    if (!bsg_write_session(json, event)) {
      goto error;
    }
  }
  CHECKED(bsg_ksjsonendContainer(json));

  bsg_ksjsonendEncode(json);
  writer.dispose(&writer);
  return true;
error:
  writer.dispose(&writer);
  return false;
}

static bool bsg_write_metadata_value(BSG_KSJSONEncodeContext *json,
                                     bsg_metadata_value *value) {
  switch (value->type) {
  case BSG_METADATA_BOOL_VALUE:
    CHECKED(bsg_ksjsonaddBooleanElement(json, value->name, value->bool_value));
    break;
  case BSG_METADATA_CHAR_VALUE:
    CHECKED(JSON_LIMITED_STRING_ELEMENT(value->name, value->char_value));
    break;
  case BSG_METADATA_NUMBER_VALUE:
    if (value->double_value == (double)((long long)value->double_value)) {
      CHECKED(bsg_ksjsonaddIntegerElement(json, value->name,
                                          (long long)value->double_value));
    } else {
      CHECKED(bsg_ksjsonaddFloatingPointElement(json, value->name,
                                                value->double_value));
    }
    break;
  case BSG_METADATA_OPAQUE_VALUE:
    CHECKED(bsg_ksjsonbeginElement(json, value->name));
    CHECKED(bsg_ksjsonaddRawJSONData(json, value->opaque_value,
                                     value->opaque_value_size));
    break;
  default:
    break;
  }

  return true;
error:
  return false;
}

static bool bsg_write_metadata(BSG_KSJSONEncodeContext *json,
                               bugsnag_metadata *metadata) {
  const int value_count = metadata->value_count;
  // The metadata values can appear in any order and each entry records which
  // "section" it should appear in. We need to group the values by section and
  // write them out in that order. This array keeps track of which values have
  // already been written, and each time we write a section we mark the values
  // as written.
  // When running through the array of metadata, we have an inner-loop to write
  // all of the entries in the same "section" (a bit like a bubble-sort, if that
  // helps explain it).
  bool written[value_count];

  memset(written, 0, sizeof(bool) * value_count);
  for (int i = 0; i < value_count; i++) {
    bsg_metadata_value *value = &metadata->values[i];
    if (written[i] || value->type == BSG_METADATA_NONE_VALUE) {
      continue;
    }

    CHECKED(bsg_ksjsonbeginObject(json, value->section));
    if (!bsg_write_metadata_value(json, value)) {
      goto error;
    }

    // we flush all of the values within this section, marking each one as
    // written
    const char *section = value->section;
    for (int j = i + 1; j < value_count; j++) {
      bsg_metadata_value *value2 = &metadata->values[j];
      if (written[j] || value2->type == BSG_METADATA_NONE_VALUE) {
        continue;
      }

      if (strncmp(section, value2->section, sizeof(value->section)) == 0) {
        if (!bsg_write_metadata_value(json, value2)) {
          goto error;
        }

        // remember that we have already written this element
        written[j] = true;
      }
    }
    CHECKED(bsg_ksjsonendContainer(json));
  }

  return true;
error:
  return false;
}

static bool bsg_write_severity(BSG_KSJSONEncodeContext *json,
                               bugsnag_severity severity) {

  switch (severity) {
  case BSG_SEVERITY_ERR:
    CHECKED(JSON_CONSTANT_ELEMENT("severity", "error"));
    break;
  case BSG_SEVERITY_WARN:
    CHECKED(JSON_CONSTANT_ELEMENT("severity", "warning"));
    break;
  case BSG_SEVERITY_INFO:
    CHECKED(JSON_CONSTANT_ELEMENT("severity", "info"));
    break;
  }
  return true;
error:
  return false;
}

static bool bsg_write_severity_reason(BSG_KSJSONEncodeContext *json,
                                      bugsnag_event *event) {

  if (!bsg_write_severity(json, event->severity)) {
    goto error;
  }
  CHECKED(bsg_ksjsonaddBooleanElement(json, "unhandled", event->unhandled));

  CHECKED(bsg_ksjsonbeginObject(json, "severityReason"));
  {
    // unhandled == false always means that the state has been overridden by the
    // user, as this codepath is only executed for unhandled native errors
    CHECKED(bsg_ksjsonaddBooleanElement(json, "unhandledOverridden",
                                        !event->unhandled));
    CHECKED(JSON_CONSTANT_ELEMENT("type", "signal"));

    bsg_error *error = &event->error;
    CHECKED(bsg_ksjsonbeginObject(json, "attributes"));
    { CHECKED(JSON_LIMITED_STRING_ELEMENT("signalType", error->errorClass)); }
    CHECKED(bsg_ksjsonendContainer(json));
  }
  CHECKED(bsg_ksjsonendContainer(json));
  return true;
error:
  return false;
}

static bool bsg_write_stackframe(BSG_KSJSONEncodeContext *json,
                                 bugsnag_stackframe *frame, bool isPC) {

  CHECKED(bsg_ksjsonbeginObject(json, NULL));
  {
    // we use a single buffer for all of the hex encoded strings
    // the bsg_uint64_to_hex doesn't prefix the '0x' so we pre-place that in the
    // buffer, and then overwrite the rest of the buffer
    char hex_str[2 /* '0x' */ + 20 /* number */ + 1 /* NULL */] = "0x";
    char *hex_output_buffer = &hex_str[2];
    bsg_uint64_to_hex(frame->frame_address, hex_output_buffer, 1);
    CHECKED(JSON_LIMITED_STRING_ELEMENT("frameAddress", hex_str));

    bsg_uint64_to_hex(frame->symbol_address, hex_output_buffer, 1);
    CHECKED(JSON_LIMITED_STRING_ELEMENT("symbolAddress", hex_str));

    bsg_uint64_to_hex(frame->load_address, hex_output_buffer, 1);
    CHECKED(JSON_LIMITED_STRING_ELEMENT("loadAddress", hex_str));

    CHECKED(
        bsg_ksjsonaddUIntegerElement(json, "lineNumber", frame->line_number));

    if (isPC) {
      CHECKED(bsg_ksjsonaddBooleanElement(json, "isPC", true));
    }

    if (string_is_not_empty(frame->filename)) {
      CHECKED(JSON_LIMITED_STRING_ELEMENT("file", frame->filename));
    }

    if (string_is_not_empty(frame->method)) {
      CHECKED(JSON_LIMITED_STRING_ELEMENT("method", frame->method));
    } else {
      bsg_uint64_to_hex(frame->symbol_address, hex_output_buffer, 1);
      CHECKED(JSON_LIMITED_STRING_ELEMENT("method", hex_str));
    }

    if (string_is_not_empty(frame->code_identifier)) {
      CHECKED(JSON_LIMITED_STRING_ELEMENT("codeIdentifier",
                                          frame->code_identifier));
    }
  }
  CHECKED(bsg_ksjsonendContainer(json));

  return true;
error:
  return false;
}

static bool bsg_write_stacktrace(BSG_KSJSONEncodeContext *json,
                                 bugsnag_stackframe *stacktrace,
                                 size_t frame_count) {

  for (int findex = 0; findex < frame_count; findex++) {
    if (!bsg_write_stackframe(json, &stacktrace[findex], findex == 0)) {
      goto error;
    }
  }

  return true;
error:
  return false;
}

static bool bsg_write_error(BSG_KSJSONEncodeContext *json, bsg_error *error) {
  CHECKED(bsg_ksjsonbeginArray(json, "exceptions"));
  {
    CHECKED(bsg_ksjsonbeginObject(json, NULL));
    {
      CHECKED(JSON_LIMITED_STRING_ELEMENT("errorClass", error->errorClass));
      CHECKED(JSON_LIMITED_STRING_ELEMENT("message", error->errorMessage));
      CHECKED(JSON_CONSTANT_ELEMENT("type", "c"));

      const ssize_t frame_count = error->frame_count;
      // assuming that the initial frame is the program counter. This logic will
      // need to be revisited if (for example) we add more intelligent
      // processing for stack overflow-type errors, like discarding the top
      // frames, which would mean no stored frame is the program counter.
      if (frame_count > 0) {
        CHECKED(bsg_ksjsonbeginArray(json, "stacktrace"));
        {
          if (!bsg_write_stacktrace(json, error->stacktrace, frame_count)) {
            goto error;
          }
        }
        CHECKED(bsg_ksjsonendContainer(json));
      }
    }
    CHECKED(bsg_ksjsonendContainer(json));
  }
  CHECKED(bsg_ksjsonendContainer(json));

  return true;
error:
  return false;
}

static bool bsg_write_user(BSG_KSJSONEncodeContext *json, bugsnag_user *user) {
  const bool has_id = string_is_not_empty(user->id);
  const bool has_name = string_is_not_empty(user->name);
  const bool has_email = string_is_not_empty(user->email);

  const bool has_user = has_id || has_name || has_email;
  if (has_user) {
    CHECKED(bsg_ksjsonbeginObject(json, "user"));
    {
      if (has_id) {
        CHECKED(JSON_LIMITED_STRING_ELEMENT("id", user->id));
      }

      if (has_name) {
        CHECKED(JSON_LIMITED_STRING_ELEMENT("name", user->name));
      }

      if (has_email) {
        CHECKED(JSON_LIMITED_STRING_ELEMENT("email", user->email));
      }
    }
    CHECKED(bsg_ksjsonendContainer(json));
  }
  return true;
error:
  return false;
}

static bool bsg_write_app(BSG_KSJSONEncodeContext *json, bsg_app_info *app) {
  CHECKED(bsg_ksjsonbeginObject(json, "app"));
  {
    CHECKED(JSON_LIMITED_STRING_ELEMENT("version", app->version));
    CHECKED(JSON_LIMITED_STRING_ELEMENT("id", app->id));
    CHECKED(JSON_LIMITED_STRING_ELEMENT("type", app->type));

    CHECKED(JSON_LIMITED_STRING_ELEMENT("releaseStage", app->release_stage));
    CHECKED(
        bsg_ksjsonaddIntegerElement(json, "versionCode", app->version_code));
    if (string_is_not_empty(app->build_uuid)) {
      CHECKED(JSON_LIMITED_STRING_ELEMENT("buildUUID", app->build_uuid));
    }

    CHECKED(JSON_LIMITED_STRING_ELEMENT("binaryArch", app->binary_arch));
    CHECKED(bsg_ksjsonaddIntegerElement(json, "duration", app->duration));
    CHECKED(bsg_ksjsonaddIntegerElement(json, "durationInForeground",
                                        app->duration_in_foreground));
    CHECKED(
        bsg_ksjsonaddBooleanElement(json, "inForeground", app->in_foreground));
    CHECKED(
        bsg_ksjsonaddBooleanElement(json, "isLaunching", app->is_launching));
  }
  CHECKED(bsg_ksjsonendContainer(json));
  return true;
error:
  return false;
}

static bool bsg_write_device(BSG_KSJSONEncodeContext *json,
                             bsg_device_info *device) {

  CHECKED(bsg_ksjsonbeginObject(json, "device"));
  {
    CHECKED(JSON_LIMITED_STRING_ELEMENT("osName", device->os_name));
    CHECKED(JSON_LIMITED_STRING_ELEMENT("id", device->id));
    CHECKED(JSON_LIMITED_STRING_ELEMENT("locale", device->locale));
    CHECKED(JSON_LIMITED_STRING_ELEMENT("osVersion", device->os_version));
    CHECKED(JSON_LIMITED_STRING_ELEMENT("manufacturer", device->manufacturer));
    CHECKED(JSON_LIMITED_STRING_ELEMENT("model", device->model));
    CHECKED(JSON_LIMITED_STRING_ELEMENT("orientation", device->orientation));

    CHECKED(bsg_ksjsonbeginObject(json, "runtimeVersions"));
    {
      CHECKED(bsg_ksjsonaddIntegerElement(json, "androidApiLevel",
                                          device->api_level));
      CHECKED(JSON_LIMITED_STRING_ELEMENT("osBuild", device->os_build));
    }
    CHECKED(bsg_ksjsonendContainer(json));

    CHECKED(bsg_ksjsonbeginArray(json, "cpuAbi"));
    {
      const int cpu_api_count = device->cpu_abi_count;
      for (int i = 0; i < cpu_api_count; i++) {
        CHECKED(JSON_LIMITED_STRING_ELEMENT(NULL, device->cpu_abi[i].value));
      }
    }
    CHECKED(bsg_ksjsonendContainer(json));

    CHECKED(bsg_ksjsonaddUIntegerElement(json, "totalMemory",
                                         device->total_memory));
    CHECKED(
        bsg_ksjsonaddBooleanElement(json, "jailbroken", device->jailbroken));

    if (device->time > 0) {
      char buffer[sizeof "2018-10-08T12:07:09Z"];
      bsg_time_to_simplified_iso8601_string(device->time, buffer);
      CHECKED(JSON_LIMITED_STRING_ELEMENT("time", buffer));
    }
  }
  CHECKED(bsg_ksjsonendContainer(json));
  return true;
error:
  return false;
}

static bool bsg_write_breadcrumb_type(BSG_KSJSONEncodeContext *json,
                                      bugsnag_breadcrumb_type type) {

  switch (type) {
  case BSG_CRUMB_ERROR:
    CHECKED(JSON_CONSTANT_ELEMENT("type", "error"));
    break;
  case BSG_CRUMB_LOG:
    CHECKED(JSON_CONSTANT_ELEMENT("type", "log"));
    break;
  case BSG_CRUMB_MANUAL:
    CHECKED(JSON_CONSTANT_ELEMENT("type", "manual"));
    break;
  case BSG_CRUMB_NAVIGATION:
    CHECKED(JSON_CONSTANT_ELEMENT("type", "navigation"));
    break;
  case BSG_CRUMB_PROCESS:
    CHECKED(JSON_CONSTANT_ELEMENT("type", "process"));
    break;
  case BSG_CRUMB_REQUEST:
    CHECKED(JSON_CONSTANT_ELEMENT("type", "request"));
    break;
  case BSG_CRUMB_STATE:
    CHECKED(JSON_CONSTANT_ELEMENT("type", "state"));
    break;
  case BSG_CRUMB_USER:
    CHECKED(JSON_CONSTANT_ELEMENT("type", "user"));
    break;
  }
  return true;
error:
  return false;
}

static bool bsg_write_breadcrumb(BSG_KSJSONEncodeContext *json,
                                 bugsnag_breadcrumb *breadcrumb) {
  CHECKED(bsg_ksjsonbeginObject(json, NULL));
  {
    CHECKED(JSON_LIMITED_STRING_ELEMENT("timestamp", breadcrumb->timestamp));
    CHECKED(JSON_LIMITED_STRING_ELEMENT("name", breadcrumb->name));
    if (!bsg_write_breadcrumb_type(json, breadcrumb->type)) {
      goto error;
    }
    if (!bsg_write_metadata(json, &breadcrumb->metadata)) {
      goto error;
    }
  }
  CHECKED(bsg_ksjsonendContainer(json));
  return true;
error:
  return false;
}

static bool bsg_write_breadcrumbs(BSG_KSJSONEncodeContext *json,
                                  bugsnag_breadcrumb *breadcrumbs,
                                  int first_crumb_index, int crumb_count,
                                  int max_crumb_count) {

  CHECKED(bsg_ksjsonbeginArray(json, "breadcrumbs"));
  {
    for (int i = 0; i < crumb_count; i++) {
      int crumb_index = i % max_crumb_count;
      if (!bsg_write_breadcrumb(json, &breadcrumbs[crumb_index])) {
        goto error;
      }
    }
  }
  CHECKED(bsg_ksjsonendContainer(json));
  return true;
error:
  return false;
}
static bool bsg_write_threads(BSG_KSJSONEncodeContext *json,
                              bsg_thread *threads, int thread_count) {

  CHECKED(bsg_ksjsonbeginArray(json, "threads"));
  {
    for (int i = 0; i < thread_count; i++) {
      bsg_thread *thread = &threads[i];
      char id_string[30];
      bsg_uint64_to_string(thread->id, id_string);

      CHECKED(bsg_ksjsonbeginObject(json, NULL));
      {
        CHECKED(JSON_LIMITED_STRING_ELEMENT("id", id_string));

        if (thread->is_reporting_thread) {
          CHECKED(
              bsg_ksjsonaddBooleanElement(json, "errorReportingThread", true));
        }

        CHECKED(JSON_LIMITED_STRING_ELEMENT("name", thread->name));
        CHECKED(JSON_LIMITED_STRING_ELEMENT("state", thread->state));
        CHECKED(JSON_CONSTANT_ELEMENT("type", "c"));
      }
      CHECKED(bsg_ksjsonendContainer(json));
    }
  }
  CHECKED(bsg_ksjsonendContainer(json));

  return true;
error:
  return false;
}

static bool bsg_write_feature_flags(BSG_KSJSONEncodeContext *json,
                                    bsg_feature_flag *flags, int flag_count) {
  if (flag_count == 0) {
    return true;
  }

  CHECKED(bsg_ksjsonbeginArray(json, "featureFlags"));
  {
    for (int i = 0; i < flag_count; i++) {
      bsg_feature_flag *flag = &flags[i];
      CHECKED(bsg_ksjsonbeginObject(json, NULL));
      {
        CHECKED(bsg_ksjsonaddStringElement(json, "featureFlag", flag->name,
                                           strlen(flag->name)));
        if (flag->variant != NULL) {
          CHECKED(bsg_ksjsonaddStringElement(json, "variant", flag->variant,
                                             strlen(flag->variant)));
        }
      }
      CHECKED(bsg_ksjsonendContainer(json));
    }
  }
  CHECKED(bsg_ksjsonendContainer(json));

  return true;
error:
  return false;
}

static bool bsg_write_session(BSG_KSJSONEncodeContext *json,
                              bugsnag_event *event) {

  if (string_is_empty(event->session_id)) {
    return true;
  }

  CHECKED(bsg_ksjsonbeginObject(json, "session"));
  {
    CHECKED(JSON_LIMITED_STRING_ELEMENT("id", event->session_id));
    CHECKED(JSON_LIMITED_STRING_ELEMENT("startedAt", event->session_start));

    CHECKED(bsg_ksjsonbeginObject(json, "events"));
    {
      CHECKED(
          bsg_ksjsonaddIntegerElement(json, "handled", event->handled_events));
      CHECKED(bsg_ksjsonaddIntegerElement(json, "unhandled",
                                          event->unhandled_events));
    }
    CHECKED(bsg_ksjsonendContainer(json));
  }
  CHECKED(bsg_ksjsonendContainer(json));

  return true;
error:
  return false;
}

static bool bsg_write_usage(BSG_KSJSONEncodeContext *json,
                            bsg_environment *env) {
  bugsnag_event *event = &env->next_event;
  CHECKED(bsg_ksjsonbeginObject(json, "usage"));
  {
    CHECKED(bsg_ksjsonbeginObject(json, "callbacks"));
    {
      static const int callbacks_count = sizeof(event->set_callback_counts) /
                                         sizeof(*event->set_callback_counts);

      for (int i = 0; i < callbacks_count; i++) {
        if (event->set_callback_counts[i].count > 0) {
          CHECKED(bsg_ksjsonaddIntegerElement(
              json, event->set_callback_counts[i].name,
              event->set_callback_counts[i].count));
        }
      }

      for (int i = 0; i < bsg_called_apis_count; i++) {
        if (bsg_was_api_called(event, i)) {
          CHECKED(
              bsg_ksjsonaddBooleanElement(json, bsg_called_api_names[i], true));
        }
      }
    }
    CHECKED(bsg_ksjsonendContainer(json));

    if (env->static_json_data != NULL) {
      const size_t length = strlen(env->static_json_data);
      // the static_json_data *must* be more than simply "{}"
      if (length > 2) {
        CHECKED(bsg_ksjsonaddRawJSONData(json, ",", 1));
        CHECKED(bsg_ksjsonaddRawJSONData(json, &(env->static_json_data[1]),
                                         length - 2));
      }
    }
  }
  CHECKED(bsg_ksjsonendContainer(json));

  return true;
error:
  return false;
}

bool bsg_lastrun_write(bsg_environment *env) {
  char *path = env->last_run_info_path;
  int fd = open(path, O_WRONLY | O_CREAT | O_TRUNC, 0644);
  if (fd == -1) {
    return false;
  }

  int size = strlen(env->next_last_run_info);
  ssize_t len = write(fd, env->next_last_run_info, size);
  return len == size;
}
