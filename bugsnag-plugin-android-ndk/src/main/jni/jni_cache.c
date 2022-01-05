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

static bsg_jni_cache global_jni_cache;
bsg_jni_cache *bsg_global_jni_cache = &global_jni_cache;

bool bsg_jni_cache_refresh(JNIEnv *env) {
  if (bsg_global_jni_cache == NULL) {
    return false;
  }

  // All objects here are local references, and MUST be refreshed on every
  // transition from Java to native.

  // Classes

  bsg_global_jni_cache->integer = bsg_safe_find_class(env, "java/lang/Integer");
  if (bsg_global_jni_cache->integer == NULL) {
    report_contents(bsg_global_jni_cache->integer, "%p");
    goto failed;
  }

  bsg_global_jni_cache->boolean = bsg_safe_find_class(env, "java/lang/Boolean");
  if (bsg_global_jni_cache->boolean == NULL) {
    report_contents(bsg_global_jni_cache->boolean, "%p");
    goto failed;
  }

  bsg_global_jni_cache->long_class = bsg_safe_find_class(env, "java/lang/Long");
  if (bsg_global_jni_cache->long_class == NULL) {
    report_contents(bsg_global_jni_cache->long_class, "%p");
    goto failed;
  }

  bsg_global_jni_cache->float_class =
      bsg_safe_find_class(env, "java/lang/Float");
  if (bsg_global_jni_cache->float_class == NULL) {
    report_contents(bsg_global_jni_cache->float_class, "%p");
    goto failed;
  }

  bsg_global_jni_cache->number = bsg_safe_find_class(env, "java/lang/Number");
  if (bsg_global_jni_cache->number == NULL) {
    report_contents(bsg_global_jni_cache->number, "%p");
    goto failed;
  }

  bsg_global_jni_cache->string = bsg_safe_find_class(env, "java/lang/String");
  if (bsg_global_jni_cache->string == NULL) {
    report_contents(bsg_global_jni_cache->string, "%p");
    goto failed;
  }

  // Methods

  bsg_global_jni_cache->arraylist =
      bsg_safe_find_class(env, "java/util/ArrayList");
  if (bsg_global_jni_cache->arraylist == NULL) {
    report_contents(bsg_global_jni_cache->arraylist, "%p");
    goto failed;
  }

  bsg_global_jni_cache->hash_map =
      bsg_safe_find_class(env, "java/util/HashMap");
  if (bsg_global_jni_cache->hash_map == NULL) {
    report_contents(bsg_global_jni_cache->hash_map, "%p");
    goto failed;
  }

  bsg_global_jni_cache->map = bsg_safe_find_class(env, "java/util/Map");
  if (bsg_global_jni_cache->map == NULL) {
    report_contents(bsg_global_jni_cache->map, "%p");
    goto failed;
  }

  bsg_global_jni_cache->native_interface =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (bsg_global_jni_cache->native_interface == NULL) {
    report_contents(bsg_global_jni_cache->native_interface, "%p");
    goto failed;
  }

  bsg_global_jni_cache->stack_trace_element =
      bsg_safe_find_class(env, "java/lang/StackTraceElement");
  if (bsg_global_jni_cache->stack_trace_element == NULL) {
    report_contents(bsg_global_jni_cache->stack_trace_element, "%p");
    goto failed;
  }

  bsg_global_jni_cache->severity =
      bsg_safe_find_class(env, "com/bugsnag/android/Severity");
  if (bsg_global_jni_cache->severity == NULL) {
    report_contents(bsg_global_jni_cache->severity, "%p");
    goto failed;
  }

  bsg_global_jni_cache->breadcrumb_type =
      bsg_safe_find_class(env, "com/bugsnag/android/BreadcrumbType");
  if (bsg_global_jni_cache->breadcrumb_type == NULL) {
    report_contents(bsg_global_jni_cache->breadcrumb_type, "%p");
    goto failed;
  }

  bsg_global_jni_cache->integer_int_value = bsg_safe_get_method_id(
      env, bsg_global_jni_cache->integer, "intValue", "()I");
  if (bsg_global_jni_cache->integer_int_value == NULL) {
    report_contents(bsg_global_jni_cache->integer_int_value, "%p");
    goto failed;
  }

  bsg_global_jni_cache->float_float_value = bsg_safe_get_method_id(
      env, bsg_global_jni_cache->float_class, "floatValue", "()F");
  if (bsg_global_jni_cache->float_float_value == NULL) {
    report_contents(bsg_global_jni_cache->float_float_value, "%p");
    goto failed;
  }

  bsg_global_jni_cache->number_double_value = bsg_safe_get_method_id(
      env, bsg_global_jni_cache->number, "doubleValue", "()D");
  if (bsg_global_jni_cache->number_double_value == NULL) {
    report_contents(bsg_global_jni_cache->number_double_value, "%p");
    goto failed;
  }

  bsg_global_jni_cache->long_long_value = bsg_safe_get_method_id(
      env, bsg_global_jni_cache->integer, "longValue", "()J");
  if (bsg_global_jni_cache->long_long_value == NULL) {
    report_contents(bsg_global_jni_cache->long_long_value, "%p");
    goto failed;
  }

  bsg_global_jni_cache->boolean_bool_value = bsg_safe_get_method_id(
      env, bsg_global_jni_cache->boolean, "booleanValue", "()Z");
  if (bsg_global_jni_cache->boolean_bool_value == NULL) {
    report_contents(bsg_global_jni_cache->boolean_bool_value, "%p");
    goto failed;
  }

  bsg_global_jni_cache->arraylist_init_with_obj =
      bsg_safe_get_method_id(env, bsg_global_jni_cache->arraylist, "<init>",
                             "(Ljava/util/Collection;)V");
  if (bsg_global_jni_cache->arraylist_init_with_obj == NULL) {
    report_contents(bsg_global_jni_cache->arraylist_init_with_obj, "%p");
    goto failed;
  }

  bsg_global_jni_cache->arraylist_get = bsg_safe_get_method_id(
      env, bsg_global_jni_cache->arraylist, "get", "(I)Ljava/lang/Object;");
  if (bsg_global_jni_cache->arraylist_get == NULL) {
    report_contents(bsg_global_jni_cache->arraylist_get, "%p");
    goto failed;
  }

  bsg_global_jni_cache->hash_map_key_set = bsg_safe_get_method_id(
      env, bsg_global_jni_cache->hash_map, "keySet", "()Ljava/util/Set;");
  if (bsg_global_jni_cache->hash_map_key_set == NULL) {
    report_contents(bsg_global_jni_cache->hash_map_key_set, "%p");
    goto failed;
  }

  bsg_global_jni_cache->hash_map_size = bsg_safe_get_method_id(
      env, bsg_global_jni_cache->hash_map, "size", "()I");
  if (bsg_global_jni_cache->hash_map_size == NULL) {
    report_contents(bsg_global_jni_cache->hash_map_size, "%p");
    goto failed;
  }

  bsg_global_jni_cache->hash_map_get =
      bsg_safe_get_method_id(env, bsg_global_jni_cache->hash_map, "get",
                             "(Ljava/lang/Object;)Ljava/lang/Object;");
  if (bsg_global_jni_cache->hash_map_get == NULL) {
    report_contents(bsg_global_jni_cache->hash_map_get, "%p");
    goto failed;
  }

  bsg_global_jni_cache->map_key_set = bsg_safe_get_method_id(
      env, bsg_global_jni_cache->map, "keySet", "()Ljava/util/Set;");
  if (bsg_global_jni_cache->map_key_set == NULL) {
    report_contents(bsg_global_jni_cache->map_key_set, "%p");
    goto failed;
  }

  bsg_global_jni_cache->map_size =
      bsg_safe_get_method_id(env, bsg_global_jni_cache->map, "size", "()I");
  if (bsg_global_jni_cache->map_size == NULL) {
    report_contents(bsg_global_jni_cache->map_size, "%p");
    goto failed;
  }

  bsg_global_jni_cache->map_get =
      bsg_safe_get_method_id(env, bsg_global_jni_cache->map, "get",
                             "(Ljava/lang/Object;)Ljava/lang/Object;");
  if (bsg_global_jni_cache->map_get == NULL) {
    report_contents(bsg_global_jni_cache->map_get, "%p");
    goto failed;
  }

  bsg_global_jni_cache->ni_get_app =
      bsg_safe_get_static_method_id(env, bsg_global_jni_cache->native_interface,
                                    "getApp", "()Ljava/util/Map;");
  if (bsg_global_jni_cache->ni_get_app == NULL) {
    report_contents(bsg_global_jni_cache->ni_get_app, "%p");
    goto failed;
  }

  bsg_global_jni_cache->ni_get_device =
      bsg_safe_get_static_method_id(env, bsg_global_jni_cache->native_interface,
                                    "getDevice", "()Ljava/util/Map;");
  if (bsg_global_jni_cache->ni_get_device == NULL) {
    report_contents(bsg_global_jni_cache->ni_get_device, "%p");
    goto failed;
  }

  bsg_global_jni_cache->ni_get_user =
      bsg_safe_get_static_method_id(env, bsg_global_jni_cache->native_interface,
                                    "getUser", "()Ljava/util/Map;");
  if (bsg_global_jni_cache->ni_get_user == NULL) {
    report_contents(bsg_global_jni_cache->ni_get_user, "%p");
    goto failed;
  }

  bsg_global_jni_cache->ni_set_user = bsg_safe_get_static_method_id(
      env, bsg_global_jni_cache->native_interface, "setUser", "([B[B[B)V");
  if (bsg_global_jni_cache->ni_set_user == NULL) {
    report_contents(bsg_global_jni_cache->ni_set_user, "%p");
    goto failed;
  }

  bsg_global_jni_cache->ni_get_metadata =
      bsg_safe_get_static_method_id(env, bsg_global_jni_cache->native_interface,
                                    "getMetadata", "()Ljava/util/Map;");
  if (bsg_global_jni_cache->ni_get_metadata == NULL) {
    report_contents(bsg_global_jni_cache->ni_get_metadata, "%p");
    goto failed;
  }

  // lookup NativeInterface.getContext()
  bsg_global_jni_cache->ni_get_context =
      bsg_safe_get_static_method_id(env, bsg_global_jni_cache->native_interface,
                                    "getContext", "()Ljava/lang/String;");
  if (bsg_global_jni_cache->ni_get_context == NULL) {
    report_contents(bsg_global_jni_cache->ni_get_context, "%p");
    goto failed;
  }

  bsg_global_jni_cache->ni_notify = bsg_safe_get_static_method_id(
      env, bsg_global_jni_cache->native_interface, "notify",
      "([B[BLcom/bugsnag/android/Severity;[Ljava/lang/StackTraceElement;)V");
  if (bsg_global_jni_cache->ni_notify == NULL) {
    report_contents(bsg_global_jni_cache->ni_notify, "%p");
    goto failed;
  }

  bsg_global_jni_cache->ni_deliver_report = bsg_safe_get_static_method_id(
      env, bsg_global_jni_cache->native_interface, "deliverReport",
      "([B[BLjava/lang/String;Z)V");
  if (bsg_global_jni_cache->ni_deliver_report == NULL) {
    report_contents(bsg_global_jni_cache->ni_deliver_report, "%p");
    goto failed;
  }

  bsg_global_jni_cache->ni_leave_breadcrumb = bsg_safe_get_static_method_id(
      env, bsg_global_jni_cache->native_interface, "leaveBreadcrumb",
      "([BLcom/bugsnag/android/BreadcrumbType;)V");
  if (bsg_global_jni_cache->ni_leave_breadcrumb == NULL) {
    report_contents(bsg_global_jni_cache->ni_leave_breadcrumb, "%p");
    goto failed;
  }

  bsg_global_jni_cache->ste_constructor = bsg_safe_get_method_id(
      env, bsg_global_jni_cache->stack_trace_element, "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
  if (bsg_global_jni_cache->ste_constructor == NULL) {
    report_contents(bsg_global_jni_cache->ste_constructor, "%p");
    goto failed;
  }

  return true;

failed:
  return false;
}
