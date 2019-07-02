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

bsg_jni_cache *bsg_populate_jni_cache(JNIEnv *env) {
  bsg_jni_cache *jni_cache = malloc(sizeof(bsg_jni_cache));
  jni_cache->integer = (*env)->FindClass(env, "java/lang/Integer");
  jni_cache->boolean = (*env)->FindClass(env, "java/lang/Boolean");
  jni_cache->long_class = (*env)->FindClass(env, "java/lang/Long");
  jni_cache->float_class = (*env)->FindClass(env, "java/lang/Float");
  jni_cache->number = (*env)->FindClass(env, "java/lang/Number");
  jni_cache->string = (*env)->FindClass(env, "java/lang/String");
  jni_cache->integer_int_value =
      (*env)->GetMethodID(env, jni_cache->integer, "intValue", "()I");
  jni_cache->float_float_value =
      (*env)->GetMethodID(env, jni_cache->float_class, "floatValue", "()F");
  jni_cache->number_double_value =
      (*env)->GetMethodID(env, jni_cache->number, "doubleValue", "()D");
  jni_cache->long_long_value =
      (*env)->GetMethodID(env, jni_cache->integer, "longValue", "()J");
  jni_cache->boolean_bool_value =
      (*env)->GetMethodID(env, jni_cache->boolean, "booleanValue", "()Z");
  jni_cache->arraylist = (*env)->FindClass(env, "java/util/ArrayList");
  jni_cache->arraylist_init_with_obj = (*env)->GetMethodID(
      env, jni_cache->arraylist, "<init>", "(Ljava/util/Collection;)V");
  jni_cache->arraylist_get = (*env)->GetMethodID(
      env, jni_cache->arraylist, "get", "(I)Ljava/lang/Object;");
  jni_cache->hash_map = (*env)->FindClass(env, "java/util/HashMap");
  jni_cache->map = (*env)->FindClass(env, "java/util/Map");
  jni_cache->hash_map_key_set = (*env)->GetMethodID(
      env, jni_cache->hash_map, "keySet", "()Ljava/util/Set;");
  jni_cache->hash_map_size =
      (*env)->GetMethodID(env, jni_cache->hash_map, "size", "()I");
  jni_cache->hash_map_get =
      (*env)->GetMethodID(env, jni_cache->hash_map, "get",
                          "(Ljava/lang/Object;)Ljava/lang/Object;");
  jni_cache->map_key_set =
      (*env)->GetMethodID(env, jni_cache->map, "keySet", "()Ljava/util/Set;");
  jni_cache->map_size = (*env)->GetMethodID(env, jni_cache->map, "size", "()I");
  jni_cache->map_get = (*env)->GetMethodID(
      env, jni_cache->map, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
  jni_cache->native_interface =
      (*env)->FindClass(env, "com/bugsnag/android/NativeInterface");
  jni_cache->get_app_data = (*env)->GetStaticMethodID(
      env, jni_cache->native_interface, "getAppData", "()Ljava/util/Map;");
  jni_cache->get_device_data = (*env)->GetStaticMethodID(
      env, jni_cache->native_interface, "getDeviceData", "()Ljava/util/Map;");
  jni_cache->get_user_data = (*env)->GetStaticMethodID(
      env, jni_cache->native_interface, "getUserData", "()Ljava/util/Map;");
  jni_cache->get_metadata = (*env)->GetStaticMethodID(
      env, jni_cache->native_interface, "getMetaData", "()Ljava/util/Map;");
  jni_cache->get_context = (*env)->GetStaticMethodID(
      env, jni_cache->native_interface, "getContext", "()Ljava/lang/String;");
  return jni_cache;
}

jobject bsg_get_map_value_obj(JNIEnv *env, bsg_jni_cache *jni_cache,
                              jobject map, const char *_key) {
    jstring key = (*env)->NewStringUTF(env, _key);
    jobject obj = (*env)->CallObjectMethod(env, map, jni_cache->hash_map_get, key);
    (*env)->DeleteLocalRef(env, key);
    return obj;
}

void bsg_copy_map_value_string(JNIEnv *env, bsg_jni_cache *jni_cache,
                               jobject map, const char *_key, char *dest,
                               int len) {
  jobject _value = bsg_get_map_value_obj(env, jni_cache, map, _key);

  if (_value != NULL) {
    char *value = (char *)(*env)->GetStringUTFChars(env, (jstring)_value, 0);
    bsg_strncpy_safe(dest, value, len);
    (*env)->ReleaseStringUTFChars(env, _value, value);
  }
}

long bsg_get_map_value_long(JNIEnv *env, bsg_jni_cache *jni_cache, jobject map,
                            const char *_key) {
  jobject _value = bsg_get_map_value_obj(env, jni_cache, map, _key);

  if (_value != NULL) {
    long value = (long)(*env)->CallDoubleMethod(env, _value,
                                                jni_cache->number_double_value);
    (*env)->DeleteLocalRef(env, _value);
    return value;
  }
  return 0;
}

float bsg_get_map_value_float(JNIEnv *env, bsg_jni_cache *jni_cache,
                              jobject map, const char *_key) {
  jobject _value = bsg_get_map_value_obj(env, jni_cache, map, _key);

  if (_value != NULL) {
    float value = (float)(*env)->CallFloatMethod(env, _value,
                                                 jni_cache->float_float_value);
    (*env)->DeleteLocalRef(env, _value);
    return value;
  }
  return 0;
}

int bsg_get_map_value_int(JNIEnv *env, bsg_jni_cache *jni_cache, jobject map,
                          const char *_key) {
  jobject _value = bsg_get_map_value_obj(env, jni_cache, map, _key);

  if (_value != NULL) {
    jint value =
        (int)(*env)->CallIntMethod(env, _value, jni_cache->integer_int_value);
    (*env)->DeleteLocalRef(env, _value);
    return value;
  }
  return 0;
}

bool bsg_get_map_value_bool(JNIEnv *env, bsg_jni_cache *jni_cache, jobject map,
                            const char *_key) {
  jobject obj = bsg_get_map_value_obj(env, jni_cache, map, _key);
  return (*env)->CallBooleanMethod(env, obj, jni_cache->boolean_bool_value);
}

int bsg_populate_cpu_abi_from_map(JNIEnv *env, bsg_jni_cache *jni_cache,
                                  jobject map, bsg_device_info *device) {
  jstring key = (*env)->NewStringUTF(env, "cpuAbi");
  jobjectArray _value =
      (*env)->CallObjectMethod(env, map, jni_cache->hash_map_get, key);
  if (_value != NULL) {
    int count = (*env)->GetArrayLength(env, _value);

    for (int i = 0; i < count && i < sizeof(device->cpu_abi); i++) {
      jstring abi_ = (jstring)((*env)->GetObjectArrayElement(env, _value, i));
      char *abi = (char *)(*env)->GetStringUTFChars(env, abi_, 0);
      bsg_strncpy_safe(device->cpu_abi[i].value, abi,
                       sizeof(device->cpu_abi[i].value));
      (*env)->ReleaseStringUTFChars(env, abi_, abi);
      device->cpu_abi_count++;
    }
    (*env)->DeleteLocalRef(env, _value);
    return count;
  }
  return 0;
}

void bsg_populate_crumb_metadata(JNIEnv *env, bugsnag_breadcrumb *crumb,
                                 jobject metadata) {
  bsg_jni_cache *jni_cache = bsg_populate_jni_cache(env);
  int size = (int)(*env)->CallIntMethod(env, metadata, jni_cache->map_size);
  jobject keyset =
      (*env)->CallObjectMethod(env, metadata, jni_cache->map_key_set);
  jobject keylist = (*env)->NewObject(
      env, jni_cache->arraylist, jni_cache->arraylist_init_with_obj, keyset);
  for (int i = 0; i < size && i < sizeof(crumb->metadata); i++) {
    jstring _key = (*env)->CallObjectMethod(env, keylist,
                                            jni_cache->arraylist_get, (jint)i);
    jstring _value =
        (*env)->CallObjectMethod(env, metadata, jni_cache->map_get, _key);
    if (_value == NULL) {
      (*env)->DeleteLocalRef(env, _key);
      (*env)->DeleteLocalRef(env, _value);
    } else {
      char *key = (char *)(*env)->GetStringUTFChars(env, _key, 0);
      char *value = (char *)(*env)->GetStringUTFChars(env, _value, 0);
      bsg_strncpy_safe(crumb->metadata[i].key, key,
                       sizeof(crumb->metadata[i].key));
      bsg_strncpy_safe(crumb->metadata[i].value, value,
                       sizeof(crumb->metadata[i].value));
      (*env)->ReleaseStringUTFChars(env, _key, key);
      (*env)->ReleaseStringUTFChars(env, _value, value);
    }
  }
  free(jni_cache);
  (*env)->DeleteLocalRef(env, keyset);
  (*env)->DeleteLocalRef(env, keylist);
}

char *bsg_binary_arch() {
#if defined(__i386__)
    return "x86";
#elif defined(__x86_64__)
    return "x86_64";
#elif defined(__arm__)
    return "arm32";
#elif defined(__aarch64__)
    return "arm64";
#else
    return "unknown";
#endif
}

void bsg_populate_app_data(JNIEnv *env, bsg_jni_cache *jni_cache,
                           bugsnag_report *report) {
  jobject data = (*env)->CallStaticObjectMethod(
      env, jni_cache->native_interface, jni_cache->get_app_data);
  bsg_copy_map_value_string(env, jni_cache, data, "version",
                            report->app.version, sizeof(report->app.version));
  bsg_copy_map_value_string(env, jni_cache, data, "versionName",
                            report->app.version_name,
                            sizeof(report->app.version_name));
  bsg_copy_map_value_string(env, jni_cache, data, "packageName",
                            report->app.package_name,
                            sizeof(report->app.package_name));
  bsg_copy_map_value_string(env, jni_cache, data, "releaseStage",
                            report->app.release_stage,
                            sizeof(report->app.release_stage));
  bsg_copy_map_value_string(env, jni_cache, data, "name", report->app.name,
                            sizeof(report->app.name));
  bsg_copy_map_value_string(env, jni_cache, data, "id", report->app.id,
                            sizeof(report->app.id));
  bsg_copy_map_value_string(env, jni_cache, data, "type", report->app.type,
                            sizeof(report->app.type));
  bsg_copy_map_value_string(env, jni_cache, data, "buildUUID",
                            report->app.build_uuid,
                            sizeof(report->app.build_uuid));
  report->app.duration_ms_offset =
      bsg_get_map_value_long(env, jni_cache, data, "duration");
  report->app.duration_in_foreground_ms_offset =
      bsg_get_map_value_long(env, jni_cache, data, "durationInForeground");
  report->app.version_code =
      bsg_get_map_value_int(env, jni_cache, data, "versionCode");
  report->app.in_foreground =
      bsg_get_map_value_bool(env, jni_cache, data, "inForeground");

  bsg_strncpy_safe(report->app.binaryArch,
                     bsg_binary_arch(),
                     sizeof(report->app.binaryArch));
  (*env)->DeleteLocalRef(env, data);
}

void bsg_populate_device_data(JNIEnv *env, bsg_jni_cache *jni_cache,
                              bugsnag_report *report) {
  jobject data = (*env)->CallStaticObjectMethod(
      env, jni_cache->native_interface, jni_cache->get_device_data);
  bsg_copy_map_value_string(env, jni_cache, data, "manufacturer",
                            report->device.manufacturer,
                            sizeof(report->device.manufacturer));
  bsg_copy_map_value_string(env, jni_cache, data, "model", report->device.model,
                            sizeof(report->device.model));
  bsg_copy_map_value_string(env, jni_cache, data, "brand", report->device.brand,
                            sizeof(report->device.brand));
  bsg_copy_map_value_string(env, jni_cache, data, "orientation",
                            report->device.orientation,
                            sizeof(report->device.orientation));
  bsg_copy_map_value_string(env, jni_cache, data, "id", report->device.id,
                            sizeof(report->device.id));
  bsg_copy_map_value_string(env, jni_cache, data, "locale",
                            report->device.locale,
                            sizeof(report->device.locale));
  bsg_copy_map_value_string(env, jni_cache, data, "locationStatus",
                            report->device.location_status,
                            sizeof(report->device.location_status));
  bsg_copy_map_value_string(env, jni_cache, data, "networkAccess",
                            report->device.network_access,
                            sizeof(report->device.network_access));
  bsg_copy_map_value_string(env, jni_cache, data, "osVersion",
                            report->device.os_version,
                            sizeof(report->device.os_version));
  bsg_copy_map_value_string(env, jni_cache, data, "screenResolution",
                            report->device.screen_resolution,
                            sizeof(report->device.screen_resolution));
  report->device.emulator =
      bsg_get_map_value_bool(env, jni_cache, data, "emulator");
  report->device.jailbroken =
      bsg_get_map_value_bool(env, jni_cache, data, "jailbroken");
  report->device.total_memory =
      bsg_get_map_value_long(env, jni_cache, data, "totalMemory");
  report->device.dpi = bsg_get_map_value_int(env, jni_cache, data, "dpi");
  report->device.screen_density =
      bsg_get_map_value_float(env, jni_cache, data, "screenDensity");
  bsg_populate_cpu_abi_from_map(env, jni_cache, data, &report->device);

  jobject _runtime_versions = bsg_get_map_value_obj(env, jni_cache, data, "runtimeVersions");

  if (_runtime_versions != NULL) {
    bsg_copy_map_value_string(env, jni_cache, _runtime_versions, "osBuild",
                              report->device.os_build,
                              sizeof(report->device.os_build));

    report->device.api_level = bsg_get_map_value_int(env, jni_cache, _runtime_versions, "androidApiLevel");
    (*env)->DeleteLocalRef(env, _runtime_versions);
  }

  (*env)->DeleteLocalRef(env, data);
}

void bsg_populate_user_data(JNIEnv *env, bsg_jni_cache *jni_cache,
                            bugsnag_report *report) {
  jobject data = (*env)->CallStaticObjectMethod(
      env, jni_cache->native_interface, jni_cache->get_user_data);
  bsg_copy_map_value_string(env, jni_cache, data, "id", report->user.id,
                            sizeof(report->user.id));
  bsg_copy_map_value_string(env, jni_cache, data, "name", report->user.name,
                            sizeof(report->user.name));
  bsg_copy_map_value_string(env, jni_cache, data, "email", report->user.email,
                            sizeof(report->user.email));
  (*env)->DeleteLocalRef(env, data);
}
void bsg_populate_context(JNIEnv *env, bsg_jni_cache *jni_cache,
                          bugsnag_report *report) {
  jstring _context = (*env)->CallStaticObjectMethod(
      env, jni_cache->native_interface, jni_cache->get_context);
  if (_context != NULL) {
    const char *value = (*env)->GetStringUTFChars(env, (jstring)_context, 0);
    strncpy(report->context, value, sizeof(report->context) - 1);
    (*env)->ReleaseStringUTFChars(env, _context, value);
  } else {
    memset(&report->context, 0, strlen(report->context));
  }
}
void bsg_populate_report(JNIEnv *env, bugsnag_report *report) {
  bsg_jni_cache *jni_cache = bsg_populate_jni_cache(env);
  bsg_populate_context(env, jni_cache, report);
  bsg_populate_app_data(env, jni_cache, report);
  bsg_populate_device_data(env, jni_cache, report);
  bsg_populate_user_data(env, jni_cache, report);
  free(jni_cache);
}
void bsg_populate_metadata(JNIEnv *env, bugsnag_report *report,
                           jobject metadata) {
  bsg_jni_cache *jni_cache = bsg_populate_jni_cache(env);
  if (metadata == NULL) {
    metadata = (*env)->CallStaticObjectMethod(env, jni_cache->native_interface,
                                              jni_cache->get_metadata);
  }
  if (metadata != NULL) {
    int size = (int)(*env)->CallIntMethod(env, metadata, jni_cache->map_size);
    jobject keyset =
        (*env)->CallObjectMethod(env, metadata, jni_cache->map_key_set);
    jobject keylist = (*env)->NewObject(
        env, jni_cache->arraylist, jni_cache->arraylist_init_with_obj, keyset);
    for (int i = 0; i < size; i++) {
      jstring _key = (*env)->CallObjectMethod(
          env, keylist, jni_cache->arraylist_get, (jint)i);
      char *section = (char *)(*env)->GetStringUTFChars(env, _key, 0);
      jobject _section =
          (*env)->CallObjectMethod(env, metadata, jni_cache->map_get, _key);
      int section_size =
          (int)(*env)->CallIntMethod(env, _section, jni_cache->map_size);
      jobject section_keyset =
          (*env)->CallObjectMethod(env, _section, jni_cache->map_key_set);
      jobject section_keylist =
          (*env)->NewObject(env, jni_cache->arraylist,
                            jni_cache->arraylist_init_with_obj, section_keyset);
      for (int j = 0; j < section_size; j++) {
        jstring section_key = (*env)->CallObjectMethod(
            env, section_keylist, jni_cache->arraylist_get, (jint)j);
        char *name = (char *)(*env)->GetStringUTFChars(env, section_key, 0);
        jobject _value = (*env)->CallObjectMethod(
            env, section, jni_cache->map_get, section_key);
        if ((*env)->IsInstanceOf(env, _value, jni_cache->number)) {
          double value = (*env)->CallDoubleMethod(
              env, _value, jni_cache->number_double_value);
          bugsnag_report_add_metadata_double(report, section, name, value);
        } else if ((*env)->IsInstanceOf(env, _value, jni_cache->boolean)) {
          bool value = (*env)->CallBooleanMethod(env, _value,
                                                 jni_cache->boolean_bool_value);
          bugsnag_report_add_metadata_bool(report, section, name, value);
        } else if ((*env)->IsInstanceOf(env, _value, jni_cache->string)) {
          char *value = (char *)(*env)->GetStringUTFChars(env, _value, 0);
          bugsnag_report_add_metadata_string(report, section, name, value);
          free(value);
        }
        (*env)->ReleaseStringUTFChars(env, section_key, name);
        (*env)->DeleteLocalRef(env, _value);
      }
      (*env)->ReleaseStringUTFChars(env, _key, section);
      (*env)->DeleteLocalRef(env, section_keyset);
      (*env)->DeleteLocalRef(env, section_keylist);
      (*env)->DeleteLocalRef(env, _section);
    }
    (*env)->DeleteLocalRef(env, keyset);
    (*env)->DeleteLocalRef(env, keylist);
  } else {
    report->metadata.value_count = 0;
  }
  free(jni_cache);
}
