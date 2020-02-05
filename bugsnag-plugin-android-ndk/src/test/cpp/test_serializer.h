#include <greatest/greatest.h>
#include <jni.h>

#include <utils/serializer.h>
#include <stdlib.h>
#include <utils/migrate.h>

typedef struct {
    void *data_ptr;
    char *expected_json;
} test_case;

enum greatest_test_res validate_serialized_json(const test_case *test_case,
                                                JSON_Value *event_val);
bsg_user_t * loadUserTestCase(jint num);
bsg_app_info * loadAppTestCase(jint num);
bsg_app_info * loadAppMetadataTestCase(jint num);
bsg_device_info * loadDeviceTestCase(jint num);
bsg_device_info * loadDeviceMetadataTestCase(jint num);
bugsnag_metadata * loadCustomMetadataTestCase(jint num);
bugsnag_event * loadContextTestCase(jint num);
bugsnag_event * loadHandledStateTestCase(jint num);
bugsnag_event * loadSessionTestCase(jint num);
bugsnag_event * loadBreadcrumbsTestCase(jint num);
bsg_stackframe_t * loadStackframeTestCase(jint num);
bsg_error * loadExceptionTestCase(jint num);
