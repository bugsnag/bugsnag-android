/** \brief The public API
 */
#include "bugsnag_ndk.h"
#include "event.h"
#include "utils/stack_unwinder.h"
#include "metadata.h"
#include "../assets/include/bugsnag.h"
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static JNIEnv *bsg_global_jni_env = NULL;

void bugsnag_set_binary_arch(JNIEnv *env);

void bugsnag_init(JNIEnv *env) { bsg_global_jni_env = env; }

void bugsnag_notify_env(JNIEnv *env, char *name, char *message,
                        bsg_severity_t severity);
void bugsnag_set_user_env(JNIEnv *env, char *id, char *email, char *name);
void bugsnag_leave_breadcrumb_env(JNIEnv *env, char *message,
                                  bsg_breadcrumb_t type);

void bugsnag_notify(char *name, char *message, bsg_severity_t severity) {
  if (bsg_global_jni_env != NULL) {
    bugsnag_notify_env(bsg_global_jni_env, name, message, severity);
  } else {
    BUGSNAG_LOG("Cannot bugsnag_notify before initializing with bugsnag_init");
  }
}

void bugsnag_set_user(char *id, char *email, char *name) {
  if (bsg_global_jni_env != NULL) {
    bugsnag_set_user_env(bsg_global_jni_env, id, email, name);
  } else {
    BUGSNAG_LOG(
        "Cannot bugsnag_set_user before initializing with bugsnag_init");
  }
}

void bugsnag_leave_breadcrumb(char *message, bsg_breadcrumb_t type) {
  if (bsg_global_jni_env != NULL) {
    bugsnag_leave_breadcrumb_env(bsg_global_jni_env, message, type);
  } else {
    BUGSNAG_LOG("Cannot bugsnag_leave_breadcrumb_env before initializing with "
                "bugsnag_init");
  }
}

void bugsnag_add_on_error(on_error on_error) {
    bugsnag_add_on_error_env(bsg_global_jni_env, on_error);
}

void bugsnag_remove_on_error(on_error on_error) {
    bugsnag_remove_on_error_env(bsg_global_jni_env, on_error);
}

jfieldID bsg_parse_jseverity(JNIEnv *env, bsg_severity_t severity,
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

void bugsnag_notify_env(JNIEnv *env, char *name, char *message,
                        bsg_severity_t severity) {
  bsg_stackframe stacktrace[BUGSNAG_FRAMES_MAX];
  ssize_t frame_count =
      bsg_unwind_stack(bsg_configured_unwind_style(), stacktrace, NULL, NULL);

  jclass interface_class =
      (*env)->FindClass(env, "com/bugsnag/android/NativeInterface");
  jmethodID notify_method = (*env)->GetStaticMethodID(
      env, interface_class, "notify",
      "(Ljava/lang/String;Ljava/lang/String;Lcom/bugsnag/android/"
      "Severity;[Ljava/lang/StackTraceElement;)V");
  jclass trace_class = (*env)->FindClass(env, "java/lang/StackTraceElement");
  jclass severity_class =
      (*env)->FindClass(env, "com/bugsnag/android/Severity");
  jmethodID trace_constructor = (*env)->GetMethodID(
      env, trace_class, "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
  jobjectArray trace = (*env)->NewObjectArray(
      env, (jsize)frame_count,
      (*env)->FindClass(env, "java/lang/StackTraceElement"), NULL);

  for (int i = 0; i < frame_count; i++) {
    bsg_stackframe frame = stacktrace[i];
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

  jstring jname = (*env)->NewStringUTF(env, name);
  jstring jmessage = (*env)->NewStringUTF(env, message);

  // set application's binary arch
  bugsnag_set_binary_arch(env);

  (*env)->CallStaticVoidMethod(env, interface_class, notify_method, jname,
                               jmessage, jseverity, trace);

  (*env)->DeleteLocalRef(env, trace_class);
  (*env)->DeleteLocalRef(env, trace);
  (*env)->DeleteLocalRef(env, severity_class);
  (*env)->DeleteLocalRef(env, jseverity);
  (*env)->DeleteLocalRef(env, interface_class);
}

void bugsnag_set_binary_arch(JNIEnv *env) {
    jclass interface_class =
        (*env)->FindClass(env, "com/bugsnag/android/NativeInterface");
    jmethodID set_arch_method = (*env)->GetStaticMethodID(
        env, interface_class, "setBinaryArch", "(Ljava/lang/String;)V");

    jstring arch = (*env)->NewStringUTF(env, bsg_binary_arch());
    (*env)->CallStaticVoidMethod(env, interface_class, set_arch_method, arch);
    (*env)->DeleteLocalRef(env, arch);
    (*env)->DeleteLocalRef(env, interface_class);
}

void bugsnag_set_user_env(JNIEnv *env, char *id, char *email, char *name) {
  jclass interface_class =
      (*env)->FindClass(env, "com/bugsnag/android/NativeInterface");
  jmethodID set_user_method = (*env)->GetStaticMethodID(
      env, interface_class, "setUser",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

  jstring jid = (*env)->NewStringUTF(env, id);
  jstring jemail = (*env)->NewStringUTF(env, email);
  jstring jname = (*env)->NewStringUTF(env, name);

  (*env)->CallStaticVoidMethod(env, interface_class, set_user_method, jid,
                               jemail, jname);

  (*env)->DeleteLocalRef(env, jid);
  (*env)->DeleteLocalRef(env, jemail);
  (*env)->DeleteLocalRef(env, jname);
  (*env)->DeleteLocalRef(env, interface_class);
}

jfieldID bsg_parse_jcrumb_type(JNIEnv *env, bsg_breadcrumb_t type,
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
                                  bsg_breadcrumb_t type) {
  jclass interface_class =
      (*env)->FindClass(env, "com/bugsnag/android/NativeInterface");
  jmethodID leave_breadcrumb_method = (*env)->GetStaticMethodID(
      env, interface_class, "leaveBreadcrumb",
      "(Ljava/lang/String;Lcom/bugsnag/android/BreadcrumbType;)V");
  jclass type_class =
      (*env)->FindClass(env, "com/bugsnag/android/BreadcrumbType");

  jobject jtype = (*env)->GetStaticObjectField(
      env, type_class, bsg_parse_jcrumb_type(env, type, type_class));
  jstring jname = (*env)->NewStringUTF(env, message);
  (*env)->CallStaticVoidMethod(env, interface_class, leave_breadcrumb_method,
                               jname, jtype);

  (*env)->DeleteLocalRef(env, jtype);
  (*env)->DeleteLocalRef(env, jname);

  (*env)->DeleteLocalRef(env, type_class);
  (*env)->DeleteLocalRef(env, interface_class);
}
