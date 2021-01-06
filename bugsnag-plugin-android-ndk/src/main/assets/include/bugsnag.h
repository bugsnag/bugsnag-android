/** Public API for interacting with Bugsnag */
#ifndef BUGSNAG_ANDROID_NDK_BUGSNAG_API_H
#define BUGSNAG_ANDROID_NDK_BUGSNAG_API_H

#include "event.h"
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef bool (*bsg_on_error)(void *);

/**
 * Configure the Bugsnag interface, optionally including the JNI environment.
 * @param env  The JNI environment to use when using convenience methods
 */
void bugsnag_start(JNIEnv *env);
/**
 * Sends an error report to Bugsnag
 * @param name     The name of the error
 * @param message  The error message
 * @param severity The severity of the error
 */
void bugsnag_notify(char *name, char *message, bugsnag_severity severity);
void bugsnag_notify_env(JNIEnv *env, char *name, char *message,
                        bugsnag_severity severity);
/**
 * Set the current user
 * @param id    The identifier of the user
 * @param email The user's email
 * @param name  The user's name
 */
void bugsnag_set_user(char *id, char *email, char *name);
void bugsnag_set_user_env(JNIEnv *env, char *id, char *email, char *name);
/**
 * Leave a breadcrumb, indicating an event of significance which will be logged
 * in subsequent error reports
 */
void bugsnag_leave_breadcrumb(char *message, bugsnag_breadcrumb_type type);
void bugsnag_leave_breadcrumb_env(JNIEnv *env, char *message,
                                  bugsnag_breadcrumb_type type);

/**
 * Adds a callback which is invoked whenever a fatal error occurs. The callback
 * will be passed a pointer to the event payload as a parameter, allowing for
 * data to be added/removed.
 * @param on_error the callback
 */
void bugsnag_add_on_error(bsg_on_error on_error);

/**
 * Removes any callback previously added in bugsnag_add_on_error
 */
void bugsnag_remove_on_error();

#ifdef __cplusplus
}
#endif

#endif // BUGSNAG_ANDROID_NDK_BUGSNAG_API_H
