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

bsg_user * loadUserTestCase(jint num) {
    bsg_user *user;

    if (num == 0) {
        user = malloc(sizeof(bsg_user));
        strcpy(user->id, "1234");
        strcpy(user->email, "fenton@io.example.com");
        strcpy(user->name, "Fenton");
    } else {
        user = malloc(sizeof(bsg_user));
        strcpy(user->id, "456");
        strcpy(user->email, "jamie@bugsnag.com");
        strcpy(user->name, "Jamie");
    }
    return user;
}

bsg_app_info * loadAppTestCase(jint num) {
    bsg_app_info *app = malloc(sizeof(bsg_app_info));
    strcpy(app->version, "22");
    strcpy(app->id, "com.bugsnag.example");
    strcpy(app->type, "android");
    strcpy(app->release_stage, "prod");
    app->version_code = 55;
    strcpy(app->build_uuid, "1234-uuid");
    strcpy(app->binaryArch, "x86");
    app->duration = 6502;
    app->duration_in_foreground = 6502;
    app->in_foreground = true;
    return app;
}

bsg_app_info * loadAppMetaDataTestCase(jint num) {
    bsg_app_info *app = malloc(sizeof(bsg_app_info));
    strcpy(app->package_name, "com.bugsnag.example");
    strcpy(app->version_name, "5.0");
    strcpy(app->active_screen, "MainActivity");
    strcpy(app->name, "PhotoSnap");
    app->low_memory = true;
    return app;
}
