#ifndef BUGSNAG_METADATA_H
#define BUGSNAG_METADATA_H

#include "event.h"
#include "jni_cache.h"
#include <jni.h>
#include <stdbool.h>

/**
 * Local references to the context, app, device and user data returned by
 * NativeInterface. Fetching these calls into Java (which may block), while
 * applying them only reads the returned HashMaps/String.
 */
typedef struct {
  jobject context;
  jobject app;
  jobject device;
  jobject user;
  bool fetched;
} bsg_event_java_state;

/**
 * Call NativeInterface to collect the current context, app, device and user
 * data. This may block on background work in the JVM, so it MUST NOT be
 * called while holding the native environment write lock.
 */
void bsg_fetch_event_java_state(JNIEnv *env, bsg_event_java_state *state);

/**
 * Copy previously fetched Java state into an event. This only reads the
 * HashMaps/String returned by bsg_fetch_event_java_state and is safe to call
 * while holding the native environment write lock.
 *
 * When apply_user_context is false the user (id/name/email) and context are
 * left untouched. Use this when the fetched state may be stale: those fields
 * are kept current by their own setters, so stale personal data must never
 * overwrite them.
 */
void bsg_apply_event_java_state(JNIEnv *env, bugsnag_event *event,
                                const bsg_event_java_state *state,
                                bool apply_user_context);

/**
 * Release the local references held by a bsg_event_java_state.
 */
void bsg_release_event_java_state(JNIEnv *env, bsg_event_java_state *state);

/**
 * Load all app, device, user, and custom metadata from NativeInterface into a
 * event. Equivalent to fetch + apply + release; must not be called while
 * holding the native environment write lock.
 */
void bsg_populate_event(JNIEnv *env, bugsnag_event *event);
/**
 * Load custom metadata from NativeInterface into a native metadata struct,
 * optionally from an object. If metadata is not provided, load from
 * NativeInterface
 */
void bsg_populate_metadata(JNIEnv *env, bugsnag_metadata *dst,
                           jobject metadata);

/**
 * Parse as java.util.Map<String, String> to populate crumb metadata
 */
void bsg_populate_crumb_metadata(JNIEnv *env, bugsnag_breadcrumb *crumb,
                                 jobject metadata);

const char *bsg_os_name();

#endif
