#ifndef BUGSNAG_SAFEJNI_H
#define BUGSNAG_SAFEJNI_H

#include <jni.h>

/**
 * A safe wrapper for the JNI's FindClass. This method checks if an exception is
 * pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 *
 * See
 * https://docs.oracle.com/en/java/javase/11/docs/specs/jni/functions.html#findclass
 * @return the class, or NULL if the class could not be found.
 */
jclass bsg_safe_find_class(JNIEnv *env, const char *clz_name);

/**
 * A safe wrapper for the JNI's GetMethodID. This method checks if an exception
 * is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
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
 * The caller is responsible for handling the invalid return value of NULL.
 *
 * See
 * https://docs.oracle.com/en/java/javase/11/docs/specs/jni/functions.html#getstaticmethodid
 * @return the method ID, or NULL if the method could not be found.
 */
jmethodID bsg_safe_get_static_method_id(JNIEnv *env, jclass clz,
                                        const char *name, const char *sig);
/**
 * A safe wrapper for the JNI's NewStringUtf. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 *
 * See
 * https://docs.oracle.com/en/java/javase/11/docs/specs/jni/functions.html#newstringutf
 * @return the java string or NULL if it could not be created
 */
jstring bsg_safe_new_string_utf(JNIEnv *env, const char *str);

/**
 * A safe wrapper for the JNI's CallBooleanMethod. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * If an exception was thrown this method returns false.
 *
 * See https://docs.oracle.com/en/java/javase/11/docs/specs/jni/functions.html
 */
jboolean bsg_safe_call_boolean_method(JNIEnv *env, jobject _value,
                                      jmethodID method);

/**
 * A safe wrapper for the JNI's CallIntMethod. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * If an exception was thrown this method returns -1.
 *
 * See https://docs.oracle.com/en/java/javase/11/docs/specs/jni/functions.html
 */
jint bsg_safe_call_int_method(JNIEnv *env, jobject _value, jmethodID method);

/**
 * A safe wrapper for the JNI's CallFloatMethod. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * If an exception was thrown this method returns -1.
 *
 * See https://docs.oracle.com/en/java/javase/11/docs/specs/jni/functions.html
 */
jfloat bsg_safe_call_float_method(JNIEnv *env, jobject _value,
                                  jmethodID method);

/**
 * A safe wrapper for the JNI's CallDoubleMethod. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * If an exception was thrown this method returns -1.
 *
 * See https://docs.oracle.com/en/java/javase/11/docs/specs/jni/functions.html
 */
jdouble bsg_safe_call_double_method(JNIEnv *env, jobject _value,
                                    jmethodID method);

/**
 * A safe wrapper for the JNI's NewObjectArray. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 *
 * See
 * https://docs.oracle.com/en/java/javase/11/docs/specs/jni/functions.html#newobjectarray
 */
jobjectArray bsg_safe_new_object_array(JNIEnv *env, jsize size, jclass clz);

/**
 * A safe wrapper for the JNI's GetObjectArrayElement. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 *
 * See
 * https://docs.oracle.com/en/java/javase/11/docs/specs/jni/functions.html#getobjectarrayelement
 */
jobject bsg_safe_get_object_array_element(JNIEnv *env, jobjectArray array,
                                          jsize size);

/**
 * A safe wrapper for the JNI's SetObjectArrayElement. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 *
 * See
 * https://docs.oracle.com/en/java/javase/11/docs/specs/jni/functions.html#setobjectarrayelement
 */
void bsg_safe_set_object_array_element(JNIEnv *env, jobjectArray array,
                                       jsize size, jobject object);

/**
 * Constructs a byte array from a string.
 */
jbyteArray bsg_byte_ary_from_string(JNIEnv *env, const char *text);

#endif
