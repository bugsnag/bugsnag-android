#ifndef BUGSNAG_SAFEJNI_H
#define BUGSNAG_SAFEJNI_H

#include <jni.h>

/**
 * A safe wrapper for the JNI's FindClass. This method checks if an exception is
 * pending and if so clears it so that execution can continue.
 *
 * This method should be preferred to using the JNI methods directly. It is the
 * responsibility of the caller to check whether the return value is NULL and
 * handle this by no-oping.
 *
 * See
 * https://docs.oracle.com/en/java/javase/11/docs/specs/jni/functions.html#findclass
 * @return the class, or NULL if the class could not be found.
 */
jclass bsg_safe_find_class(JNIEnv *env, const char *clz_name);

/**
 * A safe wrapper for the JNI's GetMethodID. This method checks if an exception
 * is pending and if so clears it so that execution can continue.
 *
 * This method should be preferred to using the JNI methods directly. It is the
 * responsibility of the caller to check whether the return value is NULL and
 * handle this by no-oping.
 *
 * See
 * https://docs.oracle.com/en/java/javase/11/docs/specs/jni/functions.html#getmethodid
 * @return the method ID, or NULL if the method could not be found.
 */
jmethodID bsg_safe_get_method_id(JNIEnv *env, jclass clz, const char *name,
                                 const char *sig);

/**
 * A safe wrapper for the JNI's GetStaticMethodID. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 *
 * This method should be preferred to using the JNI methods directly. It is the
 * responsibility of the caller to check whether the return value is NULL and
 * handle this by no-oping.
 *
 * See
 * https://docs.oracle.com/en/java/javase/11/docs/specs/jni/functions.html#getstaticmethodid
 * @return the method ID, or NULL if the method could not be found.
 */
jmethodID bsg_safe_get_static_method_id(JNIEnv *env, jclass clz,
                                        const char *name, const char *sig);

#endif
