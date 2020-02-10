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

char *bsg_serialize_event_to_json_string(bugsnag_event *event);

bool bsg_serialize_event_to_file(bsg_environment *env) __asyncsafe;

bugsnag_event *bsg_deserialize_event_from_file(char *filepath);

void bsg_serialize_context(const bugsnag_event *event, JSON_Object *event_obj);
void bsg_serialize_handled_state(const bugsnag_event *event, JSON_Object *event_obj);
void bsg_serialize_app(const bsg_app_info app, JSON_Object *event_obj);
void bsg_serialize_app_metadata(const bsg_app_info app, JSON_Object *event_obj);
void bsg_serialize_device(const bsg_device_info device, JSON_Object *event_obj);
void bsg_serialize_device_metadata(const bsg_device_info device, JSON_Object *event_obj);
void bsg_serialize_custom_metadata(const bugsnag_metadata metadata, JSON_Object *event_obj);
void bsg_serialize_user(const bsg_user_t user, JSON_Object *event_obj);
void bsg_serialize_session(bugsnag_event *event, JSON_Object *event_obj);
void bsg_serialize_stackframe(bsg_stackframe_t *stackframe, JSON_Array *stacktrace);
void bsg_serialize_error(bsg_error exc, JSON_Object *exception, JSON_Array *stacktrace);
void bsg_serialize_breadcrumbs(const bugsnag_event *event, JSON_Array *crumbs);
char *bsg_serialize_event_to_json_string(bugsnag_event *event);

#ifdef __cplusplus
}
#endif
