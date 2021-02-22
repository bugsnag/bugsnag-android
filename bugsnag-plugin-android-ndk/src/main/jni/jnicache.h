//
// Created by Karl Stenerud on 22.02.21.
//
// Cache for JNI classes and methods.
//
// Note: Technically, no JNI objects should be called outside of the JNI
// context they were fetched from because they could get unloaded at any
// time, after which accessing the pointer from C would crash.
//
// HOWEVER, because we have a controlled environment where we're not unloading
// Java classes, it is safe to cache the classes and methods that we know
// won't ever be unloaded.
//

#ifndef BUGSNAG_ANDROID_JNICACHE_H
#define BUGSNAG_ANDROID_JNICACHE_H

#include <jni.h>
#include <stdbool.h>

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
  jmethodID get_app_data;
  jmethodID get_device_data;
  jmethodID get_user_data;
  jmethodID get_breadcrumbs;
  jmethodID get_metadata;
  jmethodID get_context;
} bsg_jni_cache;

// Initialize the cache, returning true if it was successful.
// Do NOT use the cache before calling this, and do NOT use the cache if this
// returns false!
bool bsg_init_jni_cache(JNIEnv *env);

// Global access to the cache.
bsg_jni_cache *bsg_get_jni_cache();

// Return true if the cache was successfully initialized.
// Do NOT use the cache if this returns false!
bool bsg_is_jni_cache_valid();

#endif // BUGSNAG_ANDROID_JNICACHE_H
