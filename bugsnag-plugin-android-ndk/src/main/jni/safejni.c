#include "safejni.h"
#include <stdbool.h>
#include <utils/string.h>

bool bsg_check_and_clear_exc(JNIEnv *env) {
  if ((*env)->ExceptionCheck(env)) {
    (*env)->ExceptionClear(env);
    return true;
  }
  return false;
}

jclass bsg_safe_find_class(JNIEnv *env, const char *clz_name) {
  jclass clz = (*env)->FindClass(env, clz_name);
  bsg_check_and_clear_exc(env);
  return clz;
}

jmethodID bsg_safe_get_method_id(JNIEnv *env, jclass clz, const char *name,
                                 const char *sig) {
  jmethodID methodId = (*env)->GetMethodID(env, clz, name, sig);
  bsg_check_and_clear_exc(env);
  return methodId;
}

jmethodID bsg_safe_get_static_method_id(JNIEnv *env, jclass clz,
                                        const char *name, const char *sig) {
  jmethodID methodId = (*env)->GetStaticMethodID(env, clz, name, sig);
  bsg_check_and_clear_exc(env);
  return methodId;
}

jstring bsg_safe_new_string_utf(JNIEnv *env, const char *str) {
  jstring jstr = (*env)->NewStringUTF(env, str);
  bsg_check_and_clear_exc(env);
  return jstr;
}

jboolean bsg_safe_call_boolean_method(JNIEnv *env, jobject _value,
                                      jmethodID method) {
  jboolean value = (*env)->CallBooleanMethod(env, _value, method);
  if (bsg_check_and_clear_exc(env)) {
    return false; // default to false
  }
  return value;
}

jint bsg_safe_call_int_method(JNIEnv *env, jobject _value, jmethodID method) {
  jint value = (*env)->CallIntMethod(env, _value, method);
  if (bsg_check_and_clear_exc(env)) {
    return -1; // default to -1
  }
  return value;
}

jfloat bsg_safe_call_float_method(JNIEnv *env, jobject _value,
                                  jmethodID method) {
  jfloat value = (*env)->CallFloatMethod(env, _value, method);
  if (bsg_check_and_clear_exc(env)) {
    return -1; // default to -1
  }
  return value;
}

jdouble bsg_safe_call_double_method(JNIEnv *env, jobject _value,
                                    jmethodID method) {
  jdouble value = (*env)->CallDoubleMethod(env, _value, method);
  if (bsg_check_and_clear_exc(env)) {
    return -1; // default to -1
  }
  return value;
}

jobjectArray bsg_safe_new_object_array(JNIEnv *env, jsize size, jclass clz) {
  jobjectArray trace = (*env)->NewObjectArray(env, size, clz, NULL);
  bsg_check_and_clear_exc(env);
  return trace;
}

jobject bsg_safe_get_object_array_element(JNIEnv *env, jobjectArray array,
                                          jsize size) {
  jobject obj = (*env)->GetObjectArrayElement(env, array, size);
  bsg_check_and_clear_exc(env);
  return obj;
}

void bsg_safe_set_object_array_element(JNIEnv *env, jobjectArray array,
                                       jsize size, jobject object) {
  (*env)->SetObjectArrayElement(env, array, size, object);
  bsg_check_and_clear_exc(env);
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