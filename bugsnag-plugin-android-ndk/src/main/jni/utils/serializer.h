#include "../bugsnag_ndk.h"
#include "build.h"
#include <fcntl.h>
#include <parson/parson.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#ifdef __cplusplus
extern "C" {
#endif

char *bsg_serialize_event_to_json_string(bugsnag_event *event);

bool bsg_serialize_event_to_file(bsg_environment *env) __asyncsafe;

bool bsg_serialize_last_run_info_to_file(bsg_environment *env) __asyncsafe;

bugsnag_event *bsg_deserialize_event_from_file(char *filepath);

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
void bsg_serialize_stackframe(int frame_index, bugsnag_stackframe *stackframe,
                              JSON_Array *stacktrace);
void bsg_serialize_error(bsg_error exc, JSON_Object *exception,
                         JSON_Array *stacktrace);
void bsg_serialize_breadcrumbs(const bugsnag_event *event, JSON_Array *crumbs);
char *bsg_serialize_event_to_json_string(bugsnag_event *event);

int bsg_calculate_total_crumbs(int old_count);
int bsg_calculate_v1_start_index(int old_count);
int bsg_calculate_v1_crumb_index(int crumb_pos, int first_index);

#ifdef __cplusplus
}
#endif
