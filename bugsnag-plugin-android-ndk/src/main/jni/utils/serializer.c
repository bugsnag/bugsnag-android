#include "serializer.h"
#include "serializer/event_writer.h"

bool bsg_serialize_last_run_info_to_file(bsg_environment *env) {
  return bsg_lastrun_write(env);
}

bool bsg_serialize_event_to_file(bsg_environment *env) {
  return bsg_event_write(env);
}
