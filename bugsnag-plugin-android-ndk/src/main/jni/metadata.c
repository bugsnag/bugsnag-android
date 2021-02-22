#include "metadata.h"
#include "jnicache.h"
#include "safejni.h"
#include "utils/jni_utils.h"
#include "utils/string.h"
#include <malloc.h>
#include <string.h>

static void populate_metadata_value(JNIEnv *env, bugsnag_metadata *dst,
                                    bsg_jni_cache *jni_cache,
                                    const char *section, const char *name,
                                    jobject _value);

static int populate_cpu_abi_from_map(JNIEnv *env, bsg_jni_cache *jni_cache,
                                     jobject map, bsg_device_info *device) {
  int count = 0;
  jstring key = NULL;
  jobjectArray _value = NULL;

  // create Java string object for map key
  key = bsg_safe_new_string_utf(env, "cpuAbi");
  if (key == NULL) {
    goto exit;
  }

  _value = bsg_safe_call_object_method(env, map, jni_cache->hash_map_get, key);
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
      bsg_strncpy_safe(device->cpu_abi[i].value, abi,
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

void bsg_populate_crumb_metadata(JNIEnv *env, bugsnag_breadcrumb *crumb,
                                 jobject metadata) {
  bsg_jni_cache *jni_cache = bsg_get_jni_cache();
  if (jni_cache == NULL) {
    return;
  }
  if (metadata == NULL) {
    return;
  }

  jobject keyset = NULL;
  jobject keylist = NULL;

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
    if (_key == NULL) {
      continue;
    }
    jobject _value =
        bsg_safe_call_object_method(env, metadata, jni_cache->map_get, _key);
    if (_value != NULL) {
      const char *key = bsg_safe_get_string_utf_chars(env, _key);
      if (key != NULL) {
        populate_metadata_value(env, &crumb->metadata, jni_cache, "metaData",
                                key, _value);
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

static void populate_app_data(JNIEnv *env, bsg_jni_cache *jni_cache,
                              bugsnag_event *event) {
  jobject data = NULL;

  data = bsg_safe_call_static_object_method(env, jni_cache->native_interface,
                                            jni_cache->get_app_data);
  if (data == NULL) {
    goto exit;
  }

  bsg_strncpy_safe(event->app.binary_arch, bsg_binary_arch(),
                   sizeof(event->app.binary_arch));

  bsg_copy_map_value_string(env, jni_cache, data, "buildUUID",
                            event->app.build_uuid,
                            sizeof(event->app.build_uuid));
  event->app.duration_ms_offset =
      bsg_get_map_value_long(env, jni_cache, data, "duration");
  event->app.duration_in_foreground_ms_offset =
      bsg_get_map_value_long(env, jni_cache, data, "durationInForeground");

  bsg_copy_map_value_string(env, jni_cache, data, "id", event->app.id,
                            sizeof(event->app.id));
  event->app.in_foreground =
      bsg_get_map_value_bool(env, jni_cache, data, "inForeground");

  char name[64];
  bsg_copy_map_value_string(env, jni_cache, data, "name", name, sizeof(name));
  bugsnag_event_add_metadata_string(event, "app", "name", name);

  bsg_copy_map_value_string(env, jni_cache, data, "releaseStage",
                            event->app.release_stage,
                            sizeof(event->app.release_stage));
  bsg_copy_map_value_string(env, jni_cache, data, "type", event->app.type,
                            sizeof(event->app.type));
  bsg_copy_map_value_string(env, jni_cache, data, "version", event->app.version,
                            sizeof(event->app.version));
  event->app.version_code =
      bsg_get_map_value_int(env, jni_cache, data, "versionCode");

exit:
  bsg_safe_delete_local_ref(env, data);
}

char *bsg_os_name() { return "android"; }

static void populate_device_metadata(JNIEnv *env, bsg_jni_cache *jni_cache,
                                     bugsnag_event *event, void *data) {
  char brand[64];
  bsg_copy_map_value_string(env, jni_cache, data, "brand", brand,
                            sizeof(brand));
  bugsnag_event_add_metadata_string(event, "device", "brand", brand);

  bugsnag_event_add_metadata_double(
      event, "device", "dpi",
      bsg_get_map_value_int(env, jni_cache, data, "dpi"));
  bugsnag_event_add_metadata_bool(
      event, "device", "emulator",
      bsg_get_map_value_bool(env, jni_cache, data, "emulator"));

  char location_status[32];
  bsg_copy_map_value_string(env, jni_cache, data, "locationStatus",
                            location_status, sizeof(location_status));
  bugsnag_event_add_metadata_string(event, "device", "locationStatus",
                                    location_status);

  char network_access[64];
  bsg_copy_map_value_string(env, jni_cache, data, "networkAccess",
                            network_access, sizeof(network_access));
  bugsnag_event_add_metadata_string(event, "device", "networkAccess",
                                    network_access);

  bugsnag_event_add_metadata_double(
      event, "device", "screenDensity",
      bsg_get_map_value_float(env, jni_cache, data, "screenDensity"));

  char screen_resolution[32];
  bsg_copy_map_value_string(env, jni_cache, data, "screenResolution",
                            screen_resolution, sizeof(screen_resolution));
  bugsnag_event_add_metadata_string(event, "device", "screenResolution",
                                    screen_resolution);
}

static void populate_device_data(JNIEnv *env, bsg_jni_cache *jni_cache,
                                 bugsnag_event *event) {
  jobject data = NULL;
  jobject _runtime_versions = NULL;

  data = bsg_safe_call_static_object_method(env, jni_cache->native_interface,
                                            jni_cache->get_device_data);
  if (data == NULL) {
    goto exit;
  }

  populate_cpu_abi_from_map(env, jni_cache, data, &event->device);

  bsg_copy_map_value_string(env, jni_cache, data, "id", event->device.id,
                            sizeof(event->device.id));
  event->device.jailbroken =
      bsg_get_map_value_bool(env, jni_cache, data, "jailbroken");

  bsg_copy_map_value_string(env, jni_cache, data, "locale",
                            event->device.locale, sizeof(event->device.locale));

  bsg_copy_map_value_string(env, jni_cache, data, "manufacturer",
                            event->device.manufacturer,
                            sizeof(event->device.manufacturer));
  bsg_copy_map_value_string(env, jni_cache, data, "model", event->device.model,
                            sizeof(event->device.model));

  bsg_copy_map_value_string(env, jni_cache, data, "orientation",
                            event->device.orientation,
                            sizeof(event->device.orientation));
  bsg_strncpy_safe(event->device.os_name, bsg_os_name(),
                   sizeof(event->device.os_name));
  bsg_copy_map_value_string(env, jni_cache, data, "osVersion",
                            event->device.os_version,
                            sizeof(event->device.os_version));

  _runtime_versions =
      bsg_get_map_value_obj(env, jni_cache, data, "runtimeVersions");

  if (_runtime_versions != NULL) {
    bsg_copy_map_value_string(env, jni_cache, _runtime_versions, "osBuild",
                              event->device.os_build,
                              sizeof(event->device.os_build));

    event->device.api_level = bsg_get_map_value_int(
        env, jni_cache, _runtime_versions, "androidApiLevel");
  }
  event->device.total_memory =
      bsg_get_map_value_long(env, jni_cache, data, "totalMemory");

  // add fields to device metadata
  populate_device_metadata(env, jni_cache, event, data);

exit:
  bsg_safe_delete_local_ref(env, data);
  bsg_safe_delete_local_ref(env, _runtime_versions);
}

static void populate_user_data(JNIEnv *env, bsg_jni_cache *jni_cache,
                               bugsnag_event *event) {
  jobject data = NULL;

  data = bsg_safe_call_static_object_method(env, jni_cache->native_interface,
                                            jni_cache->get_user_data);
  if (data == NULL) {
    goto exit;
  }

  bsg_copy_map_value_string(env, jni_cache, data, "id", event->user.id,
                            sizeof(event->user.id));
  bsg_copy_map_value_string(env, jni_cache, data, "name", event->user.name,
                            sizeof(event->user.name));
  bsg_copy_map_value_string(env, jni_cache, data, "email", event->user.email,
                            sizeof(event->user.email));

exit:
  bsg_safe_delete_local_ref(env, data);
}

static void populate_context(JNIEnv *env, bsg_jni_cache *jni_cache,
                             bugsnag_event *event) {
  jstring _context = NULL;

  _context = bsg_safe_call_static_object_method(
      env, jni_cache->native_interface, jni_cache->get_context);
  if (_context == NULL) {
    event->context[0] = 0;
    goto exit;
  }

  const char *value = bsg_safe_get_string_utf_chars(env, (jstring)_context);
  if (value != NULL) {
    bsg_strncpy_safe(event->context, value, sizeof(event->context) - 1);
    bsg_safe_release_string_utf_chars(env, _context, value);
  }

exit:
  bsg_safe_delete_local_ref(env, _context);
}

void bsg_populate_event(JNIEnv *env, bugsnag_event *event) {
  bsg_jni_cache *jni_cache = bsg_get_jni_cache();
  if (jni_cache == NULL) {
    return;
  }
  populate_context(env, jni_cache, event);
  populate_app_data(env, jni_cache, event);
  populate_device_data(env, jni_cache, event);
  populate_user_data(env, jni_cache, event);
}

static void populate_metadata_value(JNIEnv *env, bugsnag_metadata *dst,
                                    bsg_jni_cache *jni_cache,
                                    const char *section, const char *name,
                                    jobject _value) {
  if (bsg_safe_is_instance_of(env, _value, jni_cache->number)) {
    // add a double metadata value
    double value = bsg_safe_call_double_method(env, _value,
                                               jni_cache->number_double_value);
    bsg_add_metadata_value_double(dst, section, name, value);
  } else if (bsg_safe_is_instance_of(env, _value, jni_cache->boolean)) {
    // add a boolean metadata value
    bool value = bsg_safe_call_boolean_method(env, _value,
                                              jni_cache->boolean_bool_value);
    bsg_add_metadata_value_bool(dst, section, name, value);
  } else if (bsg_safe_is_instance_of(env, _value, jni_cache->string)) {
    const char *value = bsg_safe_get_string_utf_chars(env, _value);
    if (value != NULL) {
      bsg_add_metadata_value_str(dst, section, name, value);
      free((char *)value);
    }
  }
}

static void populate_metadata_obj(JNIEnv *env, bugsnag_metadata *dst,
                                  bsg_jni_cache *jni_cache, jobject section,
                                  jobject section_keylist, int index) {
  jstring section_key = NULL;
  jobject _value = NULL;

  section_key = bsg_safe_call_object_method(
      env, section_keylist, jni_cache->arraylist_get, (jint)index);
  if (section_key == NULL) {
    goto exit;
  }

  _value = bsg_safe_call_object_method(env, section, jni_cache->map_get,
                                       section_key);
  if (_value == NULL) {
    goto exit;
  }

  const char *name = bsg_safe_get_string_utf_chars(env, section_key);
  if (name != NULL) {
    populate_metadata_value(env, dst, jni_cache, section, name, _value);
    bsg_safe_release_string_utf_chars(env, section_key, name);
  }

exit:
  bsg_safe_delete_local_ref(env, section_key);
  bsg_safe_delete_local_ref(env, _value);
}

static void populate_metadata_section(JNIEnv *env, bugsnag_metadata *dst,
                                      jobject metadata,
                                      bsg_jni_cache *jni_cache, jobject keylist,
                                      int i) {
  jstring _key = NULL;
  jobject _section = NULL;
  jobject section_keyset = NULL;
  jobject section_keylist = NULL;

  _key = bsg_safe_call_object_method(env, keylist, jni_cache->arraylist_get,
                                     (jint)i);
  if (_key == NULL) {
    goto exit;
  }
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
    populate_metadata_obj(env, dst, jni_cache, _section, section_keylist, j);
  }
  goto exit;

exit:
  bsg_safe_delete_local_ref(env, _key);
  bsg_safe_delete_local_ref(env, section_keyset);
  bsg_safe_delete_local_ref(env, section_keylist);
  bsg_safe_delete_local_ref(env, _section);
}

void bsg_populate_metadata(JNIEnv *env, bugsnag_metadata *dst,
                           jobject metadata) {
  bsg_jni_cache *jni_cache = bsg_get_jni_cache();
  if (jni_cache == NULL) {
    return;
  }

  jobject keyset = NULL;
  jobject keylist = NULL;
  jobject metadata_to_release = NULL;

  if (metadata == NULL) {
    metadata = bsg_safe_call_static_object_method(
        env, jni_cache->native_interface, jni_cache->get_metadata);
    metadata_to_release = metadata;
  }
  if (metadata == NULL) {
    dst->value_count = 0;
    goto exit;
  }
  int size = bsg_safe_call_int_method(env, metadata, jni_cache->map_size);
  if (size == -1) {
    goto exit;
  }

  // create a list of metadata keys
  keyset =
      bsg_safe_call_static_object_method(env, metadata, jni_cache->map_key_set);
  if (keyset == NULL) {
    goto exit;
  }
  keylist = bsg_safe_new_object(env, jni_cache->arraylist,
                                jni_cache->arraylist_init_with_obj, keyset);
  if (keylist == NULL) {
    goto exit;
  }

  for (int i = 0; i < size; i++) {
    populate_metadata_section(env, dst, metadata, jni_cache, keylist, i);
  }

exit:
  bsg_safe_delete_local_ref(env, keyset);
  bsg_safe_delete_local_ref(env, keylist);
  bsg_safe_delete_local_ref(env, metadata_to_release);
}
