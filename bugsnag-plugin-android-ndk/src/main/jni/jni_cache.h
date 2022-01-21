//
// Created by Karl Stenerud on 03.01.22.
//

#ifndef BUGSNAG_ANDROID_JNI_CACHE_H
#define BUGSNAG_ANDROID_JNI_CACHE_H

#include <jni.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
  jclass hash_map;
  jclass map;
  jclass arraylist;
  jclass integer;
  jclass boolean;
  jclass metadata;
  jclass native_interface;
  jclass long_class;
  jclass float_class;
  jclass number;
  jclass string;
  jclass stack_trace_element;
  jclass severity;
  jclass breadcrumb_type;
  jmethodID integer_int_value;
  jmethodID long_long_value;
  jmethodID float_float_value;
  jmethodID boolean_bool_value;
  jmethodID number_double_value;
  jmethodID hash_map_get;
  jmethodID hash_map_size;
  jmethodID hash_map_key_set;
  jmethodID map_get;
  jmethodID map_size;
  jmethodID map_key_set;
  jmethodID arraylist_init_with_obj;
  jmethodID arraylist_get;
  jmethodID ni_get_app;
  jmethodID ni_get_device;
  jmethodID ni_get_user;
  jmethodID ni_set_user;
  jmethodID ni_get_metadata;
  jmethodID ni_get_context;
  jmethodID ni_notify;
  jmethodID ni_leave_breadcrumb;
  jmethodID ni_deliver_report;
  jmethodID ste_constructor;
} bsg_jni_cache;

/**
 * Populate all references in the JNI cache.
 * This MUST be called on every Java-to-native call to ensure that references
 * remain bound to the correct JNIEnv.
 *
 * @param env The JNI env
 * @param cache The cache to refresh
 * @return false if an error occurs, in which case the cache is unusable.
 */
bool bsg_jni_cache_init(JNIEnv *env, bsg_jni_cache *cache);

#ifdef __cplusplus
}
#endif

#endif // BUGSNAG_ANDROID_JNI_CACHE_H
