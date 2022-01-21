//
// Created by Karl Stenerud on 03.01.22.
//

#include "jni_cache.h"
#include "safejni.h"
#include <stdlib.h>

#include <android/log.h>
#ifndef BUGSNAG_LOG
#define BUGSNAG_LOG(fmt, ...)                                                  \
  __android_log_print(ANDROID_LOG_WARN, "BugsnagNDK", fmt, ##__VA_ARGS__)
#endif

#define report_contents(VALUE, TYPECODE)                                       \
  BUGSNAG_LOG(#VALUE " == " TYPECODE, VALUE)

bool bsg_jni_cache_init(JNIEnv *env, bsg_jni_cache *cache) {
  if (cache == NULL) {
    return false;
  }

  // All objects here are local references, and MUST be refreshed on every
  // transition from Java to native.

  // Classes

  cache->integer = bsg_safe_find_class(env, "java/lang/Integer");
  if (cache->integer == NULL) {
    report_contents(cache->integer, "%p");
    goto failed;
  }

  cache->boolean = bsg_safe_find_class(env, "java/lang/Boolean");
  if (cache->boolean == NULL) {
    report_contents(cache->boolean, "%p");
    goto failed;
  }

  cache->long_class = bsg_safe_find_class(env, "java/lang/Long");
  if (cache->long_class == NULL) {
    report_contents(cache->long_class, "%p");
    goto failed;
  }

  cache->float_class = bsg_safe_find_class(env, "java/lang/Float");
  if (cache->float_class == NULL) {
    report_contents(cache->float_class, "%p");
    goto failed;
  }

  cache->number = bsg_safe_find_class(env, "java/lang/Number");
  if (cache->number == NULL) {
    report_contents(cache->number, "%p");
    goto failed;
  }

  cache->string = bsg_safe_find_class(env, "java/lang/String");
  if (cache->string == NULL) {
    report_contents(cache->string, "%p");
    goto failed;
  }

  // Methods

  cache->arraylist = bsg_safe_find_class(env, "java/util/ArrayList");
  if (cache->arraylist == NULL) {
    report_contents(cache->arraylist, "%p");
    goto failed;
  }

  cache->hash_map = bsg_safe_find_class(env, "java/util/HashMap");
  if (cache->hash_map == NULL) {
    report_contents(cache->hash_map, "%p");
    goto failed;
  }

  cache->map = bsg_safe_find_class(env, "java/util/Map");
  if (cache->map == NULL) {
    report_contents(cache->map, "%p");
    goto failed;
  }

  cache->native_interface =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (cache->native_interface == NULL) {
    report_contents(cache->native_interface, "%p");
    goto failed;
  }

  cache->stack_trace_element =
      bsg_safe_find_class(env, "java/lang/StackTraceElement");
  if (cache->stack_trace_element == NULL) {
    report_contents(cache->stack_trace_element, "%p");
    goto failed;
  }

  cache->severity = bsg_safe_find_class(env, "com/bugsnag/android/Severity");
  if (cache->severity == NULL) {
    report_contents(cache->severity, "%p");
    goto failed;
  }

  cache->breadcrumb_type =
      bsg_safe_find_class(env, "com/bugsnag/android/BreadcrumbType");
  if (cache->breadcrumb_type == NULL) {
    report_contents(cache->breadcrumb_type, "%p");
    goto failed;
  }

  cache->integer_int_value =
      bsg_safe_get_method_id(env, cache->integer, "intValue", "()I");
  if (cache->integer_int_value == NULL) {
    report_contents(cache->integer_int_value, "%p");
    goto failed;
  }

  cache->float_float_value =
      bsg_safe_get_method_id(env, cache->float_class, "floatValue", "()F");
  if (cache->float_float_value == NULL) {
    report_contents(cache->float_float_value, "%p");
    goto failed;
  }

  cache->number_double_value =
      bsg_safe_get_method_id(env, cache->number, "doubleValue", "()D");
  if (cache->number_double_value == NULL) {
    report_contents(cache->number_double_value, "%p");
    goto failed;
  }

  cache->long_long_value =
      bsg_safe_get_method_id(env, cache->integer, "longValue", "()J");
  if (cache->long_long_value == NULL) {
    report_contents(cache->long_long_value, "%p");
    goto failed;
  }

  cache->boolean_bool_value =
      bsg_safe_get_method_id(env, cache->boolean, "booleanValue", "()Z");
  if (cache->boolean_bool_value == NULL) {
    report_contents(cache->boolean_bool_value, "%p");
    goto failed;
  }

  cache->arraylist_init_with_obj = bsg_safe_get_method_id(
      env, cache->arraylist, "<init>", "(Ljava/util/Collection;)V");
  if (cache->arraylist_init_with_obj == NULL) {
    report_contents(cache->arraylist_init_with_obj, "%p");
    goto failed;
  }

  cache->arraylist_get = bsg_safe_get_method_id(env, cache->arraylist, "get",
                                                "(I)Ljava/lang/Object;");
  if (cache->arraylist_get == NULL) {
    report_contents(cache->arraylist_get, "%p");
    goto failed;
  }

  cache->hash_map_key_set = bsg_safe_get_method_id(
      env, cache->hash_map, "keySet", "()Ljava/util/Set;");
  if (cache->hash_map_key_set == NULL) {
    report_contents(cache->hash_map_key_set, "%p");
    goto failed;
  }

  cache->hash_map_size =
      bsg_safe_get_method_id(env, cache->hash_map, "size", "()I");
  if (cache->hash_map_size == NULL) {
    report_contents(cache->hash_map_size, "%p");
    goto failed;
  }

  cache->hash_map_get = bsg_safe_get_method_id(
      env, cache->hash_map, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
  if (cache->hash_map_get == NULL) {
    report_contents(cache->hash_map_get, "%p");
    goto failed;
  }

  cache->map_key_set =
      bsg_safe_get_method_id(env, cache->map, "keySet", "()Ljava/util/Set;");
  if (cache->map_key_set == NULL) {
    report_contents(cache->map_key_set, "%p");
    goto failed;
  }

  cache->map_size = bsg_safe_get_method_id(env, cache->map, "size", "()I");
  if (cache->map_size == NULL) {
    report_contents(cache->map_size, "%p");
    goto failed;
  }

  cache->map_get = bsg_safe_get_method_id(
      env, cache->map, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
  if (cache->map_get == NULL) {
    report_contents(cache->map_get, "%p");
    goto failed;
  }

  cache->ni_get_app = bsg_safe_get_static_method_id(
      env, cache->native_interface, "getApp", "()Ljava/util/Map;");
  if (cache->ni_get_app == NULL) {
    report_contents(cache->ni_get_app, "%p");
    goto failed;
  }

  cache->ni_get_device = bsg_safe_get_static_method_id(
      env, cache->native_interface, "getDevice", "()Ljava/util/Map;");
  if (cache->ni_get_device == NULL) {
    report_contents(cache->ni_get_device, "%p");
    goto failed;
  }

  cache->ni_get_user = bsg_safe_get_static_method_id(
      env, cache->native_interface, "getUser", "()Ljava/util/Map;");
  if (cache->ni_get_user == NULL) {
    report_contents(cache->ni_get_user, "%p");
    goto failed;
  }

  cache->ni_set_user = bsg_safe_get_static_method_id(
      env, cache->native_interface, "setUser", "([B[B[B)V");
  if (cache->ni_set_user == NULL) {
    report_contents(cache->ni_set_user, "%p");
    goto failed;
  }

  cache->ni_get_metadata = bsg_safe_get_static_method_id(
      env, cache->native_interface, "getMetadata", "()Ljava/util/Map;");
  if (cache->ni_get_metadata == NULL) {
    report_contents(cache->ni_get_metadata, "%p");
    goto failed;
  }

  // lookup NativeInterface.getContext()
  cache->ni_get_context = bsg_safe_get_static_method_id(
      env, cache->native_interface, "getContext", "()Ljava/lang/String;");
  if (cache->ni_get_context == NULL) {
    report_contents(cache->ni_get_context, "%p");
    goto failed;
  }

  cache->ni_notify = bsg_safe_get_static_method_id(
      env, cache->native_interface, "notify",
      "([B[BLcom/bugsnag/android/Severity;[Ljava/lang/StackTraceElement;)V");
  if (cache->ni_notify == NULL) {
    report_contents(cache->ni_notify, "%p");
    goto failed;
  }

  cache->ni_deliver_report = bsg_safe_get_static_method_id(
      env, cache->native_interface, "deliverReport",
      "([B[BLjava/lang/String;Z)V");
  if (cache->ni_deliver_report == NULL) {
    report_contents(cache->ni_deliver_report, "%p");
    goto failed;
  }

  cache->ni_leave_breadcrumb = bsg_safe_get_static_method_id(
      env, cache->native_interface, "leaveBreadcrumb",
      "([BLcom/bugsnag/android/BreadcrumbType;)V");
  if (cache->ni_leave_breadcrumb == NULL) {
    report_contents(cache->ni_leave_breadcrumb, "%p");
    goto failed;
  }

  cache->ste_constructor = bsg_safe_get_method_id(
      env, cache->stack_trace_element, "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
  if (cache->ste_constructor == NULL) {
    report_contents(cache->ste_constructor, "%p");
    goto failed;
  }

  return true;

failed:
  return false;
}
