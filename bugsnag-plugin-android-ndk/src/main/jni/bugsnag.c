/** \brief The public API
 */
#include "../assets/include/bugsnag.h"
#include "bugsnag_ndk.h"
#include "event.h"
#include "metadata.h"
#include "safejni.h"
#include "utils/stack_unwinder.h"
#include "utils/string.h"
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static JNIEnv *bsg_global_jni_env = NULL;

void bugsnag_set_binary_arch(JNIEnv *env);

void bugsnag_start(JNIEnv *env) { bsg_global_jni_env = env; }

void bugsnag_notify_env(JNIEnv *env, char *name, char *message,
                        bugsnag_severity severity);
void bugsnag_set_user_env(JNIEnv *env, char *id, char *email, char *name);
void bugsnag_leave_breadcrumb_env(JNIEnv *env, char *message,
                                  bugsnag_breadcrumb_type type);

void bugsnag_notify(char *name, char *message, bugsnag_severity severity) {
  if (bsg_global_jni_env != NULL) {
    bugsnag_notify_env(bsg_global_jni_env, name, message, severity);
  } else {
    BUGSNAG_LOG("Cannot bugsnag_notify before initializing with bugsnag_start");
  }
}

void bugsnag_set_user(char *id, char *email, char *name) {
  if (bsg_global_jni_env != NULL) {
    bugsnag_set_user_env(bsg_global_jni_env, id, email, name);
  } else {
    BUGSNAG_LOG(
        "Cannot bugsnag_set_user before initializing with bugsnag_start");
  }
}

void bugsnag_leave_breadcrumb(char *message, bugsnag_breadcrumb_type type) {
  if (bsg_global_jni_env != NULL) {
    bugsnag_leave_breadcrumb_env(bsg_global_jni_env, message, type);
  } else {
    BUGSNAG_LOG("Cannot bugsnag_leave_breadcrumb_env before initializing with "
                "bugsnag_start");
  }
}

jfieldID bsg_parse_jseverity(JNIEnv *env, bugsnag_severity severity,
                             jclass severity_class) {
  const char *severity_sig = "Lcom/bugsnag/android/Severity;";
  if (severity == BSG_SEVERITY_ERR) {
    return (*env)->GetStaticFieldID(env, severity_class, "ERROR", severity_sig);
  } else if (severity == BSG_SEVERITY_WARN) {
    return (*env)->GetStaticFieldID(env, severity_class, "WARNING",
                                    severity_sig);
  } else {
    return (*env)->GetStaticFieldID(env, severity_class, "INFO", severity_sig);
  }
}

jbyteArray bsg_byte_ary_from_string(JNIEnv *env, char *text) {
  if (text == NULL) {
    return NULL;
  }
  size_t text_length = bsg_strlen(text);
  jbyteArray jtext = (*env)->NewByteArray(env, text_length);
  (*env)->SetByteArrayRegion(env, jtext, 0, text_length, (jbyte *)text);

  return jtext;
}

void bsg_release_byte_ary(JNIEnv *env, jbyteArray array, char *original_text) {
  if (array != NULL) {
    (*env)->ReleaseByteArrayElements(env, array, (jbyte *)original_text,
                                     JNI_COMMIT);
  }
}

void bugsnag_notify_env(JNIEnv *env, char *name, char *message,
                        bugsnag_severity severity) {
  bugsnag_stackframe stacktrace[BUGSNAG_FRAMES_MAX];
  ssize_t frame_count =
      bsg_unwind_stack(bsg_configured_unwind_style(), stacktrace, NULL, NULL);

  // lookup com/bugsnag/android/NativeInterface
  jclass interface_class =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (interface_class == NULL) {
    return;
  }

  // lookup NativeInterface.notify()
  jmethodID notify_method = bsg_safe_get_static_method_id(
      env, interface_class, "notify",
      "([B[BLcom/bugsnag/android/Severity;[Ljava/lang/StackTraceElement;)V");
  if (notify_method == NULL) {
    return;
  }

  // lookup java/lang/StackTraceElement
  jclass trace_class = bsg_safe_find_class(env, "java/lang/StackTraceElement");
  if (trace_class == NULL) {
    return;
  }

  // lookup com/bugsnag/android/Severity
  jclass severity_class =
      bsg_safe_find_class(env, "com/bugsnag/android/Severity");
  if (severity_class == NULL) {
    return;
  }

  // lookup StackTraceElement constructor
  jmethodID trace_constructor = bsg_safe_get_method_id(
      env, trace_class, "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
  if (trace_constructor == NULL) {
    return;
  }

  jobjectArray trace = (*env)->NewObjectArray(
      env, (jsize)frame_count,
      bsg_safe_find_class(env, "java/lang/StackTraceElement"), NULL);

  for (int i = 0; i < frame_count; i++) {
    bugsnag_stackframe frame = stacktrace[i];
    jstring class = (*env)->NewStringUTF(env, "");
    jstring filename = (*env)->NewStringUTF(env, frame.filename);
    jstring method;
    if (strlen(frame.method) == 0) {
      char *frame_address = malloc(sizeof(char) * 32);
      sprintf(frame_address, "0x%lx", (unsigned long)frame.frame_address);
      method = (*env)->NewStringUTF(env, frame_address);
      free(frame_address);
    } else {
      method = (*env)->NewStringUTF(env, frame.method);
    }
    jobject jframe =
        (*env)->NewObject(env, trace_class, trace_constructor, class, method,
                          filename, frame.line_number);

    (*env)->SetObjectArrayElement(env, trace, i, jframe);
    (*env)->DeleteLocalRef(env, filename);
    (*env)->DeleteLocalRef(env, class);
    (*env)->DeleteLocalRef(env, method);
  }

  // Create a severity Error
  jobject jseverity = (*env)->GetStaticObjectField(
      env, severity_class, bsg_parse_jseverity(env, severity, severity_class));

  jbyteArray jname = bsg_byte_ary_from_string(env, name);
  jbyteArray jmessage = bsg_byte_ary_from_string(env, message);

  // set application's binary arch
  bugsnag_set_binary_arch(env);

  (*env)->CallStaticVoidMethod(env, interface_class, notify_method, jname,
                               jmessage, jseverity, trace);

  bsg_release_byte_ary(env, jname, name);
  bsg_release_byte_ary(env, jmessage, message);
  (*env)->DeleteLocalRef(env, jname);
  (*env)->DeleteLocalRef(env, jmessage);

  (*env)->DeleteLocalRef(env, trace_class);
  (*env)->DeleteLocalRef(env, trace);
  (*env)->DeleteLocalRef(env, severity_class);
  (*env)->DeleteLocalRef(env, jseverity);
  (*env)->DeleteLocalRef(env, interface_class);
}

void bugsnag_set_binary_arch(JNIEnv *env) {
  // lookup com/bugsnag/android/NativeInterface
  jclass interface_class =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (interface_class == NULL) {
    return;
  }

  // lookup NativeInterface.setBinaryArch()
  jmethodID set_arch_method = bsg_safe_get_static_method_id(
      env, interface_class, "setBinaryArch", "(Ljava/lang/String;)V");
  if (set_arch_method == NULL) {
    return;
  }

  jstring arch = (*env)->NewStringUTF(env, bsg_binary_arch());
  (*env)->CallStaticVoidMethod(env, interface_class, set_arch_method, arch);
  (*env)->DeleteLocalRef(env, arch);
  (*env)->DeleteLocalRef(env, interface_class);
}

void bugsnag_set_user_env(JNIEnv *env, char *id, char *email, char *name) {
  // lookup com/bugsnag/android/NativeInterface
  jclass interface_class =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (interface_class == NULL) {
    return;
  }

  // lookup NativeInterface.setUser()
  jmethodID set_user_method = bsg_safe_get_static_method_id(
      env, interface_class, "setUser", "([B[B[B)V");
  if (set_user_method == NULL) {
    return;
  }

  jbyteArray jid = bsg_byte_ary_from_string(env, id);
  jbyteArray jemail = bsg_byte_ary_from_string(env, email);
  jbyteArray jname = bsg_byte_ary_from_string(env, name);

  (*env)->CallStaticVoidMethod(env, interface_class, set_user_method, jid,
                               jemail, jname);

  bsg_release_byte_ary(env, jid, id);
  bsg_release_byte_ary(env, jemail, email);
  bsg_release_byte_ary(env, jname, name);

  (*env)->DeleteLocalRef(env, jid);
  (*env)->DeleteLocalRef(env, jemail);
  (*env)->DeleteLocalRef(env, jname);
  (*env)->DeleteLocalRef(env, interface_class);
}

jfieldID bsg_parse_jcrumb_type(JNIEnv *env, bugsnag_breadcrumb_type type,
                               jclass type_class) {
  const char *type_sig = "Lcom/bugsnag/android/BreadcrumbType;";
  if (type == BSG_CRUMB_USER) {
    return (*env)->GetStaticFieldID(env, type_class, "USER", type_sig);
  } else if (type == BSG_CRUMB_ERROR) {
    return (*env)->GetStaticFieldID(env, type_class, "ERROR", type_sig);
  } else if (type == BSG_CRUMB_LOG) {
    return (*env)->GetStaticFieldID(env, type_class, "LOG", type_sig);
  } else if (type == BSG_CRUMB_NAVIGATION) {
    return (*env)->GetStaticFieldID(env, type_class, "NAVIGATION", type_sig);
  } else if (type == BSG_CRUMB_PROCESS) {
    return (*env)->GetStaticFieldID(env, type_class, "PROCESS", type_sig);
  } else if (type == BSG_CRUMB_REQUEST) {
    return (*env)->GetStaticFieldID(env, type_class, "REQUEST", type_sig);
  } else if (type == BSG_CRUMB_STATE) {
    return (*env)->GetStaticFieldID(env, type_class, "STATE", type_sig);
  } else { // MANUAL is the default type
    return (*env)->GetStaticFieldID(env, type_class, "MANUAL", type_sig);
  }
}

void bugsnag_leave_breadcrumb_env(JNIEnv *env, char *message,
                                  bugsnag_breadcrumb_type type) {
  // lookup com/bugsnag/android/NativeInterface
  jclass interface_class =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (interface_class == NULL) {
    return;
  }

  // lookup NativeInterface.leaveBreadcrumb()
  jmethodID leave_breadcrumb_method = bsg_safe_get_static_method_id(
      env, interface_class, "leaveBreadcrumb",
      "([BLcom/bugsnag/android/BreadcrumbType;)V");
  if (interface_class == NULL) {
    return;
  }

  // lookup com/bugsnag/android/BreadcrumbType
  jclass type_class =
      bsg_safe_find_class(env, "com/bugsnag/android/BreadcrumbType");
  if (interface_class == NULL) {
    return;
  }

  jobject jtype = (*env)->GetStaticObjectField(
      env, type_class, bsg_parse_jcrumb_type(env, type, type_class));
  jbyteArray jmessage = bsg_byte_ary_from_string(env, message);
  (*env)->CallStaticVoidMethod(env, interface_class, leave_breadcrumb_method,
                               jmessage, jtype);
  bsg_release_byte_ary(env, jmessage, message);
  (*env)->DeleteLocalRef(env, jtype);
  (*env)->DeleteLocalRef(env, jmessage);

  (*env)->DeleteLocalRef(env, type_class);
  (*env)->DeleteLocalRef(env, interface_class);
}
