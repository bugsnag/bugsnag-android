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
#ifdef __cplusplus
}
#endif

