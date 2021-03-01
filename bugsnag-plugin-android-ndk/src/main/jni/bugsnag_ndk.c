#include "bugsnag_ndk.h"

#include <arpa/inet.h>
#include <jni.h>
#include <pthread.h>
#include <stdlib.h>
#include <string.h>

#include "handlers/signal_handler.h"
#include "handlers/cpp_handler.h"
#include "metadata.h"
#include "report.h"
#include "utils/serializer.h"
#include "utils/string.h"
#include "safejni.h"
#include "jnicache.h"

#ifdef __cplusplus
extern "C" {
#endif

static bsg_environment *bsg_global_env;
static pthread_mutex_t bsg_global_env_write_mutex = PTHREAD_MUTEX_INITIALIZER;

/**
 * All functions which will edit the environment (unless they are handling a
 * crash) must first request the lock
 */
void bsg_request_env_write_lock(void) {
  pthread_mutex_lock(&bsg_global_env_write_mutex);
}

/**
 * Once editing is complete, the lock must be released
 */
void bsg_release_env_write_lock(void) {
  pthread_mutex_unlock(&bsg_global_env_write_mutex);
}

bsg_unwinder bsg_configured_unwind_style() {
  if (bsg_global_env != NULL)
    return bsg_global_env->unwind_style;

  return BSG_CUSTOM_UNWIND;
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_NdkPlugin_enableCrashReporting(
        JNIEnv *env, jobject _this) {
  if (bsg_global_env == NULL) {
    BUGSNAG_LOG("Attempted to enable crash reporting without first calling install()");
    return;
  }
  bsg_handler_install_signal(bsg_global_env);
  bsg_handler_install_cpp(bsg_global_env);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_NdkPlugin_disableCrashReporting(
        JNIEnv *env, jobject _this) {
  bsg_handler_uninstall_signal();
  bsg_handler_uninstall_cpp();
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_enableCrashReporting(
    JNIEnv *env, jobject _this) {
  if (bsg_global_env == NULL) {
    BUGSNAG_LOG("Attempted to enable crash reporting without first calling install()");
    return;
  }
  bsg_handler_install_signal(bsg_global_env);
  bsg_handler_install_cpp(bsg_global_env);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_disableCrashReporting(
    JNIEnv *env, jobject _this) {
  bsg_handler_uninstall_signal();
  bsg_handler_uninstall_cpp();
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_install(
    JNIEnv *env, jobject _this, jstring _report_path, jboolean auto_notify,
    jint _api_level, jboolean is32bit) {
  if(!bsg_init_jni_cache(env)) {
    BUGSNAG_LOG("Could not initialize JNI cache! Some functions will be no-op");
  }
  bsg_environment *bugsnag_env = calloc(1, sizeof(bsg_environment));
  bsg_set_unwind_types((int)_api_level, (bool)is32bit,
                       &bugsnag_env->signal_unwind_style,
                       &bugsnag_env->unwind_style);
  bugsnag_env->report_header.big_endian =
      htonl(47) == 47; // potentially too clever, see man 3 htonl
  bugsnag_env->report_header.version = BUGSNAG_REPORT_VERSION;
  const char *report_path = bsg_safe_get_string_utf_chars(env, _report_path);
  if (report_path == NULL) {
    return;
  }

  sprintf(bugsnag_env->next_report_path, "%s", report_path);
  bsg_safe_release_string_utf_chars(env, _report_path, report_path);

  if ((bool)auto_notify) {
    bsg_handler_install_signal(bugsnag_env);
    bsg_handler_install_cpp(bugsnag_env);
  }

  // populate metadata from Java layer
  bsg_populate_report(env, &bugsnag_env->next_report);
  time(&bugsnag_env->start_time);
  if (bugsnag_env->next_report.app.in_foreground) {
    bugsnag_env->foreground_start_time = bugsnag_env->start_time;
  }

  // If set, save os build info to report info header
  if (strlen(bugsnag_env->next_report.device.os_build) > 0) {
    bsg_strncpy_safe(bugsnag_env->report_header.os_build,
                     bugsnag_env->next_report.device.os_build,
                     sizeof(bugsnag_env->report_header.os_build));
  }

  bsg_global_env = bugsnag_env;
  BUGSNAG_LOG("Initialization complete!");
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_deliverReportAtPath(
    JNIEnv *env, jobject _this, jstring _report_path) {
  static pthread_mutex_t bsg_native_delivery_mutex = PTHREAD_MUTEX_INITIALIZER;
  pthread_mutex_lock(&bsg_native_delivery_mutex);

  bugsnag_report *event = NULL;
  jbyteArray jpayload = NULL;
  jbyteArray jstage = NULL;
  jclass interface_class = NULL;
  char *payload = NULL;
  const char *event_path = NULL;

  event_path = bsg_safe_get_string_utf_chars(env, _report_path);
  if (event_path == NULL) {
    BUGSNAG_LOG("Report path was null");
    goto exit;
  }
  event = bsg_deserialize_report_from_file((char *)event_path);
  if (event == NULL) {
    BUGSNAG_LOG("Failed to read event at file: %s", event_path);
    goto exit;
  }
  payload = bsg_serialize_report_to_json_string(event);
  if (payload == NULL) {
    BUGSNAG_LOG("Failed to serialize event as JSON: %s", event_path);
    goto exit;
  }

  interface_class =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (interface_class == NULL) {
    goto exit;
  }

  jmethodID jdeliver_method = bsg_safe_get_static_method_id(
      env, interface_class, "deliverReport", "([B[B)V");
  if (jdeliver_method == NULL) {
    goto exit;
  }

  // generate payload bytearray
  jpayload = bsg_byte_ary_from_string(env, payload);
  if (jpayload == NULL) {
    goto exit;
  }

  // generate releaseStage bytearray
  jstage = bsg_byte_ary_from_string(env, event->app.release_stage);
  if (jstage == NULL) {
    goto exit;
  }

  // call NativeInterface.deliverReport()
  bsg_safe_call_static_void_method(env, interface_class, jdeliver_method,
                                   jstage, jpayload);
  remove(event_path);

  exit:
  if (event != NULL) {
    free(event);
  }
  bsg_safe_release_byte_array_elements(env, jpayload, (jbyte *)payload);
  if (payload != NULL) {
    free(payload);
  }
  bsg_safe_release_byte_array_elements(env, jstage,
                                       (jbyte *)event->app.release_stage);
  bsg_safe_delete_local_ref(env, jpayload);
  bsg_safe_delete_local_ref(env, interface_class);
  bsg_safe_delete_local_ref(env, jstage);
  bsg_safe_release_string_utf_chars(env, _report_path, event_path);
  pthread_mutex_unlock(&bsg_native_delivery_mutex);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addHandledEvent(JNIEnv *env,
                                                          jobject _this) {
  if (bsg_global_env == NULL)
    return;
  bsg_request_env_write_lock();
  bugsnag_report *report = &bsg_global_env->next_report;

  if (bugsnag_report_has_session(report)) {
    report->handled_events++;
  }
  bsg_release_env_write_lock();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addUnhandledEvent(JNIEnv *env,
                                                            jobject _this) {
    if (bsg_global_env == NULL)
        return;
    bsg_request_env_write_lock();
    bugsnag_report *report = &bsg_global_env->next_report;

    if (bugsnag_report_has_session(report)) {
        report->unhandled_events++;
    }
    bsg_release_env_write_lock();
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_startedSession(
    JNIEnv *env, jobject _this, jstring session_id_, jstring start_date_,
    jint handled_count, jint unhandled_count) {
  if (bsg_global_env == NULL || session_id_ == NULL)
    return;
  char *session_id = (char *)bsg_safe_get_string_utf_chars(env, session_id_);
  char *started_at = (char *)bsg_safe_get_string_utf_chars(env, start_date_);
  if (session_id != NULL && started_at != NULL) {
    bsg_request_env_write_lock();
    bugsnag_report_start_session(&bsg_global_env->next_report, session_id,
                                 started_at, handled_count, unhandled_count);

    bsg_release_env_write_lock();
  }
  bsg_safe_release_string_utf_chars(env, session_id_, session_id);
  bsg_safe_release_string_utf_chars(env, start_date_, started_at);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_stoppedSession(
    JNIEnv *env, jobject _this) {
    if (bsg_global_env == NULL) {
        return;
    }
    bsg_request_env_write_lock();
    bugsnag_report *report = &bsg_global_env->next_report;
    memset(report->session_id, 0, strlen(report->session_id));
    memset(report->session_start, 0, strlen(report->session_start));
    report->handled_events = 0;
    report->unhandled_events = 0;
    bsg_release_env_write_lock();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_clearBreadcrumbs(JNIEnv *env,
                                                           jobject _this) {
  if (bsg_global_env == NULL)
    return;
  bsg_request_env_write_lock();
  bugsnag_report_clear_breadcrumbs(&bsg_global_env->next_report);
  bsg_release_env_write_lock();
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_addBreadcrumb(
    JNIEnv *env, jobject _this, jstring name_, jstring crumb_type,
    jstring timestamp_, jobject metadata) {
  if (bsg_global_env == NULL)
    return;
  const char *name = bsg_safe_get_string_utf_chars(env, name_);
  const char *type = bsg_safe_get_string_utf_chars(env, crumb_type);
  const char *timestamp = bsg_safe_get_string_utf_chars(env, timestamp_);

  if (name != NULL && type != NULL && timestamp != NULL) {
    bugsnag_breadcrumb *crumb = calloc(1, sizeof(bugsnag_breadcrumb));
    bsg_strncpy_safe(crumb->name, name, sizeof(crumb->name));
    bsg_strncpy_safe(crumb->timestamp, timestamp, sizeof(crumb->timestamp));
    if (strcmp(type, "user") == 0) {
      crumb->type = BSG_CRUMB_USER;
    } else if (strcmp(type, "error") == 0) {
      crumb->type = BSG_CRUMB_ERROR;
    } else if (strcmp(type, "log") == 0) {
      crumb->type = BSG_CRUMB_LOG;
    } else if (strcmp(type, "navigation") == 0) {
      crumb->type = BSG_CRUMB_NAVIGATION;
    } else if (strcmp(type, "request") == 0) {
      crumb->type = BSG_CRUMB_REQUEST;
    } else if (strcmp(type, "state") == 0) {
      crumb->type = BSG_CRUMB_STATE;
    } else if (strcmp(type, "process") == 0) {
      crumb->type = BSG_CRUMB_PROCESS;
    } else {
      crumb->type = BSG_CRUMB_MANUAL;
    }

    bsg_populate_crumb_metadata(env, crumb, metadata);
    bsg_request_env_write_lock();
    bugsnag_report_add_breadcrumb(&bsg_global_env->next_report, crumb);
    bsg_release_env_write_lock();

    free(crumb);
  }
  bsg_safe_release_string_utf_chars(env, name_, name);
  bsg_safe_release_string_utf_chars(env, crumb_type, type);
  bsg_safe_release_string_utf_chars(env, timestamp_, timestamp);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateAppVersion(JNIEnv *env,
                                                           jobject _this,
                                                           jstring new_value) {
  if (bsg_global_env == NULL) {
    return;
  }
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }
  bsg_request_env_write_lock();
  bugsnag_report_set_app_version(&bsg_global_env->next_report, value);
  bsg_release_env_write_lock();
  bsg_safe_release_string_utf_chars(env, new_value, value);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateBuildUUID(JNIEnv *env,
                                                          jobject _this,
                                                          jstring new_value) {
  if (bsg_global_env == NULL) {
    return;
  }
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }
  bsg_request_env_write_lock();
  bugsnag_report_set_build_uuid(&bsg_global_env->next_report, value);
  bsg_release_env_write_lock();
  bsg_safe_release_string_utf_chars(env, new_value, value);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateContext(
    JNIEnv *env, jobject _this, jstring new_value) {
  if (bsg_global_env == NULL) {
    return;
  }
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }
  bsg_request_env_write_lock();
  bugsnag_report_set_context(&bsg_global_env->next_report, value);
  bsg_release_env_write_lock();
  if (new_value != NULL) {
    bsg_safe_release_string_utf_chars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateInForeground(
    JNIEnv *env, jobject _this, jboolean new_value, jstring activity_) {
  if (bsg_global_env == NULL) {
    return;
  }
  char *activity = (char *)bsg_safe_get_string_utf_chars(env, activity_);
  bsg_request_env_write_lock();
  bool was_in_foreground = bsg_global_env->next_report.app.in_foreground;
  bsg_global_env->next_report.app.in_foreground = (bool)new_value;
  bsg_strncpy_safe(bsg_global_env->next_report.app.active_screen, activity,
                   sizeof(bsg_global_env->next_report.app.active_screen));
  if ((bool)new_value) {
    if (!was_in_foreground) {
      time(&bsg_global_env->foreground_start_time);
    }
  } else {
    bsg_global_env->foreground_start_time = 0;
    bsg_global_env->next_report.app.duration_in_foreground_ms_offset = 0;
  }
  bsg_release_env_write_lock();
  if (activity_ != NULL) {
    bsg_safe_release_string_utf_chars(env, activity_, activity);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateLowMemory(JNIEnv *env,
                                                          jobject _this,
                                                          jboolean new_value) {
  if (bsg_global_env == NULL)
    return;
  bsg_request_env_write_lock();
  bsg_global_env->next_report.app.low_memory = (bool)new_value;
  bsg_release_env_write_lock();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateOrientation(JNIEnv *env,
                                                            jobject _this,
                                                            jint orientation) {
  if (bsg_global_env == NULL)
    return;

  bsg_request_env_write_lock();
  bugsnag_report_set_orientation(&bsg_global_env->next_report, orientation);
  bsg_release_env_write_lock();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateReleaseStage(
    JNIEnv *env, jobject _this, jstring new_value) {
  if (bsg_global_env == NULL) {
    return;
  }
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }
  bsg_request_env_write_lock();
  bugsnag_report_set_release_stage(&bsg_global_env->next_report, value);
  bsg_release_env_write_lock();
  if (new_value != NULL) {
    bsg_safe_release_string_utf_chars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateUserId(
    JNIEnv *env, jobject _this, jstring new_value) {
  if (bsg_global_env == NULL) {
    return;
  }
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }
  bsg_request_env_write_lock();
  bugsnag_report_set_user_id(&bsg_global_env->next_report, value);
  bsg_release_env_write_lock();
  if (new_value != NULL) {
    bsg_safe_release_string_utf_chars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateUserName(
    JNIEnv *env, jobject _this, jstring new_value) {
  if (bsg_global_env == NULL) {
    return;
  }
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }
  bsg_request_env_write_lock();
  bugsnag_report_set_user_name(&bsg_global_env->next_report, value);
  bsg_release_env_write_lock();
  if (new_value != NULL) {
    bsg_safe_release_string_utf_chars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateUserEmail(JNIEnv *env,
                                                          jobject _this,
                                                          jstring new_value) {
  if (bsg_global_env == NULL) {
    return;
  }
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }
  bsg_request_env_write_lock();
  bugsnag_report_set_user_email(&bsg_global_env->next_report, value);
  bsg_release_env_write_lock();
  if (new_value != NULL) {
    bsg_safe_release_string_utf_chars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addMetadataString(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_, jstring value_) {
  if (bsg_global_env == NULL) {
    return;
  }
  char *tab = (char *)bsg_safe_get_string_utf_chars(env, tab_);
  char *key = (char *)bsg_safe_get_string_utf_chars(env, key_);
  char *value = (char *)bsg_safe_get_string_utf_chars(env, value_);
  if (tab != NULL && key != NULL && value != NULL) {
    bsg_request_env_write_lock();
    bugsnag_report_add_metadata_string(&bsg_global_env->next_report, tab, key,
                                       value);
    bsg_release_env_write_lock();
  }
  bsg_safe_release_string_utf_chars(env, tab_, tab);
  bsg_safe_release_string_utf_chars(env, key_, key);
  bsg_safe_release_string_utf_chars(env, value_, value);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addMetadataDouble(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_, jdouble value_) {
  if (bsg_global_env == NULL) {
    return;
  }
  char *tab = (char *)bsg_safe_get_string_utf_chars(env, tab_);
  char *key = (char *)bsg_safe_get_string_utf_chars(env, key_);
  if (tab != NULL && key != NULL) {
    bsg_request_env_write_lock();
    bugsnag_report_add_metadata_double(&bsg_global_env->next_report, tab, key,
                                       (double) value_);
    bsg_release_env_write_lock();
  }
  bsg_safe_release_string_utf_chars(env, tab_, tab);
  bsg_safe_release_string_utf_chars(env, key_, key);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addMetadataBoolean(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_, jboolean value_) {
  if (bsg_global_env == NULL) {
    return;
  }
  char *tab = (char *)bsg_safe_get_string_utf_chars(env, tab_);
  char *key = (char *)bsg_safe_get_string_utf_chars(env, key_);
  if (tab != NULL && key != NULL) {
    bsg_request_env_write_lock();
    bugsnag_report_add_metadata_bool(&bsg_global_env->next_report, tab, key,
                                     (bool) value_);
    bsg_release_env_write_lock();
  }
  bsg_safe_release_string_utf_chars(env, tab_, tab);
  bsg_safe_release_string_utf_chars(env, key_, key);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_clearMetadataTab(JNIEnv *env,
                                                           jobject _this,
                                                           jstring tab_) {
  if (bsg_global_env == NULL) {
    return;
  }
  char *tab = (char *)bsg_safe_get_string_utf_chars(env, tab_);
  if (tab == NULL) {
    return;
  }
  bsg_request_env_write_lock();
  bugsnag_report_remove_metadata_tab(&bsg_global_env->next_report, tab);
  bsg_release_env_write_lock();
  bsg_safe_release_string_utf_chars(env, tab_, tab);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_removeMetadata(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_) {
  if (bsg_global_env == NULL) {
    return;
  }
  char *tab = (char *)bsg_safe_get_string_utf_chars(env, tab_);
  char *key = (char *)bsg_safe_get_string_utf_chars(env, key_);

  if (tab != NULL && key != NULL) {
    bsg_request_env_write_lock();
    bugsnag_report_remove_metadata(&bsg_global_env->next_report, tab, key);
    bsg_release_env_write_lock();
  }

  bsg_safe_release_string_utf_chars(env, tab_, tab);
  bsg_safe_release_string_utf_chars(env, key_, key);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateMetadata(
    JNIEnv *env, jobject _this, jobject metadata) {
  if (bsg_global_env == NULL) {
    return;
  }
  bsg_request_env_write_lock();
  bsg_populate_metadata(env, &bsg_global_env->next_report, metadata);
  bsg_release_env_write_lock();
}

#ifdef __cplusplus
}
#endif
