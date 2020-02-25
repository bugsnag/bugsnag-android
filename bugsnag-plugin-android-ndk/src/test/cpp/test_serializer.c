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

bugsnag_user * loadUserTestCase(jint num) {
    bugsnag_user *user;

    if (num == 0) {
        user = malloc(sizeof(bugsnag_user));
        strcpy(user->name, "Fenton");
        strcpy(user->email, "fenton@io.example.com");
        strcpy(user->id, "1234");
    } else {
        user = malloc(sizeof(bugsnag_user));
        strcpy(user->name, "Jamie");
        strcpy(user->email, "jamie@bugsnag.com");
        strcpy(user->id, "456");
    }
    return user;
}

bsg_app_info * loadAppTestCase(jint num) {
    bsg_app_info *app = malloc(sizeof(bsg_app_info));
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
    strcpy(app->binary_arch, "x86");
    return app;
}

bsg_app_info * loadAppMetadataTestCase(jint num) {
    bsg_app_info *app = loadAppTestCase(num);
    return app;
}

bsg_device_info * loadDeviceTestCase(jint num) {
    bsg_device_info *device = malloc(sizeof(bsg_device_info));
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
    return device;
}

bugsnag_metadata * loadCustomMetadataTestCase(jint num) {
    bugsnag_metadata *data = malloc(sizeof(bugsnag_metadata));
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
    return data;
}

bugsnag_event * loadContextTestCase(jint num) {
    bugsnag_event *data = malloc(sizeof(bugsnag_event));
    strcpy(data->context, "CustomContext");
    strcpy(data->app.active_screen, "ExampleActivity");
    return data;
}

bugsnag_event * loadHandledStateTestCase(jint num) {
    bugsnag_event *data = malloc(sizeof(bugsnag_event));
    data->unhandled = true;
    data->severity = BSG_SEVERITY_ERR;
    strcpy(data->error.errorClass, "SIGABRT");
    return data;
}

bugsnag_event * loadSessionTestCase(jint num) {
    bugsnag_event *data = malloc(sizeof(bugsnag_event));
    strcpy(data->session_id, "123");
    strcpy(data->session_start, "2018-10-08T12:07:09Z");
    data->handled_events = 2;
    data->unhandled_events = 1;
    return data;
}

bugsnag_event * loadBreadcrumbsTestCase(jint num) {
    bugsnag_event *data = malloc(sizeof(bugsnag_event));
    data->crumb_count = 1;
    data->crumb_first_index = 0;
    data->breadcrumbs[0].type = BSG_CRUMB_USER;
    strcpy(data->breadcrumbs[0].name, "Jane");
    strcpy(data->breadcrumbs[0].timestamp, "2018-10-08T12:07:09Z");
    strcpy(data->breadcrumbs[0].metadata->key, "foo");
    strcpy(data->breadcrumbs[0].metadata->value, "bar");
    return data;
}

bugsnag_stackframe * loadStackframeTestCase(jint num) {
    bugsnag_stackframe *data = malloc(sizeof(bugsnag_stackframe));
    data->frame_address = 0x20000000;
    data->symbol_address = 0x16000000;
    data->load_address = 0x12000000;
    data->line_number= 52;
    strcpy(data->filename, "foo.c");
    strcpy(data->method, "bar()");
    return data;
}

bsg_error * loadExceptionTestCase(jint num) {
    bsg_error *data = malloc(sizeof(bsg_error));
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
    return data;
}
