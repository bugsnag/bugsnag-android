#include <stdlib.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <parson/parson.h>
#include "../bugsnag_ndk.h"
#include "build.h"

#ifdef __cplusplus
extern "C" {
#endif

char *bsg_serialize_report_to_json_string(bugsnag_report *report);

bool bsg_serialize_report_to_file(bsg_environment *env) __asyncsafe;

bugsnag_report *bsg_deserialize_report_from_file(char *filepath);

void bsg_serialize_context(const bugsnag_report *report, JSON_Object *event);
void bsg_serialize_handled_state(const bugsnag_report *report, JSON_Object *event);
void bsg_serialize_app(const bsg_app_info app, JSON_Object *event);
void bsg_serialize_app_metadata(const bsg_app_info app, JSON_Object *event);
void bsg_serialize_device(const bsg_device_info device, JSON_Object *event);
void bsg_serialize_device_metadata(const bsg_device_info device, JSON_Object *event);
void bsg_serialize_custom_metadata(const bugsnag_metadata metadata, JSON_Object *event);
void bsg_serialize_user(const bsg_user user, JSON_Object *event);
void bsg_serialize_session(bugsnag_report *report, JSON_Object *event);
void bsg_serialize_stackframe(JSON_Array *stacktrace, bsg_stackframe *stackframe);
void bsg_serialize_exception(JSON_Object *exception, JSON_Array *stacktrace, const bsg_exception exc);
void bsg_serialize_breadcrumbs(const bugsnag_report *report, JSON_Array *crumbs);
char *bsg_serialize_report_to_json_string(bugsnag_report *report);




#ifdef __cplusplus
}
#endif

