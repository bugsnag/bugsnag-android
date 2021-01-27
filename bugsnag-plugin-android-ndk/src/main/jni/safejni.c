#include "safejni.h"

jclass bsg_safe_find_class(JNIEnv *env, const char *clz_name) {
  jclass clz = (*env)->FindClass(env, clz_name);
  if ((*env)->ExceptionCheck(env)) {
    (*env)->ExceptionClear(env);
  }
  return clz;
}

jmethodID bsg_safe_get_method_id(JNIEnv *env, jclass clz, const char *name,
                                 const char *sig) {
  jmethodID methodId = (*env)->GetMethodID(env, clz, name, sig);
  if ((*env)->ExceptionCheck(env)) {
    (*env)->ExceptionClear(env);
  }
  return methodId;
}

jmethodID bsg_safe_get_static_method_id(JNIEnv *env, jclass clz,
                                        const char *name, const char *sig) {
  jmethodID methodId = (*env)->GetStaticMethodID(env, clz, name, sig);
  if ((*env)->ExceptionCheck(env)) {
    (*env)->ExceptionClear(env);
  }
  return methodId;
}
