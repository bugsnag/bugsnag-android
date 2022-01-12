/** \brief The public API
 */
#include "../assets/include/bugsnag.h"
#include "bugsnag_ndk.h"
#include "event.h"
#include "jni_cache.h"
#include "metadata.h"
#include "safejni.h"
#include "utils/stack_unwinder.h"
#include "utils/string.h"
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static JNIEnv *bsg_global_jni_env = NULL;

void bugsnag_start(JNIEnv *env) { bsg_global_jni_env = env; }

void bugsnag_notify(const char *name, const char *message,
                    bugsnag_severity severity) {
  if (bsg_global_jni_env != NULL) {
    bugsnag_notify_env(bsg_global_jni_env, name, message, severity);
  } else {
    BUGSNAG_LOG("Cannot bugsnag_notify before initializing with bugsnag_start");
  }
}

void bugsnag_set_user(const char *id, const char *email, const char *name) {
  if (bsg_global_jni_env != NULL) {
    bugsnag_set_user_env(bsg_global_jni_env, id, email, name);
  } else {
    BUGSNAG_LOG(
        "Cannot bugsnag_set_user before initializing with bugsnag_start");
  }
}

void bugsnag_leave_breadcrumb(const char *message,
                              bugsnag_breadcrumb_type type) {
  if (bsg_global_jni_env != NULL) {
    bugsnag_leave_breadcrumb_env(bsg_global_jni_env, message, type);
  } else {
    BUGSNAG_LOG("Cannot bugsnag_leave_breadcrumb_env before initializing with "
                "bugsnag_start");
  }
}

static jfieldID parse_jseverity(JNIEnv *env, bugsnag_severity severity,
                                jclass severity_class) {
  const char *severity_sig = "Lcom/bugsnag/android/Severity;";
  if (severity == BSG_SEVERITY_ERR) {
    return bsg_safe_get_static_field_id(env, severity_class, "ERROR",
                                        severity_sig);
  } else if (severity == BSG_SEVERITY_WARN) {
    return bsg_safe_get_static_field_id(env, severity_class, "WARNING",
                                        severity_sig);
  } else {
    return bsg_safe_get_static_field_id(env, severity_class, "INFO",
                                        severity_sig);
  }
}

static void populate_notify_stacktrace(JNIEnv *env,
                                       bugsnag_stackframe *stacktrace,
                                       ssize_t frame_count, jclass trace_class,
                                       jmethodID trace_constructor,
                                       jobjectArray trace) {
  for (int i = 0; i < frame_count; i++) {
    bugsnag_stackframe frame = stacktrace[i];

    // create Java string objects for class/filename/method
    jstring class = bsg_safe_new_string_utf(env, "");
    if (class == NULL) {
      goto exit;
    }

    // populate filename
    jstring filename = bsg_safe_new_string_utf(env, frame.filename);
    if (filename == NULL) {
      goto exit;
    }

    // populate method
    jstring method = NULL;
    if (bsg_strlen(frame.method) == 0) {
      char frame_address[32];
      snprintf(frame_address, sizeof(frame_address), "0x%lx",
               (unsigned long)frame.frame_address);
      method = bsg_safe_new_string_utf(env, frame_address);
    } else {
      method = bsg_safe_new_string_utf(env, frame.method);
    }
    if (method == NULL) {
      goto exit;
    }

    // create StackTraceElement object
    jobject jframe =
        bsg_safe_new_object(env, trace_class, trace_constructor, class, method,
                            filename, frame.line_number);
    if (jframe == NULL) {
      goto exit;
    }

    bsg_safe_set_object_array_element(env, trace, i, jframe);
    goto exit;

  exit:
    bsg_safe_delete_local_ref(env, filename);
    bsg_safe_delete_local_ref(env, class);
  }
}

void bugsnag_notify_env(JNIEnv *env, const char *name, const char *message,
                        bugsnag_severity severity) {
  jobjectArray jtrace = NULL;
  jobject jseverity = NULL;
  jbyteArray jname = NULL;
  jbyteArray jmessage = NULL;

  if (!bsg_jni_cache_refresh(env)) {
    BUGSNAG_LOG("Could not refresh JNI cache.");
    goto exit;
  }

  bugsnag_stackframe stacktrace[BUGSNAG_FRAMES_MAX];
  memset(stacktrace, 0, sizeof(stacktrace));
  ssize_t frame_count =
      bsg_unwind_stack(bsg_configured_unwind_style(), stacktrace, NULL, NULL);

  // create StackTraceElement array
  jtrace = bsg_safe_new_object_array(env, frame_count,
                                     bsg_global_jni_cache->stack_trace_element);
  if (jtrace == NULL) {
    goto exit;
  }

  // populate stacktrace object
  populate_notify_stacktrace(env, stacktrace, frame_count,
                             bsg_global_jni_cache->stack_trace_element,
                             bsg_global_jni_cache->ste_constructor, jtrace);

  // get the severity field
  jfieldID severity_field =
      parse_jseverity(env, severity, bsg_global_jni_cache->severity);
  if (severity_field == NULL) {
    goto exit;
  }
  // get the error severity object
  jseverity = bsg_safe_get_static_object_field(
      env, bsg_global_jni_cache->severity, severity_field);
  if (jseverity == NULL) {
    goto exit;
  }

  jname = bsg_byte_ary_from_string(env, name);
  jmessage = bsg_byte_ary_from_string(env, message);

  bsg_safe_call_static_void_method(env, bsg_global_jni_cache->native_interface,
                                   bsg_global_jni_cache->ni_notify, jname,
                                   jmessage, jseverity, jtrace);

  goto exit;

exit:
  bsg_safe_release_byte_array_elements(env, jname, (jbyte *)name);
  bsg_safe_delete_local_ref(env, jname);
  bsg_safe_release_byte_array_elements(env, jmessage, (jbyte *)message);
  bsg_safe_delete_local_ref(env, jmessage);
  bsg_safe_delete_local_ref(env, jtrace);
  bsg_safe_delete_local_ref(env, jseverity);
}

void bugsnag_set_user_env(JNIEnv *env, const char *id, const char *email,
                          const char *name) {
  if (!bsg_jni_cache_refresh(env)) {
    BUGSNAG_LOG("Could not refresh JNI cache.");
    return;
  }

  jbyteArray jid = bsg_byte_ary_from_string(env, id);
  jbyteArray jemail = bsg_byte_ary_from_string(env, email);
  jbyteArray jname = bsg_byte_ary_from_string(env, name);

  bsg_safe_call_static_void_method(env, bsg_global_jni_cache->native_interface,
                                   bsg_global_jni_cache->ni_set_user, jid,
                                   jemail, jname);

  bsg_safe_release_byte_array_elements(env, jid, (jbyte *)id);
  bsg_safe_delete_local_ref(env, jid);
  bsg_safe_release_byte_array_elements(env, jemail, (jbyte *)email);
  bsg_safe_delete_local_ref(env, jemail);
  bsg_safe_release_byte_array_elements(env, jname, (jbyte *)name);
  bsg_safe_delete_local_ref(env, jname);
}

static jfieldID parse_jcrumb_type(JNIEnv *env,
                                  const bugsnag_breadcrumb_type type,
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

void bugsnag_leave_breadcrumb_env(JNIEnv *env, const char *message,
                                  const bugsnag_breadcrumb_type type) {
  jbyteArray jmessage = NULL;
  jobject jtype = NULL;

  if (!bsg_jni_cache_refresh(env)) {
    BUGSNAG_LOG("Could not refresh JNI cache.");
    goto exit;
  }

  // get breadcrumb type fieldID
  jfieldID crumb_type =
      parse_jcrumb_type(env, type, bsg_global_jni_cache->breadcrumb_type);
  if (crumb_type == NULL) {
    goto exit;
  }

  // get the breadcrumb type
  jtype = bsg_safe_get_static_object_field(
      env, bsg_global_jni_cache->breadcrumb_type, crumb_type);
  if (jtype == NULL) {
    goto exit;
  }
  jmessage = bsg_byte_ary_from_string(env, message);
  bsg_safe_call_static_void_method(env, bsg_global_jni_cache->native_interface,
                                   bsg_global_jni_cache->ni_leave_breadcrumb,
                                   jmessage, jtype);

  goto exit;

exit:
  bsg_safe_release_byte_array_elements(env, jmessage, (jbyte *)message);
  bsg_safe_delete_local_ref(env, jmessage);
  bsg_safe_delete_local_ref(env, jtype);
}
