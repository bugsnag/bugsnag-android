#include <utils/string.h>
#include "test_serializer.h"

enum greatest_test_res validate_serialized_json(const test_case *test_case,
                                                JSON_Value *event_val) {
    // convert to string
    char *serialized_string = json_serialize_to_string(event_val);
    json_value_free(event_val);

    // validate structure
    char *expected = test_case->expected_json;
    ASSERT_STR_EQ(expected, serialized_string);
    PASS();
}

void loadUserTestCase(bugsnag_event *event) {
    bugsnag_user *user = &event->user;
    strcpy(user->name, "Fenton");
    strcpy(user->email, "fenton@io.example.com");
    strcpy(user->id, "1234");
}

void loadAppTestCase(bugsnag_event *event) {
    bsg_app_info *app = &event->app;
    strcpy(app->id, "com.bugsnag.example");
    strcpy(app->release_stage, "prod");
    strcpy(app->type, "android");
    strcpy(app->version, "22");
    strcpy(app->active_screen, "MainActivity");
    app->version_code = 55;
    strcpy(app->build_uuid, "1234-uuid");
    app->duration = 6502;
    app->duration_in_foreground = 6502;
    app->duration_ms_offset = 0;
    app->duration_in_foreground_ms_offset = 0;
    app->in_foreground = true;
    app->is_launching = true;
    strcpy(app->binary_arch, "x86");
}

void loadAppMetadataTestCase(bugsnag_event *event) {
  loadAppTestCase(event);
}

void loadDeviceTestCase(bugsnag_event *event) {
    bsg_device_info *device = &event->device;
    device->api_level = 29;
    bsg_strncpy_safe(device->cpu_abi[0].value, "x86", sizeof(device->cpu_abi[0].value));
    device->cpu_abi_count = 1;
    strcpy(device->orientation, "portrait");

    struct tm time = { 0, 0, 0, 1, 12, 128 };
    device->time = mktime(&time);
    strcpy(device->id, "f5gh7");
    device->jailbroken = true;
    strcpy(device->locale, "En");
    strcpy(device->manufacturer, "Samsung");
    strcpy(device->model, "S7");
    strcpy(device->os_build, "BullDog 5.2");
    strcpy(device->os_version, "8.1");
    strcpy(device->os_name, "android");
    device->total_memory = 512340922;
}

void loadCustomMetadataTestCase(bugsnag_event *event) {
    bugsnag_metadata *data = &event->metadata;
    data->value_count = 4;

    data->values[0].type = BSG_METADATA_CHAR_VALUE;
    strcpy(data->values[0].section, "custom");
    strcpy(data->values[0].name, "str");
    strcpy(data->values[0].char_value, "Foo");

    data->values[1].type = BSG_METADATA_BOOL_VALUE;
    strcpy(data->values[1].section, "custom");
    strcpy(data->values[1].name, "bool");
    data->values[1].bool_value = true;

    data->values[2].type = BSG_METADATA_NUMBER_VALUE;
    strcpy(data->values[2].section, "custom");
    strcpy(data->values[2].name, "num");
    data->values[2].double_value = 55;

    data->values[3].type = BSG_METADATA_NONE_VALUE;
    strcpy(data->values[3].section, "custom");
    strcpy(data->values[3].name, "none");
}

void loadContextTestCase(bugsnag_event *event) {
    strcpy(event->context, "CustomContext");
    strcpy(event->app.active_screen, "ExampleActivity");
}

void loadSeverityReasonTestCase(bugsnag_event *data) {
    data->unhandled = true;
    data->severity = BSG_SEVERITY_ERR;
    strcpy(data->error.errorClass, "SIGABRT");
}

void loadSessionTestCase(bugsnag_event *data) {
    strcpy(data->session_id, "123");
    strcpy(data->session_start, "2018-10-08T12:07:09Z");
    data->handled_events = 2;
    data->unhandled_events = 1;
}

void loadBreadcrumbsTestCase(bugsnag_event *event) {
    bugsnag_breadcrumb *crumb = calloc(1, sizeof(bugsnag_breadcrumb));
    memset(crumb, 0, sizeof(bugsnag_breadcrumb));
    event->crumb_count = 4;
    event->crumb_first_index = BUGSNAG_CRUMBS_MAX - 2;

    // ensure that serialization loop is covered by test

    // first breadcrumb
    crumb = &event->breadcrumbs[BUGSNAG_CRUMBS_MAX - 2];
    crumb->type = BSG_CRUMB_USER;
    strcpy(crumb->name, "Jane");
    strcpy(crumb->timestamp, "2018-10-08T12:07:09Z");

    // metadata
    bugsnag_metadata *data = &crumb->metadata;
    data->value_count = 1;
    data->values[0].type = BSG_METADATA_CHAR_VALUE;
    strcpy(data->values[0].section, "custom");
    strcpy(data->values[0].name, "str");
    strcpy(data->values[0].char_value, "Foo");

    // second breadcrumb
    crumb = &event->breadcrumbs[BUGSNAG_CRUMBS_MAX - 1];
    crumb->type = BSG_CRUMB_MANUAL;
    strcpy(crumb->name, "Something went wrong");
    strcpy(crumb->timestamp, "2018-10-08T12:07:11Z");

    // metadata
    data = &crumb->metadata;
    data->value_count = 1;
    data->values[0].type = BSG_METADATA_BOOL_VALUE;
    strcpy(data->values[0].section, "custom");
    strcpy(data->values[0].name, "bool");
    data->values[0].bool_value = true;

    // third breadcrumb
    crumb = &event->breadcrumbs[0];
    crumb->type = BSG_CRUMB_NAVIGATION;
    strcpy(crumb->name, "MainActivity");
    strcpy(crumb->timestamp, "2018-10-08T12:07:15Z");

    // metadata
    data = &crumb->metadata;
    data->values[0].type = BSG_METADATA_NUMBER_VALUE;
    strcpy(data->values[0].section, "custom");
    strcpy(data->values[0].name, "num");
    data->values[0].double_value = 55;

    // fourth breadcrumb
    crumb = &event->breadcrumbs[1];
    crumb->type = BSG_CRUMB_STATE;
    strcpy(crumb->name, "Updated store");
    strcpy(crumb->timestamp, "2018-10-08T12:07:16Z");

    // metadata
    data = &crumb->metadata;
    data->values[0].type = BSG_METADATA_NONE_VALUE;
    strcpy(data->values[0].section, "custom");
    strcpy(data->values[0].name, "none");
}

bugsnag_stackframe *loadStackframeTestCase() {
    bugsnag_stackframe *data = calloc(1, sizeof(bugsnag_stackframe));
    data->frame_address = 0x20000000;
    data->symbol_address = 0x16000000;
    data->load_address = 0x12000000;
    data->line_number= 52;
    strcpy(data->filename, "foo.c");
    strcpy(data->method, "bar()");
    return data;
}

void loadThreadTestCase(bugsnag_event *event) {
    event->thread_count = 1;
    bsg_thread *thread = &event->threads[0];
    strcpy(thread->name, "Binder 1");
    strcpy(thread->state, "Running");
    thread->id = 1234;
}

void loadExceptionTestCase(bugsnag_event *event) {
    bsg_error *data = &event->error;
    strcpy(data->errorClass, "signal");
    strcpy(data->errorMessage, "whoops something went wrong");
    strcpy(data->type, "c");
    data->frame_count = 1;
    data->stacktrace[0].frame_address = 0x20000000;
    data->stacktrace[0].symbol_address = 0x16000000;
    data->stacktrace[0].load_address = 0x12000000;
    data->stacktrace[0].line_number= 52;
    strcpy(data->stacktrace[0].filename, "foo.c");
    strcpy(data->stacktrace[0].method, "bar()");
}
