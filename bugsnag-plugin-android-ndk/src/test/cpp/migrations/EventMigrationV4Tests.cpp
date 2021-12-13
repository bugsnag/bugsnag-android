#include <cstring>

#include <parson/parson.h>

#include "utils.hpp"

static void *create_payload_info_event() {
  auto event = (bugsnag_report_v4 *)calloc(1, sizeof(bugsnag_report_v4));

  strcpy(event->api_key, "5d1e5fbd39a74caa1200142706a90b20");
  strcpy(event->notifier.name, "Test Library");
  strcpy(event->notifier.url, "https://example.com/test-lib");
  strcpy(event->notifier.version, "2.0.11");

  return event;
}

/**
 * Create a new event in v4 format
 */
static void *create_full_event() {
  auto event = (bugsnag_report_v4 *)calloc(1, sizeof(bugsnag_report_v4));

  strcpy(event->context, "More Magic");
  strcpy(event->grouping_hash, "foo-hash");
  event->severity = BSG_SEVERITY_INFO;

  // app
  strcpy(event->app.binary_arch, "mips");
  strcpy(event->app.build_uuid, "1234-9876-adfe");
  event->app.duration = 6502;
  event->app.duration_in_foreground = 12;
  event->app.in_foreground = true;
  strcpy(event->app.id, "com.example.PhotoSnapPlus");
  strcpy(event->app.release_stage, "リリース");
  strcpy(event->app.type, "red");
  strcpy(event->app.version, "2.0.52");
  event->app.version_code = 57;

  // breadcrumbs
  insert_crumb(event->breadcrumbs, 0, "decrease torque", BSG_CRUMB_STATE,
               1638992630014, "Moving laterally 26º");
  insert_crumb(event->breadcrumbs, 1, "enable blasters", BSG_CRUMB_USER,
               1638992630301, "this is a drill.");
  event->crumb_count = 2;
  event->crumb_first_index = 0;

  // device
  event->device.cpu_abi_count = 1;
  strcpy(event->device.cpu_abi[0].value, "mipsx");
  strcpy(event->device.id, "ffffa");
  event->device.jailbroken = true;
  strcpy(event->device.locale, "en_AU#Melbun");
  strcpy(event->device.manufacturer, "HI-TEC™");
  strcpy(event->device.model, "Rasseur");
  strcpy(event->device.orientation, "sideup");
  strcpy(event->device.os_name, "BOX BOX");
  strcpy(event->device.os_version, "98.7");
  { // -- runtime versions
    strcpy(event->device.os_build, "beta1-2");
    event->device.api_level = 32;
  }
  event->device.time = 1638992630;
  event->device.total_memory = 3278623;

  // exceptions
  strcpy(event->error.errorClass, "SIGBUS");
  strcpy(event->error.errorMessage, "POSIX is serious about oncoming traffic");
  strcpy(event->error.type, "C");
  event->error.frame_count = 2;
  event->error.stacktrace[0].frame_address = 454379;
  event->error.stacktrace[0].load_address = 2367523;
  event->error.stacktrace[0].symbol_address = 776;
  strcpy(event->error.stacktrace[0].method, "makinBacon");
  strcpy(event->error.stacktrace[0].filename, "lib64/libfoo.so");
  event->error.stacktrace[1].frame_address = 342334; // will become method hex

  // metadata
  strcpy(event->app.active_screen, "Menu");
  bugsnag_event_add_metadata_bool(event, "metrics", "experimentX", false);
  bugsnag_event_add_metadata_string(event, "metrics", "subject", "percy");
  bugsnag_event_add_metadata_string(event, "app", "weather", "rain");
  bugsnag_event_add_metadata_double(event, "metrics", "counter", 47.5);

  // session info
  event->handled_events = 5;
  event->unhandled_events = 2;
  strcpy(event->session_id, "aaaaaaaaaaaaaaaa");
  strcpy(event->session_start, "2031-07-09T11:08:21+00:00");

  // user
  strcpy(event->user.email, "fenton@io.example.com");
  strcpy(event->user.name, "Fenton");
  strcpy(event->user.id, "fex01");

  return event;
}

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL
Java_com_bugsnag_android_ndk_migrations_EventMigrationV4Tests_migratePayloadInfo(
    JNIEnv *env, jobject _this, jstring temp_file) {
  const char *path = (*env).GetStringUTFChars(temp_file, nullptr);

  // (old format) event struct -> file on disk
  void *old_event = create_payload_info_event();
  bool success =
      write_struct_to_file(old_event, 4, sizeof(bugsnag_report_v4), path);
  free(old_event);

  // file on disk -> latest event type
  bugsnag_event *parsed_event = bsg_deserialize_event_from_file((char *)path);

  // write json object
  JSON_Value *event_val = json_value_init_object();
  JSON_Object *event_obj = json_value_get_object(event_val);
  json_object_set_string(event_obj, "apiKey", parsed_event->api_key);
  json_object_set_string(event_obj, "notifierName",
                         parsed_event->notifier.name);
  json_object_set_string(event_obj, "notifierURL", parsed_event->notifier.url);
  json_object_set_string(event_obj, "notifierVersion",
                         parsed_event->notifier.version);
  char *result = json_serialize_to_string(event_val);
  return (*env).NewStringUTF(result);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_migrations_EventMigrationV4Tests_migrateEvent(
    JNIEnv *env, jobject _this, jstring temp_file) {
  write_json_for_event(env, create_full_event, 4, sizeof(bugsnag_report_v4),
                       temp_file);
}

#ifdef __cplusplus
}
#endif
