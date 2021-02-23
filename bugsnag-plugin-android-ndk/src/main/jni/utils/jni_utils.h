//
// Created by Karl Stenerud on 22.02.21.
//

// Utils to deal with common JNI situations.

#ifndef BUGSNAG_ANDROID_JNI_UTILS_H
#define BUGSNAG_ANDROID_JNI_UTILS_H

#include <jni.h>
#include <stdbool.h>
#include "jnicache.h"

jobject bsg_get_map_value_obj(JNIEnv *env, bsg_jni_cache *jni_cache,
                              jobject map, const char *_key);

void bsg_copy_map_value_string(JNIEnv *env, bsg_jni_cache *jni_cache,
                               jobject map, const char *_key, char *dest,
                               int len);

long bsg_get_map_value_long(JNIEnv *env, bsg_jni_cache *jni_cache, jobject map,
                            const char *_key);

float bsg_get_map_value_float(JNIEnv *env, bsg_jni_cache *jni_cache,
                              jobject map, const char *_key);

int bsg_get_map_value_int(JNIEnv *env, bsg_jni_cache *jni_cache, jobject map,
                          const char *_key);

bool bsg_get_map_value_bool(JNIEnv *env, bsg_jni_cache *jni_cache, jobject map,
                            const char *_key);

#endif //BUGSNAG_ANDROID_JNI_UTILS_H
