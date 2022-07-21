#ifndef BUGSNAG_SAFEJNI_H
#define BUGSNAG_SAFEJNI_H

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
jclass bsg_safe_find_class(JNIEnv *env, const char *clz_name);

/**
 * A safe wrapper for the JNI's GetMethodID. This method checks if an exception
 * is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 *
 * @return the method ID, or NULL if the method could not be found.
 */
jmethodID bsg_safe_get_method_id(JNIEnv *env, jclass clz, const char *name,
                                 const char *sig);

/**
 * A safe wrapper for the JNI's GetStaticMethodID. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 *
 * @return the method ID, or NULL if the method could not be found.
 */
jmethodID bsg_safe_get_static_method_id(JNIEnv *env, jclass clz,
                                        const char *name, const char *sig);
/**
 * A safe wrapper for the JNI's NewStringUtf. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 *
 * @return the java string or NULL if it could not be created
 */
jstring bsg_safe_new_string_utf(JNIEnv *env, const char *str);

/**
 * A safe wrapper for the JNI's CallVoidMethod. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * If an exception was thrown this method returns false.
 */
bool bsg_safe_call_void_method(JNIEnv *env, jobject _value, jmethodID method,
                               ...);

/**
 * A safe wrapper for the JNI's CallBooleanMethod. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * If an exception was thrown this method returns false.
 */
jboolean bsg_safe_call_boolean_method(JNIEnv *env, jobject _value,
                                      jmethodID method);

/**
 * A safe wrapper for the JNI's CallIntMethod. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * If an exception was thrown this method returns -1.
 */
jint bsg_safe_call_int_method(JNIEnv *env, jobject _value, jmethodID method);

/**
 * A safe wrapper for the JNI's CallFloatMethod. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * If an exception was thrown this method returns -1.
 */
jfloat bsg_safe_call_float_method(JNIEnv *env, jobject _value,
                                  jmethodID method);

/**
 * A safe wrapper for the JNI's CallDoubleMethod. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * If an exception was thrown this method returns -1.
 */
jdouble bsg_safe_call_double_method(JNIEnv *env, jobject _value,
                                    jmethodID method);

/**
 * A safe wrapper for the JNI's CallLongMethod. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * If an exception was thrown this method returns 0 (same as invoking
 * CallLongMethod directly).
 */
jlong bsg_safe_call_long_method(JNIEnv *env, jobject _value, jmethodID method);

/**
 * A safe wrapper for the JNI's NewObjectArray. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 */
jobjectArray bsg_safe_new_object_array(JNIEnv *env, jsize size, jclass clz);

/**
 * A safe wrapper for the JNI's GetObjectArrayElement. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 */
jobject bsg_safe_get_object_array_element(JNIEnv *env, jobjectArray array,
                                          jsize size);

/**
 * A safe wrapper for the JNI's SetObjectArrayElement. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 */
void bsg_safe_set_object_array_element(JNIEnv *env, jobjectArray array,
                                       jsize size, jobject object);

/**
 * A safe wrapper for the JNI's GetStaticFieldId. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 */
jfieldID bsg_safe_get_static_field_id(JNIEnv *env, jclass clz, const char *name,
                                      const char *sig);

/**
 * A safe wrapper for the JNI's GetStaticObjectField. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 */
jobject bsg_safe_get_static_object_field(JNIEnv *env, jclass clz,
                                         jfieldID field);

/**
 * A safe wrapper for the JNI's NewObject. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 */
jobject bsg_safe_new_object(JNIEnv *env, jclass clz, jmethodID method, ...);

/**
 * A safe wrapper for the JNI's CallObjectMethod. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 */
jobject bsg_safe_call_object_method(JNIEnv *env, jobject _value,
                                    jmethodID method, ...);

/**
 * A safe wrapper for the JNI's CallStaticVoidMethod. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 */
void bsg_safe_call_static_void_method(JNIEnv *env, jclass clz, jmethodID method,
                                      ...);

/**
 * A safe wrapper for the JNI's CallStaticObjectMethod. This method checks if an
 * exception is pending and if so clears it so that execution can continue.
 * The caller is responsible for handling the invalid return value of NULL.
 */
jobject bsg_safe_call_static_object_method(JNIEnv *env, jclass clz,
                                           jmethodID method, ...);

/**
 * A safe wrapper for the JNI's CallStaticBooleanMethod. This method checks if
 * an exception is pending and if so clears it so that execution can continue.
 * On failure or exception, this function return false.
 */
jboolean bsg_safe_call_static_boolean_method(JNIEnv *env, jclass clz,
                                             jmethodID method, ...);

/**
 * A safe wrapper for the JNI's DeleteLocalRef. This method checks if the env
 * is NULL and no-ops if so.
 */
void bsg_safe_delete_local_ref(JNIEnv *env, jobject obj);

/**
 * A safe wrapper for the JNI's GetStringUTFChars. This method checks if the
 * parameters are NULL and returns NULL if so. The caller is responsible for
 * checking for NULL return values.
 */
const char *bsg_safe_get_string_utf_chars(JNIEnv *env, jstring string);

/**
 * A safe wrapper for the JNI's ReleaseStringUTFChars. This method checks if the
 * parameters are NULL and no-ops if so.
 */
void bsg_safe_release_string_utf_chars(JNIEnv *env, jstring string,
                                       const char *utf);

/**
 * A safe wrapper for the JNI's ReleaseByteArrayElements. This method checks if
 * an exception is pending and if so clears it so that execution can continue.
 *
 * The caller is responsible for freeing the elems parameter after invoking this
 * method, if elems was allocated using malloc or new.
 */
void bsg_safe_release_byte_array_elements(JNIEnv *env, jbyteArray array,
                                          jbyte *elems);

/**
 * A safe wrapper for the JNI's GetArrayLength. This method checks if the
 * parameters are NULL and no-ops if so. The caller is responsible for handling
 * the invalid return value of -1.
 */
jsize bsg_safe_get_array_length(JNIEnv *env, jarray array);

/**
 * A safe wrapper for the JNI's IsInstanceOf. This method checks if the
 * parameters are NULL and returns false if so.
 */
jboolean bsg_safe_is_instance_of(JNIEnv *env, jobject object, jclass clz);

/**
 * Constructs a byte array from a string.
 */
jbyteArray bsg_byte_ary_from_string(JNIEnv *env, const char *text);

#endif
