//
// Created by Karl Stenerud on 22.02.21.
//

#include "jnicache.h"
#include "safejni.h"
#include <bugsnag_ndk.h>
#include <stdatomic.h>
#include <stdbool.h>
#include <stdlib.h>

static inline jclass safe_find_class(JNIEnv *env, const char *name) {
  jclass cls = bsg_safe_find_class(env, name);
  if (cls == NULL) {
    return NULL;
  }
  return (*env)->NewGlobalRef(env, cls);
}

static bsg_jni_cache jni_cache;
static bool is_cache_valid = false;

bool bsg_init_jni_cache(JNIEnv *env) {
  static atomic_flag initialized = ATOMIC_FLAG_INIT;
  if (atomic_flag_test_and_set(&initialized)) {
    return true;
  }

  // lookup java/lang/Integer
  jni_cache.integer = safe_find_class(env, "java/lang/Integer");
  if (jni_cache.integer == NULL) {
    goto fail;
  }

  // lookup java/lang/Boolean
  jni_cache.boolean = safe_find_class(env, "java/lang/Boolean");
  if (jni_cache.boolean == NULL) {
    goto fail;
  }

  // lookup java/lang/Long
  jni_cache.long_class = safe_find_class(env, "java/lang/Long");
  if (jni_cache.long_class == NULL) {
    goto fail;
  }

  // lookup java/lang/Float
  jni_cache.float_class = safe_find_class(env, "java/lang/Float");
  if (jni_cache.float_class == NULL) {
    goto fail;
  }

  // lookup java/lang/Number
  jni_cache.number = safe_find_class(env, "java/lang/Number");
  if (jni_cache.number == NULL) {
    goto fail;
  }

  // lookup java/lang/String
  jni_cache.string = safe_find_class(env, "java/lang/String");
  if (jni_cache.string == NULL) {
    goto fail;
  }

  // lookup Integer.intValue()
  jni_cache.integer_int_value =
      bsg_safe_get_method_id(env, jni_cache.integer, "intValue", "()I");
  if (jni_cache.integer_int_value == NULL) {
    goto fail;
  }

  // lookup Integer.floatValue()
  jni_cache.float_float_value =
      bsg_safe_get_method_id(env, jni_cache.float_class, "floatValue", "()F");
  if (jni_cache.float_float_value == NULL) {
    goto fail;
  }

  // lookup Double.doubleValue()
  jni_cache.number_double_value =
      bsg_safe_get_method_id(env, jni_cache.number, "doubleValue", "()D");
  if (jni_cache.number_double_value == NULL) {
    goto fail;
  }

  // lookup Long.longValue()
  jni_cache.long_long_value =
      bsg_safe_get_method_id(env, jni_cache.integer, "longValue", "()J");
  if (jni_cache.long_long_value == NULL) {
    goto fail;
  }

  // lookup Boolean.booleanValue()
  jni_cache.boolean_bool_value =
      bsg_safe_get_method_id(env, jni_cache.boolean, "booleanValue", "()Z");
  if (jni_cache.boolean_bool_value == NULL) {
    goto fail;
  }

  // lookup java/util/ArrayList
  jni_cache.arraylist = safe_find_class(env, "java/util/ArrayList");
  if (jni_cache.arraylist == NULL) {
    goto fail;
  }

  // lookup ArrayList constructor
  jni_cache.arraylist_init_with_obj = bsg_safe_get_method_id(
      env, jni_cache.arraylist, "<init>", "(Ljava/util/Collection;)V");
  if (jni_cache.arraylist_init_with_obj == NULL) {
    goto fail;
  }

  // lookup ArrayList.get()
  jni_cache.arraylist_get = bsg_safe_get_method_id(
      env, jni_cache.arraylist, "get", "(I)Ljava/lang/Object;");
  if (jni_cache.arraylist_get == NULL) {
    goto fail;
  }

  // lookup java/util/HashMap
  jni_cache.hash_map = safe_find_class(env, "java/util/HashMap");
  if (jni_cache.hash_map == NULL) {
    goto fail;
  }

  // lookup java/util/Map
  jni_cache.map = safe_find_class(env, "java/util/Map");
  if (jni_cache.map == NULL) {
    goto fail;
  }

  // lookup java/util/Set
  jni_cache.hash_map_key_set = bsg_safe_get_method_id(
      env, jni_cache.hash_map, "keySet", "()Ljava/util/Set;");
  if (jni_cache.hash_map_key_set == NULL) {
    goto fail;
  }

  // lookup HashMap.size()
  jni_cache.hash_map_size =
      bsg_safe_get_method_id(env, jni_cache.hash_map, "size", "()I");
  if (jni_cache.hash_map_size == NULL) {
    goto fail;
  }

  // lookup HashMap.get()
  jni_cache.hash_map_get = bsg_safe_get_method_id(
      env, jni_cache.hash_map, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
  if (jni_cache.hash_map_get == NULL) {
    goto fail;
  }

  // lookup Map.keySet()
  jni_cache.map_key_set =
      bsg_safe_get_method_id(env, jni_cache.map, "keySet", "()Ljava/util/Set;");
  if (jni_cache.map_key_set == NULL) {
    goto fail;
  }

  // lookup Map.size()
  jni_cache.map_size =
      bsg_safe_get_method_id(env, jni_cache.map, "size", "()I");
  if (jni_cache.map_size == NULL) {
    goto fail;
  }

  // lookup Map.get()
  jni_cache.map_get = bsg_safe_get_method_id(
      env, jni_cache.map, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
  if (jni_cache.map_get == NULL) {
    goto fail;
  }

  // lookup com/bugsnag/android/NativeInterface
  jni_cache.native_interface =
      safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (jni_cache.native_interface == NULL) {
    goto fail;
  }

  // lookup NativeInterface.getApp()
  jni_cache.get_app_data = bsg_safe_get_static_method_id(
      env, jni_cache.native_interface, "getApp", "()Ljava/util/Map;");
  if (jni_cache.get_app_data == NULL) {
    goto fail;
  }

  // lookup NativeInterface.getDevice()
  jni_cache.get_device_data = bsg_safe_get_static_method_id(
      env, jni_cache.native_interface, "getDevice", "()Ljava/util/Map;");
  if (jni_cache.get_device_data == NULL) {
    goto fail;
  }

  // lookup NativeInterface.getUser()
  jni_cache.get_user_data = bsg_safe_get_static_method_id(
      env, jni_cache.native_interface, "getUser", "()Ljava/util/Map;");
  if (jni_cache.get_user_data == NULL) {
    goto fail;
  }

  // lookup NativeInterface.getMetadata()
  jni_cache.get_metadata = bsg_safe_get_static_method_id(
      env, jni_cache.native_interface, "getMetadata", "()Ljava/util/Map;");
  if (jni_cache.get_metadata == NULL) {
    goto fail;
  }

  // lookup NativeInterface.getContext()
  jni_cache.get_context = bsg_safe_get_static_method_id(
      env, jni_cache.native_interface, "getContext", "()Ljava/lang/String;");
  if (jni_cache.get_context == NULL) {
    goto fail;
  }

  is_cache_valid = true;
  return true;

fail:
  is_cache_valid = false;
  return false;
}

bsg_jni_cache *bsg_get_jni_cache() { return &jni_cache; }

bool bsg_is_jni_cache_valid() { return is_cache_valid; }
