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