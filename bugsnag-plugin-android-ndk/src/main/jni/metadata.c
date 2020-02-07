#include "metadata.h"
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
      env, jni_cache->native_interface, "getApp", "()Ljava/util/Map;");
  jni_cache->get_device_data = (*env)->GetStaticMethodID(
      env, jni_cache->native_interface, "getDevice", "()Ljava/util/Map;");
  jni_cache->get_user_data = (*env)->GetStaticMethodID(
      env, jni_cache->native_interface, "getUser", "()Ljava/util/Map;");
  jni_cache->get_metadata = (*env)->GetStaticMethodID(
      env, jni_cache->native_interface, "getMetadata", "()Ljava/util/Map;");
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
  if (metadata == NULL) {
    return;
  }

  bsg_jni_cache *jni_cache = bsg_populate_jni_cache(env);
  int map_size = (int)(*env)->CallIntMethod(env, metadata, jni_cache->map_size);
  jobject keyset =
      (*env)->CallObjectMethod(env, metadata, jni_cache->map_key_set);
  jobject keylist = (*env)->NewObject(
      env, jni_cache->arraylist, jni_cache->arraylist_init_with_obj, keyset);
  size_t metadata_size = sizeof(crumb->metadata) / sizeof(bsg_char_metadata_pair);

  for (int i = 0; i < map_size && i < metadata_size; i++) {
    jstring _key = (*env)->CallObjectMethod(env, keylist,
                                            jni_cache->arraylist_get, (jint)i);
    jstring _value =
        (*env)->CallObjectMethod(env, metadata, jni_cache->map_get, _key);
    if (_key == NULL || _value == NULL) {
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
                           bugsnag_event *event) {
  jobject data = (*env)->CallStaticObjectMethod(
      env, jni_cache->native_interface, jni_cache->get_app_data);
  bsg_copy_map_value_string(env, jni_cache, data, "version",
                            event->app.version, sizeof(event->app.version));
  bsg_copy_map_value_string(env, jni_cache, data, "releaseStage",
                            event->app.release_stage,
                            sizeof(event->app.release_stage));
  bsg_copy_map_value_string(env, jni_cache, data, "id", event->app.id,
                            sizeof(event->app.id));
  bsg_copy_map_value_string(env, jni_cache, data, "type", event->app.type,
                            sizeof(event->app.type));
  bsg_copy_map_value_string(env, jni_cache, data, "buildUUID",
                            event->app.build_uuid,
                            sizeof(event->app.build_uuid));
  event->app.duration_ms_offset =
      bsg_get_map_value_long(env, jni_cache, data, "duration");
  event->app.duration_in_foreground_ms_offset =
      bsg_get_map_value_long(env, jni_cache, data, "durationInForeground");
  event->app.version_code =
      bsg_get_map_value_int(env, jni_cache, data, "versionCode");
  event->app.in_foreground =
      bsg_get_map_value_bool(env, jni_cache, data, "inForeground");

  bsg_strncpy_safe(event->app.binary_arch,
                     bsg_binary_arch(),
                     sizeof(event->app.binary_arch));

  char name[64];
  bsg_copy_map_value_string(env, jni_cache, data, "name", name, sizeof(name));
  bugsnag_event_add_metadata_string(event, "app", "name", name);

  (*env)->DeleteLocalRef(env, data);
}

char *bsg_os_name() {
  return "android";
}

void bsg_populate_device_data(JNIEnv *env, bsg_jni_cache *jni_cache,
                              bugsnag_event *event) {
  jobject data = (*env)->CallStaticObjectMethod(
      env, jni_cache->native_interface, jni_cache->get_device_data);
  bsg_copy_map_value_string(env, jni_cache, data, "manufacturer",
                            event->device.manufacturer,
                            sizeof(event->device.manufacturer));
  bsg_copy_map_value_string(env, jni_cache, data, "model", event->device.model,
                            sizeof(event->device.model));
  bsg_copy_map_value_string(env, jni_cache, data, "orientation",
                            event->device.orientation,
                            sizeof(event->device.orientation));
  bsg_copy_map_value_string(env, jni_cache, data, "id", event->device.id,
                            sizeof(event->device.id));
  bsg_copy_map_value_string(env, jni_cache, data, "locale",
                            event->device.locale,
                            sizeof(event->device.locale));

  bsg_copy_map_value_string(env, jni_cache, data, "osVersion",
                            event->device.os_version,
                            sizeof(event->device.os_version));

  bsg_strcpy(event->device.os_name, bsg_os_name());
  event->device.jailbroken =
      bsg_get_map_value_bool(env, jni_cache, data, "jailbroken");
  event->device.total_memory =
      bsg_get_map_value_long(env, jni_cache, data, "totalMemory");

  bsg_populate_cpu_abi_from_map(env, jni_cache, data, &event->device);

  jobject _runtime_versions = bsg_get_map_value_obj(env, jni_cache, data, "runtimeVersions");

  if (_runtime_versions != NULL) {
    bsg_copy_map_value_string(env, jni_cache, _runtime_versions, "osBuild",
                              event->device.os_build,
                              sizeof(event->device.os_build));

    event->device.api_level = bsg_get_map_value_int(env, jni_cache, _runtime_versions, "androidApiLevel");
    (*env)->DeleteLocalRef(env, _runtime_versions);
  }

  bugsnag_event_add_metadata_bool(event, "device", "emulator", bsg_get_map_value_bool(env, jni_cache, data, "emulator"));
  bugsnag_event_add_metadata_double(event, "device", "dpi", bsg_get_map_value_int(env, jni_cache, data, "dpi"));
  bugsnag_event_add_metadata_double(event, "device", "screenDensity", bsg_get_map_value_float(env, jni_cache, data, "screenDensity"));

  char location_status[32];
  bsg_copy_map_value_string(env, jni_cache, data, "locationStatus", location_status, sizeof(location_status));
  bugsnag_event_add_metadata_string(event, "device", "locationStatus", location_status);

  char brand[64];
  bsg_copy_map_value_string(env, jni_cache, data, "brand", brand, sizeof(brand));
  bugsnag_event_add_metadata_string(event, "device", "brand", brand);

  char network_access[64];
  bsg_copy_map_value_string(env, jni_cache, data, "networkAccess", network_access, sizeof(network_access));
  bugsnag_event_add_metadata_string(event, "device", "networkAccess", network_access);

  char screen_resolution[32];
  bsg_copy_map_value_string(env, jni_cache, data, "screenResolution", screen_resolution, sizeof(screen_resolution));
  bugsnag_event_add_metadata_string(event, "device", "screenResolution", screen_resolution);

  (*env)->DeleteLocalRef(env, data);
}

void bsg_populate_user_data(JNIEnv *env, bsg_jni_cache *jni_cache,
                            bugsnag_event *event) {
  jobject data = (*env)->CallStaticObjectMethod(
      env, jni_cache->native_interface, jni_cache->get_user_data);
  bsg_copy_map_value_string(env, jni_cache, data, "id", event->user.id,
                            sizeof(event->user.id));
  bsg_copy_map_value_string(env, jni_cache, data, "name", event->user.name,
                            sizeof(event->user.name));
  bsg_copy_map_value_string(env, jni_cache, data, "email", event->user.email,
                            sizeof(event->user.email));
  (*env)->DeleteLocalRef(env, data);
}

void bsg_populate_context(JNIEnv *env, bsg_jni_cache *jni_cache,
                          bugsnag_event *event) {
  jstring _context = (*env)->CallStaticObjectMethod(
      env, jni_cache->native_interface, jni_cache->get_context);
  if (_context != NULL) {
    const char *value = (*env)->GetStringUTFChars(env, (jstring)_context, 0);
    strncpy(event->context, value, sizeof(event->context) - 1);
    (*env)->ReleaseStringUTFChars(env, _context, value);
  } else {
    memset(&event->context, 0, strlen(event->context));
  }
}

void bsg_populate_event(JNIEnv *env, bugsnag_event *event) {
  bsg_jni_cache *jni_cache = bsg_populate_jni_cache(env);
  bsg_populate_context(env, jni_cache, event);
  bsg_populate_app_data(env, jni_cache, event);
  bsg_populate_device_data(env, jni_cache, event);
  bsg_populate_user_data(env, jni_cache, event);
  free(jni_cache);
}

void bsg_populate_metadata(JNIEnv *env, bugsnag_event *event,
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
          bugsnag_event_add_metadata_double(event, section, name, value);
        } else if ((*env)->IsInstanceOf(env, _value, jni_cache->boolean)) {
          bool value = (*env)->CallBooleanMethod(env, _value,
                                                 jni_cache->boolean_bool_value);
          bugsnag_event_add_metadata_bool(event, section, name, value);
        } else if ((*env)->IsInstanceOf(env, _value, jni_cache->string)) {
          char *value = (char *)(*env)->GetStringUTFChars(env, _value, 0);
          bugsnag_event_add_metadata_string(event, section, name, value);
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
    event->metadata.value_count = 0;
  }
  free(jni_cache);
}
