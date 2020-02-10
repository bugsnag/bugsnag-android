#include <greatest/greatest.h>
#include <utils/serializer.h>
#include <stdlib.h>
#include <utils/migrate.h>

#define SERIALIZE_TEST_FILE "/data/data/com.bugsnag.android.ndk.test/cache/foo.crash"

bugsnag_breadcrumb *init_breadcrumb(const char *name, const char *message, bsg_breadcrumb_t type);


bool bsg_report_header_write(bsg_report_header *header, int fd);

bool bsg_report_v1_write(bsg_report_header *header, bugsnag_report_v1 *report,
                         int fd) {
  if (!bsg_report_header_write(header, fd)) {
    return false;
  }
  ssize_t len = write(fd, report, sizeof(bugsnag_report_v1));
  return len == sizeof(bugsnag_report_v1);
}

bool bsg_report_v2_write(bsg_report_header *header, bugsnag_report_v2 *report,
                         int fd) {
  if (!bsg_report_header_write(header, fd)) {
    return false;
  }
  ssize_t len = write(fd, report, sizeof(bugsnag_report_v2));
  return len == sizeof(bugsnag_report_v2);
}

bool bsg_serialize_report_v1_to_file(bsg_environment *env, bugsnag_report_v1 *report) {
  int fd = open(env->next_event_path, O_WRONLY | O_CREAT, 0644);
  if (fd == -1) {
    return false;
  }
  return bsg_report_v1_write(&env->report_header, report, fd);
}

bool bsg_serialize_report_v2_to_file(bsg_environment *env, bugsnag_report_v2 *report) {
  int fd = open(env->next_event_path, O_WRONLY | O_CREAT, 0644);
  if (fd == -1) {
    return false;
  }
  return bsg_report_v2_write(&env->report_header, report, fd);
}


void generate_basic_report(bugsnag_event *event) {
  strcpy(event->grouping_hash, "foo-hash");
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
  event->app.in_foreground = true;
  bugsnag_event_add_metadata_bool(event, "metrics", "experimentX", false);
  bugsnag_event_add_metadata_string(event, "metrics", "subject", "percy");
  bugsnag_event_add_metadata_string(event, "app", "weather", "rain");
  bugsnag_event_add_metadata_double(event, "metrics", "counter", 47.8);

  bugsnag_breadcrumb *crumb1 = init_breadcrumb("decrease torque", "Moving laterally 26º", BSG_CRUMB_STATE);
  bugsnag_event_add_breadcrumb(event, crumb1);
  bugsnag_breadcrumb *crumb2 = init_breadcrumb("enable blasters", "this is a drill.", BSG_CRUMB_USER);
  bugsnag_event_add_breadcrumb(event, crumb2);

  event->handled_events = 1;
  event->unhandled_events = 1;
  strcpy(event->session_id, "f1ab");
  strcpy(event->session_start, "2019-03-19T12:58:19+00:00");

  strcpy(event->notifier.version, "1.0");
  strcpy(event->notifier.url, "bugsnag.com");
  strcpy(event->notifier.name, "Test Notifier");
}

bugsnag_report_v2 *bsg_generate_report_v2(void) {
  bugsnag_report_v2 *report = calloc(1, sizeof(bugsnag_report_v2));
  generate_basic_report((bugsnag_event *) report);

  strcpy(report->exception.name, "SIGBUS");
  strcpy(report->exception.message, "POSIX is serious about oncoming traffic");
  report->exception.stacktrace[0].frame_address = 454379;
  report->exception.frame_count = 1;
  strcpy(report->exception.type, "C");
  strcpy(report->exception.stacktrace[0].method, "makinBacon");

  strcpy(report->app.package_name, "com.example.foo");
  strcpy(report->app.version_name, "2.5");
  return report;
}

bugsnag_report_v1 *bsg_generate_report_v1(void) {
  bugsnag_report_v1 *report = calloc(1, sizeof(bugsnag_report_v1));
  strcpy(report->session_id, "f1ab");
  strcpy(report->session_start, "2019-03-19T12:58:19+00:00");
  report->handled_events = 1;
  return report;
}

bugsnag_event *bsg_generate_event(void) {
  bugsnag_event *report = calloc(1, sizeof(bugsnag_event));
  generate_basic_report(report);
  report->unhandled_events = 2;
  return report;
}

TEST test_report_to_file(void) {
  bsg_environment *env = malloc(sizeof(bsg_environment));
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

TEST test_file_to_report(void) {
  bsg_environment *env = malloc(sizeof(bsg_environment));
  env->report_header.version = 7;
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

TEST test_report_v1_migration(void) {
  bsg_environment *env = malloc(sizeof(bsg_environment));
  env->report_header.version = 1;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");
  bugsnag_report_v1 *generated_report = bsg_generate_report_v1();
  memcpy(&env->next_event, generated_report, sizeof(bugsnag_report_v1));
  strcpy(env->next_event_path, SERIALIZE_TEST_FILE);
  bsg_serialize_report_v1_to_file(env, generated_report);

  bugsnag_event *event = bsg_deserialize_event_from_file(SERIALIZE_TEST_FILE);
  ASSERT(event != NULL);
  ASSERT(strcmp("f1ab", event->session_id) == 0);
  ASSERT(strcmp("2019-03-19T12:58:19+00:00", event->session_start) == 0);
  ASSERT_EQ(1, event->handled_events);
  ASSERT_EQ(1, event->unhandled_events);

  free(generated_report);
  free(env);
  free(event);
  PASS();
}

TEST test_report_v2_migration(void) {
  bsg_environment *env = malloc(sizeof(bsg_environment));
  env->report_header.version = 2;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");

  bugsnag_report_v2 *generated_report = bsg_generate_report_v2();
  memcpy(&env->next_event, generated_report, sizeof(bugsnag_report_v2));
  strcpy(env->next_event_path, SERIALIZE_TEST_FILE);
  bsg_serialize_report_v2_to_file(env, generated_report);

  bugsnag_event *event = bsg_deserialize_event_from_file(SERIALIZE_TEST_FILE);
  ASSERT(event != NULL);

  // bsg_library -> bsg_notifier
  ASSERT_STR_EQ("Test Notifier", event->notifier.name);
  ASSERT_STR_EQ("bugsnag.com", event->notifier.url);
  ASSERT_STR_EQ("1.0", event->notifier.version);

  // bsg_exception -> bsg_error
  ASSERT_STR_EQ("SIGBUS", event->error.errorClass);
  ASSERT_STR_EQ("POSIX is serious about oncoming traffic", event->error.errorMessage);
  ASSERT_STR_EQ("C", event->error.type);
  ASSERT_EQ(1, event->error.frame_count);
  ASSERT_STR_EQ("makinBacon", event->error.stacktrace[0].method);

  // event.device
  ASSERT_STR_EQ("android", event->device.os_name);

  // package_name/version_name are migrated to metadata
  ASSERT_STR_EQ("com.example.foo", event->metadata.values[0].char_value);
  ASSERT_STR_EQ("2.5", event->metadata.values[1].char_value);

  free(generated_report);
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
  ASSERT_EQ(454379, json_object_get_number(json_array_get_object(stacktrace, 0), "frameAddress"));
  ASSERT(strcmp("0x5393e", json_object_get_string(json_array_get_object(stacktrace, 1), "method")) == 0);
  ASSERT_EQ(342334, json_object_get_number(json_array_get_object(stacktrace, 1), "frameAddress"));
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
  RUN_TEST(test_report_v2_migration);
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
