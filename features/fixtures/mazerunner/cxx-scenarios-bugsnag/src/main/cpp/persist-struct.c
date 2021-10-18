#include <bugsnag.h>
#include <jni.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <string.h>
#include "event.h"

bool bsg_serialize_event_to_file(bsg_environment *env);

void populate_event(bugsnag_event *event) {
    strcpy(event->api_key, "5d1e5fbd39a74caa1200142706a90b20");
    strcpy(event->context, "SomeActivity");
    strcpy(event->grouping_hash, "foo-hash");
    event->unhandled = true;

    // error
    strcpy(event->error.errorClass, "SIGBUS");
    strcpy(event->error.errorMessage, "POSIX is serious about oncoming traffic");
    event->error.stacktrace[0].frame_address = 454379;
    event->error.stacktrace[1].frame_address = 342334;
    event->error.frame_count = 2;
    strcpy(event->error.type, "c");
    strcpy(event->error.stacktrace[0].method, "makinBacon");

    // app
    strcpy(event->app.id, "com.example.PhotoSnapPlus");
    strcpy(event->app.type, "android");
    strcpy(event->app.active_screen, "ExampleActivity");
    strcpy(event->app.binary_arch, "x86");
    strcpy(event->app.release_stage, "リリース");
    strcpy(event->app.version, "2.0.52");
    strcpy(event->app.build_uuid, "1234-9876-adfe");
    event->app.version_code = 57;
    event->app.duration = 6502;
    event->app.duration_in_foreground = 3822;
    event->app.duration_ms_offset = 20;
    event->app.duration_in_foreground_ms_offset = 30;
    event->app.in_foreground = true;
    event->app.is_launching = true;

    // device
    strcpy(event->device.manufacturer, "HI-TEC™");
    strcpy(event->device.model, "Rasseur");
    strcpy(event->device.locale, "en_AU#Melbun");
    strcpy(event->device.id, "device-id-123");
    strcpy(event->device.orientation, "landscape");
    strcpy(event->device.os_name, "android");
    strcpy(event->device.os_build, "custom_build");
    strcpy(event->device.os_version, "11.50.2");
    event->device.total_memory = 234678100;
    event->device.api_level = 27;
    event->device.jailbroken = true;
    event->device.time = 1509109234;
    event->device.cpu_abi_count = 1;
    strcpy(event->device.cpu_abi[0].value, "x86");

    // user
    strcpy(event->user.id, "fex");
    strcpy(event->user.email, "fenton@io.example.com");
    strcpy(event->user.name, "Fenton");

    // metadata
    bugsnag_event_add_metadata_bool(event, "metrics", "experimentX", false);
    bugsnag_event_add_metadata_string(event, "metrics", "subject", "percy");
    bugsnag_event_add_metadata_string(event, "app", "weather", "rain");
    bugsnag_event_add_metadata_double(event, "metrics", "counter", 47.8);

    // session
    event->handled_events = 2;
    event->unhandled_events = 1;
    strcpy(event->session_id, "f1ab");
    strcpy(event->session_start, "2019-03-19T12:58:19+00:00");

    // breadcrumbs
    event->crumb_count = 0;
    event->crumb_first_index = 0;
    bugsnag_breadcrumb *crumb = calloc(1, sizeof(bugsnag_breadcrumb));
    crumb->type = BSG_CRUMB_STATE;
    strcpy(crumb->name, "decrease torque");
    strcpy(crumb->timestamp, "2018-08-29T21:41:39Z");
    bsg_cache_add_metadata_value_string(&crumb->metadata, "metaData", "message", "Moving laterally 26º");
    bsg_cache_add_breadcrumb(event, crumb);
}

/**
 * Persists a struct to disk by using forward declarations of Bugsnag's internal NDK code.
 */
void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXPersistLegacyStructScenario_persistStruct(
        JNIEnv *env,
        jobject instance,
        jstring path) {
    bsg_environment *bsg = calloc(1, sizeof(bsg_environment));
    bsg->report_header.big_endian = htonl(47) == 47;
    bsg->report_header.version = BUGSNAG_EVENT_VERSION;

    // set event path on environment
    const char *event_path = (*env)->GetStringUTFChars(env, path, NULL);
    sprintf(bsg->next_event_path, "%s", event_path);

    // populate event then persist it
    populate_event(&bsg->next_event);
    bsg_serialize_event_to_file(bsg);
}
