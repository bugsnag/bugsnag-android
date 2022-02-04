// Copied from safejni.h in bugsnag-plugin-android-ndk (see PLAT-5794).
// Please keep logic code in sync.

#ifndef BUGSNAG_ANR_SAFEJNI_H
#define BUGSNAG_ANR_SAFEJNI_H

#include <jni.h>
#include <stdbool.h>

/**
 * This provides safe JNI calls by wrapping functions and calling
 * ExceptionClear(). This approach prevents crashes and undefined behaviour,
 * providing the caller checks the return value of each invocation.
 *
 * For an overview of the methods decorated here, please see
 * https://docs.oracle.com/en/java/javase/11/docs/specs/jni/functions.html
 */

/**
 * A safe wrapper for the JNI's FindClass. This method checks if an exception is
 * pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 *
 * @return the class, or NULL if the class could not be found.
 */
jclass bsg_anr_safe_find_class(JNIEnv *env, const char *clz_name);

/**
 * A safe wrapper for the JNI's GetMethodID. This method checks if an exception
 * is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 *
 * @return the method ID, or NULL if the method could not be found.
 */
jmethodID bsg_anr_safe_get_method_id(JNIEnv *env, jclass clz, const char *name,
                                     const char *sig);

/**
 * A safe wrapper for the JNI's GetStaticMethodID. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 *
 * @return the method ID, or NULL if the method could not be found.
 */
jmethodID bsg_anr_safe_get_static_method_id(JNIEnv *env, jclass clz,
                                            const char *name, const char *sig);
/**
 * A safe wrapper for the JNI's NewStringUtf. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 *
 * @return the java string or NULL if it could not be created
 */
jstring bsg_anr_safe_new_string_utf(JNIEnv *env, const char *str);

/**
 * A safe wrapper for the JNI's GetStaticFieldId. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 */
jfieldID bsg_anr_safe_get_static_field_id(JNIEnv *env, jclass clz,
                                          const char *name, const char *sig);

/**
 * A safe wrapper for the JNI's GetStaticObjectField. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 */
jobject bsg_anr_safe_get_static_object_field(JNIEnv *env, jclass clz,
                                             jfieldID field);

/**
 * A safe wrapper for the JNI's NewObject. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 */
jobject bsg_anr_safe_new_object(JNIEnv *env, jclass clz, jmethodID method, ...);

/**
 * A safe wrapper for the JNI's DeleteLocalRef. This method checks if the env
 * is NULL and no-ops if so.
 */
void bsg_anr_safe_delete_local_ref(JNIEnv *env, jobject obj);

/**
 * Clear any raised exceptions. This will also report any raised exceptions to
 * stderr.
 * @param env The JNI env
 * @return true if there was a raised exception
 */
bool bsg_anr_check_and_clear_exc(JNIEnv *env);

#endif
