#include "metadata.h"
#include "report.h"
#include "utils/string.h"
#include <malloc.h>
#include <string.h>

typedef struct {
  jclass hash_map;
  jclass map;
  jclass arraylist;
  jclass integer;
  jclass boolean;
  jclass metadata;
  jclass native_interface;
  jmethodID integer_int_value;
  jmethodID boolean_bool_value;
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

static bsg_jni_cache bsg_global_jni_cache;

void bsg_populate_jni_cache(JNIEnv *env) {
  bsg_global_jni_cache.integer = (*env)->FindClass(env, "java/lang/Integer");
  bsg_global_jni_cache.boolean = (*env)->FindClass(env, "java/lang/Boolean");
  bsg_global_jni_cache.integer_int_value =
      (*env)->GetMethodID(env, bsg_global_jni_cache.integer, "intValue", "()I");
  bsg_global_jni_cache.boolean_bool_value = (*env)->GetMethodID(
      env, bsg_global_jni_cache.boolean, "booleanValue", "()Z");
  bsg_global_jni_cache.arraylist =
      (*env)->FindClass(env, "java/util/ArrayList");
  bsg_global_jni_cache.arraylist_init_with_obj =
      (*env)->GetMethodID(env, bsg_global_jni_cache.arraylist, "<init>",
                          "(Ljava/util/Collection;)V");
  bsg_global_jni_cache.arraylist_get = (*env)->GetMethodID(
      env, bsg_global_jni_cache.arraylist, "get", "(I)Ljava/lang/Object;");
  bsg_global_jni_cache.hash_map = (*env)->FindClass(env, "java/util/HashMap");
  bsg_global_jni_cache.map = (*env)->FindClass(env, "java/util/Map");
  bsg_global_jni_cache.hash_map_key_set = (*env)->GetMethodID(
      env, bsg_global_jni_cache.hash_map, "keySet", "()Ljava/util/Set;");
  bsg_global_jni_cache.hash_map_size =
      (*env)->GetMethodID(env, bsg_global_jni_cache.hash_map, "size", "()I");
  bsg_global_jni_cache.hash_map_get =
      (*env)->GetMethodID(env, bsg_global_jni_cache.hash_map, "get",
                          "(Ljava/lang/Object;)Ljava/lang/Object;");
    bsg_global_jni_cache.map_key_set = (*env)->GetMethodID(
      env, bsg_global_jni_cache.map, "keySet", "()Ljava/util/Set;");
  bsg_global_jni_cache.map_size =
      (*env)->GetMethodID(env, bsg_global_jni_cache.map, "size", "()I");
  bsg_global_jni_cache.map_get =
      (*env)->GetMethodID(env, bsg_global_jni_cache.map, "get",
                          "(Ljava/lang/Object;)Ljava/lang/Object;");
  bsg_global_jni_cache.native_interface =
      (*env)->FindClass(env, "com/bugsnag/android/NativeInterface");
  bsg_global_jni_cache.get_app_data =
      (*env)->GetStaticMethodID(env, bsg_global_jni_cache.native_interface,
                                "getAppData", "()Ljava/util/Map;");
  bsg_global_jni_cache.get_device_data =
      (*env)->GetStaticMethodID(env, bsg_global_jni_cache.native_interface,
                                "getDeviceData", "()Ljava/util/Map;");
  bsg_global_jni_cache.get_user_data =
      (*env)->GetStaticMethodID(env, bsg_global_jni_cache.native_interface,
                                "getUserData", "()Ljava/util/Map;");
  bsg_global_jni_cache.get_context =
      (*env)->GetStaticMethodID(env, bsg_global_jni_cache.native_interface,
                                "getContext", "()Ljava/lang/String;");
}

void bsg_copy_map_value_string(JNIEnv *env, jobject map, const char *_key,
                               char *dest) {
  jstring key = (*env)->NewStringUTF(env, _key);
  jobject _value = (*env)->CallObjectMethod(
      env, map, bsg_global_jni_cache.hash_map_get, key);
  if (_value != NULL) {
    const char *value = (*env)->GetStringUTFChars(env, (jstring)_value, 0);
    strcpy(dest, value);
    (*env)->ReleaseStringUTFChars(env, _value, value);
  }
}

int bsg_get_map_value_int(JNIEnv *env, jobject map, const char *_key) {
  jstring key = (*env)->NewStringUTF(env, _key);
  jobject _value = (*env)->CallObjectMethod(
      env, map, bsg_global_jni_cache.hash_map_get, key);
  if (_value != NULL) {
    jint value = (int)(*env)->CallIntMethod(
        env, _value, bsg_global_jni_cache.integer_int_value);
    (*env)->DeleteLocalRef(env, _value);
    return value;
  }
    return 0;
}

bool bsg_get_map_value_bool(JNIEnv *env, jobject map, const char *_key) {
  jstring key = (*env)->NewStringUTF(env, _key);
  return (bool)(*env)->CallObjectMethod(
      env, map, bsg_global_jni_cache.hash_map_get, key);
}

void bsg_populate_crumb_metadata(JNIEnv *env, bugsnag_breadcrumb *crumb,
                                 jobject metadata) {
    bsg_populate_jni_cache(env);
  int size = (int)(*env)->CallIntMethod(env, metadata,
                                           bsg_global_jni_cache.map_size);
  jobject keyset = (*env)->CallObjectMethod(
      env, metadata, bsg_global_jni_cache.map_key_set);
  jobject keylist =
      (*env)->NewObject(env, bsg_global_jni_cache.arraylist,
                        bsg_global_jni_cache.arraylist_init_with_obj, keyset);
  for (int i = 0; i < size && i < sizeof(crumb->metadata); i++) {
    jstring _key = (*env)->CallObjectMethod(
        env, keylist, bsg_global_jni_cache.arraylist_get, (jint)i);
    jstring _value = (*env)->CallObjectMethod(
        env, metadata, bsg_global_jni_cache.map_get, _key);
    char *key = (char *)(*env)->GetStringUTFChars(env, _key, 0);
    char *value = (char *)(*env)->GetStringUTFChars(env, _value, 0);
    bsg_strncpy_safe(crumb->metadata[i].key, key,
                     sizeof(crumb->metadata[i].key));
    bsg_strncpy_safe(crumb->metadata[i].value, value,
                     sizeof(crumb->metadata[i].value));
    (*env)->ReleaseStringUTFChars(env, _key, key);
    (*env)->ReleaseStringUTFChars(env, _value, value);
  }
  (*env)->DeleteLocalRef(env, keyset);
  (*env)->DeleteLocalRef(env, keylist);
}

void bsg_populate_app_data(JNIEnv *env, bugsnag_report *report) {
  jobject data =
      (*env)->CallStaticObjectMethod(env, bsg_global_jni_cache.native_interface,
                                     bsg_global_jni_cache.get_app_data);
  bsg_copy_map_value_string(env, data, "version", report->app.version);
  bsg_copy_map_value_string(env, data, "versionName", report->app.version_name);
  bsg_copy_map_value_string(env, data, "packageName", report->app.package_name);
  bsg_copy_map_value_string(env, data, "releaseStage",
                            report->app.release_stage);
  bsg_copy_map_value_string(env, data, "name", report->app.name);
  bsg_copy_map_value_string(env, data, "id", report->app.id);
  bsg_copy_map_value_string(env, data, "buildUUID", report->app.build_uuid);
  report->app.version_code = bsg_get_map_value_int(env, data, "versionCode");
  report->app.in_foreground = bsg_get_map_value_bool(env, data, "inForeground");

  (*env)->DeleteLocalRef(env, data);
}

void bsg_populate_device_data(JNIEnv *env, bugsnag_report *report) {
  jobject data =
      (*env)->CallStaticObjectMethod(env, bsg_global_jni_cache.native_interface,
                                     bsg_global_jni_cache.get_device_data);
  bsg_copy_map_value_string(env, data, "manufacturer",
                            report->device.manufacturer);
  bsg_copy_map_value_string(env, data, "model", report->device.model);
  bsg_copy_map_value_string(env, data, "brand", report->device.brand);
  bsg_copy_map_value_string(env, data, "orientation",
                            report->device.orientation);
  bsg_copy_map_value_string(env, data, "id", report->device.id);
  bsg_copy_map_value_string(env, data, "locale", report->device.locale);
  bsg_copy_map_value_string(env, data, "locationStatus",
                            report->device.location_status);
  bsg_copy_map_value_string(env, data, "networkAccess",
                            report->device.network_access);
  bsg_copy_map_value_string(env, data, "osBuild", report->device.os_build);
  bsg_copy_map_value_string(env, data, "osVersion", report->device.os_version);
  bsg_copy_map_value_string(env, data, "screenResolution",
                            report->device.screen_resolution);
  // total_memory
  // cpu_abi, cpu_abi_count
  // jailbroken
  // api_level
  // dpi
  // screen_density
  // free_memory, free_disk?
  (*env)->DeleteLocalRef(env, data);
}

void bsg_populate_user_data(JNIEnv *env, bugsnag_report *report) {
  jobject data =
      (*env)->CallStaticObjectMethod(env, bsg_global_jni_cache.native_interface,
                                     bsg_global_jni_cache.get_user_data);
  bsg_copy_map_value_string(env, data, "id", report->user.id);
  bsg_copy_map_value_string(env, data, "name", report->user.name);
  bsg_copy_map_value_string(env, data, "email", report->user.email);
  (*env)->DeleteLocalRef(env, data);
}
void bsg_populate_context(JNIEnv *env, bugsnag_report *report) {
  jstring _context =
      (*env)->CallStaticObjectMethod(env, bsg_global_jni_cache.native_interface,
                                     bsg_global_jni_cache.get_context);
  if (_context != NULL) {
    const char *value = (*env)->GetStringUTFChars(env, (jstring)_context, 0);
    strncpy(report->context, value, sizeof(report->context) - 1);
    (*env)->ReleaseStringUTFChars(env, _context, value);
  } else {
    memset(&report->context, 0, strlen(report->context));
  }
}
void bsg_populate_metadata(JNIEnv *env, bugsnag_report *report) {

}
