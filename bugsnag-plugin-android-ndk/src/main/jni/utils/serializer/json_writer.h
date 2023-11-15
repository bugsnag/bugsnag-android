#pragma once
#include <parson/parson.h>

#include "../../event.h"

char *bsg_event_to_json(bugsnag_event *event);

/** Serialization components (exposed for testing) */

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
/**
 * Append a JSON-serialized stackframe to an array
 *
 * @param stackframe the frame to serialize
 * @param is_pc      true if the current frame is the program counter
 * @param stacktrace the destination array
 */
void bsg_serialize_stackframe(bugsnag_stackframe *stackframe, bool is_pc,
                              JSON_Array *stacktrace);
void bsg_serialize_error(bsg_error exc, JSON_Object *exception,
                         JSON_Array *stacktrace);
void bsg_serialize_breadcrumbs(const bugsnag_event *event, JSON_Array *crumbs);
void bsg_serialize_threads(const bugsnag_event *event, JSON_Array *threads);
void bsg_serialize_feature_flags(const bugsnag_event *event,
                                 JSON_Array *feature_flags);
