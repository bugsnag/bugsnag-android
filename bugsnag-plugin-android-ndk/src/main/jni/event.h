#ifndef BUGSNAG_EVENT_H
#define BUGSNAG_EVENT_H

#include "../assets/include/event.h"

#ifdef __cplusplus
extern "C" {
#endif

/*********************************
 * (start) NDK-SPECIFIC BITS
 *********************************/

void bugsnag_event_add_metadata_double(bugsnag_event *event, char *section,
                                       char *name, double value);
void bugsnag_event_add_metadata_string(bugsnag_event *event, char *section,
                                       char *name, char *value);
void bugsnag_event_add_metadata_bool(bugsnag_event *event, char *section,
                                     char *name, bool value);
void bugsnag_event_add_breadcrumb(bugsnag_event *event,
                                  bugsnag_breadcrumb *crumb);
void bugsnag_event_clear_breadcrumbs(bugsnag_event *event);
void bugsnag_event_remove_metadata(bugsnag_event *event, char *section,
                                   char *name);
void bugsnag_event_remove_metadata_tab(bugsnag_event *event, char *section);
void bugsnag_event_set_context(bugsnag_event *event, char *value);
void bugsnag_event_set_orientation(bugsnag_event *event, int value);
void bugsnag_event_set_app_version(bugsnag_event *event, char *value);
void bugsnag_event_set_build_uuid(bugsnag_event *event, char *value);
void bugsnag_event_set_release_stage(bugsnag_event *event, char *value);
void bugsnag_event_set_user_email(bugsnag_event *event, char *value);
void bugsnag_event_set_user_id(bugsnag_event *event, char *value);
void bugsnag_event_set_user_name(bugsnag_event *event, char *value);
void bugsnag_event_start_session(bugsnag_event *event, char *session_id,
                                 char *started_at, int handled_count, int unhandled_count);
bool bugsnag_event_has_session(bugsnag_event *event);

#ifdef __cplusplus
}
#endif
#endif
