#ifndef BUGSNAG_METADATA_H
#define BUGSNAG_METADATA_H

#include <jni.h>
#include "report.h"

/**
 * Load all app, device, user, and custom metadata from NativeInterface into a report
 */
void bsg_populate_report(JNIEnv *env, bugsnag_report *report);
/**
 * Load custom metadata from NativeInterface into a report, optionally from an object.
 * If metadata is not provided, load from NativeInterface
 */
void bsg_populate_metadata(JNIEnv *env, bugsnag_report *report, jobject metadata);

/**
 * Parse as java.util.Map<String, String> to populate crumb metadata
 */
void bsg_populate_crumb_metadata(JNIEnv *env, bugsnag_breadcrumb *crumb,
                                 jobject metadata);
#endif
