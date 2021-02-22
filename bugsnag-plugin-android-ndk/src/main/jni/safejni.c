#include "safejni.h"
#include <bugsnag_ndk.h>
#include <stdbool.h>
#include <string.h>
#include <utils/string.h>

typedef int exception_result;
static const exception_result exception_none = 0;
static const exception_result exception_thrown = 1;

/**
 * Check for an exception and clear it if set.
 * @param env The JNI environment.
 * @return exception_thrown if there was an exception was thrown, exception_none
 * otherwise.
 */
static exception_result bsg_check_and_clear_exc(JNIEnv *env) {
  if (env == NULL) {
    return exception_none;
  }
  if ((*env)->ExceptionCheck(env) == JNI_FALSE) {
    return exception_none;
  }
  (*env)->ExceptionClear(env);
  return exception_thrown;
}

jclass bsg_safe_find_class(JNIEnv *env, const char *clz_name) {
  if (env == NULL) {
    return NULL;
  }
  if (clz_name == NULL) {
    return NULL;
  }
  jclass clz = (*env)->FindClass(env, clz_name);
  if (bsg_check_and_clear_exc(env) == exception_thrown || clz == NULL) {
    BUGSNAG_LOG("Could not find class \"%s\"", clz_name);
    return NULL;
  }
  return clz;
}

jmethodID bsg_safe_get_method_id(JNIEnv *env, jclass clz, const char *name,
                                 const char *sig) {
  if (env == NULL || clz == NULL || name == NULL || sig == NULL) {
    return NULL;
  }
  jmethodID methodId = (*env)->GetMethodID(env, clz, name, sig);
  if (bsg_check_and_clear_exc(env) == exception_thrown || methodId == NULL) {
    BUGSNAG_LOG("Could not find method \"%s\" with signature \"%s\"", name,
                sig);
    return NULL;
  }
  return methodId;
}

jmethodID bsg_safe_get_static_method_id(JNIEnv *env, jclass clz,
                                        const char *name, const char *sig) {
  if (env == NULL || clz == NULL || name == NULL || sig == NULL) {
    return NULL;
  }
  jmethodID methodId = (*env)->GetStaticMethodID(env, clz, name, sig);
  if (bsg_check_and_clear_exc(env) == exception_thrown || methodId == NULL) {
    BUGSNAG_LOG("Could not find static method \"%s\" with signature \"%s\"",
                name, sig);
    return NULL;
  }
  return methodId;
}

jstring bsg_safe_new_string_utf(JNIEnv *env, const char *str) {
  if (env == NULL || str == NULL) {
    return NULL;
  }
  jstring jstr = (*env)->NewStringUTF(env, str);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return NULL;
  }
  return jstr;
}

jboolean bsg_safe_call_boolean_method(JNIEnv *env, jobject _value,
                                      jmethodID method) {
  if (env == NULL || _value == NULL) {
    return false;
  }
  jboolean value = (*env)->CallBooleanMethod(env, _value, method);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return false; // default to false
  }
  return value;
}

jint bsg_safe_call_int_method(JNIEnv *env, jobject _value, jmethodID method) {
  if (env == NULL || _value == NULL) {
    return -1;
  }
  jint value = (*env)->CallIntMethod(env, _value, method);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return -1; // default to -1
  }
  return value;
}

jfloat bsg_safe_call_float_method(JNIEnv *env, jobject _value,
                                  jmethodID method) {
  if (env == NULL || _value == NULL) {
    return -1;
  }
  jfloat value = (*env)->CallFloatMethod(env, _value, method);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return -1; // default to -1
  }
  return value;
}

jdouble bsg_safe_call_double_method(JNIEnv *env, jobject _value,
                                    jmethodID method) {
  if (env == NULL || _value == NULL) {
    return -1;
  }
  jdouble value = (*env)->CallDoubleMethod(env, _value, method);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return -1; // default to -1
  }
  return value;
}

jobjectArray bsg_safe_new_object_array(JNIEnv *env, jsize size, jclass clz) {
  if (env == NULL || clz == NULL) {
    return NULL;
  }
  jobjectArray trace = (*env)->NewObjectArray(env, size, clz, NULL);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return NULL;
  }
  return trace;
}

jobject bsg_safe_get_object_array_element(JNIEnv *env, jobjectArray array,
                                          jsize size) {
  if (env == NULL || array == NULL) {
    return NULL;
  }
  jobject obj = (*env)->GetObjectArrayElement(env, array, size);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return NULL;
  }
  return obj;
}

bool bsg_safe_set_object_array_element(JNIEnv *env, jobjectArray array,
                                       jsize size, jobject object) {
  if (env == NULL || array == NULL) {
    return false;
  }
  (*env)->SetObjectArrayElement(env, array, size, object);
  return bsg_check_and_clear_exc(env) == exception_none;
}

jfieldID bsg_safe_get_static_field_id(JNIEnv *env, jclass clz, const char *name,
                                      const char *sig) {
  if (env == NULL || clz == NULL || name == NULL || sig == NULL) {
    return NULL;
  }
  jfieldID field_id = (*env)->GetStaticFieldID(env, clz, name, sig);
  if (bsg_check_and_clear_exc(env) == exception_thrown || field_id == NULL) {
    BUGSNAG_LOG("Could not find static field ID \"%s\" with signature \"%s\"",
                name, sig);
    return NULL;
  }
  return field_id;
}

jobject bsg_safe_get_static_object_field(JNIEnv *env, jclass clz,
                                         jfieldID field) {
  if (env == NULL || clz == NULL) {
    return NULL;
  }
  jobject obj = (*env)->GetStaticObjectField(env, clz, field);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return NULL;
  }
  return obj;
}

jobject bsg_safe_new_object(JNIEnv *env, jclass clz, jmethodID method, ...) {
  if (env == NULL || clz == NULL) {
    return NULL;
  }
  va_list args;
  va_start(args, method);
  jobject obj = (*env)->NewObjectV(env, clz, method, args);
  va_end(args);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return NULL;
  }
  return obj;
}

jobject bsg_safe_call_object_method(JNIEnv *env, jobject _value,
                                    jmethodID method, ...) {
  if (env == NULL || _value == NULL) {
    return NULL;
  }
  va_list args;
  va_start(args, method);
  jobject value = (*env)->CallObjectMethodV(env, _value, method, args);
  va_end(args);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return NULL;
  }
  return value;
}

bool bsg_safe_call_static_void_method(JNIEnv *env, jclass clz, jmethodID method,
                                      ...) {
  if (env == NULL || clz == NULL) {
    return false;
  }
  va_list args;
  va_start(args, method);
  (*env)->CallStaticVoidMethodV(env, clz, method, args);
  va_end(args);
  return bsg_check_and_clear_exc(env) == exception_none;
}

jobject bsg_safe_call_static_object_method(JNIEnv *env, jclass clz,
                                           jmethodID method, ...) {
  if (env == NULL || clz == NULL) {
    return NULL;
  }
  va_list args;
  va_start(args, method);
  jobject obj = (*env)->CallStaticObjectMethodV(env, clz, method, args);
  va_end(args);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return NULL;
  }
  return obj;
}

void bsg_safe_delete_local_ref(JNIEnv *env, jobject obj) {
  if (env == NULL || obj == NULL) {
    return;
  }
  (*env)->DeleteLocalRef(env, obj);
  bsg_check_and_clear_exc(env);
}

const char *bsg_safe_get_string_utf_chars(JNIEnv *env, jstring string) {
  if (env == NULL || string == NULL) {
    return NULL;
  }
  const char *str = (*env)->GetStringUTFChars(env, string, NULL);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return NULL;
  }
  return str;
}

void bsg_safe_release_string_utf_chars(JNIEnv *env, jstring string,
                                       const char *utf) {
  if (env == NULL || string == NULL || utf == NULL) {
    return;
  }
  (*env)->ReleaseStringUTFChars(env, string, utf);
  bsg_check_and_clear_exc(env);
}

void bsg_safe_release_byte_array_elements(JNIEnv *env, jbyteArray array,
                                          jbyte *elems) {
  if (env == NULL || array == NULL) {
    return;
  }

  // If mode is anything other than JNI_COMMIT, the JNI method will try and call
  // delete[] on the elems parameter, which leads to bad things happening (e.g.
  // aborting will cause it to free, blowing up any custom allocators).
  // Therefore JNI_COMMIT will always be called and the caller should free the
  // elems parameter themselves if necessary.
  // https://android.googlesource.com/platform/art/+/refs/heads/master/runtime/jni/jni_internal.cc#2689
  (*env)->ReleaseByteArrayElements(env, array, elems, JNI_COMMIT);
  bsg_check_and_clear_exc(env);
}

jsize bsg_safe_get_array_length(JNIEnv *env, jarray array) {
  if (env == NULL || array == NULL) {
    return -1;
  }
  jsize len = (*env)->GetArrayLength(env, array);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return -1;
  }
  return len;
}

jboolean bsg_safe_is_instance_of(JNIEnv *env, jobject object, jclass clz) {
  if (env == NULL || clz == NULL) {
    return false;
  }
  jboolean val = (*env)->IsInstanceOf(env, object, clz);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return false;
  }
  return val;
}

jbyteArray bsg_byte_ary_from_string(JNIEnv *env, const char *text) {
  if (env == NULL || text == NULL) {
    return NULL;
  }

  size_t text_length = strlen(text);
  jbyteArray jtext = (*env)->NewByteArray(env, text_length);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    return NULL;
  }

  (*env)->SetByteArrayRegion(env, jtext, 0, text_length, (jbyte *)text);
  if (bsg_check_and_clear_exc(env) == exception_thrown) {
    (*env)->DeleteLocalRef(env, jtext);
    bsg_check_and_clear_exc(env);
    return NULL;
  }

  return jtext;
}
