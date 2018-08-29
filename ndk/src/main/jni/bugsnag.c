/** \brief The public API
 */
#include "bugsnag_ndk.h"
#include "report.h"
#include "utils/stack_unwinder.h"
#include <jni.h>

static JNIEnv *bsg_global_jni_env = NULL;

void bugsnag_init(JNIEnv *env) { bsg_global_jni_env = env; }

void bugsnag_notify_env(JNIEnv *env, char *name, char *message,
                        bsg_severity_t severity);
void bugsnag_set_user_env(JNIEnv *env, char *id, char *email, char *name);
void bugsnag_leave_breadcrumb_env(JNIEnv *env, char *name,
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

void bugsnag_leave_breadcrumb(char *name, bsg_breadcrumb_t type) {
  if (bsg_global_jni_env != NULL) {
    bugsnag_leave_breadcrumb_env(bsg_global_jni_env, name, type);
  } else {
    BUGSNAG_LOG("Cannot bugsnag_leave_breadcrumb_env before initializing with "
                "bugsnag_init");
  }
}

void bugsnag_notify_env(JNIEnv *env, char *name, char *message,
                        bsg_severity_t severity) {
  bsg_stackframe stacktrace[BUGSNAG_FRAMES_MAX];
  ssize_t frame_count =
      bsg_unwind_stack(bsg_configured_unwind_style(), stacktrace, NULL, NULL);

  jclass trace_class = (*env)->FindClass(env, "java/lang/StackTraceElement");
  jmethodID trace_constructor = (*env)->GetMethodID(
      env, trace_class, "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
  jobjectArray trace = (*env)->NewObjectArray(
      env, frame_count, (*env)->FindClass(env, "java/lang/StackTraceElement"),
      NULL);

  for (int i = 0; i < frame_count; i++) {
    bsg_stackframe frame = stacktrace[i];

    jstring class = (*env)->NewStringUTF(env, "");
    jstring filename = (*env)->NewStringUTF(env, frame.filename);
    jstring method = (*env)->NewStringUTF(env, frame.method);
    jobject jframe = (*env)->NewObject(env, trace_class, trace_constructor,
                                       class, method, filename, 0);

    (*env)->SetObjectArrayElement(env, trace, i, jframe);
    (*env)->DeleteLocalRef(env, filename);
    (*env)->DeleteLocalRef(env, class);
    (*env)->DeleteLocalRef(env, method);
  }

  // Create a severity Error
  jclass severity_class =
      (*env)->FindClass(env, "com/bugsnag/android/Severity");
  jfieldID error_field;
  if (severity == BSG_SEVERITY_ERR) {
    error_field = (*env)->GetStaticFieldID(env, severity_class, "ERROR",
                                           "Lcom/bugsnag/android/Severity;");
  } else if (severity == BSG_SEVERITY_WARN) {
    error_field = (*env)->GetStaticFieldID(env, severity_class, "WARNING",
                                           "Lcom/bugsnag/android/Severity;");
  } else {
    error_field = (*env)->GetStaticFieldID(env, severity_class, "INFO",
                                           "Lcom/bugsnag/android/Severity;");
  }
  jobject jseverity =
      (*env)->GetStaticObjectField(env, severity_class, error_field);

  jstring jname = (*env)->NewStringUTF(env, name);
  jstring jmessage = (*env)->NewStringUTF(env, message);

  jclass interface_class =
      (*env)->FindClass(env, "com/bugsnag/android/NativeInterface");
  jmethodID notify_method = (*env)->GetStaticMethodID(
      env, interface_class, "notify",
      "(Ljava/lang/String;Ljava/lang/String;Lcom/bugsnag/android/"
      "Severity;[Ljava/lang/StackTraceElement;)V");
  (*env)->CallStaticVoidMethod(env, interface_class, notify_method, jname,
                               jmessage, jseverity, trace);

  (*env)->DeleteLocalRef(env, trace_class);
  (*env)->DeleteLocalRef(env, trace);
  (*env)->DeleteLocalRef(env, severity_class);
  (*env)->DeleteLocalRef(env, jseverity);
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

void bugsnag_leave_breadcrumb_env(JNIEnv *env, char *name,
                                  bsg_breadcrumb_t type) {
  jclass interface_class =
      (*env)->FindClass(env, "com/bugsnag/android/NativeInterface");
  jmethodID leave_breadcrumb_method = (*env)->GetStaticMethodID(
      env, interface_class, "leaveBreadcrumb",
      "(Ljava/lang/String;Lcom/bugsnag/android/BreadcrumbType;)V");

  jclass type_class =
      (*env)->FindClass(env, "com/bugsnag/android/BreadcrumbType");
  jfieldID type_field;
  if (type == BSG_CRUMB_USER) {
    type_field = (*env)->GetStaticFieldID(
        env, type_class, "USER", "Lcom/bugsnag/android/BreadcrumbType;");
  } else if (type == BSG_CRUMB_ERROR) {
    type_field = (*env)->GetStaticFieldID(
        env, type_class, "ERROR", "Lcom/bugsnag/android/BreadcrumbType;");
  } else if (type == BSG_CRUMB_LOG) {
    type_field = (*env)->GetStaticFieldID(
        env, type_class, "LOG", "Lcom/bugsnag/android/BreadcrumbType;");
  } else if (type == BSG_CRUMB_NAVIGATION) {
    type_field = (*env)->GetStaticFieldID(
        env, type_class, "NAVIGATION", "Lcom/bugsnag/android/BreadcrumbType;");
  } else if (type == BSG_CRUMB_PROCESS) {
    type_field = (*env)->GetStaticFieldID(
        env, type_class, "PROCESS", "Lcom/bugsnag/android/BreadcrumbType;");
  } else if (type == BSG_CRUMB_REQUEST) {
    type_field = (*env)->GetStaticFieldID(
        env, type_class, "REQUEST", "Lcom/bugsnag/android/BreadcrumbType;");
  } else if (type == BSG_CRUMB_STATE) {
    type_field = (*env)->GetStaticFieldID(
        env, type_class, "STATE", "Lcom/bugsnag/android/BreadcrumbType;");
  } else { // MANUAL is the default type
    type_field = (*env)->GetStaticFieldID(
        env, type_class, "MANUAL", "Lcom/bugsnag/android/BreadcrumbType;");
  }

  jobject jtype = (*env)->GetStaticObjectField(env, type_class, type_field);
  jstring jname = (*env)->NewStringUTF(env, name);
  (*env)->CallStaticVoidMethod(env, interface_class, leave_breadcrumb_method,
                               jname, jtype);

  (*env)->DeleteLocalRef(env, jtype);
  (*env)->DeleteLocalRef(env, jname);

  (*env)->DeleteLocalRef(env, type_class);
  (*env)->DeleteLocalRef(env, interface_class);
}
