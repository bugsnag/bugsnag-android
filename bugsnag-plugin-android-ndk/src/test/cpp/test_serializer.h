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
bsg_user * loadUserTestCase(jint num);
bsg_app_info * loadAppTestCase(jint num);
bsg_app_info * loadAppMetaDataTestCase(jint num);
bsg_device_info * loadDeviceTestCase(jint num);
bsg_device_info * loadDeviceMetaDataTestCase(jint num);
bugsnag_metadata * loadCustomMetaDataTestCase(jint num);
bugsnag_report * loadContextTestCase(jint num);
bugsnag_report * loadHandledStateTestCase(jint num);
bugsnag_report * loadSessionTestCase(jint num);
bugsnag_report * loadBreadcrumbsTestCase(jint num);
bsg_stackframe * loadStackframeTestCase(jint num);
