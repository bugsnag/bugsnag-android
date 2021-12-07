#include <greatest/greatest.h>
#include <jni.h>

#include <stdlib.h>

#include <parson/parson.h>

#include <utils/serializer.h>

typedef struct {
    void *data_ptr;
    char *expected_json;
} test_case;

enum greatest_test_res validate_serialized_json(const test_case *test_case,
                                                JSON_Value *event_val);
void loadUserTestCase(bugsnag_event *event);
void loadAppTestCase(bugsnag_event *event);
void loadAppMetadataTestCase(bugsnag_event *event);
void loadDeviceTestCase(bugsnag_event *event);
void loadCustomMetadataTestCase(bugsnag_event *event);
void loadContextTestCase(bugsnag_event *event);
void loadSessionTestCase(bugsnag_event *event);
void loadSeverityReasonTestCase(bugsnag_event *event);
void loadBreadcrumbsTestCase(bugsnag_event *event);
void loadThreadTestCase(bugsnag_event *event);
bugsnag_stackframe *loadStackframeTestCase();
void loadExceptionTestCase(bugsnag_event *event);
