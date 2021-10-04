#include <greatest/greatest.h>
#include <utils/serializer.h>
#include <stdlib.h>
#include <utils/migrate.h>

#define SERIALIZE_TEST_FILE "/data/data/com.bugsnag.android.ndk.test/cache/foo.crash"

bugsnag_breadcrumb *init_breadcrumb(const char *name, char *message, bugsnag_breadcrumb_type type);

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

bool bsg_report_v3_write(bsg_report_header *header, bugsnag_report_v3 *report,
                         int fd) {
    if (!bsg_report_header_write(header, fd)) {
        return false;
    }
    ssize_t len = write(fd, report, sizeof(bugsnag_report_v3));
    return len == sizeof(bugsnag_report_v3);
}

bool bsg_report_v4_write(bsg_report_header *header, bugsnag_report_v4 *report,
                         int fd) {
  if (!bsg_report_header_write(header, fd)) {
    return false;
  }
  ssize_t len = write(fd, report, sizeof(bugsnag_report_v4));
  return len == sizeof(bugsnag_report_v4);
}

bool bsg_report_v5_write(bsg_report_header *header, bugsnag_report_v5 *report,
                         int fd) {
  if (!bsg_report_header_write(header, fd)) {
    return false;
  }
  ssize_t len = write(fd, report, sizeof(bugsnag_report_v5));
  return len == sizeof(bugsnag_report_v5);
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

bool bsg_serialize_report_v3_to_file(bsg_environment *env, bugsnag_report_v3 *report) {
    int fd = open(env->next_event_path, O_WRONLY | O_CREAT, 0644);
    if (fd == -1) {
        return false;
    }
    return bsg_report_v3_write(&env->report_header, report, fd);
}

bool bsg_serialize_report_v4_to_file(bsg_environment *env, bugsnag_report_v4 *report) {
  int fd = open(env->next_event_path, O_WRONLY | O_CREAT, 0644);
  if (fd == -1) {
    return false;
  }
  return bsg_report_v4_write(&env->report_header, report, fd);
}

bool bsg_serialize_report_v5_to_file(bsg_environment *env, bugsnag_report_v5 *report) {
  int fd = open(env->next_event_path, O_WRONLY | O_CREAT, 0644);
  if (fd == -1) {
    return false;
  }
  return bsg_report_v5_write(&env->report_header, report, fd);
}

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
  bugsnag_event_add_metadata_bool(event, "metrics", "experimentX", false);
  bugsnag_event_add_metadata_string(event, "metrics", "subject", "percy");
  bugsnag_event_add_metadata_string(event, "app", "weather", "rain");
  bugsnag_event_add_metadata_double(event, "metrics", "counter", 47.8);

  event->crumb_count = 0;
  event->crumb_first_index = 0;
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

bugsnag_report_v5 *bsg_generate_report_v5(void) {
  bugsnag_report_v5 *event = calloc(1, sizeof(bugsnag_report_v5));
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
  bugsnag_event_add_metadata_bool(event, "metrics", "experimentX", false);
  bugsnag_event_add_metadata_string(event, "metrics", "subject", "percy");
  bugsnag_event_add_metadata_string(event, "app", "weather", "rain");
  bugsnag_event_add_metadata_double(event, "metrics", "counter", 47.8);

  bugsnag_breadcrumb *crumb1 = init_breadcrumb("decrease torque", "Moving laterally 26º",
                                               BSG_CRUMB_STATE);
  bugsnag_breadcrumb *crumb2 = init_breadcrumb("enable blasters", "this is a drill.",
                                               BSG_CRUMB_USER);
  memcpy(&event->breadcrumbs[0], crumb1, sizeof(bugsnag_breadcrumb));
  memcpy(&event->breadcrumbs[1], crumb2, sizeof(bugsnag_breadcrumb));
  event->crumb_count = 2;
  event->crumb_first_index = 0;

  event->handled_events = 1;
  event->unhandled_events = 1;
  strcpy(event->session_id, "f1ab");
  strcpy(event->session_start, "2019-03-19T12:58:19+00:00");

  strcpy(event->notifier.version, "1.0");
  strcpy(event->notifier.url, "bugsnag.com");
  strcpy(event->notifier.name, "Test Notifier");
  return event;
}

bugsnag_report_v4 *bsg_generate_report_v4(void) {
  bugsnag_report_v4 *event = calloc(1, sizeof(bugsnag_report_v4));
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
  bugsnag_event_add_metadata_bool(event, "metrics", "experimentX", false);
  bugsnag_event_add_metadata_string(event, "metrics", "subject", "percy");
  bugsnag_event_add_metadata_string(event, "app", "weather", "rain");
  bugsnag_event_add_metadata_double(event, "metrics", "counter", 47.8);

  bugsnag_breadcrumb *crumb1 = init_breadcrumb("decrease torque", "Moving laterally 26º",
                                               BSG_CRUMB_STATE);
  bugsnag_breadcrumb *crumb2 = init_breadcrumb("enable blasters", "this is a drill.",
                                               BSG_CRUMB_USER);
  memcpy(&event->breadcrumbs[0], crumb1, sizeof(bugsnag_breadcrumb));
  memcpy(&event->breadcrumbs[1], crumb2, sizeof(bugsnag_breadcrumb));
  event->crumb_count = 2;
  event->crumb_first_index = 0;

  event->handled_events = 1;
  event->unhandled_events = 1;
  strcpy(event->session_id, "f1ab");
  strcpy(event->session_start, "2019-03-19T12:58:19+00:00");

  strcpy(event->notifier.version, "1.0");
  strcpy(event->notifier.url, "bugsnag.com");
  strcpy(event->notifier.name, "Test Notifier");
  return event;
}

bugsnag_report_v3 *bsg_generate_report_v3(void) {
  bugsnag_report_v3 *event = calloc(1, sizeof(bugsnag_report_v3));
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
  bugsnag_event_add_metadata_bool(event, "metrics", "experimentX", false);
  bugsnag_event_add_metadata_string(event, "metrics", "subject", "percy");
  bugsnag_event_add_metadata_string(event, "app", "weather", "rain");
  bugsnag_event_add_metadata_double(event, "metrics", "counter", 47.8);

  bugsnag_breadcrumb *crumb1 = init_breadcrumb("decrease torque", "Moving laterally 26º",
                                               BSG_CRUMB_STATE);
  bugsnag_breadcrumb *crumb2 = init_breadcrumb("enable blasters", "this is a drill.",
                                               BSG_CRUMB_USER);
  memcpy(&event->breadcrumbs[0], crumb1, sizeof(bugsnag_breadcrumb));
  memcpy(&event->breadcrumbs[1], crumb2, sizeof(bugsnag_breadcrumb));
  event->crumb_count = 2;
  event->crumb_first_index = 0;

  event->handled_events = 1;
  event->unhandled_events = 1;
  strcpy(event->session_id, "f1ab");
  strcpy(event->session_start, "2019-03-19T12:58:19+00:00");

  strcpy(event->notifier.version, "1.0");
  strcpy(event->notifier.url, "bugsnag.com");
  strcpy(event->notifier.name, "Test Notifier");
  return event;
}

bugsnag_report_v2 *bsg_generate_report_v2(void) {
  bugsnag_report_v2 *event = calloc(1, sizeof(bugsnag_report_v2));
  strcpy(event->context, "SomeActivity");
  strcpy(event->app.id, "com.example.PhotoSnapPlus");
  strcpy(event->app.release_stage, "リリース");
  strcpy(event->app.version, "2.0.52");
  event->app.version_code = 57;
  strcpy(event->app.build_uuid, "1234-9876-adfe");
  strcpy(event->device.manufacturer, "HI-TEC™");
  strcpy(event->device.model, "Rasseur");
  strcpy(event->device.locale, "en_AU#Melbun");
  strcpy(event->user.email, "fenton@io.example.com");
  strcpy(event->user.id, "fex");
  event->device.total_memory = 234678100;
  event->app.duration = 6502;
  bugsnag_event_add_metadata_bool(event, "metrics", "experimentX", false);
  bugsnag_event_add_metadata_string(event, "metrics", "subject", "percy");
  bugsnag_event_add_metadata_string(event, "app", "weather", "rain");
  bugsnag_event_add_metadata_double(event, "metrics", "counter", 47.8);


  event->handled_events = 1;
  event->unhandled_events = 1;
  strcpy(event->session_id, "f1ab");
  strcpy(event->session_start, "2019-03-19T12:58:19+00:00");

  strcpy(event->notifier.version, "1.0");
  strcpy(event->notifier.url, "bugsnag.com");
  strcpy(event->notifier.name, "Test Notifier");

  strcpy(event->exception.name, "SIGBUS");
  strcpy(event->exception.message, "POSIX is serious about oncoming traffic");
  event->exception.stacktrace[0].frame_address = 454379;
  event->exception.frame_count = 1;
  strcpy(event->exception.type, "C");
  strcpy(event->exception.stacktrace[0].method, "makinBacon");

  strcpy(event->app.package_name, "com.example.foo");
  strcpy(event->app.version_name, "2.5");

  bugsnag_breadcrumb_v1 *crumb1 = &event->breadcrumbs[0];
  crumb1->type = BSG_CRUMB_STATE;
  strcpy(crumb1->timestamp, "2018-08-29T21:41:39Z");
  strcpy(crumb1->name, "decrease torque");
  strcpy(crumb1->metadata[0].key, "message");
  strcpy(crumb1->metadata[0].value, "Moving laterally 26º");

  bugsnag_breadcrumb_v1 *crumb2 = &event->breadcrumbs[1];
  crumb2->type = BSG_CRUMB_USER;
  strcpy(crumb2->timestamp, "2018-08-29T21:41:39Z");
  strcpy(crumb2->name, "enable blasters");
  strcpy(crumb2->metadata[0].key, "message");
  strcpy(crumb2->metadata[0].value, "this is a drill.");

  event->crumb_first_index = 0;
  event->crumb_count = 2;
  return event;
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
  bugsnag_event_add_breadcrumb(report, crumb1);

  bugsnag_breadcrumb *crumb2 = init_breadcrumb("enable blasters", "this is a drill.",
                                               BSG_CRUMB_USER);
  bugsnag_event_add_breadcrumb(report, crumb2);

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

TEST test_file_to_report(void) {
  bsg_environment *env = calloc(1, sizeof(bsg_environment));
  env->report_header.version = 5;
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
  bsg_environment *env = calloc(1, sizeof(bsg_environment));
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
  ASSERT_FALSE(event->app.is_launching);
  ASSERT_EQ(0, event->thread_count);

  free(generated_report);
  free(env);
  free(event);
  PASS();
}

TEST test_report_v2_migration(void) {
  bsg_environment *env = calloc(1, sizeof(bsg_environment));

  bugsnag_report_v2 *generated_report = bsg_generate_report_v2();
  memcpy(&env->next_event, generated_report, sizeof(bugsnag_report_v2));
  strcpy(env->next_event_path, SERIALIZE_TEST_FILE);

  env->report_header.version = 2;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");
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

  // breadcrumbs are migrated correctly
  ASSERT_EQ(2, event->crumb_count);
  ASSERT_EQ(0, event->crumb_first_index);

  bugsnag_breadcrumb_v1 *crumb = &generated_report->breadcrumbs[0];
  bsg_char_metadata_pair *val = &crumb->metadata[0];

  ASSERT_STR_EQ("decrease torque", crumb->name);
  ASSERT_STR_EQ("2018-08-29T21:41:39Z", crumb->timestamp);
  ASSERT_EQ(BSG_CRUMB_STATE, crumb->type);
  ASSERT_STR_EQ("message", val->key);
  ASSERT_STR_EQ("Moving laterally 26º", val->value);
  ASSERT_FALSE(event->app.is_launching);
  ASSERT_EQ(0, event->thread_count);

  free(generated_report);
  free(env);
  free(event);
  PASS();
}

TEST test_report_v3_migration(void) {
  bsg_environment *env = calloc(1, sizeof(bsg_environment));
  env->report_header.version = 3;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");

  bugsnag_report_v3 *generated_report = bsg_generate_report_v3();
  memcpy(&env->next_event, generated_report, sizeof(bugsnag_report_v3));
  strcpy(env->next_event_path, SERIALIZE_TEST_FILE);
  bsg_serialize_report_v3_to_file(env, generated_report);

  bugsnag_event *event = bsg_deserialize_event_from_file(SERIALIZE_TEST_FILE);
  ASSERT(event != NULL);

  // api key is set to sensible default
  ASSERT_STR_EQ("", event->api_key);

  // other fields appear reasonable and are copied over
  ASSERT_STR_EQ("Test Notifier", event->notifier.name);
  ASSERT_STR_EQ("bugsnag.com", event->notifier.url);
  ASSERT_STR_EQ("1.0", event->notifier.version);
  ASSERT_STR_EQ("SIGBUS", event->error.errorClass);
  ASSERT_STR_EQ("POSIX is serious about oncoming traffic", event->error.errorMessage);
  ASSERT_STR_EQ("C", event->error.type);
  ASSERT_FALSE(event->app.is_launching);
  ASSERT_EQ(0, event->thread_count);

  free(generated_report);
  free(env);
  free(event);
  PASS();
}

TEST test_report_v4_migration(void) {
  bsg_environment *env = calloc(1, sizeof(bsg_environment));
  env->report_header.version = 4;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");

  bugsnag_report_v4 *generated_report = bsg_generate_report_v4();
  memcpy(&env->next_event, generated_report, sizeof(bugsnag_report_v4));
  strcpy(env->next_event_path, SERIALIZE_TEST_FILE);
  bsg_serialize_report_v4_to_file(env, generated_report);

  bugsnag_event *event = bsg_deserialize_event_from_file(SERIALIZE_TEST_FILE);
  ASSERT(event != NULL);

  // values are migrated correctly
  ASSERT_STR_EQ("com.example.PhotoSnapPlus", event->app.id);
  ASSERT_STR_EQ("リリース", event->app.release_stage);
  ASSERT_STR_EQ("2.0.52", event->app.version);
  ASSERT_EQ(57, event->app.version_code);
  ASSERT_STR_EQ("1234-9876-adfe", event->app.build_uuid);
  ASSERT_FALSE(event->app.in_foreground);
  ASSERT_FALSE(event->app.is_launching);
  ASSERT_EQ(0, event->thread_count);

  free(generated_report);
  free(env);
  free(event);
  PASS();
}

void prepare_v5_report(bsg_environment *env, bugsnag_report_v5 *generated_report,
                       int first_crumb_index) {
  strcpy(generated_report->context, "SomeActivity");

  for (int k = 0; k < V2_BUGSNAG_CRUMBS_MAX + first_crumb_index; k++) {
    int index = k % V2_BUGSNAG_CRUMBS_MAX;
    char *str = calloc(1, sizeof(char) * 64);
    sprintf(str, "%d", k);
    bugsnag_breadcrumb *crumb = init_breadcrumb(str, "Oh crumbs", BSG_CRUMB_STATE);
    memcpy(&generated_report->breadcrumbs[index], crumb, sizeof(bugsnag_breadcrumb));
    free(crumb);
    free(str);
  }
  generated_report->crumb_count = V2_BUGSNAG_CRUMBS_MAX;
  generated_report->crumb_first_index = first_crumb_index;
  memcpy(&env->next_event, generated_report, sizeof(bugsnag_report_v5));
  strcpy(env->next_event_path, SERIALIZE_TEST_FILE);
  bsg_serialize_report_v5_to_file(env, generated_report);
}

TEST test_report_v5_migration(void) {
  bsg_environment *env = calloc(1, sizeof(bsg_environment));
  env->report_header.version = 5;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");

  // verify that breadcrumbs and context (after breadcrumbs in struct) are migrated correctly.
  bugsnag_report_v5 *generated_report = calloc(1, sizeof(bugsnag_report_v5));
  prepare_v5_report(env, generated_report, 0);
  bugsnag_event *event = bsg_deserialize_event_from_file(SERIALIZE_TEST_FILE);
  ASSERT(event != NULL);

  // values are migrated correctly
  ASSERT_STR_EQ("SomeActivity", event->context);
  ASSERT_EQ(25, event->crumb_count);
  ASSERT_EQ(0, event->crumb_first_index);

  for (int k = 0; k < V2_BUGSNAG_CRUMBS_MAX; ++k) {
    char *str = calloc(1, sizeof(char) * 64);
    sprintf(str, "%d", k);
    bugsnag_breadcrumb *crumb = &event->breadcrumbs[k];
    ASSERT_STR_EQ(str, crumb->name);
    ASSERT_EQ(BSG_CRUMB_STATE, crumb->type);
    free(str);
  }

  ASSERT_EQ(0, event->thread_count);

  free(generated_report);
  free(env);
  free(event);
  PASS();
}

TEST test_report_v5_migration_crumb_index(void) {
  bsg_environment *env = calloc(1, sizeof(bsg_environment));
  env->report_header.version = 5;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");

  // verify that breadcrumbs and context (after breadcrumbs in struct) are migrated correctly.
  bugsnag_report_v5 *generated_report = calloc(1, sizeof(bugsnag_report_v5));
  int offset = 12;
  prepare_v5_report(env, generated_report, offset);
  strcpy(env->next_event_path, SERIALIZE_TEST_FILE);
  bsg_serialize_report_v5_to_file(env, generated_report);

  bugsnag_event *event = bsg_deserialize_event_from_file(SERIALIZE_TEST_FILE);
  ASSERT(event != NULL);

  // values are migrated correctly
  ASSERT_STR_EQ("SomeActivity", event->context);
  ASSERT_EQ(25, event->crumb_count);
  ASSERT_EQ(0, event->crumb_first_index);

  ASSERT_STR_EQ("12", event->breadcrumbs[0].name);
  ASSERT_STR_EQ("13", event->breadcrumbs[1].name);
  ASSERT_STR_EQ("26", event->breadcrumbs[14].name);
  ASSERT_STR_EQ("36", event->breadcrumbs[24].name);

  free(generated_report);
  free(env);
  free(event);
  PASS();
}

TEST test_report_v6_migration(void) {
  bsg_environment *env = calloc(1, sizeof(bsg_environment));
  env->report_header.version = 4;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");

  bugsnag_report_v5 *generated_report = bsg_generate_report_v5();
  memcpy(&env->next_event, generated_report, sizeof(bugsnag_report_v5));
  strcpy(env->next_event_path, SERIALIZE_TEST_FILE);
  bsg_serialize_report_v5_to_file(env, generated_report);

  bugsnag_event *event = bsg_deserialize_event_from_file(SERIALIZE_TEST_FILE);
  ASSERT(event != NULL);

  // values are migrated correctly
  ASSERT_STR_EQ("com.example.PhotoSnapPlus", event->app.id);
  ASSERT_STR_EQ("リリース", event->app.release_stage);
  ASSERT_STR_EQ("2.0.52", event->app.version);
  ASSERT_EQ(57, event->app.version_code);
  ASSERT_STR_EQ("1234-9876-adfe", event->app.build_uuid);
  ASSERT_FALSE(event->app.in_foreground);
  ASSERT_FALSE(event->app.is_launching);
  ASSERT_EQ(0, event->thread_count);

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

void migrate_app_v2(bugsnag_report_v4 *report_v4, bugsnag_event *event);

TEST test_migrate_app_v2(void) {
  bugsnag_report_v4 *report_v4 = calloc(1, sizeof(bugsnag_report_v4));
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
  bsg_app_info_v2 *seed = &report_v4->app;
  bsg_app_info *app = &event->app;

  strcpy(seed->id, "id");
  strcpy(seed->release_stage, "beta");
  strcpy(seed->type, "c");
  strcpy(seed->version, "1.5.2");
  strcpy(seed->active_screen, "ExampleActivity");
  strcpy(seed->build_uuid, "10f9");
  strcpy(seed->binary_arch, "x86");
  seed->version_code = 5;
  seed->duration = 52;
  seed->duration_in_foreground = 25;
  seed->duration_ms_offset = 3;
  seed->duration_in_foreground_ms_offset = 11;

  // migrate pre-populated data
  migrate_app_v2(report_v4, event);

  ASSERT_STR_EQ("id", app->id);
  ASSERT_STR_EQ("beta", app->release_stage);
  ASSERT_STR_EQ("c", app->type);
  ASSERT_STR_EQ("1.5.2", app->version);
  ASSERT_STR_EQ("ExampleActivity", app->active_screen);
  ASSERT_STR_EQ("10f9", app->build_uuid);
  ASSERT_STR_EQ("x86", app->binary_arch);
  ASSERT_FALSE(app->is_launching);

  free(report_v4);
  free(event);
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
}

SUITE(suite_struct_migration) {
  RUN_TEST(test_report_v1_migration);
  RUN_TEST(test_report_v2_migration);
  RUN_TEST(test_report_v3_migration);
  RUN_TEST(test_report_v4_migration);
  RUN_TEST(test_report_v5_migration);
  RUN_TEST(test_report_v5_migration_crumb_index);
  RUN_TEST(test_report_v6_migration);
  RUN_TEST(test_migrate_app_v2);
}
