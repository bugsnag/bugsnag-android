#include "metadata.h"
#include "safejni.h"
#include "jnicache.h"
#include "report.h"
#include "utils/jni_utils.h"
#include "utils/string.h"
#include <malloc.h>
#include <string.h>

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
    int count = bsg_safe_get_array_length(env, _value);

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
    }
    bsg_safe_delete_local_ref(env, _value);
    return count;
  }
  return 0;
}

void bsg_populate_crumb_metadata(JNIEnv *env, bugsnag_breadcrumb *crumb,
                                 jobject metadata) {
  if (!bsg_is_jni_cache_valid()) {
    return;
  }

  bsg_jni_cache *jni_cache = bsg_get_jni_cache();
  jobject keyset = NULL;
  jobject keylist = NULL;

  if (metadata == NULL) {
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
      bsg_safe_delete_local_ref(env, _key);
      bsg_safe_delete_local_ref(env, _value);
    } else {
      const char *key = bsg_safe_get_string_utf_chars(env, _key);

      if (key != NULL) {
        const char *value = bsg_safe_get_string_utf_chars(env, _value);

        if (value != NULL) {
          bsg_strncpy_safe(crumb->metadata[i].key, key,
                           sizeof(crumb->metadata[i].key));
          bsg_strncpy_safe(crumb->metadata[i].value, value,
                           sizeof(crumb->metadata[i].value));
          bsg_safe_release_string_utf_chars(env, _value, value);
        }
        bsg_safe_release_string_utf_chars(env, _key, key);
      }
    }
  }
  goto exit;

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
  bsg_safe_delete_local_ref(env, data);
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
    bsg_safe_delete_local_ref(env, _runtime_versions);
  }

  bsg_safe_delete_local_ref(env, data);
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
  bsg_safe_delete_local_ref(env, data);
}

void bsg_populate_context(JNIEnv *env, bsg_jni_cache *jni_cache,
                          bugsnag_report *report) {
  jstring _context = bsg_safe_call_static_object_method(
          env, jni_cache->native_interface, jni_cache->get_context);
  if (_context != NULL) {
    const char *value = bsg_safe_get_string_utf_chars(env, (jstring)_context);
    if (value != NULL) {
      bsg_strncpy_safe(report->context, value, sizeof(report->context) - 1);
      bsg_safe_release_string_utf_chars(env, _context, value);
    }
  } else {
    memset(&report->context, 0, strlen(report->context));
  }
}

void bsg_populate_report(JNIEnv *env, bugsnag_report *report) {
  if (!bsg_is_jni_cache_valid()) {
    return;
  }

  bsg_jni_cache *jni_cache = bsg_get_jni_cache();
  bsg_populate_context(env, jni_cache, report);
  bsg_populate_app_data(env, jni_cache, report);
  bsg_populate_device_data(env, jni_cache, report);
  bsg_populate_user_data(env, jni_cache, report);
}

void bsg_populate_metadata_value(JNIEnv *env, bugsnag_report *dst,
                                 bsg_jni_cache *jni_cache, char *section,
                                 char *name, jobject _value) {
  if (bsg_safe_is_instance_of(env, _value, jni_cache->number)) {
    // add a double metadata value
    double value = bsg_safe_call_double_method(env, _value,
                                               jni_cache->number_double_value);
    bugsnag_report_add_metadata_double(dst, section, name, value);
  } else if (bsg_safe_is_instance_of(env, _value, jni_cache->boolean)) {
    // add a boolean metadata value
    bool value = bsg_safe_call_boolean_method(env, _value,
                                              jni_cache->boolean_bool_value);
    bugsnag_report_add_metadata_bool(dst, section, name, value);
  } else if (bsg_safe_is_instance_of(env, _value, jni_cache->string)) {
    char *value = (char *) bsg_safe_get_string_utf_chars(env, _value);
    if (value != NULL) {
      bugsnag_report_add_metadata_string(dst, section, name, value);
      bsg_safe_release_string_utf_chars(env, _value, name);
    }
  }
}

void bsg_populate_metadata_obj(JNIEnv *env, bugsnag_report *dst,
                               bsg_jni_cache *jni_cache, jobject section,
                               void *section_keylist, int index) {
  jstring section_key = bsg_safe_call_object_method(
          env, section_keylist, jni_cache->arraylist_get, (jint)index);
  if (section_key == NULL) {
    return;
  }

  jobject _value = bsg_safe_call_object_method(env, section, jni_cache->map_get,
                                               section_key);
  char *name = (char *) bsg_safe_get_string_utf_chars(env, section_key);
  if (name != NULL) {
    bsg_populate_metadata_value(env, dst, jni_cache, section, name, _value);
    bsg_safe_release_string_utf_chars(env, section_key, name);
  }
  bsg_safe_delete_local_ref(env, _value);
}

void bsg_populate_metadata_section(JNIEnv *env, bugsnag_report *report,
                                   jobject metadata, bsg_jni_cache *jni_cache,
                                   jobject keylist, int i) {
  jstring _key = NULL;
  const char *section = NULL;
  jobject _section = NULL;
  jobject section_keyset = NULL;
  jobject section_keylist = NULL;

  _key = bsg_safe_call_object_method(env, keylist, jni_cache->arraylist_get,
                                     (jint)i);
  if (_key == NULL) {
    goto exit;
  }
  section = bsg_safe_get_string_utf_chars(env, _key);
  if (section == NULL) {
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
    bsg_populate_metadata_obj(env, report, jni_cache, (char *)section, section_keylist, j);
  }
  goto exit;

  exit:
  bsg_safe_release_string_utf_chars(env, _key, section);
  bsg_safe_delete_local_ref(env, section_keyset);
  bsg_safe_delete_local_ref(env, section_keylist);
  bsg_safe_delete_local_ref(env, _section);
}

void bsg_populate_metadata(JNIEnv *env, bugsnag_report *report,
                           jobject metadata) {
  if (!bsg_is_jni_cache_valid()) {
    return;
  }

  bsg_jni_cache *jni_cache = bsg_get_jni_cache();
  jobject keyset = NULL;
  jobject keylist = NULL;

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
  bsg_safe_delete_local_ref(env, keyset);
  bsg_safe_delete_local_ref(env, keylist);
}
