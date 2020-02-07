#include "bugsnag_ndk.h"

#include <arpa/inet.h>
#include <jni.h>
#include <pthread.h>
#include <stdlib.h>
#include <string.h>

#include "handlers/signal_handler.h"
#include "handlers/cpp_handler.h"
#include "metadata.h"
#include "event.h"
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

void bugsnag_add_on_error(bsg_on_error on_error) {
  if (bsg_global_env != NULL) {
    bsg_global_env->on_error = on_error;
  }
}

void bugsnag_remove_on_error() {
  if (bsg_global_env != NULL) {
    bsg_global_env->on_error = NULL;
  }
}

bool bsg_run_on_error() {
  bsg_on_error on_error = bsg_global_env->on_error;
  if (on_error != NULL) {
      return on_error(&bsg_global_env->next_event);
  }
  return true;
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
    JNIEnv *env, jobject _this, jstring _event_path, jboolean auto_detect_ndk_crashes,
    jint _api_level, jboolean is32bit) {
  bsg_environment *bugsnag_env = calloc(1, sizeof(bsg_environment));
  bsg_set_unwind_types((int)_api_level, (bool)is32bit,
                       &bugsnag_env->signal_unwind_style,
                       &bugsnag_env->unwind_style);
  bugsnag_env->report_header.big_endian =
      htonl(47) == 47; // potentially too clever, see man 3 htonl
  bugsnag_env->report_header.version = BUGSNAG_EVENT_VERSION;
  const char *event_path = (*env)->GetStringUTFChars(env, _event_path, 0);
  sprintf(bugsnag_env->next_event_path, "%s", event_path);

  if ((bool)auto_detect_ndk_crashes) {
    bsg_handler_install_signal(bugsnag_env);
    bsg_handler_install_cpp(bugsnag_env);
  }

  // populate metadata from Java layer
  bsg_populate_event(env, &bugsnag_env->next_event);
  time(&bugsnag_env->start_time);
  if (bugsnag_env->next_event.app.in_foreground) {
    bugsnag_env->foreground_start_time = bugsnag_env->start_time;
  }

  // If set, save os build info to report info header
  if (strlen(bugsnag_env->next_event.device.os_build) > 0) {
    bsg_strncpy_safe(bugsnag_env->report_header.os_build,
                     bugsnag_env->next_event.device.os_build,
                     sizeof(bugsnag_env->report_header.os_build));
  }

  (*env)->ReleaseStringUTFChars(env, _event_path, event_path);
  bsg_global_env = bugsnag_env;
  BUGSNAG_LOG("Initialization complete!");
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_deliverReportAtPath(
    JNIEnv *env, jobject _this, jstring _report_path) {
  static pthread_mutex_t bsg_native_delivery_mutex = PTHREAD_MUTEX_INITIALIZER;
  pthread_mutex_lock(&bsg_native_delivery_mutex);
  const char *event_path = (*env)->GetStringUTFChars(env, _report_path, 0);
  bugsnag_event *event =
          bsg_deserialize_event_from_file((char *) event_path);

  if (event != NULL) {
    char *payload = bsg_serialize_event_to_json_string(event);
    if (payload != NULL) {
      jclass interface_class =
          (*env)->FindClass(env, "com/bugsnag/android/NativeInterface");
      jmethodID jdeliver_method =
          (*env)->GetStaticMethodID(env, interface_class, "deliverReport",
                                    "([B[B)V");
      size_t payload_length = bsg_strlen(payload);
      jbyteArray jpayload = (*env)->NewByteArray(env, payload_length);
      (*env)->SetByteArrayRegion(env, jpayload, 0, payload_length, (jbyte *)payload);

      size_t stage_length = bsg_strlen(event->app.release_stage);
      jbyteArray jstage = (*env)->NewByteArray(env, stage_length);
      (*env)->SetByteArrayRegion(env, jstage, 0, stage_length, (jbyte *)event->app.release_stage);

      (*env)->CallStaticVoidMethod(env, interface_class, jdeliver_method,
                                   jstage, jpayload);
      (*env)->ReleaseByteArrayElements(env, jpayload, (jbyte *)payload, 0); // <-- frees payload
      (*env)->ReleaseByteArrayElements(env, jstage, (jbyte *)event->app.release_stage, JNI_COMMIT);
      (*env)->DeleteLocalRef(env, jpayload);
      (*env)->DeleteLocalRef(env, jstage);
    } else {
      BUGSNAG_LOG("Failed to serialize event as JSON: %s", event_path);
    }
    free(event);
  } else {
    BUGSNAG_LOG("Failed to read event at file: %s", event_path);
  }
  remove(event_path);
  (*env)->ReleaseStringUTFChars(env, _report_path, event_path);
  pthread_mutex_unlock(&bsg_native_delivery_mutex);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addHandledEvent(JNIEnv *env,
                                                          jobject _this) {
  if (bsg_global_env == NULL)
    return;
  bsg_request_env_write_lock();
  bugsnag_event *event = &bsg_global_env->next_event;

  if (bugsnag_event_has_session(event)) {
    event->handled_events++;
  }
  bsg_release_env_write_lock();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addUnhandledEvent(JNIEnv *env,
                                                            jobject _this) {
    if (bsg_global_env == NULL)
        return;
    bsg_request_env_write_lock();
    bugsnag_event *event = &bsg_global_env->next_event;

    if (bugsnag_event_has_session(event)) {
        event->unhandled_events++;
    }
    bsg_release_env_write_lock();
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_startedSession(
    JNIEnv *env, jobject _this, jstring session_id_, jstring start_date_,
    jint handled_count, jint unhandled_count) {
  if (bsg_global_env == NULL || session_id_ == NULL)
    return;
  char *session_id = (char *)(*env)->GetStringUTFChars(env, session_id_, 0);
  char *started_at = (char *)(*env)->GetStringUTFChars(env, start_date_, 0);
  bsg_request_env_write_lock();
  bugsnag_event_start_session(&bsg_global_env->next_event, session_id,
                              started_at, handled_count, unhandled_count);

  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, session_id_, session_id);
  (*env)->ReleaseStringUTFChars(env, start_date_, started_at);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_pausedSession(
    JNIEnv *env, jobject _this) {
    if (bsg_global_env == NULL) {
        return;
    }
    bsg_request_env_write_lock();
    bugsnag_event *event = &bsg_global_env->next_event;
    memset(event->session_id, 0, strlen(event->session_id));
    memset(event->session_start, 0, strlen(event->session_start));
    event->handled_events = 0;
    event->unhandled_events = 0;
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
  bugsnag_event_add_breadcrumb(&bsg_global_env->next_event, crumb);
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
  bugsnag_app_set_version(&bsg_global_env->next_event, value);
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
  bugsnag_app_set_build_uuid(&bsg_global_env->next_event, value);
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
  bugsnag_event_set_context(&bsg_global_env->next_event, value);
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
  bool was_in_foreground = bsg_global_env->next_event.app.in_foreground;
  bsg_global_env->next_event.app.in_foreground = (bool)new_value;
  bsg_strncpy_safe(bsg_global_env->next_event.app.active_screen, activity,
                   sizeof(bsg_global_env->next_event.app.active_screen));
  if ((bool)new_value) {
    if (!was_in_foreground) {
      time(&bsg_global_env->foreground_start_time);
    }
  } else {
    bsg_global_env->foreground_start_time = 0;
    bsg_global_env->next_event.app.duration_in_foreground_ms_offset = 0;
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
  bugsnag_event_add_metadata_bool(&bsg_global_env->next_event, "app", "lowMemory", (bool)new_value);
  bsg_release_env_write_lock();
}

const char *bsg_orientation_from_degrees(int orientation) {
  if (orientation < 0) {
    return "unknown";
  } else if (orientation >= 315 || orientation <= 45) {
    return "portrait";
  } else if (orientation <= 135) {
    return "landscape";
  } else if (orientation <= 225) {
    return "portrait";
  } else {
    return "landscape";
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateOrientation(JNIEnv *env,
                                                            jobject _this,
                                                            jint orientation) {
  if (bsg_global_env == NULL)
    return;

  bsg_request_env_write_lock();
  bugsnag_device_set_orientation(&bsg_global_env->next_event,
                                 (char *) bsg_orientation_from_degrees(orientation));
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
  bugsnag_app_set_release_stage(&bsg_global_env->next_event, value);
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
  bugsnag_event_set_user_id(&bsg_global_env->next_event, value);
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
  bugsnag_event_set_user_name(&bsg_global_env->next_event, value);
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
  bugsnag_event_set_user_email(&bsg_global_env->next_event, value);
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
  bugsnag_event_add_metadata_string(&bsg_global_env->next_event, tab, key,
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
  bugsnag_event_add_metadata_double(&bsg_global_env->next_event, tab, key,
                                    (double) value_);
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
  bugsnag_event_add_metadata_bool(&bsg_global_env->next_event, tab, key,
                                  (bool) value_);
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
  bugsnag_event_clear_metadata_section(&bsg_global_env->next_event, tab);
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
  bugsnag_event_clear_metadata(&bsg_global_env->next_event, tab, key);
  bsg_release_env_write_lock();

  (*env)->ReleaseStringUTFChars(env, tab_, tab);
  (*env)->ReleaseStringUTFChars(env, key_, key);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateMetadata(
    JNIEnv *env, jobject _this, jobject metadata) {
  if (bsg_global_env == NULL)
    return;
  bsg_request_env_write_lock();
  bsg_populate_metadata(env, &bsg_global_env->next_event, metadata);
  bsg_release_env_write_lock();
}

#ifdef __cplusplus
}
#endif
