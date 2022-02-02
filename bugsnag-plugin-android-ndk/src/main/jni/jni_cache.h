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
  JavaVM *jvm;

  jclass boolean;
  jmethodID boolean_bool_value;

  jclass float_class;
  jmethodID float_float_value;

  jclass number;
  jmethodID number_double_value;

  jclass string;

  jclass map;
  jmethodID map_get;
  jmethodID map_size;
  jmethodID map_key_set;

  jclass hash_map;
  jmethodID hash_map_get;
  jmethodID hash_map_size;
  jmethodID hash_map_key_set;

  jclass arraylist;
  jmethodID arraylist_init_with_obj;
  jmethodID arraylist_get;

  jclass native_interface;
  jmethodID ni_get_app;
  jmethodID ni_get_device;
  jmethodID ni_get_user;
  jmethodID ni_set_user;
  jmethodID ni_get_metadata;
  jmethodID ni_get_context;
  jmethodID ni_notify;
  jmethodID ni_leave_breadcrumb;
  jmethodID ni_deliver_report;

  jclass stack_trace_element;
  jmethodID ste_constructor;

  jclass severity;

  jclass breadcrumb_type;

  bool initialized;
} bsg_jni_cache_t;

extern bsg_jni_cache_t *bsg_jni_cache;

/**
 * Populate all references in the JNI cache.
 *
 * @param env The JNI env
 * @return false if an error occurs, in which case the cache is unusable.
 */
bool bsg_jni_cache_init(JNIEnv *env);

#ifdef __cplusplus
}
#endif

#endif // BUGSNAG_ANDROID_JNI_CACHE_H
