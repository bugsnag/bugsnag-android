//
// Created by Karl Stenerud on 22.02.21.
//

#include "jni_utils.h"
#include "jnicache.h"
#include "safejni.h"
#include "utils/string.h"

jobject bsg_get_map_value_obj(JNIEnv *env, bsg_jni_cache *jni_cache,
                              jobject map, const char *_key) {
  // create Java string object for map key
  jstring key = bsg_safe_new_string_utf(env, _key);
  if (key == NULL) {
    return NULL;
  }

  jobject obj =
          bsg_safe_call_object_method(env, map, jni_cache->hash_map_get, key);
  bsg_safe_delete_local_ref(env, key);
  return obj;
}

void bsg_copy_map_value_string(JNIEnv *env, bsg_jni_cache *jni_cache,
                               jobject map, const char *_key, char *dest,
                               int len) {
  jobject _value = bsg_get_map_value_obj(env, jni_cache, map, _key);

  if (_value != NULL) {
    const char *value = bsg_safe_get_string_utf_chars(env, (jstring)_value);
    if (value != NULL) {
      bsg_strncpy_safe(dest, value, len);
      bsg_safe_release_string_utf_chars(env, _value, value);
    }
  }
}

long bsg_get_map_value_long(JNIEnv *env, bsg_jni_cache *jni_cache, jobject map,
                            const char *_key) {
  jobject _value = bsg_get_map_value_obj(env, jni_cache, map, _key);

  if (_value != NULL) {
    long value = bsg_safe_call_double_method(env, _value,
                                             jni_cache->number_double_value);
    bsg_safe_delete_local_ref(env, _value);
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
    bsg_safe_delete_local_ref(env, _value);
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
    bsg_safe_delete_local_ref(env, _value);
    return value;
  }
  return 0;
}

bool bsg_get_map_value_bool(JNIEnv *env, bsg_jni_cache *jni_cache, jobject map,
                            const char *_key) {
  jobject obj = bsg_get_map_value_obj(env, jni_cache, map, _key);
  return bsg_safe_call_boolean_method(env, obj, jni_cache->boolean_bool_value);
}
