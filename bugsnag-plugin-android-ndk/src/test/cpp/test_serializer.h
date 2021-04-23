#include <greatest/greatest.h>
#include <jni.h>

#include <utils/serializer.h>
#include <stdlib.h>
#include <utils/migrate.h>
#include <parson/parson.h>
#include "event.h"

typedef struct {
    void *data_ptr;
    char *expected_json;
} test_case;

enum greatest_test_res validate_serialized_json(const test_case *test_case,
                                                JSON_Value *event_val);
bugsnag_user * loadUserTestCase(jint num);
bsg_app_info * loadAppTestCase(jint num);
bsg_app_info * loadAppMetadataTestCase(jint num);
bsg_device_info * loadDeviceTestCase(jint num);
bugsnag_metadata * loadCustomMetadataTestCase(jint num);
bugsnag_event * loadContextTestCase(jint num);
bugsnag_event * loadSeverityReasonTestCase(jint num);
bugsnag_event * loadSessionTestCase(jint num);
bugsnag_event * loadBreadcrumbsTestCase(jint num);
bugsnag_stackframe * loadStackframeTestCase(jint num);
bsg_error * loadExceptionTestCase(jint num);

// Exposed internals from serializer.c
void bsg_serialize_context(const bugsnag_event *event, JSON_Object *event_obj);
void bsg_serialize_severity_reason(const bugsnag_event *event,
                                   JSON_Object *event_obj);
void bsg_serialize_app(const bsg_app_info app, JSON_Object *event_obj);
void bsg_serialize_app_metadata(const bsg_app_info app, JSON_Object *event_obj);
void bsg_serialize_device(const bsg_device_info device, JSON_Object *event_obj);
void bsg_serialize_device_metadata(const bsg_device_info device,
                                   JSON_Object *event_obj);
void bsg_serialize_custom_metadata(const bugsnag_metadata metadata,
                                   JSON_Object *event_obj);
void bsg_serialize_user(const bugsnag_user user, JSON_Object *event_obj);
void bsg_serialize_session(bugsnag_event *event, JSON_Object *event_obj);
void bsg_serialize_stackframe(bugsnag_stackframe *stackframe,
                              JSON_Array *stacktrace);
void bsg_serialize_error(bsg_error exc, JSON_Object *exception,
                         JSON_Array *stacktrace);
void bsg_serialize_breadcrumbs(const bugsnag_event *event, JSON_Array *crumbs);
