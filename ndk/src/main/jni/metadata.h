#ifndef BUGSNAG_METADATA_H
#define BUGSNAG_METADATA_H

#include <jni.h>
#include "report.h"

/**
 * Load cache of class/method references from the JNI environment.
 * Required for other population tasks.
 */
void bsg_populate_jni_cache(JNIEnv *env);
/**
 * Load app metadata from NativeInterface into a report
 */
void bsg_populate_app_data(JNIEnv *env, bugsnag_report *report);
/**
 * Load device metadata from NativeInterface into a report
 */
void bsg_populate_device_data(JNIEnv *env, bugsnag_report *report);
/**
 * Load user metadata from NativeInterface into a report
 */
void bsg_populate_user_data(JNIEnv *env, bugsnag_report *report);
/**
 * Load context from NativeInterface into a report
 */
void bsg_populate_context(JNIEnv *env, bugsnag_report *report);
/**
 * Load breadcrumbs from NativeInterface into a report
 */
void bsg_populate_breadcrumbs(JNIEnv *env, bugsnag_report *report);
/**
 * Load custom metadata from NativeInterface into a report
 */
void bsg_populate_metadata(JNIEnv *env, bugsnag_report *report);

#endif
