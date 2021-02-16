#include "safejni.h"
#include <stdbool.h>
#include <utils/string.h>

bool bsg_check_and_clear_exc(JNIEnv *env) {
  if (env == NULL) {
    return false;
  }
  if ((*env)->ExceptionCheck(env)) {
    (*env)->ExceptionClear(env);
    return true;
  }
  return false;
}

jclass bsg_safe_find_class(JNIEnv *env, const char *clz_name) {
  if (env == NULL) {
    return NULL;
  }
  if (clz_name == NULL) {
    return NULL;
  }
  jclass clz = (*env)->FindClass(env, clz_name);
  bsg_check_and_clear_exc(env);
  return clz;
}

jmethodID bsg_safe_get_method_id(JNIEnv *env, jclass clz, const char *name,
                                 const char *sig) {
  if (env == NULL || clz == NULL || name == NULL || sig == NULL) {
    return NULL;
  }
  jmethodID methodId = (*env)->GetMethodID(env, clz, name, sig);
  bsg_check_and_clear_exc(env);
  return methodId;
}

jmethodID bsg_safe_get_static_method_id(JNIEnv *env, jclass clz,
                                        const char *name, const char *sig) {
  if (env == NULL || clz == NULL || name == NULL || sig == NULL) {
    return NULL;
  }
  jmethodID methodId = (*env)->GetStaticMethodID(env, clz, name, sig);
  bsg_check_and_clear_exc(env);
  return methodId;
}

jstring bsg_safe_new_string_utf(JNIEnv *env, const char *str) {
  if (env == NULL || str == NULL) {
    return NULL;
  }
  jstring jstr = (*env)->NewStringUTF(env, str);
  bsg_check_and_clear_exc(env);
  return jstr;
}

jboolean bsg_safe_call_boolean_method(JNIEnv *env, jobject _value,
                                      jmethodID method) {
  if (env == NULL || _value == NULL) {
    return false;
  }
  jboolean value = (*env)->CallBooleanMethod(env, _value, method);
  if (bsg_check_and_clear_exc(env)) {
    return false; // default to false
  }
  return value;
}

jint bsg_safe_call_int_method(JNIEnv *env, jobject _value, jmethodID method) {
  if (env == NULL || _value == NULL) {
    return -1;
  }
  jint value = (*env)->CallIntMethod(env, _value, method);
  if (bsg_check_and_clear_exc(env)) {
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
  if (bsg_check_and_clear_exc(env)) {
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
  if (bsg_check_and_clear_exc(env)) {
    return -1; // default to -1
  }
  return value;
}

jobjectArray bsg_safe_new_object_array(JNIEnv *env, jsize size, jclass clz) {
  if (env == NULL || clz == NULL) {
    return NULL;
  }
  jobjectArray trace = (*env)->NewObjectArray(env, size, clz, NULL);
  bsg_check_and_clear_exc(env);
  return trace;
}

jobject bsg_safe_get_object_array_element(JNIEnv *env, jobjectArray array,
                                          jsize size) {
  if (env == NULL || array == NULL) {
    return NULL;
  }
  jobject obj = (*env)->GetObjectArrayElement(env, array, size);
  bsg_check_and_clear_exc(env);
  return obj;
}

void bsg_safe_set_object_array_element(JNIEnv *env, jobjectArray array,
                                       jsize size, jobject object) {
  if (env == NULL || array == NULL) {
    return;
  }
  (*env)->SetObjectArrayElement(env, array, size, object);
  bsg_check_and_clear_exc(env);
}

jfieldID bsg_safe_get_static_field_id(JNIEnv *env, jclass clz, const char *name,
                                      const char *sig) {
  if (env == NULL || clz == NULL || name == NULL || sig == NULL) {
    return NULL;
  }
  jfieldID field_id = (*env)->GetStaticFieldID(env, clz, name, sig);
  bsg_check_and_clear_exc(env);
  return field_id;
}

jobject bsg_safe_get_static_object_field(JNIEnv *env, jclass clz,
                                         jfieldID field) {
  if (env == NULL || clz == NULL) {
    return NULL;
  }
  jobject obj = (*env)->GetStaticObjectField(env, clz, field);
  bsg_check_and_clear_exc(env);
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
  bsg_check_and_clear_exc(env);
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
  bsg_check_and_clear_exc(env);
  return value;
}

void bsg_safe_call_static_void_method(JNIEnv *env, jclass clz, jmethodID method,
                                      ...) {
  if (env == NULL || clz == NULL) {
    return;
  }
  va_list args;
  va_start(args, method);
  (*env)->CallStaticVoidMethodV(env, clz, method, args);
  va_end(args);
  bsg_check_and_clear_exc(env);
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
  bsg_check_and_clear_exc(env);
  return obj;
}

void bsg_safe_delete_local_ref(JNIEnv *env, jobject obj) {
  if (env != NULL) {
    (*env)->DeleteLocalRef(env, obj);
  }
}

const char *bsg_safe_get_string_utf_chars(JNIEnv *env, jstring string) {
  if (env != NULL && string != NULL) {
    return (*env)->GetStringUTFChars(env, string, NULL);
  }
  return NULL;
}

void bsg_safe_release_string_utf_chars(JNIEnv *env, jstring string,
                                       const char *utf) {
  if (env != NULL && string != NULL && utf != NULL) {
    (*env)->ReleaseStringUTFChars(env, string, utf);
  }
}

void bsg_safe_release_byte_array_elements(JNIEnv *env, jbyteArray array,
                                          jbyte *elems) {
  // If mode is anything other than JNI_COMMIT, the JNI method will try and call
  // delete[] on the elems parameter, which leads to bad things happening (e.g.
  // aborting will cause it to free, blowing up any custom allocators).
  // Therefore JNI_COMMIT will always be called and the caller should free the
  // elems parameter themselves if necessary.
  // https://android.googlesource.com/platform/art/+/refs/heads/master/runtime/jni/jni_internal.cc#2689
  if (env != NULL && array != NULL) {
    (*env)->ReleaseByteArrayElements(env, array, elems, JNI_COMMIT);
  }
}

jsize bsg_safe_get_array_length(JNIEnv *env, jarray array) {
  if (env != NULL && array != NULL) {
    return (*env)->GetArrayLength(env, array);
  }
  return -1;
}

jboolean bsg_safe_is_instance_of(JNIEnv *env, jobject object, jclass clz) {
  if (env != NULL && clz != NULL) {
    return (*env)->IsInstanceOf(env, object, clz);
  }
  return false;
}

jbyteArray bsg_byte_ary_from_string(JNIEnv *env, const char *text) {
  if (text == NULL) {
    return NULL;
  }
  size_t text_length = bsg_strlen(text);
  jbyteArray jtext = (*env)->NewByteArray(env, text_length);

  if (bsg_check_and_clear_exc(env)) {
    return NULL;
  }
  (*env)->SetByteArrayRegion(env, jtext, 0, text_length, (jbyte *)text);

  if (bsg_check_and_clear_exc(env)) {
    return NULL;
  }
  return jtext;
}