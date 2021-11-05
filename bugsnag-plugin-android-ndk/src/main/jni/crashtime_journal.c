//
// A note on style:
// - bsg_ctj_xyz functions are non-static, and exposed to other internal parts
// of Bugsnag. They must reset the path builder before and after running (so
// that a stacking bug won't break other functions), and must flush the writer
// at the end.
// - set_xyz functions are static, and perform a complete operation on behalf of
// a bsg_ctj_xyz function, including resets and flushing at the end.
// - add_xyz functions are static, partial helpers that fill out complex
// structures at whatever level the path builder is currently at. They must
// unstack any stacking they cause, but must not reset or flush.
// - All functions that write to the journal return boolean, and must be
// checked. Use the RETURN_ON_FALSE() macro.

#include "crashtime_journal.h"
#include "crashtime_journal_primitives.h"
#include "event_cache.h"
#include "utils/number_to_string.h"
#include "utils/path_builder.h"
#include "utils/serializer.h"
#include <time.h>

#define KEY_API_KEY "apiKey"
#define KEY_APP "app"
#define KEY_ATTRIBUTES "attributes"
#define KEY_BINARY_ARCH "binaryArch"
#define KEY_BREADCRUMBS "breadcrumbs"
#define KEY_BUILD_UUID "buildUUID"
#define KEY_CONTEXT "context"
#define KEY_DEVICE "device"
#define KEY_DURATION "duration"
#define KEY_DURATION_IN_FG "durationInForeground"
#define KEY_EMAIL "email"
#define KEY_ERROR_CLASS "errorClass"
#define KEY_EVENTS "events"
#define KEY_EXCEPTIONS "exceptions"
#define KEY_FILE "file"
#define KEY_FRAME_ADDRESS "frameAddress"
#define KEY_GROUPING_HASH "groupingHash"
#define KEY_HANDLED "handled"
#define KEY_ID "id"
#define KEY_IN_FG "inForeground"
#define KEY_IS_LAUNCHING "isLaunching"
#define KEY_IS_PC "isPC"
#define KEY_JAILBROKEN "jailbroken"
#define KEY_LINE_NUMBER "lineNumber"
#define KEY_LOAD_ADDRESS "loadAddress"
#define KEY_LOCALE "locale"
#define KEY_MANUFACTURER "manufacturer"
#define KEY_MESSAGE "message"
#define KEY_METADATA "metaData"
#define KEY_METHOD "method"
#define KEY_MODEL "model"
#define KEY_NAME "name"
#define KEY_ORIENTATION "orientation"
#define KEY_OS_NAME "osName"
#define KEY_OS_VERSION "osVersion"
#define KEY_RELEASE_STAGE "releaseStage"
#define KEY_RUNTIME "runtime"
#define KEY_SESSION "session"
#define KEY_SESSION_STARTED_AT "startedAt"
#define KEY_SEVERITY "severity"
#define KEY_SEVERITY_REASON "severityReason"
#define KEY_SIGNAL_TYPE "signalType"
#define KEY_STACKTRACE "stacktrace"
#define KEY_STATE "state"
#define KEY_SYMBOL_ADDRESS "symbolAddress"
#define KEY_THREADS "threads"
#define KEY_TIME_UNIX "time"
#define KEY_TIME_NOW "timeNow"
#define KEY_TOTAL_MEMORY "totalMemory"
#define KEY_TYPE "type"
#define KEY_UNHANDLED "unhandled"
#define KEY_UNHANDLED_OVERRIDDEN "unhandledOverridden"
#define KEY_USER "user"
#define KEY_VERSION "version"
#define KEY_VERSION_CODE "versionCode"

/* IMPORTANT: Keep this consistent with:
 * - Severity in
 * bugsnag-android-core/src/main/java/com/bugsnag/android/Severity.kt
 */
static const char *g_severities[] = {
    "error",   // BSG_SEVERITY_ERR
    "warning", // BSG_SEVERITY_WARN
    "info",    // BSG_SEVERITY_INFO,
};
static const int g_severities_count =
    sizeof(g_severities) / sizeof(*g_severities);

const char *get_severity(bugsnag_severity severity) {
  if (severity >= g_severities_count) {
    return "unknown";
  }
  return g_severities[severity];
}

static const char *g_breadcrumb_types[] = {
    "manual",     // BSG_CRUMB_MANUAL
    "error",      // BSG_CRUMB_ERROR
    "log",        // BSG_CRUMB_LOG
    "navigation", // BSG_CRUMB_NAVIGATION
    "procss",     // BSG_CRUMB_PROCESS
    "request",    // BSG_CRUMB_REQUEST
    "state",      // BSG_CRUMB_STATE
    "user",       // BSG_CRUMB_USER
};
static const int g_breadcrumb_types_count =
    sizeof(g_breadcrumb_types) / sizeof(*g_breadcrumb_types);

const char *get_breadcrumb_type(bugsnag_breadcrumb_type type) {
  if (type >= g_breadcrumb_types_count) {
    return "unknown";
  }
  return g_breadcrumb_types[type];
}

/**
 * Convenience macro to stop the current function and return false if a call
 * returns false.
 */
#define RETURN_ON_FALSE(...)                                                   \
  do {                                                                         \
    if (!(__VA_ARGS__)) {                                                      \
      return false;                                                            \
    }                                                                          \
  } while (0)

static bool stack_new_map_in_list() {
  bsg_pb_stack_new_list_entry();
  RETURN_ON_FALSE(bsg_ctj_set_empty_map(bsg_pb_path()));
  bsg_pb_unstack();
  bsg_pb_stack_list_index(-1);
  return true;
}

static void stack_current_exception() {
  bsg_pb_stack_map_key(KEY_EXCEPTIONS);
  bsg_pb_stack_list_index(-1);
}

// Low-level helpers. These fill out objects at the current path level. They
// don't reset or flush.

static bool add_string(const char *name, const char *value) {
  bsg_pb_stack_map_key(name);
  RETURN_ON_FALSE(bsg_ctj_set_string(bsg_pb_path(), value));
  bsg_pb_unstack();
  return true;
}

static bool add_uint(const char *name, uint64_t value) {
  char buff[20] = "0x";
  RETURN_ON_FALSE(bsg_hex64_to_string(value, buff + 2));
  add_string(name, buff);
  return true;
}

static bool add_double(const char *name, double value) {
  bsg_pb_stack_map_key(name);
  RETURN_ON_FALSE(bsg_ctj_set_double(bsg_pb_path(), value));
  bsg_pb_unstack();
  return true;
}

static bool add_boolean(const char *name, bool value) {
  bsg_pb_stack_map_key(name);
  RETURN_ON_FALSE(bsg_ctj_set_boolean(bsg_pb_path(), value));
  bsg_pb_unstack();
  return true;
}

static bool clear_value(const char *name) {
  bsg_pb_stack_map_key(name);
  RETURN_ON_FALSE(bsg_ctj_clear_value(bsg_pb_path()));
  bsg_pb_unstack();
  return true;
}

static bool add_stack_frame(const bugsnag_stackframe *frame, bool is_pc) {
  //  {
  //    "file": "controllers/auth/session_controller.c",
  //    "lineNumber": 1234,
  //    "method": "create",
  //    "isPC": true,
  //    "loadAddress": "0x000000010d131040",
  //    "frameAddress": "0x000000010d131040",
  //    "symbolAddress": "0x000000010d130fe6"
  //  }

  stack_new_map_in_list();
  RETURN_ON_FALSE(add_uint(KEY_FRAME_ADDRESS, frame->frame_address));
  RETURN_ON_FALSE(add_uint(KEY_SYMBOL_ADDRESS, frame->symbol_address));
  RETURN_ON_FALSE(add_uint(KEY_LOAD_ADDRESS, frame->load_address));
  RETURN_ON_FALSE(add_uint(KEY_LINE_NUMBER, frame->line_number));
  RETURN_ON_FALSE(add_string(KEY_TYPE, "c"));

  if (is_pc) {
    RETURN_ON_FALSE(add_boolean(KEY_IS_PC, true));
  }
  if (frame->filename[0] != 0) {
    RETURN_ON_FALSE(add_string(KEY_FILE, frame->filename));
  }
  if (frame->method[0] != 0) {
    RETURN_ON_FALSE(add_string(KEY_METHOD, frame->method));
  } else {
    RETURN_ON_FALSE(add_uint(KEY_METHOD, frame->frame_address));
  }

  bsg_pb_unstack();
  return true;
}

static bool add_exception(const bsg_error *exc) {
  //  "exceptions": [
  //    {
  //      "errorClass": "NoMethodError",
  //      "message": "Unable to connect to database",
  //      "stacktrace": [
  //        {
  //          "file": "controllers/auth/session_controller.c",
  //          "lineNumber": 1234,
  //          "method": "create",
  //          "isPC": true,
  //          "loadAddress": "0x000000010d131040",
  //          "frameAddress": "0x000000010d131040",
  //          "symbolAddress": "0x000000010d130fe6"
  //        }
  //      ],
  //      "type": "android"
  //    }
  //  ],

  stack_current_exception();

  RETURN_ON_FALSE(add_string(KEY_ERROR_CLASS, exc->errorClass));
  RETURN_ON_FALSE(add_string(KEY_MESSAGE, exc->errorMessage));
  RETURN_ON_FALSE(add_string(KEY_TYPE, "c"));

  bsg_pb_stack_map_key(KEY_STACKTRACE);
  // assuming that the initial frame is the program counter. This logic will
  // need to be revisited if (for example) we add more intelligent processing
  // for stack overflow-type errors, like discarding the top frames, which
  // would mean no stored frame is the program counter.
  if (exc->frame_count > 0) {
    RETURN_ON_FALSE(add_stack_frame(&exc->stacktrace[0], true));
  }
  for (int iFrame = 1; iFrame < exc->frame_count; iFrame++) {
    RETURN_ON_FALSE(add_stack_frame(&exc->stacktrace[iFrame], false));
  }

  bsg_pb_unstack();
  bsg_pb_unstack();
  bsg_pb_unstack();
  return true;
}

static bool add_severity_reason(const bugsnag_event *event) {
  //  "severityReason": {
  //    "unhandledOverridden": false,
  //    "type": "signal",
  //    "attributes": {
  //      "errorClass": "ActiveRecord::RecordNotFound",
  //    }
  //  }

  bsg_pb_stack_map_key(KEY_SEVERITY_REASON);

  // unhandled == false always means that the state has been overridden by the
  // user, as this codepath is only executed for unhandled native errors
  RETURN_ON_FALSE(add_boolean(KEY_UNHANDLED_OVERRIDDEN, !event->unhandled));

  // FUTURE(dm): severityReason/unhandled attributes are currently
  // over-optimized for signal handling. in the future we may want to handle
  // C++ exceptions, etc as well.
  RETURN_ON_FALSE(add_string(KEY_TYPE, "signal"));

  bsg_pb_stack_map_key(KEY_ATTRIBUTES);
  RETURN_ON_FALSE(add_string(KEY_SIGNAL_TYPE, event->error.errorClass));
  bsg_pb_unstack();

  bsg_pb_unstack();
  return true;
}

static bool add_session(const bugsnag_event *event) {
  //  "session": {
  //    "id": "123",
  //    "startedAt": "2018-08-07T10:16:34.564Z",
  //    "events": {
  //      "handled": 2,
  //      "unhandled": 1
  //    }
  //  }

  bsg_pb_stack_map_key(KEY_SESSION);
  RETURN_ON_FALSE(add_string(KEY_ID, event->session_id));
  RETURN_ON_FALSE(add_string(KEY_SESSION_STARTED_AT, event->session_start));
  bsg_pb_stack_map_key(KEY_EVENTS);
  RETURN_ON_FALSE(add_uint(KEY_UNHANDLED, event->unhandled_events));
  RETURN_ON_FALSE(add_uint(KEY_HANDLED, event->handled_events));
  bsg_pb_unstack();
  bsg_pb_unstack();
  return true;
}

static bool add_thread(const bsg_thread *thread) {
  stack_new_map_in_list();
  RETURN_ON_FALSE(add_uint(KEY_ID, thread->id));
  RETURN_ON_FALSE(add_string(KEY_NAME, thread->name));
  RETURN_ON_FALSE(add_string(KEY_STATE, thread->state));
  RETURN_ON_FALSE(add_string(KEY_TYPE, "c"));
  bsg_pb_unstack();
  return true;
}

static bool add_threads(const bugsnag_event *event) {
  if (event->thread_count <= 0) {
    return true;
  }
  bsg_pb_stack_map_key(KEY_THREADS);

  for (int index = 0; index < event->thread_count; index++) {
    RETURN_ON_FALSE(add_thread(&event->threads[index]));
  }
  bsg_pb_unstack();
  return true;
}

// One-shot helpers for single value stores. These will also reset and flush the
// writer.

static bool set_top_level_string(const char *key, const char *value) {
  bsg_pb_reset();
  RETURN_ON_FALSE(add_string(key, value));
  bsg_pb_reset();
  return bsg_ctj_flush();
}

static bool set_event_string(const char *key, const char *value) {
  bsg_pb_reset();
  RETURN_ON_FALSE(add_string(key, value));
  bsg_pb_reset();
  return bsg_ctj_flush();
}

static bool set_event_boolean(const char *key, bool value) {
  bsg_pb_reset();
  RETURN_ON_FALSE(add_boolean(key, value));
  bsg_pb_reset();
  return bsg_ctj_flush();
}

static bool set_event_subobject_string(const char *event_key,
                                       const char *object_key,
                                       const char *value) {
  bsg_pb_reset();
  bsg_pb_stack_map_key(event_key);
  RETURN_ON_FALSE(add_string(object_key, value));
  bsg_pb_reset();
  return bsg_ctj_flush();
}

static bool set_event_subobject_double(const char *event_key,
                                       const char *object_key, double value) {
  bsg_pb_reset();
  bsg_pb_stack_map_key(event_key);
  RETURN_ON_FALSE(add_double(object_key, value));
  bsg_pb_reset();
  return bsg_ctj_flush();
}

static bool set_event_subobject_uint(const char *event_key,
                                     const char *object_key, uint64_t value) {
  bsg_pb_reset();
  bsg_pb_stack_map_key(event_key);
  RETURN_ON_FALSE(add_uint(object_key, value));
  bsg_pb_reset();
  return bsg_ctj_flush();
}

static bool set_event_subobject_boolean(const char *event_key,
                                        const char *object_key, bool value) {
  bsg_pb_reset();
  bsg_pb_stack_map_key(event_key);
  RETURN_ON_FALSE(add_boolean(object_key, value));
  bsg_pb_reset();
  return bsg_ctj_flush();
}

static bool set_exception_string(const char *key, const char *value) {
  bsg_pb_reset();
  stack_current_exception();
  RETURN_ON_FALSE(add_string(key, value));
  bsg_pb_reset();
  return bsg_ctj_flush();
}

// Public API. All of these MUST flush the writer, or call a one-shot set_xyz()
// helper (which will flush the writer).

bool bsg_ctj_store_event(const bugsnag_event *event) {
  // This fills out many different parts of the current event in the journal
  // document.

  bsg_pb_reset();

  RETURN_ON_FALSE(bsg_ctj_record_current_time());

  RETURN_ON_FALSE(add_exception(&event->error));
  if (bsg_cache_has_session(event)) {
    RETURN_ON_FALSE(add_session(event));
  }
  RETURN_ON_FALSE(add_threads(event));
  RETURN_ON_FALSE(add_severity_reason(event));
  RETURN_ON_FALSE(
      add_string(KEY_SEVERITY, bsg_severity_string(event->severity)));
  RETURN_ON_FALSE(add_boolean(KEY_UNHANDLED, event->unhandled));

  bsg_pb_reset();

  RETURN_ON_FALSE(bsg_ctj_set_device_time_seconds(event->device.time));

  return bsg_ctj_flush();
}

bool bsg_ctj_set_api_key(const char *api_key) {
  // {
  //   "apiKey": "YOUR-NOTIFIER-API-KEY"
  // }

  return set_top_level_string(KEY_API_KEY, api_key);
}

bool bsg_ctj_set_event_context(const char *context) {
  // {
  //   "context": "auth/session#create"
  // }

  return set_event_string(KEY_CONTEXT, context);
}

bool bsg_ctj_set_event_user(const char *id, const char *email,
                            const char *name) {
  // {
  //    "user": {
  //         "id": "19",
  //         "name": "Robert Hawkins",
  //         "email": "bob@example.com"
  //       }
  //     }
  // }

  bsg_pb_reset();
  bsg_pb_stack_map_key(KEY_USER);
  RETURN_ON_FALSE(add_string(KEY_ID, id));
  RETURN_ON_FALSE(add_string(KEY_EMAIL, email));
  RETURN_ON_FALSE(add_string(KEY_NAME, name));
  bsg_pb_reset();
  return bsg_ctj_flush();
}

bool bsg_ctj_set_app_binary_arch(const char *arch) {
  // {
  //       "app": {
  //         "binaryArch": "x86_64"
  //       }
  // }

  return set_event_subobject_string(KEY_APP, KEY_BINARY_ARCH, arch);
}

bool bsg_ctj_set_app_build_uuid(const char *uuid) {
  // {
  //       "app": {
  //         "buildUUID": "BE5BA3D0-971C-4418-9ECF-E2D1ABCB66BE"
  //       }
  // }

  return set_event_subobject_string(KEY_APP, KEY_BUILD_UUID, uuid);
}

bool bsg_ctj_set_app_id(const char *id) {
  // {
  //       "app": {
  //         "id": "com.bugsnag.android.example.debug"
  //       }
  // }

  return set_event_subobject_string(KEY_APP, KEY_ID, id);
}

bool bsg_ctj_set_app_release_stage(const char *value) {
  // {
  //       "app": {
  //         "releaseStage": "staging"
  //       }
  // }

  return set_event_subobject_string(KEY_APP, KEY_RELEASE_STAGE, value);
}

bool bsg_ctj_set_app_type(const char *value) {
  // {
  //       "app": {
  //         "type": "c"
  //       }
  // }

  return set_event_subobject_string(KEY_APP, KEY_TYPE, value);
}

bool bsg_ctj_set_app_version(const char *value) {
  // {
  //       "app": {
  //         "version": "1.1.3"
  //       }
  // }

  return set_event_subobject_string(KEY_APP, KEY_VERSION, value);
}

bool bsg_ctj_set_app_version_code(int value) {
  // {
  //       "app": {
  //         "versionCode": 12
  //       }
  // }

  return set_event_subobject_uint(KEY_APP, KEY_VERSION_CODE, value);
}

bool bsg_ctj_set_app_duration(time_t value) {
  // {
  //       "app": {
  //         "duration": 1275
  //       }
  // }

  return set_event_subobject_uint(KEY_APP, KEY_DURATION, value);
}

bool bsg_ctj_set_app_duration_in_foreground(time_t value) {
  // {
  //       "app": {
  //         "duration": 983
  //       }
  // }

  return set_event_subobject_uint(KEY_APP, KEY_DURATION_IN_FG, value);
}

bool bsg_ctj_set_app_in_foreground(bool value) {
  // {
  //       "app": {
  //         "inForeground": true
  //       }
  // }

  return set_event_subobject_boolean(KEY_APP, KEY_IN_FG, value);
}

bool bsg_ctj_set_app_is_launching(bool value) {
  // {
  //       "app": {
  //         "isLaunching": true
  //       }
  // }

  return set_event_subobject_boolean(KEY_APP, KEY_IS_LAUNCHING, value);
}

bool bsg_ctj_set_device_jailbroken(bool value) {
  // {
  //       "device": {
  //         "jailbroken": false
  //       }
  // }

  return set_event_subobject_boolean(KEY_DEVICE, KEY_JAILBROKEN, value);
}

bool bsg_ctj_set_device_id(const char *value) {
  // {
  //       "device": {
  //         "id": "fd124e87760c4281aef"
  //       }
  // }

  return set_event_subobject_string(KEY_DEVICE, KEY_ID, value);
}

bool bsg_ctj_set_device_locale(const char *value) {
  // {
  //       "device": {
  //         "locale": "en_US"
  //       }
  // }

  return set_event_subobject_string(KEY_DEVICE, KEY_LOCALE, value);
}

bool bsg_ctj_set_device_manufacturer(const char *value) {
  // {
  //       "device": {
  //         "manufacturer": "LGE"
  //       }
  // }

  return set_event_subobject_string(KEY_DEVICE, KEY_MANUFACTURER, value);
}

bool bsg_ctj_set_device_model(const char *value) {
  // {
  //       "device": {
  //         "model": "Nexus 6P"
  //       }
  // }

  return set_event_subobject_string(KEY_DEVICE, KEY_MODEL, value);
}

bool bsg_ctj_set_device_os_version(const char *value) {
  // {
  //       "device": {
  //         "osVersion": "8.0.1"
  //       }
  // }

  return set_event_subobject_string(KEY_DEVICE, KEY_OS_VERSION, value);
}

bool bsg_ctj_set_device_total_memory(long value) {
  // {
  //       "device": {
  //         "totalMemory": 201326592
  //       }
  // }

  return set_event_subobject_uint(KEY_DEVICE, KEY_TOTAL_MEMORY, value);
}

bool bsg_ctj_set_device_orientation(const char *value) {
  // {
  //       "device": {
  //         "orientation": "portrait"
  //       }
  // }

  return set_event_subobject_string(KEY_DEVICE, KEY_ORIENTATION, value);
}

bool bsg_ctj_set_device_time_seconds(time_t value) {
  // {
  //       "device": {
  //         "time": "0x123456789abc"
  //       }
  // }

  // IMPORTANT: This value MUST be converted to an RFC 3339 timestamp and stored
  // under "time" upon reloading the journal!
  // Note: We convert to milliseconds because we will eventually want to reach
  // this level of precision in future.
  return set_event_subobject_uint(KEY_DEVICE, KEY_TIME_UNIX,
                                  (uint64_t)value * 1000);
}

bool bsg_ctj_record_current_time() {
  // {
  //       "runtime": {
  //         "timeNow": "0x123456789abc"
  //       }
  // }

  // IMPORTANT: This value MUST be converted to an RFC 3339 timestamp and stored
  // under "time" upon reloading the journal!
  // Note: We convert to milliseconds because we will eventually want to reach
  // this level of precision in future.
  time_t now = time(NULL);
  return set_event_subobject_uint(KEY_RUNTIME, KEY_TIME_NOW,
                                  (uint64_t)now * 1000);
}

bool bsg_ctj_set_device_os_name(const char *value) {
  // {
  //       "device": {
  //         "osName": "android"
  //       }
  // }

  return set_event_subobject_string(KEY_DEVICE, KEY_OS_NAME, value);
}

bool bsg_ctj_set_error_class(const char *value) {
  // {
  //       "exceptions": [
  //         {
  //           "errorClass": "NoMethodError"
  //         }
  //       ]
  // }

  return set_exception_string(KEY_ERROR_CLASS, value);
}

bool bsg_ctj_set_error_message(const char *value) {
  // {
  //       "exceptions": [
  //         {
  //           "message": "Unable to connect to database"
  //         }
  //       ]
  // }

  return set_exception_string(KEY_MESSAGE, value);
}

bool bsg_ctj_set_error_type(const char *value) {
  // {
  //       "exceptions": [
  //         {
  //           "type": "android"
  //         }
  //       ]
  // }

  return set_exception_string(KEY_TYPE, value);
}

bool bsg_ctj_set_event_severity(bugsnag_severity value) {
  // {
  //       "severity": "error"
  // }

  return set_event_string(KEY_SEVERITY, get_severity(value));
}

bool bsg_ctj_set_event_unhandled(bool value) {
  // {
  //       "unhandled": true
  // }

  return set_event_boolean(KEY_UNHANDLED, value);
}

bool bsg_ctj_set_event_grouping_hash(const char *value) {
  // {
  //       "groupingHash": "buggy_file.c"
  // }

  return set_event_string(KEY_GROUPING_HASH, value);
}

bool bsg_ctj_set_metadata_double(const char *section, const char *name,
                                 double value) {
  // {
  //       "metadata": {
  //         "some_section": {
  //           "some_name": 1.5
  //         }
  //       }
  // }

  bsg_pb_reset();
  bsg_pb_stack_map_key(KEY_METADATA);
  bsg_pb_stack_map_key(section);
  RETURN_ON_FALSE(add_double(name, value));
  bsg_pb_reset();
  return bsg_ctj_flush();
}

bool bsg_ctj_set_metadata_string(const char *section, const char *name,
                                 const char *value) {
  // {
  //       "metadata": {
  //         "some_section": {
  //           "some_name": "value"
  //         }
  //       }
  // }

  bsg_pb_reset();
  bsg_pb_stack_map_key(KEY_METADATA);
  bsg_pb_stack_map_key(section);
  RETURN_ON_FALSE(add_string(name, value));
  bsg_pb_reset();
  return bsg_ctj_flush();
}

bool bsg_ctj_set_metadata_bool(const char *section, const char *name,
                               bool value) {
  // {
  //       "metadata": {
  //         "some_section": {
  //           "some_name": false
  //         }
  //       }
  // }

  bsg_pb_reset();
  bsg_pb_stack_map_key(KEY_METADATA);
  bsg_pb_stack_map_key(section);
  RETURN_ON_FALSE(add_boolean(name, value));
  bsg_pb_reset();
  return bsg_ctj_flush();
}

bool bsg_ctj_clear_metadata(const char *section, const char *name) {
  bsg_pb_reset();
  bsg_pb_stack_map_key(KEY_METADATA);
  bsg_pb_stack_map_key(section);
  bsg_pb_stack_map_key(name);
  RETURN_ON_FALSE(bsg_ctj_clear_value(bsg_pb_path()));
  bsg_pb_reset();
  return bsg_ctj_flush();
}

bool bsg_ctj_clear_metadata_section(const char *section) {
  bsg_pb_reset();
  bsg_pb_stack_map_key(KEY_METADATA);
  bsg_pb_stack_map_key(section);
  RETURN_ON_FALSE(bsg_ctj_clear_value(bsg_pb_path()));
  bsg_pb_reset();
  return bsg_ctj_flush();
}
