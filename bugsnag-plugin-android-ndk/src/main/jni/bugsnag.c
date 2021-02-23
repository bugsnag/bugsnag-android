/** \brief The public API
 */
#include "bugsnag_ndk.h"
#include "report.h"
#include "utils/stack_unwinder.h"
#include "utils/string.h"
#include "metadata.h"
#include "safejni.h"
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static JNIEnv *bsg_global_jni_env = NULL;

void bugsnag_set_binary_arch(JNIEnv *env);

void bugsnag_init(JNIEnv *env) {
  bsg_global_jni_env = env;
}

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

static jfieldID bsg_parse_jseverity(JNIEnv *env, bsg_severity_t severity,
                             jclass severity_class) {
  const char *severity_sig = "Lcom/bugsnag/android/Severity;";
  if (severity == BSG_SEVERITY_ERR) {
    return bsg_safe_get_static_field_id(env, severity_class, "ERROR", severity_sig);
  } else if (severity == BSG_SEVERITY_WARN) {
    return bsg_safe_get_static_field_id(env, severity_class, "WARNING",
                                        severity_sig);
  } else {
    return bsg_safe_get_static_field_id(env, severity_class, "INFO", severity_sig);
  }
}

static bool populate_single_notify_stacktrace(JNIEnv *env, bsg_stackframe *frame,
                                    int index, jclass trace_class,
                                    jmethodID trace_constructor,
                                    jobjectArray trace) {
  jstring class = NULL;
  jstring filename = NULL;
  jstring method = NULL;
  jobject jframe = NULL;
  bool success = false;

  // create Java string objects for class/filename/method
  class = bsg_safe_new_string_utf(env, "");
  if (class == NULL) {
    goto exit;
  }

  filename = bsg_safe_new_string_utf(env, frame->filename);
  if (filename == NULL) {
    goto exit;
  }
  if (strlen(frame->method) == 0) {
    char frame_address[32];
    sprintf(frame_address, "0x%lx", (unsigned long)frame->frame_address);
    method = bsg_safe_new_string_utf(env, frame_address);
  } else {
    method = bsg_safe_new_string_utf(env, frame->method);
  }

  // create StackTraceElement object
  jframe = bsg_safe_new_object(env, trace_class, trace_constructor, class, method,
                              filename, frame->line_number);
  if (jframe == NULL) {
    goto exit;
  }

  bsg_safe_set_object_array_element(env, trace, index, jframe);
  success = true;

  exit:
  bsg_safe_delete_local_ref(env, class);
  bsg_safe_delete_local_ref(env, filename);
  bsg_safe_delete_local_ref(env, method);
  bsg_safe_delete_local_ref(env, jframe);
  return success;
}

static void bsg_populate_notify_stacktrace(JNIEnv *env, bsg_stackframe *stacktrace,
                                   ssize_t frame_count, jclass trace_class,
                                   jmethodID trace_constructor,
                                   jobjectArray trace) {
  for (int i = 0; i < frame_count; i++) {
    if (!populate_single_notify_stacktrace(env, &stacktrace[i], i,
            trace_class, trace_constructor, trace)) {
      return;
    }
  }
}

void bugsnag_notify_env(JNIEnv *env, char *name, char *message,
                        bsg_severity_t severity) {
  jbyteArray jname = NULL;
  jbyteArray jmessage = NULL;
  jclass interface_class = NULL;
  jclass trace_class = NULL;
  jclass severity_class = NULL;
  jobjectArray trace = NULL;
  jobject jseverity = NULL;

  bsg_stackframe stacktrace[BUGSNAG_FRAMES_MAX];
  ssize_t frame_count =
      bsg_unwind_stack(bsg_configured_unwind_style(), stacktrace, NULL, NULL);

  // lookup com/bugsnag/android/NativeInterface
  interface_class =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (interface_class == NULL) {
    goto exit;
  }

  // lookup NativeInterface.notify()
  jmethodID notify_method = bsg_safe_get_static_method_id(
      env, interface_class, "notify",
      "([B[BLcom/bugsnag/android/Severity;[Ljava/lang/StackTraceElement;)V");
  if (notify_method == NULL) {
    goto exit;
  }

  // lookup java/lang/StackTraceElement
  trace_class = bsg_safe_find_class(env, "java/lang/StackTraceElement");
  if (trace_class == NULL) {
    goto exit;
  }

  // lookup com/bugsnag/android/Severity
  severity_class = bsg_safe_find_class(env, "com/bugsnag/android/Severity");
  if (severity_class == NULL) {
    goto exit;
  }

  // lookup StackTraceElement constructor
  jmethodID trace_constructor = bsg_safe_get_method_id(
      env, trace_class, "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
  if (trace_constructor == NULL) {
    goto exit;
  }

  // create StackTraceElement array
  trace = bsg_safe_new_object_array(env, frame_count, trace_class);
  if (trace == NULL) {
    goto exit;
  }

  // populate stacktrace object
  bsg_populate_notify_stacktrace(env, stacktrace, frame_count, trace_class,
                                 trace_constructor, trace);

  // get the severity field
  jfieldID severity_field = bsg_parse_jseverity(env, severity, severity_class);
  if (severity_field == NULL) {
    goto exit;
  }
  // get the error severity object
  jseverity =
      bsg_safe_get_static_object_field(env, severity_class, severity_field);
  if (jseverity == NULL) {
    goto exit;
  }

  jname = bsg_byte_ary_from_string(env, name);
  jmessage = bsg_byte_ary_from_string(env, message);

  // set application's binary arch
  bugsnag_set_binary_arch(env);

  bsg_safe_call_static_void_method(env, interface_class, notify_method, jname,
                                   jmessage, jseverity, trace);

  exit:
  if (jname != NULL) {
    bsg_safe_release_byte_array_elements(env, jname, (jbyte *)name);
    bsg_safe_delete_local_ref(env, jname);
  }
  if (jmessage != NULL) {
    bsg_safe_release_byte_array_elements(env, jmessage, (jbyte *)message);
    bsg_safe_delete_local_ref(env, jmessage);
  }
  bsg_safe_delete_local_ref(env, interface_class);
  bsg_safe_delete_local_ref(env, trace_class);
  bsg_safe_delete_local_ref(env, severity_class);
  bsg_safe_delete_local_ref(env, trace);
  bsg_safe_delete_local_ref(env, jseverity);
}

void bugsnag_set_binary_arch(JNIEnv *env) {
  jclass interface_class = NULL;
  jstring arch = NULL;

  // lookup com/bugsnag/android/NativeInterface
  interface_class =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (interface_class == NULL) {
    goto exit;
  }

  // lookup NativeInterface.setBinaryArch()
  jmethodID set_arch_method = bsg_safe_get_static_method_id(
      env, interface_class, "setBinaryArch", "(Ljava/lang/String;)V");
  if (set_arch_method == NULL) {
    goto exit;
  }

  // call NativeInterface.setBinaryArch()
  arch = bsg_safe_new_string_utf(env, bsg_binary_arch());
  if (arch == NULL) {
    goto exit;
  }

  bsg_safe_call_static_void_method(env, interface_class, set_arch_method,
                                   arch);

  exit:
  bsg_safe_delete_local_ref(env, interface_class);
  bsg_safe_delete_local_ref(env, arch);
}

void bugsnag_set_user_env(JNIEnv *env, char *id, char *email, char *name) {
  // lookup com/bugsnag/android/NativeInterface
  jclass interface_class = NULL;

  interface_class =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (interface_class == NULL) {
    goto exit;
  }

  // lookup NativeInterface.setUser()
  jmethodID set_user_method = bsg_safe_get_static_method_id(env, interface_class,
                                                  "setUser", "([B[B[B)V");
  if (set_user_method == NULL) {
    goto exit;
  }

  jbyteArray jid = bsg_byte_ary_from_string(env, id);
  jbyteArray jemail = bsg_byte_ary_from_string(env, email);
  jbyteArray jname = bsg_byte_ary_from_string(env, name);

  bsg_safe_call_static_void_method(env, interface_class, set_user_method, jid,
                                   jemail, jname);

  bsg_safe_release_byte_array_elements(env, jid, (jbyte *)id);
  bsg_safe_release_byte_array_elements(env, jemail, (jbyte *)email);
  bsg_safe_release_byte_array_elements(env, jname, (jbyte *)name);

  bsg_safe_delete_local_ref(env, jid);
  bsg_safe_delete_local_ref(env, jemail);
  bsg_safe_delete_local_ref(env, jname);

  exit:
  bsg_safe_delete_local_ref(env, interface_class);
}

static jfieldID bsg_parse_jcrumb_type(JNIEnv *env, bsg_breadcrumb_t type,
                                      jclass type_class) {
  const char *type_sig = "Lcom/bugsnag/android/BreadcrumbType;";
  if (type == BSG_CRUMB_USER) {
    return bsg_safe_get_static_field_id(env, type_class, "USER", type_sig);
  } else if (type == BSG_CRUMB_ERROR) {
    return bsg_safe_get_static_field_id(env, type_class, "ERROR", type_sig);
  } else if (type == BSG_CRUMB_LOG) {
    return bsg_safe_get_static_field_id(env, type_class, "LOG", type_sig);
  } else if (type == BSG_CRUMB_NAVIGATION) {
    return bsg_safe_get_static_field_id(env, type_class, "NAVIGATION",
                                        type_sig);
  } else if (type == BSG_CRUMB_PROCESS) {
    return bsg_safe_get_static_field_id(env, type_class, "PROCESS", type_sig);
  } else if (type == BSG_CRUMB_REQUEST) {
    return bsg_safe_get_static_field_id(env, type_class, "REQUEST", type_sig);
  } else if (type == BSG_CRUMB_STATE) {
    return bsg_safe_get_static_field_id(env, type_class, "STATE", type_sig);
  } else { // MANUAL is the default type
    return bsg_safe_get_static_field_id(env, type_class, "MANUAL", type_sig);
  }
}

void bugsnag_leave_breadcrumb_env(JNIEnv *env, char *message,
                                  bsg_breadcrumb_t type) {
  jbyteArray jmessage = NULL;
  jclass interface_class = NULL;
  jclass type_class = NULL;
  jobject jtype = NULL;

  // lookup com/bugsnag/android/NativeInterface
  interface_class =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (interface_class == NULL) {
    goto exit;
  }

  // lookup NativeInterface.leaveBreadcrumb()
  jmethodID leave_breadcrumb_method = bsg_safe_get_static_method_id(
      env, interface_class, "leaveBreadcrumb",
      "([BLcom/bugsnag/android/BreadcrumbType;)V");
  if (leave_breadcrumb_method == NULL) {
    goto exit;
  }

  // lookup com/bugsnag/android/BreadcrumbType
  type_class = bsg_safe_find_class(env, "com/bugsnag/android/BreadcrumbType");
  if (type_class == NULL) {
    goto exit;
  }

  // get breadcrumb type fieldID
  jfieldID crumb_type = bsg_parse_jcrumb_type(env, type, type_class);
  if (crumb_type == NULL) {
    goto exit;
  }

  // get the breadcrumb type
  jtype = bsg_safe_get_static_object_field(env, type_class, crumb_type);
  if (jtype == NULL) {
    goto exit;
  }
  jmessage = bsg_byte_ary_from_string(env, message);
  bsg_safe_call_static_void_method(env, interface_class,
                                   leave_breadcrumb_method, jmessage, jtype);

  exit:
  bsg_safe_release_byte_array_elements(env, jmessage, (jbyte *)message);
  bsg_safe_delete_local_ref(env, jmessage);
  bsg_safe_delete_local_ref(env, interface_class);
  bsg_safe_delete_local_ref(env, type_class);
  bsg_safe_delete_local_ref(env, jtype);
}
