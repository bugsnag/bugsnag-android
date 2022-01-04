//
// Created by Karl Stenerud on 03.01.22.
//

#include "jni_cache.h"
#include "safejni.h"
#include <stdlib.h>

bsg_jni_cache *bsg_global_jni_cache = NULL;

bool bsg_jni_cache_init(JNIEnv *env) {
  // Use a simple reference lock since the worst-case scenario after
  // a race condition is a few hundred bytes of wasted heap, once.
  if (bsg_global_jni_cache != NULL) {
    return true;
  }

  bsg_jni_cache *jni_cache = calloc(1, sizeof(bsg_jni_cache));
  bsg_jni_cache_refresh(env);

  // Care should be taken not to load objects as local references here.

  // lookup Integer.intValue()
  jni_cache->integer_int_value =
      bsg_safe_get_method_id(env, jni_cache->integer, "intValue", "()I");
  if (jni_cache->integer_int_value == NULL) {
    goto failed;
  }

  // lookup Integer.floatValue()
  jni_cache->float_float_value =
      bsg_safe_get_method_id(env, jni_cache->float_class, "floatValue", "()F");
  if (jni_cache->float_float_value == NULL) {
    goto failed;
  }

  // lookup Double.doubleValue()
  jni_cache->number_double_value =
      bsg_safe_get_method_id(env, jni_cache->number, "doubleValue", "()D");
  if (jni_cache->number_double_value == NULL) {
    goto failed;
  }

  // lookup Long.longValue()
  jni_cache->long_long_value =
      bsg_safe_get_method_id(env, jni_cache->integer, "longValue", "()J");
  if (jni_cache->long_long_value == NULL) {
    goto failed;
  }

  // lookup Boolean.booleanValue()
  jni_cache->boolean_bool_value =
      bsg_safe_get_method_id(env, jni_cache->boolean, "booleanValue", "()Z");
  if (jni_cache->boolean_bool_value == NULL) {
    goto failed;
  }

  // lookup ArrayList constructor
  jni_cache->arraylist_init_with_obj = bsg_safe_get_method_id(
      env, jni_cache->arraylist, "<init>", "(Ljava/util/Collection;)V");
  if (jni_cache->arraylist_init_with_obj == NULL) {
    goto failed;
  }

  // lookup ArrayList.get()
  jni_cache->arraylist_get = bsg_safe_get_method_id(
      env, jni_cache->arraylist, "get", "(I)Ljava/lang/Object;");
  if (jni_cache->arraylist_get == NULL) {
    goto failed;
  }

  // lookup java/util/Set
  jni_cache->hash_map_key_set = bsg_safe_get_method_id(
      env, jni_cache->hash_map, "keySet", "()Ljava/util/Set;");
  if (jni_cache->hash_map_key_set == NULL) {
    goto failed;
  }

  // lookup HashMap.size()
  jni_cache->hash_map_size =
      bsg_safe_get_method_id(env, jni_cache->hash_map, "size", "()I");
  if (jni_cache->hash_map_size == NULL) {
    goto failed;
  }

  // lookup HashMap.get()
  jni_cache->hash_map_get =
      bsg_safe_get_method_id(env, jni_cache->hash_map, "get",
                             "(Ljava/lang/Object;)Ljava/lang/Object;");
  if (jni_cache->hash_map_get == NULL) {
    goto failed;
  }

  // lookup Map.keySet()
  jni_cache->map_key_set = bsg_safe_get_method_id(env, jni_cache->map, "keySet",
                                                  "()Ljava/util/Set;");
  if (jni_cache->map_key_set == NULL) {
    goto failed;
  }

  // lookup Map.size()
  jni_cache->map_size =
      bsg_safe_get_method_id(env, jni_cache->map, "size", "()I");
  if (jni_cache->map_size == NULL) {
    goto failed;
  }

  // lookup Map.get()
  jni_cache->map_get = bsg_safe_get_method_id(
      env, jni_cache->map, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
  if (jni_cache->map_get == NULL) {
    goto failed;
  }

  // lookup NativeInterface.getApp()
  jni_cache->ni_get_app = bsg_safe_get_static_method_id(
      env, jni_cache->native_interface, "getApp", "()Ljava/util/Map;");
  if (jni_cache->ni_get_app == NULL) {
    goto failed;
  }

  // lookup NativeInterface.getDevice()
  jni_cache->ni_get_device = bsg_safe_get_static_method_id(
      env, jni_cache->native_interface, "getDevice", "()Ljava/util/Map;");
  if (jni_cache->ni_get_device == NULL) {
    goto failed;
  }

  // lookup NativeInterface.getUser()
  jni_cache->ni_get_user = bsg_safe_get_static_method_id(
      env, jni_cache->native_interface, "getUser", "()Ljava/util/Map;");
  if (jni_cache->ni_get_user == NULL) {
    goto failed;
  }

  jni_cache->ni_set_user = bsg_safe_get_static_method_id(
      env, jni_cache->native_interface, "setUser", "([B[B[B)V");

  // lookup NativeInterface.getMetadata()
  jni_cache->ni_get_metadata = bsg_safe_get_static_method_id(
      env, jni_cache->native_interface, "getMetadata", "()Ljava/util/Map;");
  if (jni_cache->ni_get_metadata == NULL) {
    goto failed;
  }

  // lookup NativeInterface.getContext()
  jni_cache->ni_get_context = bsg_safe_get_static_method_id(
      env, jni_cache->native_interface, "getContext", "()Ljava/lang/String;");
  if (jni_cache->ni_get_context == NULL) {
    goto failed;
  }

  // lookup NativeInterface.notify()
  jni_cache->ni_notify = bsg_safe_get_static_method_id(
      env, jni_cache->native_interface, "notify",
      "([B[BLcom/bugsnag/android/Severity;[Ljava/lang/StackTraceElement;)V");
  if (jni_cache->ni_notify == NULL) {
    goto failed;
  }

  jni_cache->ni_deliver_report = bsg_safe_get_static_method_id(
      env, jni_cache->native_interface, "deliverReport",
      "([B[BLjava/lang/String;Z)V");
  if (jni_cache->ni_deliver_report == NULL) {
    goto failed;
  }

  jni_cache->ni_leave_breadcrumb = bsg_safe_get_static_method_id(
      env, jni_cache->native_interface, "leaveBreadcrumb",
      "([BLcom/bugsnag/android/BreadcrumbType;)V");
  if (jni_cache->ni_leave_breadcrumb == NULL) {
    goto failed;
  }

  jni_cache->ste_constructor = bsg_safe_get_method_id(
      env, jni_cache->stack_trace_element, "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
  if (jni_cache->ste_constructor == NULL) {
    goto failed;
  }

  bsg_global_jni_cache = jni_cache;
  return true;

failed:
  free(bsg_global_jni_cache);
  return false;
}

bool bsg_jni_cache_refresh(JNIEnv *env) {
  if (bsg_global_jni_cache == NULL) {
    return false;
  }

  // lookup java/lang/Integer
  bsg_global_jni_cache->integer = bsg_safe_find_class(env, "java/lang/Integer");
  if (bsg_global_jni_cache->integer == NULL) {
    goto failed;
  }

  // lookup java/lang/Boolean
  bsg_global_jni_cache->boolean = bsg_safe_find_class(env, "java/lang/Boolean");
  if (bsg_global_jni_cache->boolean == NULL) {
    goto failed;
  }

  // lookup java/lang/Long
  bsg_global_jni_cache->long_class = bsg_safe_find_class(env, "java/lang/Long");
  if (bsg_global_jni_cache->long_class == NULL) {
    goto failed;
  }

  // lookup java/lang/Float
  bsg_global_jni_cache->float_class = bsg_safe_find_class(env, "java/lang/Float");
  if (bsg_global_jni_cache->float_class == NULL) {
    goto failed;
  }

  // lookup java/lang/Number
  bsg_global_jni_cache->number = bsg_safe_find_class(env, "java/lang/Number");
  if (bsg_global_jni_cache->number == NULL) {
    goto failed;
  }

  // lookup java/lang/String
  bsg_global_jni_cache->string = bsg_safe_find_class(env, "java/lang/String");
  if (bsg_global_jni_cache->string == NULL) {
    goto failed;
  }

  // lookup java/util/ArrayList
  bsg_global_jni_cache->arraylist = bsg_safe_find_class(env, "java/util/ArrayList");
  if (bsg_global_jni_cache->arraylist == NULL) {
    goto failed;
  }

  // lookup java/util/HashMap
  bsg_global_jni_cache->hash_map = bsg_safe_find_class(env, "java/util/HashMap");
  if (bsg_global_jni_cache->hash_map == NULL) {
    goto failed;
  }

  // lookup java/util/Map
  bsg_global_jni_cache->map = bsg_safe_find_class(env, "java/util/Map");
  if (bsg_global_jni_cache->map == NULL) {
    goto failed;
  }

  // lookup com/bugsnag/android/NativeInterface
  bsg_global_jni_cache->native_interface =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (bsg_global_jni_cache->native_interface == NULL) {
    goto failed;
  }

  bsg_global_jni_cache->stack_trace_element =
      bsg_safe_find_class(env, "java/lang/StackTraceElement");
  if (bsg_global_jni_cache->stack_trace_element == NULL) {
    goto failed;
  }

  bsg_global_jni_cache->severity =
      bsg_safe_find_class(env, "com/bugsnag/android/Severity");
  if (bsg_global_jni_cache->severity == NULL) {
    goto failed;
  }

  bsg_global_jni_cache->breadcrumb_type =
      bsg_safe_find_class(env, "com/bugsnag/android/BreadcrumbType");
  if (bsg_global_jni_cache->breadcrumb_type == NULL) {
    goto failed;
  }

  return true;

failed:
  return false;
}
