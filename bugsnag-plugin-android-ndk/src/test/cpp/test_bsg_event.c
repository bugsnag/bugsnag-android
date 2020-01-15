#include <greatest/greatest.h>
#include <event.h>
#include <utils/string.h>
#include <event.h>

bugsnag_event *init_event() {
    bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
    bsg_strncpy_safe(event->context, "Foo", sizeof(event->context));

    bsg_strncpy_safe(event->app.binaryArch, "x86", sizeof(event->app.binaryArch));
    bsg_strncpy_safe(event->app.build_uuid, "123", sizeof(event->app.build_uuid));
    bsg_strncpy_safe(event->app.id, "fa02", sizeof(event->app.id));
    bsg_strncpy_safe(event->app.release_stage, "dev", sizeof(event->app.release_stage));
    bsg_strncpy_safe(event->app.type, "C", sizeof(event->app.type));
    bsg_strncpy_safe(event->app.version, "1.0", sizeof(event->app.version));
    event->app.version_code = 55;
    event->app.duration = 9019;
    event->app.duration_in_foreground = 7017;
    event->app.in_foreground = true;
    return event;
}

TEST test_event_context(void) {
    bugsnag_event *event = init_event();
    ASSERT_STR_EQ("Foo", bugsnag_event_get_context(event));
    bugsnag_event_set_context(event, "SomeContext");
    ASSERT_STR_EQ("SomeContext", bugsnag_event_get_context(event));
    free(event);
    PASS();
}

TEST test_event_binary_arch(void) {
    bugsnag_event *event = init_event();
    ASSERT_STR_EQ("x86", bugsnag_app_get_binary_arch(event));
    bugsnag_app_set_binary_arch(event, "armeabi-v7a");
    ASSERT_STR_EQ("armeabi-v7a", bugsnag_app_get_binary_arch(event));
    free(event);
    PASS();
}

TEST test_event_build_uuid(void) {
    bugsnag_event *event = init_event();
    ASSERT_STR_EQ("123", bugsnag_app_get_build_uuid(event));
    bugsnag_app_set_build_uuid(event, "my-id-123");
    ASSERT_STR_EQ("my-id-123", bugsnag_app_get_build_uuid(event));
    free(event);
    PASS();
}

TEST test_event_id(void) {
    bugsnag_event *event = init_event();
    ASSERT_STR_EQ("fa02", bugsnag_app_get_id(event));
    bugsnag_app_set_id(event, "my-id-123");
    ASSERT_STR_EQ("my-id-123", bugsnag_app_get_id(event));
    free(event);
    PASS();
}

TEST test_event_release_stage(void) {
    bugsnag_event *event = init_event();
    ASSERT_STR_EQ("dev", bugsnag_app_get_release_stage(event));
    bugsnag_app_set_release_stage(event, "beta");
    ASSERT_STR_EQ("beta", bugsnag_app_get_release_stage(event));
    free(event);
    PASS();
}

TEST test_event_type(void) {
    bugsnag_event *event = init_event();
    ASSERT_STR_EQ("C", bugsnag_app_get_type(event));
    bugsnag_app_set_type(event, "C++");
    ASSERT_STR_EQ("C++", bugsnag_app_get_type(event));
    free(event);
    PASS();
}

TEST test_event_version(void) {
    bugsnag_event *event = init_event();
    ASSERT_STR_EQ("1.0", bugsnag_app_get_version(event));
    bugsnag_app_set_version(event, "2.2");
    ASSERT_STR_EQ("2.2", bugsnag_app_get_version(event));
    free(event);
    PASS();
}

TEST test_event_version_code(void) {
    bugsnag_event *event = init_event();
    ASSERT_EQ(55, bugsnag_app_get_version_code(event));
    bugsnag_app_set_version_code(event, 99);
    ASSERT_EQ(99, bugsnag_app_get_version_code(event));
    free(event);
    PASS();
}

TEST test_event_duration(void) {
    bugsnag_event *event = init_event();
    ASSERT_EQ(9019, bugsnag_app_get_duration(event));
    bugsnag_app_set_duration(event, 552);
    ASSERT_EQ(552, bugsnag_app_get_duration(event));
    free(event);
    PASS();
}

TEST test_event_duration_in_foreground(void) {
    bugsnag_event *event = init_event();
    ASSERT_EQ(7017, bugsnag_app_get_duration_in_foreground(event));
    bugsnag_app_set_duration_in_foreground(event, 209);
    ASSERT_EQ(209, bugsnag_app_get_duration_in_foreground(event));
    free(event);
    PASS();
}

TEST test_event_in_foreground(void) {
    bugsnag_event *event = init_event();
    ASSERT(bugsnag_app_get_in_foreground(event));
    bugsnag_app_set_in_foreground(event, false);
    ASSERT_FALSE(bugsnag_app_get_in_foreground(event));
    free(event);
    PASS();
}


SUITE(event_mutators) {
    RUN_TEST(test_event_context);
    RUN_TEST(test_event_binary_arch);
    RUN_TEST(test_event_build_uuid);
    RUN_TEST(test_event_id);
    RUN_TEST(test_event_release_stage);
    RUN_TEST(test_event_type);
    RUN_TEST(test_event_version);
    RUN_TEST(test_event_version_code);
    RUN_TEST(test_event_duration);
    RUN_TEST(test_event_duration_in_foreground);
    RUN_TEST(test_event_in_foreground);
}
