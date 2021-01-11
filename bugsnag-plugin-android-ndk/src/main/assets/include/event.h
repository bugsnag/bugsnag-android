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
} bugsnag_severity;

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
} bugsnag_breadcrumb_type;

typedef struct {
  char name[64];
  char email[64];
  char id[64];
} bugsnag_user;

typedef enum {
  BSG_METADATA_NONE_VALUE,
  BSG_METADATA_BOOL_VALUE,
  BSG_METADATA_CHAR_VALUE,
  BSG_METADATA_NUMBER_VALUE,
} bugsnag_metadata_type;

typedef struct {
  uintptr_t frame_address;
  uintptr_t symbol_address;
  uintptr_t load_address;
  uintptr_t line_number;

  char filename[256];
  char method[256];
} bugsnag_stackframe;

#ifdef __cplusplus
extern "C" {
#endif

/* Accessors for event.api_key */

/**
 * Retrieves the event api key
 * @param event_ptr a pointer to the event supplied in an on_error callback
 * @return the event api key, or NULL if this has not been set
 */
char *bugsnag_event_get_api_key(void *event_ptr);

/**
 * Sets the event api key
 * @param event_ptr a pointer to the event supplied in an on_error callback
 * @param value the new event api key value, which cannot be NULL
 */
void bugsnag_event_set_api_key(void *event_ptr, char *value);

/* Accessors for event.context */

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

/**
 * Retrieves the binary_arch value reported for this event.
 *
 * To obtain a pointer to the bugsnag event you are modifying, you will need to
 * implement an on_error callback. on_error callbacks are executed from within a
 * signal handler so your implementation must be async-safe, otherwise the
 * process may terminate before an error report can be captured.
 *
 * @param event_ptr - a pointer to the bugsnag event
 * @return the binary_arch in the event
 */
char *bugsnag_app_get_binary_arch(void *event_ptr);

/**
 * Sets a new value for the binary_arch value reported for this event.
 *
 * To obtain a pointer to the bugsnag event you are modifying, you will need to
 * implement an on_error callback. on_error callbacks are executed from within a
 * signal handler so your implementation must be async-safe, otherwise the
 * process may terminate before an error report can be captured.
 *
 * @param event_ptr - a pointer to the bugsnag event
 * @param value - the new value for the binary_arch field (nullable)
 */
void bugsnag_app_set_binary_arch(void *event_ptr, char *value);

char *bugsnag_app_get_build_uuid(void *event_ptr);
void bugsnag_app_set_build_uuid(void *event_ptr, char *value);

time_t bugsnag_app_get_duration(void *event_ptr);
void bugsnag_app_set_duration(void *event_ptr, time_t value);

time_t bugsnag_app_get_duration_in_foreground(void *event_ptr);
void bugsnag_app_set_duration_in_foreground(void *event_ptr, time_t value);

char *bugsnag_app_get_id(void *event_ptr);
void bugsnag_app_set_id(void *event_ptr, char *value);

bool bugsnag_app_get_in_foreground(void *event_ptr);
void bugsnag_app_set_in_foreground(void *event_ptr, bool value);

char *bugsnag_app_get_release_stage(void *event_ptr);
void bugsnag_app_set_release_stage(void *event_ptr, char *value);

char *bugsnag_app_get_type(void *event_ptr);
void bugsnag_app_set_type(void *event_ptr, char *value);

char *bugsnag_app_get_version(void *event_ptr);
void bugsnag_app_set_version(void *event_ptr, char *value);

int bugsnag_app_get_version_code(void *event_ptr);
void bugsnag_app_set_version_code(void *event_ptr, int value);

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
void bugsnag_device_set_model(void *event_ptr, char *value);

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

/* Accessors for event.error */

char *bugsnag_error_get_error_class(void *event_ptr);
void bugsnag_error_set_error_class(void *event_ptr, char *value);

char *bugsnag_error_get_error_message(void *event_ptr);
void bugsnag_error_set_error_message(void *event_ptr, char *value);

char *bugsnag_error_get_error_type(void *event_ptr);
void bugsnag_error_set_error_type(void *event_ptr, char *value);

/* Accessors for event.user */

/**
 * Retrieves the user value reported for this event. The user struct has an ID,
 * email, and name.
 *
 * To obtain a pointer to the bugsnag event you are modifying, you will need to
 * implement an on_error callback. on_error callbacks are executed from within a
 * signal handler so your implementation must be async-safe, otherwise the
 * process may terminate before an error report can be captured.
 *
 * @param event_ptr - a pointer to the bugsnag event
 * @return the user in the event, represented as a struct
 */
bugsnag_user bugsnag_event_get_user(void *event_ptr);
void bugsnag_event_set_user(void *event_ptr, char *id, char *email, char *name);

/* Accessors for event.severity */

bugsnag_severity bugsnag_event_get_severity(void *event_ptr);
void bugsnag_event_set_severity(void *event_ptr, bugsnag_severity value);

/* Accessors for event.unhandled */

/**
 * Whether the event was a crash (i.e. unhandled) or handled error in which the
 * system continued running.
 *
 * Unhandled errors count towards your stability score. If you don't want
 * certain errors to count towards your stability score, you can alter this
 * property through bugsnag_add_on_error.
 *
 * @param event_ptr a pointer to the event supplied in a bsg_on_error callback
 * @return whether the event is unhandled or not
 */
bool bugsnag_event_is_unhandled(void *event_ptr);

/**
 * Whether the event was a crash (i.e. unhandled) or handled error in which the
 * system continued running.
 *
 * Unhandled errors count towards your stability score. If you don't want
 * certain errors to count towards your stability score, you can alter this
 * property through bugsnag_add_on_error.
 *
 * @param event_ptr a pointer to the event supplied in a bsg_on_error callback
 * @param value the new unhandled value
 */
void bugsnag_event_set_unhandled(void *event_ptr, bool value);

/* Accessors for event.groupingHash */

char *bugsnag_event_get_grouping_hash(void *event_ptr);
void bugsnag_event_set_grouping_hash(void *event_ptr, char *value);

/* Accessors for event.metadata */

void bugsnag_event_add_metadata_double(void *event_ptr, char *section,
                                       char *name, double value);
void bugsnag_event_add_metadata_string(void *event_ptr, char *section,
                                       char *name, char *value);
void bugsnag_event_add_metadata_bool(void *event_ptr, char *section, char *name,
                                     bool value);

void bugsnag_event_clear_metadata_section(void *event_ptr, char *section);
void bugsnag_event_clear_metadata(void *event_ptr, char *section, char *name);

/**
 * Retrieves the metadata type for a given section and key in this event.
 *
 * You should call this method before attempting to call
 * bugsnag_event_get_metadata. If a value has been set for a given section/name,
 * this method will return one of: BSG_METADATA_CHAR_VALUE,
 * BSG_METADATA_NUMBER_VALUE, BSG_METADATA_BOOL_VALUE. You should then call the
 * appropriate bugsnag_event_get_metadata method to retrieve the actual value.
 *
 * If no value has been set, this method will return BSG_METADATA_NONE_VALUE.
 *
 * To obtain a pointer to the bugsnag event you are modifying, you will need to
 * implement an on_error callback. on_error callbacks are executed from within a
 * signal handler so your implementation must be async-safe, otherwise the
 * process may terminate before an error report can be captured.
 *
 * @param event_ptr - a pointer to the bugsnag event
 * @param section - the metadata section key
 * @param name - the metadata section name
 * @return the type of the metadata, or BSG_METADATA_NONE_VALUE if no value
 * exists
 */
bugsnag_metadata_type bugsnag_event_has_metadata(void *event_ptr, char *section,
                                                 char *name);

/**
 * Retrieves the metadata value for a given section and key in this event.
 *
 * Before calling this method you should first check whether a metadata value
 * exists by using bugsnag_event_has_metadata. If no value exists, a default
 * value will be returned. For numeric values, the default value will be 0.0.
 *
 * To obtain a pointer to the bugsnag event you are modifying, you will need to
 * implement an on_error callback. on_error callbacks are executed from within a
 * signal handler so your implementation must be async-safe, otherwise the
 * process may terminate before an error report can be captured.
 *
 * @param event_ptr - a pointer to the bugsnag event
 * @param section - the metadata section key
 * @param name - the metadata section name
 * @param value - the value to set on the given key/name
 */
double bugsnag_event_get_metadata_double(void *event_ptr, char *section,
                                         char *name);
char *bugsnag_event_get_metadata_string(void *event_ptr, char *section,
                                        char *name);
bool bugsnag_event_get_metadata_bool(void *event_ptr, char *section,
                                     char *name);

/* Accessors for event.error.stacktrace */

int bugsnag_event_get_stacktrace_size(void *event_ptr);
bugsnag_stackframe *bugsnag_event_get_stackframe(void *event_ptr, int index);

#ifdef __cplusplus
}
#endif

#endif
