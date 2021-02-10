#include "metadata.h"
#include "safejni.h"
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

/**
 * Creates a cache of JNI methods/classes that are commonly used.
 *
 * Class and method objects can be kept safely since they aren't moved or
 * removed from the JVM - care should be taken not to load objects as local
 * references here.
 */
bsg_jni_cache *bsg_populate_jni_cache(JNIEnv *env) {
  bsg_jni_cache *jni_cache = malloc(sizeof(bsg_jni_cache));

  // lookup java/lang/Integer
  jni_cache->integer = bsg_safe_find_class(env, "java/lang/Integer");
  if (jni_cache->integer == NULL) {
    return NULL;
  }

  // lookup java/lang/Boolean
  jni_cache->boolean = bsg_safe_find_class(env, "java/lang/Boolean");
  if (jni_cache->boolean == NULL) {
    return NULL;
  }

  // lookup java/lang/Long
  jni_cache->long_class = bsg_safe_find_class(env, "java/lang/Long");
  if (jni_cache->long_class == NULL) {
    return NULL;
  }

  // lookup java/lang/Float
  jni_cache->float_class = bsg_safe_find_class(env, "java/lang/Float");
  if (jni_cache->float_class == NULL) {
    return NULL;
  }

  // lookup java/lang/Number
  jni_cache->number = bsg_safe_find_class(env, "java/lang/Number");
  if (jni_cache->number == NULL) {
    return NULL;
  }

  // lookup java/lang/String
  jni_cache->string = bsg_safe_find_class(env, "java/lang/String");
  if (jni_cache->string == NULL) {
    return NULL;
  }

  // lookup Integer.intValue()
  jni_cache->integer_int_value =
      bsg_safe_get_method_id(env, jni_cache->integer, "intValue", "()I");
  if (jni_cache->integer_int_value == NULL) {
    return NULL;
  }

  // lookup Integer.floatValue()
  jni_cache->float_float_value =
      bsg_safe_get_method_id(env, jni_cache->float_class, "floatValue", "()F");
  if (jni_cache->float_float_value == NULL) {
    return NULL;
  }

  // lookup Double.doubleValue()
  jni_cache->number_double_value =
      bsg_safe_get_method_id(env, jni_cache->number, "doubleValue", "()D");
  if (jni_cache->number_double_value == NULL) {
    return NULL;
  }

  // lookup Long.longValue()
  jni_cache->long_long_value =
      bsg_safe_get_method_id(env, jni_cache->integer, "longValue", "()J");
  if (jni_cache->long_long_value == NULL) {
    return NULL;
  }

  // lookup Boolean.booleanValue()
  jni_cache->boolean_bool_value =
      bsg_safe_get_method_id(env, jni_cache->boolean, "booleanValue", "()Z");
  if (jni_cache->boolean_bool_value == NULL) {
    return NULL;
  }

  // lookup java/util/ArrayList
  jni_cache->arraylist = bsg_safe_find_class(env, "java/util/ArrayList");
  if (jni_cache->arraylist == NULL) {
    return NULL;
  }

  // lookup ArrayList constructor
  jni_cache->arraylist_init_with_obj = bsg_safe_get_method_id(
      env, jni_cache->arraylist, "<init>", "(Ljava/util/Collection;)V");
  if (jni_cache->arraylist_init_with_obj == NULL) {
    return NULL;
  }

  // lookup ArrayList.get()
  jni_cache->arraylist_get = bsg_safe_get_method_id(
      env, jni_cache->arraylist, "get", "(I)Ljava/lang/Object;");
  if (jni_cache->arraylist_get == NULL) {
    return NULL;
  }

  // lookup java/util/HashMap
  jni_cache->hash_map = bsg_safe_find_class(env, "java/util/HashMap");
  if (jni_cache->hash_map == NULL) {
    return NULL;
  }

  // lookup java/util/Map
  jni_cache->map = bsg_safe_find_class(env, "java/util/Map");
  if (jni_cache->map == NULL) {
    return NULL;
  }

  // lookup java/util/Set
  jni_cache->hash_map_key_set = bsg_safe_get_method_id(
      env, jni_cache->hash_map, "keySet", "()Ljava/util/Set;");
  if (jni_cache->hash_map_key_set == NULL) {
    return NULL;
  }

  // lookup HashMap.size()
  jni_cache->hash_map_size =
      bsg_safe_get_method_id(env, jni_cache->hash_map, "size", "()I");
  if (jni_cache->hash_map_size == NULL) {
    return NULL;
  }

  // lookup HashMap.get()
  jni_cache->hash_map_get =
      bsg_safe_get_method_id(env, jni_cache->hash_map, "get",
                             "(Ljava/lang/Object;)Ljava/lang/Object;");
  if (jni_cache->hash_map_get == NULL) {
    return NULL;
  }

  // lookup Map.keySet()
  jni_cache->map_key_set = bsg_safe_get_method_id(env, jni_cache->map, "keySet",
                                                  "()Ljava/util/Set;");
  if (jni_cache->map_key_set == NULL) {
    return NULL;
  }

  // lookup Map.size()
  jni_cache->map_size =
      bsg_safe_get_method_id(env, jni_cache->map, "size", "()I");
  if (jni_cache->map_size == NULL) {
    return NULL;
  }

  // lookup Map.get()
  jni_cache->map_get = bsg_safe_get_method_id(
      env, jni_cache->map, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
  if (jni_cache->map_get == NULL) {
    return NULL;
  }

  // lookup com/bugsnag/android/NativeInterface
  jni_cache->native_interface =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (jni_cache->native_interface == NULL) {
    return NULL;
  }

  // lookup NativeInterface.getApp()
  jni_cache->get_app_data = bsg_safe_get_static_method_id(
      env, jni_cache->native_interface, "getAppData", "()Ljava/util/Map;");
  if (jni_cache->get_app_data == NULL) {
    return NULL;
  }

  // lookup NativeInterface.getDevice()
  jni_cache->get_device_data = bsg_safe_get_static_method_id(
      env, jni_cache->native_interface, "getDeviceData", "()Ljava/util/Map;");
  if (jni_cache->get_device_data == NULL) {
    return NULL;
  }

  // lookup NativeInterface.getUser()
  jni_cache->get_user_data = bsg_safe_get_static_method_id(
      env, jni_cache->native_interface, "getUserData", "()Ljava/util/Map;");
  if (jni_cache->get_user_data == NULL) {
    return NULL;
  }

  // lookup NativeInterface.getMetadata()
  jni_cache->get_metadata = bsg_safe_get_static_method_id(
      env, jni_cache->native_interface, "getMetaData", "()Ljava/util/Map;");
  if (jni_cache->get_metadata == NULL) {
    return NULL;
  }

  // lookup NativeInterface.getContext()
  jni_cache->get_context = bsg_safe_get_static_method_id(
      env, jni_cache->native_interface, "getContext", "()Ljava/lang/String;");
  if (jni_cache->get_context == NULL) {
    return NULL;
  }
  return jni_cache;
}

jobject bsg_get_map_value_obj(JNIEnv *env, bsg_jni_cache *jni_cache,
                              jobject map, const char *_key) {
  // create Java string object for map key
  jstring key = bsg_safe_new_string_utf(env, _key);
  if (key == NULL) {
    return NULL;
  }

  jobject obj =
      bsg_safe_call_object_method(env, map, jni_cache->hash_map_get, key);
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
    long value = bsg_safe_call_double_method(env, _value,
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
    float value =
        bsg_safe_call_float_method(env, _value, jni_cache->float_float_value);
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
        bsg_safe_call_int_method(env, _value, jni_cache->integer_int_value);
    (*env)->DeleteLocalRef(env, _value);
    return value;
  }
  return 0;
}

bool bsg_get_map_value_bool(JNIEnv *env, bsg_jni_cache *jni_cache, jobject map,
                            const char *_key) {
  jobject obj = bsg_get_map_value_obj(env, jni_cache, map, _key);
  return bsg_safe_call_boolean_method(env, obj, jni_cache->boolean_bool_value);
}

int bsg_populate_cpu_abi_from_map(JNIEnv *env, bsg_jni_cache *jni_cache,
                                  jobject map, bsg_device_info *device) {
  // create Java string object for map key
  jstring key = bsg_safe_new_string_utf(env, "cpuAbi");
  if (key == NULL) {
    return 0;
  }

  jobjectArray _value =
      bsg_safe_call_object_method(env, map, jni_cache->hash_map_get, key);
  if (_value != NULL) {
    int count = (*env)->GetArrayLength(env, _value);

    // get the ABI as a Java string and copy it to bsg_device_info
    for (int i = 0; i < count && i < sizeof(device->cpu_abi); i++) {
      jstring jabi = bsg_safe_get_object_array_element(env, _value, i);
      if (jabi == NULL) {
        break;
      }

      char *abi = (char *)(*env)->GetStringUTFChars(env, jabi, 0);
      bsg_strncpy_safe(device->cpu_abi[i].value, abi,
                       sizeof(device->cpu_abi[i].value));
      (*env)->ReleaseStringUTFChars(env, jabi, abi);
      device->cpu_abi_count++;
    }
    (*env)->DeleteLocalRef(env, _value);
    return count;
  }
  return 0;
}

void bsg_populate_crumb_metadata(JNIEnv *env, bugsnag_breadcrumb *crumb,
                                 jobject metadata) {
  bsg_jni_cache *jni_cache = NULL;
  jobject keyset = NULL;
  jobject keylist = NULL;

  if (metadata == NULL) {
    goto exit;
  }
  jni_cache = bsg_populate_jni_cache(env);
  if (jni_cache == NULL) {
    goto exit;
  }

  // get size of metadata map
  jint map_size = bsg_safe_call_int_method(env, metadata, jni_cache->map_size);
  if (map_size == -1) {
    goto exit;
  }

  // create a list of metadata keys
  keyset = bsg_safe_call_object_method(env, metadata, jni_cache->map_key_set);
  if (keyset == NULL) {
    goto exit;
  }
  keylist = bsg_safe_new_object(env, jni_cache->arraylist,
                                jni_cache->arraylist_init_with_obj, keyset);
  if (keylist == NULL) {
    goto exit;
  }

  for (int i = 0; i < map_size; i++) {
    jstring _key = bsg_safe_call_object_method(
        env, keylist, jni_cache->arraylist_get, (jint)i);
    jstring _value =
        bsg_safe_call_object_method(env, metadata, jni_cache->map_get, _key);

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
    }
  }
  goto exit;

  exit:
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
  jobject data = bsg_safe_call_static_object_method(
      env, jni_cache->native_interface, jni_cache->get_app_data);
  if (data == NULL) {
    return;
  }

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
  jobject data = bsg_safe_call_static_object_method(
      env, jni_cache->native_interface, jni_cache->get_device_data);
  if (data == NULL) {
    return;
  }

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
  jobject data = bsg_safe_call_static_object_method(
      env, jni_cache->native_interface, jni_cache->get_user_data);
  if (data == NULL) {
    return;
  }

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
  jstring _context = bsg_safe_call_static_object_method(
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
  if (jni_cache == NULL) {
    return;
  }
  bsg_populate_context(env, jni_cache, report);
  bsg_populate_app_data(env, jni_cache, report);
  bsg_populate_device_data(env, jni_cache, report);
  bsg_populate_user_data(env, jni_cache, report);
  free(jni_cache);
}

void bsg_populate_metadata_value(JNIEnv *env, bugsnag_report *dst,
                                 bsg_jni_cache *jni_cache, char *section,
                                 char *name, jobject _value) {
  if ((*env)->IsInstanceOf(env, _value, jni_cache->number)) {
    // add a double metadata value
    double value = bsg_safe_call_double_method(env, _value,
                                               jni_cache->number_double_value);
    bugsnag_report_add_metadata_double(dst, section, name, value);
  } else if ((*env)->IsInstanceOf(env, _value, jni_cache->boolean)) {
    // add a boolean metadata value
    bool value = bsg_safe_call_boolean_method(env, _value,
                                              jni_cache->boolean_bool_value);
    bugsnag_report_add_metadata_bool(dst, section, name, value);
  } else if ((*env)->IsInstanceOf(env, _value, jni_cache->string)) {
    char *value = (char *)(*env)->GetStringUTFChars(env, _value, 0);
    bugsnag_report_add_metadata_string(dst, section, name, value);
    free(value);
  }
}

void bsg_populate_metadata_obj(JNIEnv *env, bugsnag_report *dst,
                               bsg_jni_cache *jni_cache, char *section,
                               void *section_keylist, int index) {
  jstring section_key = bsg_safe_call_object_method(
      env, section_keylist, jni_cache->arraylist_get, (jint)index);
  if (section_key == NULL) {
    return;
  }
  char *name = (char *)(*env)->GetStringUTFChars(env, section_key, 0);
  jobject _value = bsg_safe_call_object_method(env, section, jni_cache->map_get,
                                               section_key);
  bsg_populate_metadata_value(env, dst, jni_cache, section, name, _value);
  (*env)->ReleaseStringUTFChars(env, section_key, name);
  (*env)->DeleteLocalRef(env, _value);
}

void bsg_populate_metadata_section(JNIEnv *env, bugsnag_report *report,
                                   jobject metadata, bsg_jni_cache *jni_cache,
                                   jobject keylist, int i) {
  jstring _key = NULL;
  char *section = NULL;
  jobject _section = NULL;
  jobject section_keyset = NULL;
  jobject section_keylist = NULL;

  _key = bsg_safe_call_object_method(env, keylist, jni_cache->arraylist_get,
                                     (jint)i);
  if (_key == NULL) {
    goto exit;
  }
  section = (char *)(*env)->GetStringUTFChars(env, _key, 0);
  _section =
      bsg_safe_call_object_method(env, metadata, jni_cache->map_get, _key);
  if (_section == NULL) {
    goto exit;
  }
  jint section_size =
      bsg_safe_call_int_method(env, _section, jni_cache->map_size);
  if (section_size == -1) {
    goto exit;
  }
  section_keyset =
      bsg_safe_call_object_method(env, _section, jni_cache->map_key_set);
  if (section_keyset == NULL) {
    goto exit;
  }

  section_keylist =
      bsg_safe_new_object(env, jni_cache->arraylist,
                          jni_cache->arraylist_init_with_obj, section_keyset);
  if (section_keylist == NULL) {
    goto exit;
  }
  for (int j = 0; j < section_size; j++) {
    bsg_populate_metadata_obj(env, report, jni_cache, section, section_keylist, j);
  }
  goto exit;

  exit:
  (*env)->ReleaseStringUTFChars(env, _key, section);
  (*env)->DeleteLocalRef(env, section_keyset);
  (*env)->DeleteLocalRef(env, section_keylist);
  (*env)->DeleteLocalRef(env, _section);
}

void bsg_populate_metadata(JNIEnv *env, bugsnag_report *report,
                           jobject metadata) {
  jobject keyset = NULL;
  jobject keylist = NULL;
  bsg_jni_cache *jni_cache = bsg_populate_jni_cache(env);

  if (jni_cache == NULL) {
    goto exit;
  }
  if (metadata == NULL) {
    metadata = bsg_safe_call_static_object_method(
        env, jni_cache->native_interface, jni_cache->get_metadata);
  }
  if (metadata != NULL) {
    int size = bsg_safe_call_int_method(env, metadata, jni_cache->map_size);
    if (size == -1) {
      goto exit;
    }

    // create a list of metadata keys
    keyset = bsg_safe_call_static_object_method(env, metadata,
                                                jni_cache->map_key_set);
    if (keyset == NULL) {
      goto exit;
    }
    keylist = bsg_safe_new_object(env, jni_cache->arraylist,
                                  jni_cache->arraylist_init_with_obj, keyset);
    if (keylist == NULL) {
      goto exit;
    }

    for (int i = 0; i < size; i++) {
      bsg_populate_metadata_section(env, report, metadata, jni_cache, keylist, i);
    }
  } else {
    report->metadata.value_count = 0;
  }
  goto exit;

// cleanup
  exit:
  if (jni_cache != NULL) {
    free(jni_cache);
  }
  (*env)->DeleteLocalRef(env, keyset);
  (*env)->DeleteLocalRef(env, keylist);
}
