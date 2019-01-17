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

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_install(
    JNIEnv *env, jobject _this, jstring _report_path, jboolean auto_notify,
    jint _api_level, jboolean is32bit) {
  bsg_environment *bugsnag_env = calloc(1, sizeof(bsg_environment));
  bugsnag_env->unwind_style = bsg_get_unwind_type((int)_api_level, (bool)is32bit);
  bugsnag_env->report_header.big_endian =
      htonl(47) == 47; // potentially too clever, see man 3 htonl
  bugsnag_env->report_header.version = BUGSNAG_REPORT_VERSION;
  const char *report_path = (*env)->GetStringUTFChars(env, _report_path, 0);
  sprintf(bugsnag_env->next_report_path, "%s", report_path);

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

  (*env)->ReleaseStringUTFChars(env, _report_path, report_path);
  bsg_global_env = bugsnag_env;
  BUGSNAG_LOG("Initialization complete!");
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_deliverReportAtPath(
    JNIEnv *env, jobject _this, jstring _report_path) {
  static pthread_mutex_t bsg_native_delivery_mutex = PTHREAD_MUTEX_INITIALIZER;
  pthread_mutex_lock(&bsg_native_delivery_mutex);
  const char *report_path = (*env)->GetStringUTFChars(env, _report_path, 0);
  bugsnag_report *report =
      bsg_deserialize_report_from_file((char *)report_path);

  if (report != NULL) {
    char *payload = bsg_serialize_report_to_json_string(report);
    if (payload != NULL) {
      jclass interface_class =
          (*env)->FindClass(env, "com/bugsnag/android/NativeInterface");
      jmethodID jdeliver_method =
          (*env)->GetStaticMethodID(env, interface_class, "deliverReport",
                                    "(Ljava/lang/String;Ljava/lang/String;)V");
      jstring jpayload = (*env)->NewStringUTF(env, payload);
      jstring jstage = (*env)->NewStringUTF(env, report->app.release_stage);
      (*env)->CallStaticVoidMethod(env, interface_class, jdeliver_method,
                                   jstage, jpayload);
      (*env)->DeleteLocalRef(env, jpayload);
      (*env)->DeleteLocalRef(env, jstage);
      free(payload);
    } else {
      BUGSNAG_LOG("Failed to serialize report as JSON: %s", report_path);
    }
    free(report);
  } else {
    BUGSNAG_LOG("Failed to read report at file: %s", report_path);
  }
  remove(report_path);
  (*env)->ReleaseStringUTFChars(env, _report_path, report_path);
  pthread_mutex_unlock(&bsg_native_delivery_mutex);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addHandledEvent(JNIEnv *env,
                                                          jobject _this) {
  if (bsg_global_env == NULL)
    return;
  bsg_request_env_write_lock();
  bsg_global_env->next_report.handled_events++;
  bsg_release_env_write_lock();
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_startedSession(
    JNIEnv *env, jobject _this, jstring session_id_, jstring start_date_) {
  if (bsg_global_env == NULL || session_id_ == NULL)
    return;
  char *session_id = (char *)(*env)->GetStringUTFChars(env, session_id_, 0);
  char *started_at = (char *)(*env)->GetStringUTFChars(env, start_date_, 0);
  bsg_request_env_write_lock();
  bugsnag_report_start_session(&bsg_global_env->next_report, session_id,
                               started_at);
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, session_id_, session_id);
  (*env)->ReleaseStringUTFChars(env, start_date_, started_at);
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
  const char *name = (*env)->GetStringUTFChars(env, name_, 0);
  const char *type = (*env)->GetStringUTFChars(env, crumb_type, 0);
  const char *timestamp = (*env)->GetStringUTFChars(env, timestamp_, 0);
  bugsnag_breadcrumb *crumb = calloc(1, sizeof(bugsnag_breadcrumb));
  strncpy(crumb->name, name, sizeof(crumb->name));
  strncpy(crumb->timestamp, timestamp, sizeof(crumb->timestamp));
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
  (*env)->ReleaseStringUTFChars(env, name_, name);
  (*env)->ReleaseStringUTFChars(env, crumb_type, type);
  (*env)->ReleaseStringUTFChars(env, timestamp_, timestamp);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateAppVersion(JNIEnv *env,
                                                           jobject _this,
                                                           jstring new_value) {
  if (bsg_global_env == NULL)
    return;
  char *value = new_value == NULL
                    ? NULL
                    : (char *)(*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  bugsnag_report_set_app_version(&bsg_global_env->next_report, value);
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, new_value, value);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateBuildUUID(JNIEnv *env,
                                                          jobject _this,
                                                          jstring new_value) {
  if (bsg_global_env == NULL)
    return;
  char *value = new_value == NULL
                    ? NULL
                    : (char *)(*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  bugsnag_report_set_build_uuid(&bsg_global_env->next_report, value);
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, new_value, value);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateContext(
    JNIEnv *env, jobject _this, jstring new_value) {
  if (bsg_global_env == NULL)
    return;
  char *value = new_value == NULL
                    ? NULL
                    : (char *)(*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  bugsnag_report_set_context(&bsg_global_env->next_report, value);
  bsg_release_env_write_lock();
  if (new_value != NULL) {
    (*env)->ReleaseStringUTFChars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateInForeground(
    JNIEnv *env, jobject _this, jboolean new_value, jstring activity_) {
  if (bsg_global_env == NULL)
    return;
  char *activity = activity_ == NULL
                       ? NULL
                       : (char *)(*env)->GetStringUTFChars(env, activity_, 0);
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
    (*env)->ReleaseStringUTFChars(env, activity_, activity);
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
  if (bsg_global_env == NULL)
    return;
  char *value = new_value == NULL
                    ? NULL
                    : (char *)(*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  bugsnag_report_set_release_stage(&bsg_global_env->next_report, value);
  bsg_release_env_write_lock();
  if (new_value != NULL) {
    (*env)->ReleaseStringUTFChars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateUserId(
    JNIEnv *env, jobject _this, jstring new_value) {
  if (bsg_global_env == NULL)
    return;
  char *value = new_value == NULL
                    ? NULL
                    : (char *)(*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  bugsnag_report_set_user_id(&bsg_global_env->next_report, value);
  bsg_release_env_write_lock();
  if (new_value != NULL) {
    (*env)->ReleaseStringUTFChars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateUserName(
    JNIEnv *env, jobject _this, jstring new_value) {
  if (bsg_global_env == NULL)
    return;
  char *value = new_value == NULL
                    ? NULL
                    : (char *)(*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  bugsnag_report_set_user_name(&bsg_global_env->next_report, value);
  bsg_release_env_write_lock();
  if (new_value != NULL) {
    (*env)->ReleaseStringUTFChars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateUserEmail(JNIEnv *env,
                                                          jobject _this,
                                                          jstring new_value) {
  if (bsg_global_env == NULL)
    return;
  char *value = new_value == NULL
                    ? NULL
                    : (char *)(*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  bugsnag_report_set_user_email(&bsg_global_env->next_report, value);
  bsg_release_env_write_lock();
  if (new_value != NULL) {
    (*env)->ReleaseStringUTFChars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addMetadataString(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_, jstring value_) {
  if (bsg_global_env == NULL)
    return;
  char *tab = (char *)(*env)->GetStringUTFChars(env, tab_, 0);
  char *key = (char *)(*env)->GetStringUTFChars(env, key_, 0);
  char *value = (char *)(*env)->GetStringUTFChars(env, value_, 0);
  bsg_request_env_write_lock();
  bugsnag_report_add_metadata_string(&bsg_global_env->next_report, tab, key,
                                     value);
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, tab_, tab);
  (*env)->ReleaseStringUTFChars(env, key_, key);
  (*env)->ReleaseStringUTFChars(env, value_, value);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addMetadataDouble(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_, jdouble value_) {
  if (bsg_global_env == NULL)
    return;
  char *tab = (char *)(*env)->GetStringUTFChars(env, tab_, 0);
  char *key = (char *)(*env)->GetStringUTFChars(env, key_, 0);
  bsg_request_env_write_lock();
  bugsnag_report_add_metadata_double(&bsg_global_env->next_report, tab, key,
                                     (double)value_);
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, tab_, tab);
  (*env)->ReleaseStringUTFChars(env, key_, key);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addMetadataBoolean(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_, jboolean value_) {
  if (bsg_global_env == NULL)
    return;
  char *tab = (char *)(*env)->GetStringUTFChars(env, tab_, 0);
  char *key = (char *)(*env)->GetStringUTFChars(env, key_, 0);
  bsg_request_env_write_lock();
  bugsnag_report_add_metadata_bool(&bsg_global_env->next_report, tab, key,
                                   (bool)value_);
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, tab_, tab);
  (*env)->ReleaseStringUTFChars(env, key_, key);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_clearMetadataTab(JNIEnv *env,
                                                           jobject _this,
                                                           jstring tab_) {
  if (bsg_global_env == NULL)
    return;
  char *tab = (char *)(*env)->GetStringUTFChars(env, tab_, 0);
  bsg_request_env_write_lock();
  bugsnag_report_remove_metadata_tab(&bsg_global_env->next_report, tab);
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, tab_, tab);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_removeMetadata(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_) {
  if (bsg_global_env == NULL)
    return;
  char *tab = (char *)(*env)->GetStringUTFChars(env, tab_, 0);
  char *key = (char *)(*env)->GetStringUTFChars(env, key_, 0);

  bsg_request_env_write_lock();
  bugsnag_report_remove_metadata(&bsg_global_env->next_report, tab, key);
  bsg_release_env_write_lock();

  (*env)->ReleaseStringUTFChars(env, tab_, tab);
  (*env)->ReleaseStringUTFChars(env, key_, key);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateMetadata(
    JNIEnv *env, jobject _this, jobject metadata) {
  if (bsg_global_env == NULL)
    return;
  bsg_request_env_write_lock();
  bsg_populate_metadata(env, &bsg_global_env->next_report, metadata);
  bsg_release_env_write_lock();
}

#ifdef __cplusplus
}
#endif
