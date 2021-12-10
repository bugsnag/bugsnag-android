#include "serializer.h"
#include "serializer/event_reader.h"
#include "serializer/event_writer.h"
#include "serializer/json_writer.h"

bool bsg_serialize_last_run_info_to_file(bsg_environment *env) {
  return bsg_lastrun_write(env);
}

bool bsg_serialize_event_to_file(bsg_environment *env) {
  return bsg_event_write(env);
}

bugsnag_event *bsg_deserialize_event_from_file(char *filepath) {
  return bsg_read_event(filepath);
}

char *bsg_serialize_event_to_json_string(bugsnag_event *event) {
  return bsg_event_to_json(event);
}
