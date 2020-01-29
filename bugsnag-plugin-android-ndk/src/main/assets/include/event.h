#ifndef BUGSNAG_ANDROID_NDK_EVENT_API_H
#define BUGSNAG_ANDROID_NDK_EVENT_API_H

#include <stdbool.h>
#include <sys/types.h>

typedef enum {
  /** An unhandled exception */
  BSG_SEVERITY_ERR,
  /** A handled exception */
  BSG_SEVERITY_WARN,
  /** Custom, notable error messages */
  BSG_SEVERITY_INFO,
} bsg_severity_t;

typedef enum {
  /**
   * Any breadcrumb recorded via \link bugsnag_leave_breadcrumb
   */
  BSG_CRUMB_MANUAL,
  /**
   * Call to bugsnag_notify*()
   * (Internal use only)
   */
  BSG_CRUMB_ERROR,
  /**
   * Log message
   */
  BSG_CRUMB_LOG,
  /**
   *  Navigation action, such as an overall change in the application view
   *  context
   */
  BSG_CRUMB_NAVIGATION,
  /**
   *  Background process, such performing a database query
   */
  BSG_CRUMB_PROCESS,
  /**
   *  Network request
   */
  BSG_CRUMB_REQUEST,
  /**
   *  Change in application state
   */
  BSG_CRUMB_STATE,
  /**
   *  User event, such as authentication or control events
   */
  BSG_CRUMB_USER,
} bsg_breadcrumb_t;

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Retrieves the event context
 * @param event_ptr a pointer to the event supplied in an on_error callback
 * @return the event context, or NULL if this has not been set
 */
char *bugsnag_event_get_context(void *event_ptr);

/**
 * Sets the event context
 * @param event_ptr a pointer to the event supplied in an on_error callback
 * @param value the new event context value, which can be NULL
 */
void bugsnag_event_set_context(void *event_ptr, char *value);


/* Accessors for event.app */


char *bugsnag_app_get_binary_arch(void *event_ptr);
void bugsnag_app_set_binary_arch(void *event_ptr, char *value);

char *bugsnag_app_get_build_uuid(void *event_ptr);
void bugsnag_app_set_build_uuid(void *event_ptr, char *value);

char *bugsnag_app_get_id(void *event_ptr);
void bugsnag_app_set_id(void *event_ptr, char *value);

char *bugsnag_app_get_release_stage(void *event_ptr);
void bugsnag_app_set_release_stage(void *event_ptr, char *value);

char *bugsnag_app_get_type(void *event_ptr);
void bugsnag_app_set_type(void *event_ptr, char *value);

char *bugsnag_app_get_version(void *event_ptr);
void bugsnag_app_set_version(void *event_ptr, char *value);

int bugsnag_app_get_version_code(void *event_ptr);
void bugsnag_app_set_version_code(void *event_ptr, int value);

time_t bugsnag_app_get_duration(void *event_ptr);
void bugsnag_app_set_duration(void *event_ptr, time_t value);

time_t bugsnag_app_get_duration_in_foreground(void *event_ptr);
void bugsnag_app_set_duration_in_foreground(void *event_ptr, time_t value);

bool bugsnag_app_get_in_foreground(void *event_ptr);
void bugsnag_app_set_in_foreground(void *event_ptr, bool value);

#ifdef __cplusplus
}
#endif

#endif
