#include "metadata.h"
#include "jni_cache.h"
#include "safejni.h"
#include "utils/string.h"
#include <malloc.h>
#include <string.h>

static jobject get_map_value_obj(JNIEnv *env, jobject map, const char *_key) {
  jobject obj = NULL;
  jstring key = NULL;

  if (!bsg_jni_cache->initialized) {
    goto exit;
  }

  key = bsg_safe_new_string_utf(env, _key);
  if (key == NULL) {
    goto exit;
  }

  obj = bsg_safe_call_object_method(env, map, bsg_jni_cache->HashMap_get, key);

exit:
  bsg_safe_delete_local_ref(env, key);
  return obj;
}

static void copy_map_value_string(JNIEnv *env, jobject map, const char *_key,
                                  char *dest, int len) {
  jobject _value = get_map_value_obj(env, map, _key);

  if (_value == NULL) {
    return;
  }

  const char *value = bsg_safe_get_string_utf_chars(env, (jstring)_value);
  if (value == NULL) {
    return;
  }

  bsg_strncpy(dest, value, len);
  bsg_safe_release_string_utf_chars(env, _value, value);
  bsg_safe_delete_local_ref(env, _value);
}

static long get_map_value_long(JNIEnv *env, jobject map, const char *_key) {
  jobject _value = NULL;
  long value = 0;

  if (!bsg_jni_cache->initialized) {
    goto exit;
  }

  _value = get_map_value_obj(env, map, _key);
  if (_value == NULL) {
    goto exit;
  }

  value = bsg_safe_call_double_method(env, _value,
                                      bsg_jni_cache->number_double_value);

exit:
  bsg_safe_delete_local_ref(env, _value);
  return value;
}

static float get_map_value_float(JNIEnv *env, jobject map, const char *_key) {
  jobject _value = NULL;
  float value = 0;

  if (!bsg_jni_cache->initialized) {
    goto exit;
  }

  _value = get_map_value_obj(env, map, _key);
  if (_value == NULL) {
    goto exit;
  }

  value =
      bsg_safe_call_float_method(env, _value, bsg_jni_cache->Float_floatValue);

exit:
  bsg_safe_delete_local_ref(env, _value);
  return value;
}

static bool get_map_value_bool(JNIEnv *env, jobject map, const char *_key) {
  jobject _value = NULL;
  bool value = 0;

  if (!bsg_jni_cache->initialized) {
    goto exit;
  }

  _value = get_map_value_obj(env, map, _key);
  if (_value == NULL) {
    goto exit;
  }

  value = bsg_safe_call_boolean_method(env, _value,
                                       bsg_jni_cache->Boolean_booleanValue);

exit:
  bsg_safe_delete_local_ref(env, _value);
  return value;
}

static int populate_cpu_abi_from_map(JNIEnv *env, jobject map,
                                     bsg_device_info *device) {
  jstring key = NULL;
  jobjectArray _value = NULL;
  int count = 0;

  if (!bsg_jni_cache->initialized) {
    goto exit;
  }

  key = bsg_safe_new_string_utf(env, "cpuAbi");
  if (key == NULL) {
    goto exit;
  }

  _value =
      bsg_safe_call_object_method(env, map, bsg_jni_cache->HashMap_get, key);
  if (_value == NULL) {
    goto exit;
  }

  count = bsg_safe_get_array_length(env, _value);

  // get the ABI as a Java string and copy it to bsg_device_info
  for (int i = 0; i < count && i < sizeof(device->cpu_abi); i++) {
    jstring jabi = bsg_safe_get_object_array_element(env, _value, i);
    if (jabi == NULL) {
      break;
    }

    const char *abi = bsg_safe_get_string_utf_chars(env, jabi);
    if (abi != NULL) {
      bsg_strncpy(device->cpu_abi[i].value, abi,
                  sizeof(device->cpu_abi[i].value));
      bsg_safe_release_string_utf_chars(env, jabi, abi);
      device->cpu_abi_count++;
    }
    bsg_safe_delete_local_ref(env, jabi);
  }

exit:
  bsg_safe_delete_local_ref(env, key);
  bsg_safe_delete_local_ref(env, _value);
  return count;
}

static void populate_app_data(JNIEnv *env, bugsnag_event *event) {
  if (!bsg_jni_cache->initialized) {
    return;
  }

  jobject data =
      bsg_safe_call_static_object_method(env, bsg_jni_cache->NativeInterface,
                                         bsg_jni_cache->NativeInterface_getApp);
  if (data == NULL) {
    return;
  }

  copy_map_value_string(env, data, "binaryArch", event->app.binary_arch,
                        sizeof(event->app.binary_arch));
  copy_map_value_string(env, data, "buildUUID", event->app.build_uuid,
                        sizeof(event->app.build_uuid));
  event->app.duration_ms_offset = get_map_value_long(env, data, "duration");
  event->app.duration_in_foreground_ms_offset =
      get_map_value_long(env, data, "durationInForeground");

  copy_map_value_string(env, data, "id", event->app.id, sizeof(event->app.id));
  event->app.in_foreground = get_map_value_bool(env, data, "inForeground");
  event->app.is_launching = true;

  char name[64];
  copy_map_value_string(env, data, "name", name, sizeof(name));
  bugsnag_event_add_metadata_string(event, "app", "name", name);

  copy_map_value_string(env, data, "releaseStage", event->app.release_stage,
                        sizeof(event->app.release_stage));
  copy_map_value_string(env, data, "type", event->app.type,
                        sizeof(event->app.type));
  copy_map_value_string(env, data, "version", event->app.version,
                        sizeof(event->app.version));
  event->app.version_code = get_map_value_long(env, data, "versionCode");

  bool restricted = get_map_value_bool(env, data, "backgroundWorkRestricted");

  if (restricted) {
    bugsnag_event_add_metadata_bool(event, "app", "backgroundWorkRestricted",
                                    restricted);
  }

  char process_name[64];
  copy_map_value_string(env, data, "processName", process_name,
                        sizeof(process_name));
  bugsnag_event_add_metadata_string(event, "app", "processName", process_name);

  long total_memory = get_map_value_long(env, data, "memoryLimit");
  bugsnag_event_add_metadata_double(event, "app", "memoryLimit",
                                    (double)total_memory);

  bsg_safe_delete_local_ref(env, data);
}

static void populate_device_metadata(JNIEnv *env, bugsnag_event *event,
                                     void *data) {
  char brand[64];
  copy_map_value_string(env, data, "brand", brand, sizeof(brand));
  bugsnag_event_add_metadata_string(event, "device", "brand", brand);

  bugsnag_event_add_metadata_double(event, "device", "dpi",
                                    get_map_value_long(env, data, "dpi"));
  bugsnag_event_add_metadata_bool(event, "device", "emulator",
                                  get_map_value_bool(env, data, "emulator"));

  char location_status[32];
  copy_map_value_string(env, data, "locationStatus", location_status,
                        sizeof(location_status));
  bugsnag_event_add_metadata_string(event, "device", "locationStatus",
                                    location_status);

  char network_access[64];
  copy_map_value_string(env, data, "networkAccess", network_access,
                        sizeof(network_access));
  bugsnag_event_add_metadata_string(event, "device", "networkAccess",
                                    network_access);

  bugsnag_event_add_metadata_double(
      event, "device", "screenDensity",
      get_map_value_float(env, data, "screenDensity"));

  char screen_resolution[32];
  copy_map_value_string(env, data, "screenResolution", screen_resolution,
                        sizeof(screen_resolution));
  bugsnag_event_add_metadata_string(event, "device", "screenResolution",
                                    screen_resolution);
}

static void populate_device_data(JNIEnv *env, bugsnag_event *event) {
  if (!bsg_jni_cache->initialized) {
    return;
  }

  jobject data = bsg_safe_call_static_object_method(
      env, bsg_jni_cache->NativeInterface,
      bsg_jni_cache->NativeInterface_getDevice);
  if (data == NULL) {
    return;
  }

  populate_cpu_abi_from_map(env, data, &event->device);

  copy_map_value_string(env, data, "id", event->device.id,
                        sizeof(event->device.id));
  event->device.jailbroken = get_map_value_bool(env, data, "jailbroken");

  copy_map_value_string(env, data, "locale", event->device.locale,
                        sizeof(event->device.locale));

  copy_map_value_string(env, data, "manufacturer", event->device.manufacturer,
                        sizeof(event->device.manufacturer));
  copy_map_value_string(env, data, "model", event->device.model,
                        sizeof(event->device.model));

  copy_map_value_string(env, data, "orientation", event->device.orientation,
                        sizeof(event->device.orientation));
  bsg_strncpy(event->device.os_name, bsg_os_name(),
              sizeof(event->device.os_name));
  copy_map_value_string(env, data, "osVersion", event->device.os_version,
                        sizeof(event->device.os_version));

  jobject _runtime_versions = get_map_value_obj(env, data, "runtimeVersions");
  if (_runtime_versions != NULL) {
    copy_map_value_string(env, _runtime_versions, "osBuild",
                          event->device.os_build,
                          sizeof(event->device.os_build));

    char api_level[8];
    copy_map_value_string(env, _runtime_versions, "androidApiLevel", api_level,
                          sizeof(api_level));
    event->device.api_level = strtol(api_level, NULL, 10);
  }
  event->device.total_memory = get_map_value_long(env, data, "totalMemory");

  // add fields to device metadata
  populate_device_metadata(env, event, data);

  bsg_safe_delete_local_ref(env, data);
  bsg_safe_delete_local_ref(env, _runtime_versions);
}

static void populate_user_data(JNIEnv *env, bugsnag_event *event) {
  if (!bsg_jni_cache->initialized) {
    return;
  }

  jobject data = bsg_safe_call_static_object_method(
      env, bsg_jni_cache->NativeInterface,
      bsg_jni_cache->NativeInterface_getUser);
  if (data == NULL) {
    return;
  }
  copy_map_value_string(env, data, "id", event->user.id,
                        sizeof(event->user.id));
  copy_map_value_string(env, data, "name", event->user.name,
                        sizeof(event->user.name));
  copy_map_value_string(env, data, "email", event->user.email,
                        sizeof(event->user.email));

  bsg_safe_delete_local_ref(env, data);
}

static void populate_context(JNIEnv *env, bugsnag_event *event) {
  if (!bsg_jni_cache->initialized) {
    return;
  }

  jstring _context = bsg_safe_call_static_object_method(
      env, bsg_jni_cache->NativeInterface,
      bsg_jni_cache->NativeInterface_getContext);
  if (_context != NULL) {
    const char *value = bsg_safe_get_string_utf_chars(env, (jstring)_context);
    if (value != NULL) {
      bsg_strncpy(event->context, value, sizeof(event->context) - 1);
      bsg_safe_release_string_utf_chars(env, _context, value);
    }
  } else {
    memset(&event->context, 0, bsg_strlen(event->context));
  }

  bsg_safe_delete_local_ref(env, _context);
}

static void populate_metadata_value(JNIEnv *env, bugsnag_metadata *dst,
                                    const char *section, const char *name,
                                    jobject _value) {
  if (!bsg_jni_cache->initialized) {
    return;
  }

  if (bsg_safe_is_instance_of(env, _value, bsg_jni_cache->number)) {
    // add a double metadata value
    double value = bsg_safe_call_double_method(
        env, _value, bsg_jni_cache->number_double_value);
    bsg_add_metadata_value_double(dst, section, name, value);
  } else if (bsg_safe_is_instance_of(env, _value, bsg_jni_cache->Boolean)) {
    // add a boolean metadata value
    bool value = bsg_safe_call_boolean_method(
        env, _value, bsg_jni_cache->Boolean_booleanValue);
    bsg_add_metadata_value_bool(dst, section, name, value);
  } else if (bsg_safe_is_instance_of(env, _value, bsg_jni_cache->String)) {
    const char *value = bsg_safe_get_string_utf_chars(env, _value);
    if (value != NULL) {
      bsg_add_metadata_value_str(dst, section, name, value);
    }
  } else if (bsg_safe_is_instance_of(env, _value, bsg_jni_cache->OpaqueValue)) {
    jstring _json = bsg_safe_call_object_method(
        env, _value, bsg_jni_cache->OpaqueValue_getJson);
    const char *json = bsg_safe_get_string_utf_chars(env, _json);

    if (json != NULL) {
      bsg_add_metadata_value_opaque(dst, section, name, json);
      bsg_safe_release_string_utf_chars(env, _json, json);
    }
  }
}

static void populate_metadata_obj(JNIEnv *env, bugsnag_metadata *dst,
                                  jobject section, jobject section_keylist,
                                  int index) {
  jstring section_key = NULL;
  const char *name = NULL;
  jobject _value = NULL;

  if (!bsg_jni_cache->initialized) {
    goto exit;
  }

  section_key = bsg_safe_call_object_method(
      env, section_keylist, bsg_jni_cache->ArrayList_get, (jint)index);
  if (section_key == NULL) {
    goto exit;
  }

  _value = bsg_safe_call_object_method(env, section, bsg_jni_cache->Map_get,
                                       section_key);
  name = bsg_safe_get_string_utf_chars(env, section_key);
  if (name == NULL) {
    goto exit;
  }

  populate_metadata_value(env, dst, section, name, _value);

exit:
  bsg_safe_release_string_utf_chars(env, section_key, name);
  bsg_safe_delete_local_ref(env, section_key);
  bsg_safe_delete_local_ref(env, _value);
}

static void populate_metadata_section(JNIEnv *env, bugsnag_metadata *dst,
                                      jobject metadata, jobject keylist,
                                      int i) {
  jstring _key = NULL;
  const char *section = NULL;
  jobject _section = NULL;
  jobject section_keyset = NULL;
  jobject section_keylist = NULL;

  if (!bsg_jni_cache->initialized) {
    goto exit;
  }

  _key = bsg_safe_call_object_method(env, keylist, bsg_jni_cache->ArrayList_get,
                                     (jint)i);
  if (_key == NULL) {
    goto exit;
  }
  section = bsg_safe_get_string_utf_chars(env, _key);
  if (section == NULL) {
    goto exit;
  }
  _section =
      bsg_safe_call_object_method(env, metadata, bsg_jni_cache->Map_get, _key);
  if (_section == NULL) {
    goto exit;
  }
  jint section_size =
      bsg_safe_call_int_method(env, _section, bsg_jni_cache->Map_size);
  if (section_size == -1) {
    goto exit;
  }
  section_keyset =
      bsg_safe_call_object_method(env, _section, bsg_jni_cache->Map_keySet);
  if (section_keyset == NULL) {
    goto exit;
  }

  section_keylist = bsg_safe_new_object(
      env, bsg_jni_cache->ArrayList,
      bsg_jni_cache->ArrayList_constructor_collection, section_keyset);
  if (section_keylist == NULL) {
    goto exit;
  }
  for (int j = 0; j < section_size; j++) {
    populate_metadata_obj(env, dst, _section, section_keylist, j);
  }
  goto exit;

exit:
  bsg_safe_release_string_utf_chars(env, _key, section);
  bsg_safe_delete_local_ref(env, _key);
  bsg_safe_delete_local_ref(env, _section);
  bsg_safe_delete_local_ref(env, section_keyset);
  bsg_safe_delete_local_ref(env, section_keylist);
}

// Internal API

void bsg_populate_metadata(JNIEnv *env, bugsnag_metadata *dst,
                           jobject metadata) {
  jobject _metadata = NULL;
  jobject keyset = NULL;
  jobject keylist = NULL;

  if (!bsg_jni_cache->initialized) {
    goto exit;
  }

  if (metadata == NULL) {
    _metadata = bsg_safe_call_static_object_method(
        env, bsg_jni_cache->NativeInterface,
        bsg_jni_cache->NativeInterface_getMetadata);
    metadata = _metadata;
  }

  if (metadata == NULL) {
    dst->value_count = 0;
    goto exit;
  }

  int size = bsg_safe_call_int_method(env, metadata, bsg_jni_cache->Map_size);
  if (size == -1) {
    goto exit;
  }

  // create a list of metadata keys
  keyset = bsg_safe_call_static_object_method(env, metadata,
                                              bsg_jni_cache->Map_keySet);
  if (keyset == NULL) {
    goto exit;
  }
  keylist = bsg_safe_new_object(env, bsg_jni_cache->ArrayList,
                                bsg_jni_cache->ArrayList_constructor_collection,
                                keyset);
  if (keylist == NULL) {
    goto exit;
  }

  for (int i = 0; i < size; i++) {
    populate_metadata_section(env, dst, metadata, keylist, i);
  }

exit:
  bsg_safe_delete_local_ref(env, _metadata);
  bsg_safe_delete_local_ref(env, keyset);
  bsg_safe_delete_local_ref(env, keylist);
}

void bsg_populate_crumb_metadata(JNIEnv *env, bugsnag_breadcrumb *crumb,
                                 jobject metadata) {
  jobject keyset = NULL;
  jobject keylist = NULL;

  if (metadata == NULL) {
    goto exit;
  }
  if (!bsg_jni_cache->initialized) {
    goto exit;
  }

  // get size of metadata map
  jint map_size =
      bsg_safe_call_int_method(env, metadata, bsg_jni_cache->Map_size);
  if (map_size == -1) {
    goto exit;
  }

  // create a list of metadata keys
  keyset =
      bsg_safe_call_object_method(env, metadata, bsg_jni_cache->Map_keySet);
  if (keyset == NULL) {
    goto exit;
  }
  keylist = bsg_safe_new_object(env, bsg_jni_cache->ArrayList,
                                bsg_jni_cache->ArrayList_constructor_collection,
                                keyset);
  if (keylist == NULL) {
    goto exit;
  }

  for (int i = 0; i < map_size; i++) {
    jstring _key = bsg_safe_call_object_method(
        env, keylist, bsg_jni_cache->ArrayList_get, (jint)i);
    jobject _value = bsg_safe_call_object_method(env, metadata,
                                                 bsg_jni_cache->Map_get, _key);

    if (_key != NULL && _value != NULL) {
      const char *key = bsg_safe_get_string_utf_chars(env, _key);
      if (key != NULL) {
        populate_metadata_value(env, &crumb->metadata, "metaData", key, _value);
        bsg_safe_release_string_utf_chars(env, _key, key);
      }
    }
    bsg_safe_delete_local_ref(env, _key);
    bsg_safe_delete_local_ref(env, _value);
  }

exit:
  bsg_safe_delete_local_ref(env, keyset);
  bsg_safe_delete_local_ref(env, keylist);
}

void bsg_populate_event(JNIEnv *env, bugsnag_event *event) {
  if (!bsg_jni_cache->initialized) {
    return;
  }
  populate_context(env, event);
  populate_app_data(env, event);
  populate_device_data(env, event);
  populate_user_data(env, event);
}

const char *bsg_os_name() { return "android"; }
