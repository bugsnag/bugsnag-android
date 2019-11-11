#ifndef BUGSNAG_REPORT_H
#define BUGSNAG_REPORT_H

#include "../assets/include/report.h"

#ifdef __cplusplus
extern "C" {
#endif

/*********************************
 * (start) NDK-SPECIFIC BITS
 *********************************/

void bugsnag_report_add_metadata_double(bugsnag_report *report, char *section,
                                        char *name, double value);
void bugsnag_report_add_metadata_string(bugsnag_report *report, char *section,
                                        char *name, char *value);
void bugsnag_report_add_metadata_bool(bugsnag_report *report, char *section,
                                      char *name, bool value);
void bugsnag_report_add_breadcrumb(bugsnag_report *report,
                                   bugsnag_breadcrumb *crumb);
void bugsnag_report_clear_breadcrumbs(bugsnag_report *report);
void bugsnag_report_remove_metadata(bugsnag_report *report, char *section,
                                    char *name);
void bugsnag_report_remove_metadata_tab(bugsnag_report *report, char *section);
void bugsnag_report_set_context(bugsnag_report *report, char *value);
void bugsnag_report_set_orientation(bugsnag_report *report, int value);
void bugsnag_report_set_app_version(bugsnag_report *report, char *value);
void bugsnag_report_set_build_uuid(bugsnag_report *report, char *value);
void bugsnag_report_set_release_stage(bugsnag_report *report, char *value);
void bugsnag_report_set_user_email(bugsnag_report *report, char *value);
void bugsnag_report_set_user_id(bugsnag_report *report, char *value);
void bugsnag_report_set_user_name(bugsnag_report *report, char *value);
void bugsnag_report_start_session(bugsnag_report *report, char *session_id,
                                  char *started_at, int handled_count, int unhandled_count);
bool bugsnag_report_has_session(bugsnag_report *report);

#ifdef __cplusplus
}
#endif
#endif
