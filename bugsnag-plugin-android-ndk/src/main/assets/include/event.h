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

typedef struct {
    char name[64];
    char email[64];
    char id[64];
} bsg_user_t;

typedef enum {
    BSG_NONE_VALUE,
    BSG_BOOL_VALUE,
    BSG_CHAR_VALUE,
    BSG_NUMBER_VALUE,
} bsg_metadata_t;

typedef struct {
    uintptr_t frame_address;
    uintptr_t symbol_address;
    uintptr_t load_address;
    uintptr_t line_number;

    char filename[256];
    char method[256];
} bsg_stackframe_t;

#ifdef __cplusplus
extern "C" {
#endif

bsg_user_t bugsnag_event_get_user(void *event_ptr);

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


/* Accessors for event.device */


bool bugsnag_device_get_jailbroken(void *event_ptr);
void bugsnag_device_set_jailbroken(void *event_ptr, bool value);

char *bugsnag_device_get_id(void *event_ptr);
void bugsnag_device_set_id(void *event_ptr, char *value);

char *bugsnag_device_get_locale(void *event_ptr);
void bugsnag_device_set_locale(void *event_ptr, char *value);

char *bugsnag_device_get_manufacturer(void *event_ptr);
void bugsnag_device_set_manufacturer(void *event_ptr, char *value);

char *bugsnag_device_get_model(void *event_ptr);
void bugsnag_app_set_model(void *event_ptr, char *value);

char *bugsnag_device_get_os_version(void *event_ptr);
void bugsnag_device_set_os_version(void *event_ptr, char *value);

long bugsnag_device_get_total_memory(void *event_ptr);
void bugsnag_device_set_total_memory(void *event_ptr, long value);

char *bugsnag_device_get_orientation(void *event_ptr);
void bugsnag_device_set_orientation(void *event_ptr, char *value);

time_t bugsnag_device_get_time(void *event_ptr);
void bugsnag_device_set_time(void *event_ptr, time_t value);

char *bugsnag_device_get_os_name(void *event_ptr);
void bugsnag_device_set_os_name(void *event_ptr, char *value);

char *bugsnag_error_get_error_class(void *event_ptr);
void bugsnag_error_set_error_class(void *event_ptr, char *value);

char *bugsnag_error_get_error_message(void *event_ptr);
void bugsnag_error_set_error_message(void *event_ptr, char *value);

char *bugsnag_error_get_error_type(void *event_ptr);
void bugsnag_error_set_error_type(void *event_ptr, char *value);

void bugsnag_event_set_user(void *event_ptr, char* id, char* email, char* name);

bsg_severity_t bugsnag_event_get_severity(void *event_ptr);
void bugsnag_event_set_severity(void *event_ptr, bsg_severity_t value);

bool bugsnag_event_is_unhandled(void *event_ptr);

char *bugsnag_event_get_grouping_hash(void *event_ptr);
void bugsnag_event_set_grouping_hash(void *event_ptr, char *value);

/* Metadata */

void bugsnag_event_add_metadata_double(void *event_ptr, char *section, char *name, double value);
void bugsnag_event_add_metadata_string(void *event_ptr, char *section, char *name, char *value);
void bugsnag_event_add_metadata_bool(void *event_ptr, char *section, char *name, bool value);

void bugsnag_event_clear_metadata_section(void *event_ptr, char *section);
void bugsnag_event_clear_metadata(void *event_ptr, char *section, char *name);

bsg_metadata_t bugsnag_event_has_metadata(void *event_ptr, char *section, char *name);
double bugsnag_event_get_metadata_double(void *event_ptr, char *section, char *name);
char *bugsnag_event_get_metadata_string(void *event_ptr, char *section, char *name);
bool bugsnag_event_get_metadata_bool(void *event_ptr, char *section, char *name);

/* Stacktrace */

int bugsnag_event_get_stacktrace_size(void *event_ptr);
bsg_stackframe_t *bugsnag_event_get_stackframe(void *event_ptr, int index);

#ifdef __cplusplus
}
#endif

#endif
