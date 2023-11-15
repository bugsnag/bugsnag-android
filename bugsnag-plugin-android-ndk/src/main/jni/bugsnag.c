/** \brief The public API
 */
#include "include/bugsnag.h"
#include "bugsnag_ndk.h"
#include "event.h"
#include "jni_cache.h"
#include "metadata.h"
#include "safejni.h"
#include "utils/stack_unwinder.h"
#include "utils/string.h"
#include <jni.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void bugsnag_start(JNIEnv *env) {
  if (!bsg_jni_cache_init(env)) {
    BUGSNAG_LOG("Could not init JNI jni_cache.");
  }
}

void bugsnag_notify(const char *name, const char *message,
                    bugsnag_severity severity) {
  JNIEnv *env = bsg_jni_cache_get_env();
  if (env == NULL) {
    return;
  }

  bugsnag_notify_env(env, name, message, severity);
}

void bugsnag_set_user(const char *id, const char *email, const char *name) {
  JNIEnv *env = bsg_jni_cache_get_env();
  if (env == NULL) {
    return;
  }

  bugsnag_set_user_env(env, id, email, name);
}

void bugsnag_leave_breadcrumb(const char *message,
                              bugsnag_breadcrumb_type type) {
  JNIEnv *env = bsg_jni_cache_get_env();
  if (env == NULL) {
    return;
  }

  bugsnag_leave_breadcrumb_env(env, message, type);
}

static jfieldID parse_jseverity(JNIEnv *env, bugsnag_severity severity) {
  if (!bsg_jni_cache->initialized) {
    return NULL;
  }

  const char *severity_sig = "Lcom/bugsnag/android/Severity;";
  if (severity == BSG_SEVERITY_ERR) {
    return bsg_safe_get_static_field_id(env, bsg_jni_cache->Severity, "ERROR",
                                        severity_sig);
  } else if (severity == BSG_SEVERITY_WARN) {
    return bsg_safe_get_static_field_id(env, bsg_jni_cache->Severity, "WARNING",
                                        severity_sig);
  } else {
    return bsg_safe_get_static_field_id(env, bsg_jni_cache->Severity, "INFO",
                                        severity_sig);
  }
}

static void populate_notify_stacktrace(JNIEnv *env,
                                       bugsnag_stackframe *stacktrace,
                                       ssize_t frame_count,
                                       jobjectArray trace) {
  if (!bsg_jni_cache->initialized) {
    return;
  }

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

    jobject line_number = bsg_safe_call_static_object_method(
        env, bsg_jni_cache->Long, bsg_jni_cache->Long_valueOf,
        frame.line_number);
    jobject frame_address = bsg_safe_call_static_object_method(
        env, bsg_jni_cache->Long, bsg_jni_cache->Long_valueOf,
        frame.frame_address);
    jobject symbol_address = bsg_safe_call_static_object_method(
        env, bsg_jni_cache->Long, bsg_jni_cache->Long_valueOf,
        frame.symbol_address);
    jobject load_address = bsg_safe_call_static_object_method(
        env, bsg_jni_cache->Long, bsg_jni_cache->Long_valueOf,
        frame.load_address);

    jstring code_identifier =
        bsg_safe_new_string_utf(env, frame.code_identifier);
    if (code_identifier == NULL) {
      goto exit;
    }

    // create NativeStackframe object
    jobject jframe = bsg_safe_new_object(
        env, bsg_jni_cache->NativeStackframe,
        bsg_jni_cache->NativeStackframe_constructor, method, filename,
        line_number, frame_address, symbol_address, load_address, NULL,
        bsg_jni_cache->ErrorType_C, code_identifier);
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

  if (!bsg_jni_cache->initialized) {
    BUGSNAG_LOG("bugsnag_notify_env failed: JNI cache not initialized.");
    goto exit;
  }

  bugsnag_stackframe stacktrace[BUGSNAG_FRAMES_MAX];
  memset(stacktrace, 0, sizeof(stacktrace));
  ssize_t frame_count = bsg_unwind_concurrent_stack(stacktrace, NULL, NULL);

  // create StackTraceElement array
  jtrace = bsg_safe_new_object_array(env, frame_count,
                                     bsg_jni_cache->NativeStackframe);
  if (jtrace == NULL) {
    goto exit;
  }

  // populate stacktrace object
  populate_notify_stacktrace(env, stacktrace, frame_count, jtrace);

  // get the severity field
  jfieldID severity_field = parse_jseverity(env, severity);
  if (severity_field == NULL) {
    goto exit;
  }
  // get the error severity object
  jseverity = bsg_safe_get_static_object_field(env, bsg_jni_cache->Severity,
                                               severity_field);
  if (jseverity == NULL) {
    goto exit;
  }

  jname = bsg_byte_ary_from_string(env, name);
  jmessage = bsg_byte_ary_from_string(env, message);

  bsg_safe_call_static_void_method(env, bsg_jni_cache->NativeInterface,
                                   bsg_jni_cache->NativeInterface_notify, jname,
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

  if (!bsg_jni_cache->initialized) {
    BUGSNAG_LOG("bugsnag_set_user_env failed: JNI cache not initialized.");
    return;
  }

  jbyteArray jid = bsg_byte_ary_from_string(env, id);
  jbyteArray jemail = bsg_byte_ary_from_string(env, email);
  jbyteArray jname = bsg_byte_ary_from_string(env, name);

  bsg_safe_call_static_void_method(env, bsg_jni_cache->NativeInterface,
                                   bsg_jni_cache->NativeInterface_setUser, jid,
                                   jemail, jname);

  bsg_safe_release_byte_array_elements(env, jid, (jbyte *)id);
  bsg_safe_delete_local_ref(env, jid);
  bsg_safe_release_byte_array_elements(env, jemail, (jbyte *)email);
  bsg_safe_delete_local_ref(env, jemail);
  bsg_safe_release_byte_array_elements(env, jname, (jbyte *)name);
  bsg_safe_delete_local_ref(env, jname);
}

static jfieldID parse_jcrumb_type(JNIEnv *env,
                                  const bugsnag_breadcrumb_type type) {
  if (!bsg_jni_cache->initialized) {
    return NULL;
  }

  const char *type_sig = "Lcom/bugsnag/android/BreadcrumbType;";
  if (type == BSG_CRUMB_USER) {
    return bsg_safe_get_static_field_id(env, bsg_jni_cache->BreadcrumbType,
                                        "USER", type_sig);
  } else if (type == BSG_CRUMB_ERROR) {
    return bsg_safe_get_static_field_id(env, bsg_jni_cache->BreadcrumbType,
                                        "ERROR", type_sig);
  } else if (type == BSG_CRUMB_LOG) {
    return bsg_safe_get_static_field_id(env, bsg_jni_cache->BreadcrumbType,
                                        "LOG", type_sig);
  } else if (type == BSG_CRUMB_NAVIGATION) {
    return bsg_safe_get_static_field_id(env, bsg_jni_cache->BreadcrumbType,
                                        "NAVIGATION", type_sig);
  } else if (type == BSG_CRUMB_PROCESS) {
    return bsg_safe_get_static_field_id(env, bsg_jni_cache->BreadcrumbType,
                                        "PROCESS", type_sig);
  } else if (type == BSG_CRUMB_REQUEST) {
    return bsg_safe_get_static_field_id(env, bsg_jni_cache->BreadcrumbType,
                                        "REQUEST", type_sig);
  } else if (type == BSG_CRUMB_STATE) {
    return bsg_safe_get_static_field_id(env, bsg_jni_cache->BreadcrumbType,
                                        "STATE", type_sig);
  } else { // MANUAL is the default type
    return bsg_safe_get_static_field_id(env, bsg_jni_cache->BreadcrumbType,
                                        "MANUAL", type_sig);
  }
}

void bugsnag_leave_breadcrumb_env(JNIEnv *env, const char *message,
                                  const bugsnag_breadcrumb_type type) {
  jbyteArray jmessage = NULL;
  jobject jtype = NULL;

  if (!bsg_jni_cache->initialized) {
    BUGSNAG_LOG(
        "bugsnag_leave_breadcrumb_env failed: JNI cache not initialized.");
    goto exit;
  }

  // get breadcrumb type fieldID
  jfieldID crumb_type = parse_jcrumb_type(env, type);
  if (crumb_type == NULL) {
    goto exit;
  }

  // get the breadcrumb type
  jtype = bsg_safe_get_static_object_field(env, bsg_jni_cache->BreadcrumbType,
                                           crumb_type);
  if (jtype == NULL) {
    goto exit;
  }
  jmessage = bsg_byte_ary_from_string(env, message);
  bsg_safe_call_static_void_method(
      env, bsg_jni_cache->NativeInterface,
      bsg_jni_cache->NativeInterface_leaveBreadcrumb, jmessage, jtype);

  goto exit;

exit:
  bsg_safe_release_byte_array_elements(env, jmessage, (jbyte *)message);
  bsg_safe_delete_local_ref(env, jmessage);
  bsg_safe_delete_local_ref(env, jtype);
}
