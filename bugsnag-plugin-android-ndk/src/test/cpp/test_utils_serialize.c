#include <greatest/greatest.h>
#include <utils/serializer.h>
#include <stdlib.h>
#include <utils/migrate.h>

#define SERIALIZE_TEST_FILE "/data/data/com.bugsnag.android.ndk.test/cache/foo.crash"

bugsnag_breadcrumb *init_breadcrumb(const char *name, const char *message, bsg_breadcrumb_t type);

void generate_basic_report(bugsnag_report *report) {
  strcpy(report->context, "SomeActivity");
  strcpy(report->exception.name, "SIGBUS");
  strcpy(report->exception.message, "POSIX is serious about oncoming traffic");
  report->exception.stacktrace[0].frame_address = 454379;
  report->exception.stacktrace[1].frame_address = 342334;
  report->exception.frame_count = 2;
  strcpy(report->exception.stacktrace[0].method, "makinBacon");
  strcpy(report->app.name, "PhotoSnap Plus");
  strcpy(report->app.id, "com.example.PhotoSnapPlus");
  strcpy(report->app.package_name, "com.example.PhotoSnapPlus");
  strcpy(report->app.release_stage, "リリース");
  strcpy(report->app.version, "2.0.52");
  report->app.version_code = 57;
  strcpy(report->app.version_name, "2.0");
  strcpy(report->app.build_uuid, "1234-9876-adfe");
  strcpy(report->device.manufacturer, "HI-TEC™");
  strcpy(report->device.model, "Rasseur");
  strcpy(report->device.locale, "en_AU#Melbun");
  strcpy(report->user.email, "fenton@io.example.com");
  strcpy(report->user.id, "fex");
  report->device.total_memory = 234678100;
  report->app.duration = 6502;
  report->app.in_foreground = true;
  report->app.low_memory = false;
  report->app.memory_usage = 456009;
  bugsnag_report_add_metadata_bool(report, "metrics", "experimentX", false);
  bugsnag_report_add_metadata_string(report, "metrics", "subject", "percy");
  bugsnag_report_add_metadata_string(report, "app", "weather", "rain");
  bugsnag_report_add_metadata_double(report, "metrics", "counter", 47.8);

  bugsnag_breadcrumb *crumb1 = init_breadcrumb("decrease torque", "Moving laterally 26º", BSG_CRUMB_STATE);
  bugsnag_report_add_breadcrumb(report, crumb1);
  bugsnag_breadcrumb *crumb2 = init_breadcrumb("enable blasters", "this is a drill.", BSG_CRUMB_USER);
  bugsnag_report_add_breadcrumb(report, crumb2);

  report->handled_events = 1;
  report->unhandled_events = 1;
  strcpy(report->session_id, "f1ab");
  strcpy(report->session_start, "2019-03-19T12:58:19+00:00");
}

bugsnag_report_v1 *bsg_generate_report_v1(void) {
  bugsnag_report_v1 *report = calloc(1, sizeof(bugsnag_report_v1));
  generate_basic_report(report);
  return report;
}

bugsnag_report *bsg_generate_report(void) {
  bugsnag_report *report = calloc(1, sizeof(bugsnag_report));
  generate_basic_report(report);
  report->unhandled_events = 2;
  return report;
}

TEST test_report_to_file(void) {
  bsg_environment *env = malloc(sizeof(bsg_environment));
  env->report_header.version = 7;
  env->report_header.big_endian = 1;
  bugsnag_report *report = bsg_generate_report();
  memcpy(&env->next_report, report, sizeof(bugsnag_report));
  strcpy(env->report_header.os_build, "macOS Sierra");
  strcpy(env->next_report_path, SERIALIZE_TEST_FILE);
  ASSERT(bsg_serialize_report_to_file(env));
  free(report);
  free(env);
  PASS();
}

TEST test_file_to_report(void) {
  bsg_environment *env = malloc(sizeof(bsg_environment));
  env->report_header.version = 7;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");
  bugsnag_report *generated_report = bsg_generate_report();
  memcpy(&env->next_report, generated_report, sizeof(bugsnag_report));
  strcpy(env->next_report_path, SERIALIZE_TEST_FILE);
  bsg_serialize_report_to_file(env);

  bugsnag_report *report = bsg_deserialize_report_from_file(SERIALIZE_TEST_FILE);
  ASSERT(report != NULL);
  ASSERT(strcmp("SIGBUS", report->exception.name) == 0);
  ASSERT(strcmp("POSIX is serious about oncoming traffic", report->exception.message) == 0);
  free(generated_report);
  free(env);
  free(report);
  PASS();
}

TEST test_report_v1_migration(void) {
  bsg_environment *env = malloc(sizeof(bsg_environment));
  env->report_header.version = 1;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");
  bugsnag_report_v1 *generated_report = bsg_generate_report_v1();
  memcpy(&env->next_report, generated_report, sizeof(bugsnag_report_v1));
  strcpy(env->next_report_path, SERIALIZE_TEST_FILE);
  bsg_serialize_report_to_file(env);

  bugsnag_report *report = bsg_deserialize_report_from_file(SERIALIZE_TEST_FILE);
  ASSERT(report != NULL);
  ASSERT(strcmp("f1ab", report->session_id) == 0);
  ASSERT(strcmp("2019-03-19T12:58:19+00:00", report->session_start) == 0);
  ASSERT_EQ(1, report->handled_events);
  ASSERT_EQ(1, report->unhandled_events);

  free(generated_report);
  free(env);
  free(report);
  PASS();
}

// helper function
JSON_Value *bsg_generate_json(void) {
  bugsnag_report *report = bsg_generate_report();
  char *json = bsg_serialize_report_to_json_string(report);
  JSON_Value *root_value = json_parse_string(json);
  free(json);
  free(report);
  return root_value;
}

TEST test_report_app_info_to_json(void) {
  JSON_Value *root_value = bsg_generate_json();
  JSON_Object *event = json_value_get_object(root_value);
  ASSERT(strcmp("2.0.52", json_object_dotget_string(event, "app.version")) == 0);
  ASSERT(strcmp( "PhotoSnap Plus", json_object_dotget_string(event, "metaData.app.name")) == 0);
  ASSERT(strcmp( "com.example.PhotoSnapPlus", json_object_dotget_string(event, "metaData.app.packageName")) == 0);
  ASSERT(strcmp( "com.example.PhotoSnapPlus", json_object_dotget_string(event, "app.id")) == 0);
  ASSERT(strcmp( "リリース", json_object_dotget_string(event, "app.releaseStage")) == 0);
  ASSERT_EQ(57, json_object_dotget_number(event, "app.versionCode"));
  ASSERT(strcmp( "2.0", json_object_dotget_string(event, "metaData.app.versionName")) == 0);
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

TEST test_report_context_to_json(void) {
  JSON_Value *root_value = bsg_generate_json();
  JSON_Object *event = json_value_get_object(root_value);
  ASSERT(event != NULL);
  ASSERT(strcmp( "SomeActivity", json_object_get_string(event, "context")) == 0);
  PASS();
}

TEST test_report_device_info_to_json(void) {
  JSON_Value *root_value = bsg_generate_json();
  JSON_Object *event = json_value_get_object(root_value);
  ASSERT(event != NULL);
  ASSERT(strcmp( "HI-TEC™", json_object_dotget_string(event, "device.manufacturer")) == 0);
  ASSERT(strcmp( "Rasseur", json_object_dotget_string(event, "device.model")) == 0);
  ASSERT(strcmp( "en_AU#Melbun", json_object_dotget_string(event, "metaData.device.locale")) == 0);
  json_value_free(root_value);
  PASS();
}

TEST test_report_user_info_to_json(void) {
  JSON_Value *root_value = bsg_generate_json();
  JSON_Object *event = json_value_get_object(root_value);
  ASSERT(event != NULL);
  ASSERT(strcmp( "fex", json_object_dotget_string(event, "user.id")) == 0);
  ASSERT(strcmp( "fenton@io.example.com", json_object_dotget_string(event, "user.email")) == 0);
  ASSERT(json_object_dotget_string(event, "user.name") == NULL);
  json_value_free(root_value);
  PASS();
}

TEST test_report_custom_info_to_json(void) {
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

TEST test_report_exception_to_json(void) {
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
  ASSERT_EQ(454379, json_object_get_number(json_array_get_object(stacktrace, 0), "frameAddress"));
  ASSERT(strcmp("0x5393e", json_object_get_string(json_array_get_object(stacktrace, 1), "method")) == 0);
  ASSERT_EQ(342334, json_object_get_number(json_array_get_object(stacktrace, 1), "frameAddress"));
  json_value_free(root_value);
  PASS();
}

TEST test_report_breadcrumbs_to_json(void) {
  JSON_Value *root_value = bsg_generate_json();
  JSON_Object *event = json_value_get_object(root_value);
  ASSERT(event != NULL);
  JSON_Array *breadcrumbs = json_object_get_array(event, "breadcrumbs");
  ASSERT(breadcrumbs != NULL);
  ASSERT_EQ(2, json_array_get_count(breadcrumbs));
  JSON_Object *crumb1 = json_array_get_object(breadcrumbs, 0);
  JSON_Object *crumb2 = json_array_get_object(breadcrumbs, 1);
  ASSERT(strcmp("decrease torque", json_object_get_string(crumb1, "name")) == 0);
  ASSERT(strcmp("state", json_object_get_string(crumb1, "type")) == 0);
  ASSERT_EQ(1, json_object_get_count(json_object_get_object(crumb1, "metaData")));
  ASSERT(strcmp("Moving laterally 26º", json_object_get_string(json_object_get_object(crumb1, "metaData"), "message")) == 0);
  ASSERT(strcmp("enable blasters", json_object_get_string(crumb2, "name")) == 0);
  ASSERT(strcmp("user", json_object_get_string(crumb2, "type")) == 0);
  ASSERT_EQ(1, json_object_get_count(json_object_get_object(crumb2, "metaData")));
  ASSERT(strcmp("this is a drill.", json_object_get_string(json_object_get_object(crumb2, "metaData"), "message")) == 0);
  json_value_free(root_value);
  PASS();
}

SUITE(serialize_utils) {
  RUN_TEST(test_report_to_file);
  RUN_TEST(test_file_to_report);
  RUN_TEST(test_report_v1_migration);
  RUN_TEST(test_session_handled_counts);
  RUN_TEST(test_report_context_to_json);
  RUN_TEST(test_report_app_info_to_json);
  RUN_TEST(test_report_device_info_to_json);
  RUN_TEST(test_report_user_info_to_json);
  RUN_TEST(test_report_custom_info_to_json);
  RUN_TEST(test_report_exception_to_json);
  RUN_TEST(test_report_breadcrumbs_to_json);
}

