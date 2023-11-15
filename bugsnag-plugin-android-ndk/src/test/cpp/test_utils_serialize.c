#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>

#include <greatest/greatest.h>
#include <parson/parson.h>

#include <featureflags.h>
#include <utils/serializer.h>
#include <utils/serializer/event_reader.h>
#include <utils/serializer/buffered_writer.h>


#define SERIALIZE_TEST_FILE "/data/data/com.bugsnag.android.ndk.test/cache/foo.crash"

bugsnag_breadcrumb *init_breadcrumb(const char *name, const char *message, bugsnag_breadcrumb_type type);

bool bsg_report_header_write(bsg_report_header *header, int fd);

void generate_basic_report(bugsnag_event *event) {
  strcpy(event->grouping_hash, "foo-hash");
  strcpy(event->api_key, "5d1e5fbd39a74caa1200142706a90b20");
  strcpy(event->context, "SomeActivity");
  strcpy(event->error.errorClass, "SIGBUS");
  strcpy(event->error.errorMessage, "POSIX is serious about oncoming traffic");
  event->error.stacktrace[0].frame_address = 454379;
  event->error.stacktrace[1].frame_address = 342334;
  event->error.frame_count = 2;
  strcpy(event->error.type, "C");
  strcpy(event->error.stacktrace[0].method, "makinBacon");
  strcpy(event->app.id, "com.example.PhotoSnapPlus");
  strcpy(event->app.release_stage, "リリース");
  strcpy(event->app.version, "2.0.52");
  event->app.version_code = 57;
  strcpy(event->app.build_uuid, "1234-9876-adfe");
  strcpy(event->device.manufacturer, "HI-TEC™");
  strcpy(event->device.model, "Rasseur");
  strcpy(event->device.locale, "en_AU#Melbun");
  strcpy(event->device.os_name, "android");
  strcpy(event->user.email, "fenton@io.example.com");
  strcpy(event->user.id, "fex");
  event->device.total_memory = 234678100;
  event->app.duration = 6502;
  event->metadata.value_count = 4;
  event->metadata.values[0] = (bsg_metadata_value) {
    .name = {"weather"},
    .section = {"app"},
    .type = BSG_METADATA_CHAR_VALUE,
    .char_value = {"rain"},
  };
  event->metadata.values[1] = (bsg_metadata_value) {
    .name = {"experimentX"},
    .section = {"metrics"},
    .type = BSG_METADATA_BOOL_VALUE,
    .bool_value = false,
  };
  event->metadata.values[2] = (bsg_metadata_value) {
    .name = {"subject"},
    .section = {"metrics"},
    .type = BSG_METADATA_CHAR_VALUE,
    .char_value = {"percy"},
  };
  event->metadata.values[3] = (bsg_metadata_value) {
    .name = {"counter"},
    .section = {"metrics"},
    .type = BSG_METADATA_NUMBER_VALUE,
    .double_value = 47.8,
  };

  event->crumb_count = 0;
  event->crumb_first_index = 0;
  bugsnag_breadcrumb *crumb1 = init_breadcrumb("decrease torque", "Moving laterally 26º", BSG_CRUMB_STATE);
  bsg_event_add_breadcrumb(event, crumb1);

  bugsnag_breadcrumb *crumb2 = init_breadcrumb("enable blasters", "this is a drill.", BSG_CRUMB_USER);
  bsg_event_add_breadcrumb(event, crumb2);

  event->handled_events = 1;
  event->unhandled_events = 1;
  strcpy(event->session_id, "f1ab");
  strcpy(event->session_start, "2019-03-19T12:58:19+00:00");

  strcpy(event->notifier.version, "1.0");
  strcpy(event->notifier.url, "bugsnag.com");
  strcpy(event->notifier.name, "Test Notifier");
}

bugsnag_event *bsg_generate_event(void) {
  bugsnag_event *report = calloc(1, sizeof(bugsnag_event));
  strcpy(report->grouping_hash, "foo-hash");
  strcpy(report->api_key, "5d1e5fbd39a74caa1200142706a90b20");
  strcpy(report->context, "SomeActivity");
  strcpy(report->error.errorClass, "SIGBUS");
  strcpy(report->error.errorMessage, "POSIX is serious about oncoming traffic");
  report->error.stacktrace[0].frame_address = 454379;
  report->error.stacktrace[1].frame_address = 342334;
  report->error.frame_count = 2;
  strcpy(report->error.type, "C");
  strcpy(report->error.stacktrace[0].method, "makinBacon");
  strcpy(report->app.id, "com.example.PhotoSnapPlus");
  strcpy(report->app.release_stage, "リリース");
  strcpy(report->app.version, "2.0.52");
  report->app.version_code = 57;
  strcpy(report->app.build_uuid, "1234-9876-adfe");
  strcpy(report->device.manufacturer, "HI-TEC™");
  strcpy(report->device.model, "Rasseur");
  strcpy(report->device.locale, "en_AU#Melbun");
  strcpy(report->device.os_name, "android");
  strcpy(report->user.email, "fenton@io.example.com");
  strcpy(report->user.id, "fex");
  report->device.total_memory = 234678100;
  report->app.duration = 6502;
  bugsnag_event_add_metadata_bool(report, "metrics", "experimentX", false);
  bugsnag_event_add_metadata_string(report, "metrics", "subject", "percy");
  bugsnag_event_add_metadata_string(report, "app", "weather", "rain");
  bugsnag_event_add_metadata_double(report, "metrics", "counter", 47.8);

  report->crumb_count = 0;
  report->crumb_first_index = 0;
  bugsnag_breadcrumb *crumb1 = init_breadcrumb("decrease torque", "Moving laterally 26º",
                                               BSG_CRUMB_STATE);
  bsg_event_add_breadcrumb(report, crumb1);

  bugsnag_breadcrumb *crumb2 = init_breadcrumb("enable blasters", "this is a drill.",
                                               BSG_CRUMB_USER);
  bsg_event_add_breadcrumb(report, crumb2);

  report->handled_events = 1;
  report->unhandled_events = 1;
  strcpy(report->session_id, "f1ab");
  strcpy(report->session_start, "2019-03-19T12:58:19+00:00");

  strcpy(report->notifier.version, "1.0");
  strcpy(report->notifier.url, "bugsnag.com");
  strcpy(report->notifier.name, "Test Notifier");
  report->unhandled_events = 2;
  return report;
}


void bsg_update_next_run_info(bsg_environment *env);

char *test_read_last_run_info(const bsg_environment *env) {
  int fd = open(SERIALIZE_TEST_FILE, O_RDONLY);
  size_t size = sizeof(env->next_last_run_info);
  char *buf = calloc(1, size);
  read(fd, buf, size);
  return buf;
}

TEST test_last_run_info_serialization(void) {
  bsg_environment *env = calloc(1, sizeof(bsg_environment));
  strcpy(env->last_run_info_path, SERIALIZE_TEST_FILE);
  
  // update LastRunInfo with defaults
  env->next_event.app.is_launching = false;
  env->consecutive_launch_crashes = 1;
  bsg_update_next_run_info(env);
  ASSERT_STR_EQ("consecutiveLaunchCrashes=1\ncrashed=true\ncrashedDuringLaunch=false\0", env->next_last_run_info);

  // update LastRunInfo with consecutive crashes
  env->next_event.app.is_launching = true;
  env->consecutive_launch_crashes = 7;
  bsg_update_next_run_info(env);
  ASSERT_STR_EQ("consecutiveLaunchCrashes=8\ncrashed=true\ncrashedDuringLaunch=true\0", env->next_last_run_info);

  free(env);
  PASS();
}

TEST test_report_to_file(void) {
  bsg_environment *env = calloc(1, sizeof(bsg_environment));
  env->report_header.version = 7;
  env->report_header.big_endian = 1;
  bugsnag_event *report = bsg_generate_event();
  memcpy(&env->next_event, report, sizeof(bugsnag_event));
  strcpy(env->report_header.os_build, "macOS Sierra");
  strcpy(env->next_event_path, SERIALIZE_TEST_FILE);
  ASSERT(bsg_serialize_event_to_file(env));
  free(report);
  free(env);
  PASS();
}

TEST test_report_with_feature_flags_to_file(void) {
  bsg_environment *env = calloc(1, sizeof(bsg_environment));
  env->report_header.version = BSG_MIGRATOR_CURRENT_VERSION;
  env->report_header.big_endian = 1;
  bugsnag_event *report = bsg_generate_event();
  memcpy(&env->next_event, report, sizeof(bugsnag_event));
  bsg_set_feature_flag(&env->next_event, "sample_group", "a");
  bsg_set_feature_flag(&env->next_event, "demo_mode", NULL);
  strcpy(env->report_header.os_build, "macOS Sierra");
  strcpy(env->next_event_path, SERIALIZE_TEST_FILE);
  ASSERT(bsg_serialize_event_to_file(env));
  free(report);
  free(env);
  PASS();
}

TEST test_file_to_report(void) {
  bsg_environment *env = calloc(1, sizeof(bsg_environment));
  env->report_header.version = BSG_MIGRATOR_CURRENT_VERSION;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");
  bugsnag_event *generated_report = bsg_generate_event();
  memcpy(&env->next_event, generated_report, sizeof(bugsnag_event));
  strcpy(env->next_event_path, SERIALIZE_TEST_FILE);
  bsg_serialize_event_to_file(env);

  bugsnag_event *report = bsg_deserialize_event_from_file(SERIALIZE_TEST_FILE);
  ASSERT(report != NULL);
  ASSERT(strcmp("SIGBUS", report->error.errorClass) == 0);
  ASSERT(strcmp("POSIX is serious about oncoming traffic", report->error.errorMessage) == 0);
  free(generated_report);
  free(env);
  free(report);
  PASS();
}

TEST test_report_with_feature_flags_from_file(void) {
  bsg_environment *env = calloc(1, sizeof(bsg_environment));
  env->report_header.version = BSG_MIGRATOR_CURRENT_VERSION;
  env->report_header.big_endian = 1;
  bugsnag_event *report = bsg_generate_event();
  memcpy(&env->next_event, report, sizeof(bugsnag_event));
  bsg_set_feature_flag(&env->next_event, "sample_group", "a");
  bsg_set_feature_flag(&env->next_event, "demo_mode", NULL);
  strcpy(env->report_header.os_build, "macOS Sierra");
  strcpy(env->next_event_path, "/data/data/com.bugsnag.android.ndk.test/cache/features.crash");
  ASSERT(bsg_serialize_event_to_file(env));

  bugsnag_event *event = bsg_deserialize_event_from_file("/data/data/com.bugsnag.android.ndk.test/cache/features.crash");

  ASSERT_EQ(2, event->feature_flag_count);

  free(report);
  free(env);
  free(event);
  PASS();
}

TEST test_report_with_opaque_metadata_from_file(void) {
  bsg_environment *env = calloc(1, sizeof(bsg_environment));
  env->report_header.version = BSG_MIGRATOR_CURRENT_VERSION;
  env->report_header.big_endian = 1;
  bugsnag_event *report = bsg_generate_event();
  memcpy(&env->next_event, report, sizeof(bugsnag_event));
  bsg_add_metadata_value_opaque(&env->next_event.metadata, "opaque", "map", "{\"user\": \"Bobby Tables\"}");
  bsg_add_metadata_value_opaque(&env->next_event.metadata, "opaque", "list", "[1,2,3,4]");
  strcpy(env->report_header.os_build, "macOS Sierra");
  strcpy(env->next_event_path, "/data/data/com.bugsnag.android.ndk.test/cache/features.crash");
  ASSERT(bsg_serialize_event_to_file(env));

  bugsnag_event *event = bsg_deserialize_event_from_file("/data/data/com.bugsnag.android.ndk.test/cache/features.crash");

  ASSERT_EQ(6, event->metadata.value_count);

  ASSERT_EQ(BSG_METADATA_OPAQUE_VALUE, bugsnag_event_has_metadata(event, "opaque", "map"));
  ASSERT_EQ(BSG_METADATA_OPAQUE_VALUE, bugsnag_event_has_metadata(event, "opaque", "list"));

  free(report);
  free(env);
  free(event);
  PASS();
}

// helper function
JSON_Value *bsg_generate_json(void) {
  bugsnag_event *event = bsg_generate_event();
  char *json = bsg_serialize_event_to_json_string(event);
  JSON_Value *root_value = json_parse_string(json);
  free(json);
  free(event);
  return root_value;
}

TEST test_app_info_to_json(void) {
  JSON_Value *root_value = bsg_generate_json();
  JSON_Object *event = json_value_get_object(root_value);
  ASSERT(strcmp("2.0.52", json_object_dotget_string(event, "app.version")) == 0);
  ASSERT(strcmp( "リリース", json_object_dotget_string(event, "app.releaseStage")) == 0);
  ASSERT_EQ(57, json_object_dotget_number(event, "app.versionCode"));
  ASSERT(strcmp( "1234-9876-adfe", json_object_dotget_string(event, "app.buildUUID")) == 0);
  json_value_free(root_value);
  PASS();
}

TEST test_session_handled_counts(void) {
  JSON_Value *root_value = bsg_generate_json();
  JSON_Object *event = json_value_get_object(root_value);
  ASSERT(strcmp("f1ab", json_object_dotget_string(event, "session.id")) == 0);
  ASSERT(strcmp("2019-03-19T12:58:19+00:00", json_object_dotget_string(event, "session.startedAt")) == 0);
  ASSERT_EQ(1, json_object_dotget_number(event, "session.events.handled"));
  ASSERT_EQ(2, json_object_dotget_number(event, "session.events.unhandled"));
  PASS();
}

TEST test_grouping_hash_to_json(void) {
  JSON_Value *root_value = bsg_generate_json();
  JSON_Object *event = json_value_get_object(root_value);
  ASSERT(event != NULL);
  ASSERT_STR_EQ("foo-hash", json_object_get_string(event, "groupingHash"));
  PASS();
}

TEST test_context_to_json(void) {
  JSON_Value *root_value = bsg_generate_json();
  JSON_Object *event = json_value_get_object(root_value);
  ASSERT(event != NULL);
  ASSERT(strcmp( "SomeActivity", json_object_get_string(event, "context")) == 0);
  PASS();
}

TEST test_device_info_to_json(void) {
  JSON_Value *root_value = bsg_generate_json();
  JSON_Object *event = json_value_get_object(root_value);
  ASSERT(event != NULL);
  ASSERT(strcmp( "HI-TEC™", json_object_dotget_string(event, "device.manufacturer")) == 0);
  ASSERT(strcmp( "Rasseur", json_object_dotget_string(event, "device.model")) == 0);
  ASSERT(strcmp( "en_AU#Melbun", json_object_dotget_string(event, "device.locale")) == 0);
  json_value_free(root_value);
  PASS();
}

TEST test_user_info_to_json(void) {
  JSON_Value *root_value = bsg_generate_json();
  JSON_Object *event = json_value_get_object(root_value);
  ASSERT(event != NULL);
  ASSERT(strcmp( "fex", json_object_dotget_string(event, "user.id")) == 0);
  ASSERT(strcmp( "fenton@io.example.com", json_object_dotget_string(event, "user.email")) == 0);
  ASSERT(json_object_dotget_string(event, "user.name") == NULL);
  json_value_free(root_value);
  PASS();
}

TEST test_custom_info_to_json(void) {
  JSON_Value *root_value = bsg_generate_json();
  JSON_Object *event = json_value_get_object(root_value);
  ASSERT(event != NULL);
  ASSERT(strcmp( "percy", json_object_dotget_string(event, "metaData.metrics.subject")) == 0);
  ASSERT(strcmp( "rain", json_object_dotget_string(event, "metaData.app.weather")) == 0);
  ASSERT(json_object_dotget_boolean(event, "metaData.app.experimentX") == -1);
  ASSERT(json_object_dotget_number(event, "metaData.app.counter") - 47.8 < 0.01);
  json_value_free(root_value);
  PASS();
}

TEST test_exception_to_json(void) {
  JSON_Value *root_value = bsg_generate_json();
  JSON_Object *event = json_value_get_object(root_value);
  ASSERT(event != NULL);
  JSON_Array *exceptions = json_object_get_array(event, "exceptions");
  ASSERT(exceptions != NULL);
  JSON_Object *exception = json_array_get_object(exceptions, 0);
  ASSERT(exception != NULL);
  ASSERT(strcmp("SIGBUS", json_object_get_string(exception, "errorClass")) == 0);
  ASSERT(strcmp("POSIX is serious about oncoming traffic", json_object_get_string(exception, "message")) == 0);
  ASSERT(strcmp("c", json_object_get_string(exception, "type")) == 0);
  JSON_Array *stacktrace = json_object_get_array(exception, "stacktrace");
  ASSERT(stacktrace != NULL);
  ASSERT_EQ(2, json_array_get_count(stacktrace));
  ASSERT(strcmp("makinBacon", json_object_get_string(json_array_get_object(stacktrace, 0), "method")) == 0);
  ASSERT_STR_EQ("0x6eeeb", json_object_get_string(json_array_get_object(stacktrace, 0), "frameAddress"));
  ASSERT(strcmp("0x5393e", json_object_get_string(json_array_get_object(stacktrace, 1), "method")) == 0);
  ASSERT_STR_EQ("0x5393e", json_object_get_string(json_array_get_object(stacktrace, 1), "frameAddress"));
  json_value_free(root_value);
  PASS();
}

TEST test_breadcrumbs_to_json(void) {
  JSON_Value *root_value = bsg_generate_json();
  JSON_Object *event = json_value_get_object(root_value);
  ASSERT(event != NULL);
  JSON_Array *breadcrumbs = json_object_get_array(event, "breadcrumbs");
  ASSERT(breadcrumbs != NULL);
  ASSERT_EQ(2, json_array_get_count(breadcrumbs));

  JSON_Object *crumb1 = json_array_get_object(breadcrumbs, 0);
  ASSERT_STR_EQ("decrease torque", json_object_get_string(crumb1, "name"));
  ASSERT_STR_EQ("state", json_object_get_string(crumb1, "type"));
  ASSERT_EQ(1, json_object_get_count(json_object_get_object(crumb1, "metaData")));
  ASSERT_STR_EQ("Moving laterally 26º", json_object_get_string(json_object_get_object(crumb1, "metaData"), "message"));

  JSON_Object *crumb2 = json_array_get_object(breadcrumbs, 1);
  ASSERT_STR_EQ("enable blasters", json_object_get_string(crumb2, "name"));
  ASSERT_STR_EQ("user", json_object_get_string(crumb2, "type"));
  ASSERT_EQ(1, json_object_get_count(json_object_get_object(crumb2, "metaData")));
  ASSERT_STR_EQ("this is a drill.", json_object_get_string(json_object_get_object(crumb2, "metaData"), "message"));
  PASS();
}


SUITE(suite_json_serialization) {
  RUN_TEST(test_last_run_info_serialization);
  RUN_TEST(test_session_handled_counts);
  RUN_TEST(test_context_to_json);
  RUN_TEST(test_grouping_hash_to_json);
  RUN_TEST(test_app_info_to_json);
  RUN_TEST(test_device_info_to_json);
  RUN_TEST(test_user_info_to_json);
  RUN_TEST(test_custom_info_to_json);
  RUN_TEST(test_exception_to_json);
  RUN_TEST(test_breadcrumbs_to_json);
}

SUITE(suite_struct_to_file) {
  RUN_TEST(test_report_to_file);
  RUN_TEST(test_file_to_report);
  RUN_TEST(test_report_with_feature_flags_to_file);
  RUN_TEST(test_report_with_feature_flags_from_file);
  RUN_TEST(test_report_with_opaque_metadata_from_file);
}
