/** Public API for interacting with Bugsnag */
#ifndef BUGSNAG_ANDROID_NDK_BUGSNAG_API_H
#define BUGSNAG_ANDROID_NDK_BUGSNAG_API_H

#include <jni.h>
#include "report.h"

#ifdef __cplusplus
extern "C" {
#endif
/**
 * Configure the Bugsnag interface, optionally including the JNI environment.
 * @param env  The JNI environment to use when using convenience methods
 */
void bugsnag_init(JNIEnv *env);
/**
 * Sends an error report to Bugsnag
 * @param name     The name of the error
 * @param message  The error message
 * @param severity The severity of the error
 */
void bugsnag_notify(char* name, char* message, bsg_severity_t severity);
void bugsnag_notify_env(JNIEnv *env, char* name, char* message, bsg_severity_t severity);
/**
 * Set the current user
 * @param id    The identifier of the user
 * @param email The user's email
 * @param name  The user's name
 */
void bugsnag_set_user(char* id, char* email, char* name);
void bugsnag_set_user_env(JNIEnv *env, char* id, char* email, char* name);
/**
 * Leave a breadcrumb, indicating an event of significance which will be logged in subsequent
 * error reports
 */
void bugsnag_leave_breadcrumb(char *name, bsg_breadcrumb_t type);
void bugsnag_leave_breadcrumb_env(JNIEnv *env, char *name, bsg_breadcrumb_t type);

#ifdef __cplusplus
}
#endif

#endif //BUGSNAG_ANDROID_NDK_BUGSNAG_API_H
