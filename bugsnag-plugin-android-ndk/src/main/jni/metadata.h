#ifndef BUGSNAG_METADATA_H
#define BUGSNAG_METADATA_H

#include "event.h"
#include "jni_cache.h"
#include <jni.h>

/**
 * Load all app, device, user, and custom metadata from NativeInterface into a
 * event
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
