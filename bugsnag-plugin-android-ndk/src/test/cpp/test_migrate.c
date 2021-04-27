#include <greatest/greatest.h>
#include <utils/serializer.h>
#include <stdlib.h>
#include <utils/migrate.h>
#include <utils/migrate_internal.h>

#define SERIALIZE_TEST_FILE "/data/data/com.bugsnag.android.ndk.test/cache/foo.crash"

bool bsg_report_header_write(bsg_report_header *header, int fd);

static int bsg_find_next_free_metadata_index_v5(bugsnag_metadata_v5 *const metadata) {
  if (metadata->value_count < BUGSNAG_METADATA_MAX) {
    return metadata->value_count;
  } else {
    for (int i = 0; i < metadata->value_count; i++) {
      if (metadata->values[i].type == BSG_METADATA_NONE_VALUE) {
        return i;
      }
    }
  }
  return -1;
}

static int bsg_allocate_metadata_index_v5(bugsnag_metadata_v5 *metadata, const char *section,
                                          const char *name) {
  int index = bsg_find_next_free_metadata_index_v5(metadata);
  if (index < 0) {
    return index;
  }
  bsg_strncpy_safe(metadata->values[index].section, section,
                   sizeof(metadata->values[index].section));
  bsg_strncpy_safe(metadata->values[index].name, name,
                   sizeof(metadata->values[index].name));
  if (metadata->value_count < BUGSNAG_METADATA_MAX) {
    metadata->value_count = index + 1;
  }
  return index;
}

static void bsg_add_metadata_value_str_v5(bugsnag_metadata_v5 *metadata, const char *section,
                                          const char *name, const char *value) {
  int index = bsg_allocate_metadata_index_v5(metadata, section, name);
  if (index >= 0) {
    metadata->values[index].type = BSG_METADATA_CHAR_VALUE;
    bsg_strncpy_safe(metadata->values[index].char_value, value,
                     sizeof(metadata->values[index].char_value));
  }
}

static bugsnag_breadcrumb_v5 *init_breadcrumb_v5(const char *name, char *message, bugsnag_breadcrumb_type type) {
  bugsnag_breadcrumb_v5 *crumb = calloc(1, sizeof(*crumb));
  crumb->type = type;
  strcpy(crumb->name, name);
  strcpy(crumb->timestamp, "2018-08-29T21:41:39Z");
  bsg_add_metadata_value_str_v5(&crumb->metadata, "metaData", "message", message);
  return crumb;
}

static void bugsnag_event_add_breadcrumb_v5(bugsnag_event_v5 *event,
                                  bugsnag_breadcrumb_v5 *crumb) {
  int crumb_index;
  if (event->crumb_count < BUGSNAG_CRUMBS_MAX) {
    crumb_index = event->crumb_count;
    event->crumb_count++;
  } else {
    crumb_index = event->crumb_first_index;
    event->crumb_first_index =
            (event->crumb_first_index + 1) % BUGSNAG_CRUMBS_MAX;
  }
  memcpy(&event->breadcrumbs[crumb_index], crumb, sizeof(*crumb));
}

static bool bsg_report_v1_write(bsg_report_header *header, bugsnag_report_v1 *report,
                         int fd) {
  if (!bsg_report_header_write(header, fd)) {
    return false;
  }
  ssize_t len = write(fd, report, sizeof(*report));
  return len == sizeof(*report);
}

static bool bsg_report_v2_write(bsg_report_header *header, bugsnag_report_v2 *report,
                         int fd) {
  if (!bsg_report_header_write(header, fd)) {
    return false;
  }
  ssize_t len = write(fd, report, sizeof(*report));
  return len == sizeof(*report);
}

static bool bsg_report_v3_write(bsg_report_header *header, bugsnag_report_v3 *report,
                         int fd) {
    if (!bsg_report_header_write(header, fd)) {
        return false;
    }
    ssize_t len = write(fd, report, sizeof(*report));
    return len == sizeof(*report);
}

static bool bsg_report_v4_write(bsg_report_header *header, bugsnag_report_v4 *report,
                                int fd) {
  if (!bsg_report_header_write(header, fd)) {
    return false;
  }
  ssize_t len = write(fd, report, sizeof(*report));
  return len == sizeof(*report);
}

static bool bsg_report_v5_write(bsg_report_header *header, bugsnag_event_v5 *report,
                                int fd) {
  if (!bsg_report_header_write(header, fd)) {
    return false;
  }
  ssize_t len = write(fd, report, sizeof(*report));
  return len == sizeof(*report);
}

static bool bsg_serialize_report_v1_to_file(bsg_environment *env, bugsnag_report_v1 *report) {
  int fd = open(env->next_event_path, O_WRONLY | O_CREAT, 0644);
  if (fd == -1) {
    return false;
  }
  return bsg_report_v1_write(&env->report_header, report, fd);
}

static bool bsg_serialize_report_v2_to_file(bsg_environment *env, bugsnag_report_v2 *report) {
  int fd = open(env->next_event_path, O_WRONLY | O_CREAT, 0644);
  if (fd == -1) {
    return false;
  }
  return bsg_report_v2_write(&env->report_header, report, fd);
}

static bool bsg_serialize_report_v3_to_file(bsg_environment *env, bugsnag_report_v3 *report) {
    int fd = open(env->next_event_path, O_WRONLY | O_CREAT, 0644);
    if (fd == -1) {
        return false;
    }
    return bsg_report_v3_write(&env->report_header, report, fd);
}

static bool bsg_serialize_report_v4_to_file(bsg_environment *env, bugsnag_report_v4 *report) {
  int fd = open(env->next_event_path, O_WRONLY | O_CREAT, 0644);
  if (fd == -1) {
    return false;
  }
  return bsg_report_v4_write(&env->report_header, report, fd);
}

static bool bsg_serialize_report_v5_to_file(bsg_environment *env, bugsnag_event_v5 *report) {
  int fd = open(env->next_event_path, O_WRONLY | O_CREAT, 0644);
  if (fd == -1) {
    return false;
  }
  return bsg_report_v5_write(&env->report_header, report, fd);
}


static void generate_basic_report_v5(bugsnag_event_v5 *event) {
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
  bugsnag_breadcrumb_v5 *crumb1 = init_breadcrumb_v5("decrease torque", "Moving laterally 26º", BSG_CRUMB_STATE);
  bugsnag_event_add_breadcrumb_v5(event, crumb1);

  bugsnag_breadcrumb_v5 *crumb2 = init_breadcrumb_v5("enable blasters", "this is a drill.", BSG_CRUMB_USER);
  bugsnag_event_add_breadcrumb_v5(event, crumb2);

  event->handled_events = 1;
  event->unhandled_events = 1;
  strcpy(event->session_id, "f1ab");
  strcpy(event->session_start, "2019-03-19T12:58:19+00:00");

  strcpy(event->notifier.version, "1.0");
  strcpy(event->notifier.url, "bugsnag.com");
  strcpy(event->notifier.name, "Test Notifier");
}

static bugsnag_event_v5 *bsg_generate_report_v5(void) {
  bugsnag_event_v5 *report = calloc(1, sizeof(*report));
  generate_basic_report_v5((bugsnag_event_v5 *) report);
  return report;
}

static bugsnag_report_v4 *bsg_generate_report_v4(void) {
  bugsnag_report_v4 *report = calloc(1, sizeof(*report));
  generate_basic_report_v5((bugsnag_event_v5 *) report);
  return report;
}

static bugsnag_report_v3 *bsg_generate_report_v3(void) {
    bugsnag_report_v3 *report = calloc(1, sizeof(*report));
  generate_basic_report_v5((bugsnag_event_v5 *) report);
    return report;
}

static bugsnag_report_v2 *bsg_generate_report_v2(void) {
  bugsnag_report_v2 *report = calloc(1, sizeof(*report));
  generate_basic_report_v5((bugsnag_event_v5 *) report);

  strcpy(report->exception.name, "SIGBUS");
  strcpy(report->exception.message, "POSIX is serious about oncoming traffic");
  report->exception.stacktrace[0].frame_address = 454379;
  report->exception.frame_count = 1;
  strcpy(report->exception.type, "C");
  strcpy(report->exception.stacktrace[0].method, "makinBacon");

  strcpy(report->app.package_name, "com.example.foo");
  strcpy(report->app.version_name, "2.5");

  bugsnag_breadcrumb_v1 *crumb1 = &report->breadcrumbs[0];
  crumb1->type = BSG_CRUMB_STATE;
  strcpy(crumb1->timestamp, "2018-08-29T21:41:39Z");
  strcpy(crumb1->name, "decrease torque");
  strcpy(crumb1->metadata[0].key, "message");
  strcpy(crumb1->metadata[0].value, "Moving laterally 26º");

  bugsnag_breadcrumb_v1 *crumb2 = &report->breadcrumbs[1];
  crumb2->type = BSG_CRUMB_USER;
  strcpy(crumb2->timestamp, "2018-08-29T21:41:39Z");
  strcpy(crumb2->name, "enable blasters");
  strcpy(crumb2->metadata[0].key, "message");
  strcpy(crumb2->metadata[0].value, "this is a drill.");

  report->crumb_first_index = 0;
  report->crumb_count = 2;

  return report;
}

static bugsnag_report_v1 *bsg_generate_report_v1(void) {
  bugsnag_report_v1 *report = calloc(1, sizeof(*report));
  strcpy(report->session_id, "f1ab");
  strcpy(report->session_start, "2019-03-19T12:58:19+00:00");
  report->handled_events = 1;
  return report;
}

static bugsnag_event_v5 *bsg_generate_event_v5(void) {
  bugsnag_event_v5 *report = calloc(1, sizeof(*report));
  generate_basic_report_v5(report);
  report->unhandled_events = 2;
  return report;
}

TEST test_report_v1_migration(void) {
  bsg_environment *env = malloc(sizeof(*env));
  env->report_header.version = 1;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");
  bugsnag_report_v1 *generated_report = bsg_generate_report_v1();
  memcpy(&env->next_event, generated_report, sizeof(*generated_report));
  strcpy(env->next_event_path, SERIALIZE_TEST_FILE);
  bsg_serialize_report_v1_to_file(env, generated_report);

  bugsnag_event *event = bsg_deserialize_event_from_file(SERIALIZE_TEST_FILE);
  ASSERT(event != NULL);
  ASSERT(strcmp("f1ab", event->session_id) == 0);
  ASSERT(strcmp("2019-03-19T12:58:19+00:00", event->session_start) == 0);
  ASSERT_EQ(1, event->handled_events);
  ASSERT_EQ(1, event->unhandled_events);
  ASSERT_FALSE(event->app.is_launching);

  free(generated_report);
  free(env);
  free(event);
  PASS();
}

TEST test_report_v2_migration(void) {
  bsg_environment *env = malloc(sizeof(*env));

  bugsnag_report_v2 *generated_report = bsg_generate_report_v2();
  memcpy(&env->next_event, generated_report, sizeof(*generated_report));
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

  free(generated_report);
  free(env);
  free(event);
  PASS();
}

TEST test_report_v3_migration(void) {
  bsg_environment *env = malloc(sizeof(*env));
  env->report_header.version = 3;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");

  bugsnag_report_v3 *generated_report = bsg_generate_report_v3();
  memcpy(&env->next_event, generated_report, sizeof(*generated_report));
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

  free(generated_report);
  free(env);
  free(event);
  PASS();
}

TEST test_report_v4_migration(void) {
  bsg_environment *env = malloc(sizeof(*env));
  env->report_header.version = 4;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");

  bugsnag_report_v4 *generated_report = bsg_generate_report_v4();
  memcpy(&env->next_event, generated_report, sizeof(*generated_report));
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

  free(generated_report);
  free(env);
  free(event);
  PASS();
}

TEST test_report_v5_migration(void) {
  bsg_environment *env = malloc(sizeof(*env));
  env->report_header.version = 5;
  env->report_header.big_endian = 1;
  strcpy(env->report_header.os_build, "macOS Sierra");

  bugsnag_event_v5 *generated_report = bsg_generate_report_v5();
  memcpy(&env->next_event, generated_report, sizeof(*generated_report));
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

  free(generated_report);
  free(env);
  free(event);
  PASS();
}

TEST test_migrate_app_v2(void) {
  bugsnag_report_v4 *report_v4 = malloc(sizeof(*report_v4));
  bugsnag_event_v5 *event = malloc(sizeof(*event));
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
  bsg_migrate_app_v2(report_v4, event);

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

SUITE(serialize_migrate) {
  RUN_TEST(test_report_v1_migration);
  RUN_TEST(test_report_v2_migration);
  RUN_TEST(test_report_v3_migration);
  RUN_TEST(test_report_v4_migration);
  RUN_TEST(test_report_v5_migration);
  RUN_TEST(test_migrate_app_v2);
}
