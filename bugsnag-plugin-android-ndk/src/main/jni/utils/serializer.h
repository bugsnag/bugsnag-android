#include "../bugsnag_ndk.h"

#ifdef __cplusplus
extern "C" {
#endif

bugsnag_event *bsg_deserialize_event_from_file(char *filepath);

char *bsg_serialize_event_to_json_string(bugsnag_event *event);

bool bsg_serialize_event_to_file(bsg_environment *env) __asyncsafe;

bool bsg_serialize_last_run_info_to_file(bsg_environment *env) __asyncsafe;

#ifdef __cplusplus
}
#endif
