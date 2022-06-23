#include "bugsnag_ndk.h"

#include <arpa/inet.h>
#include <jni.h>
#include <pthread.h>
#include <stdlib.h>
#include <string.h>
#include <utils/memory.h>

#include "event.h"
#include "featureflags.h"
#include "handlers/cpp_handler.h"
#include "handlers/signal_handler.h"
#include "jni_cache.h"
#include "metadata.h"
#include "safejni.h"
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
static void request_env_write_lock(void) {
  pthread_mutex_lock(&bsg_global_env_write_mutex);
}

/**
 * Once editing is complete, the lock must be released
 */
static void release_env_write_lock(void) {
  pthread_mutex_unlock(&bsg_global_env_write_mutex);
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

bool bsg_begin_handling_crash() {
  static bool expected = false;
  return atomic_compare_exchange_strong(&bsg_global_env->handling_crash,
                                        &expected, true);
}

void bsg_finish_handling_crash() {
  bsg_global_env->crash_handled = true;
  bsg_global_env->handling_crash = false;
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_NdkPlugin_enableCrashReporting(
    JNIEnv *env, jobject _this) {
  if (bsg_global_env == NULL) {
    BUGSNAG_LOG(
        "Attempted to enable crash reporting without first calling install()");
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

JNIEXPORT jstring JNICALL
Java_com_bugsnag_android_NdkPlugin_getBinaryArch(JNIEnv *env, jobject _this) {
#if defined(__i386__)
  const char *binary_arch = "x86";
#elif defined(__x86_64__)
  const char *binary_arch = "x86_64";
#elif defined(__arm__)
  const char *binary_arch = "arm32";
#elif defined(__aarch64__)
  const char *binary_arch = "arm64";
#else
  const char *binary_arch = "unknown";
#endif
  return bsg_safe_new_string_utf(env, binary_arch);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_enableCrashReporting(JNIEnv *env,
                                                               jobject _this) {
  if (bsg_global_env == NULL) {
    BUGSNAG_LOG(
        "Attempted to enable crash reporting without first calling install()");
    return;
  }
  bsg_handler_install_signal(bsg_global_env);
  bsg_handler_install_cpp(bsg_global_env);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_disableCrashReporting(JNIEnv *env,
                                                                jobject _this) {
  bsg_handler_uninstall_signal();
  bsg_handler_uninstall_cpp();
}

/**
 * Updates information to be serialized to LastRunInfo if this session
 * terminates abnormally.
 */
void bsg_update_next_run_info(bsg_environment *env) {
  bool launching = env->next_event.app.is_launching;
  char *crashed_value = launching ? "true" : "false";
  int launch_crashes = env->consecutive_launch_crashes;
  if (launching) {
    launch_crashes += 1;
  }
  sprintf(env->next_last_run_info,
          "consecutiveLaunchCrashes=%d\ncrashed=true\ncrashedDuringLaunch=%s",
          launch_crashes, crashed_value);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_install(
    JNIEnv *env, jobject _this, jstring _api_key, jstring _event_path,
    jstring _last_run_info_path, jint consecutive_launch_crashes,
    jboolean auto_detect_ndk_crashes, jint _api_level, jboolean is32bit,
    jint send_threads) {

  if (!bsg_jni_cache_init(env)) {
    BUGSNAG_LOG("Could not init JNI jni_cache.");
  }

  bsg_environment *bugsnag_env = calloc(1, sizeof(bsg_environment));
  bsg_unwinder_init();
  bugsnag_env->report_header.big_endian =
      htonl(47) == 47; // potentially too clever, see man 3 htonl
  bugsnag_env->report_header.version = BUGSNAG_EVENT_VERSION;
  bugsnag_env->consecutive_launch_crashes = consecutive_launch_crashes;
  bugsnag_env->send_threads = send_threads;
  bugsnag_env->handling_crash = ATOMIC_VAR_INIT(false);

  // copy event path to env struct
  const char *event_path = bsg_safe_get_string_utf_chars(env, _event_path);
  if (event_path == NULL) {
    goto error;
  }
  sprintf(bugsnag_env->next_event_path, "%s", event_path);
  bsg_safe_release_string_utf_chars(env, _event_path, event_path);

  // copy last run info path to env struct
  const char *last_run_info_path =
      bsg_safe_get_string_utf_chars(env, _last_run_info_path);
  if (last_run_info_path == NULL) {
    goto error;
  }
  bsg_strncpy(bugsnag_env->last_run_info_path, last_run_info_path,
              sizeof(bugsnag_env->last_run_info_path));
  bsg_safe_release_string_utf_chars(env, _last_run_info_path,
                                    last_run_info_path);

  if ((bool)auto_detect_ndk_crashes) {
    bsg_init_memory(bugsnag_env);
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
  if (bsg_strlen(bugsnag_env->next_event.device.os_build) > 0) {
    bsg_strncpy(bugsnag_env->report_header.os_build,
                bugsnag_env->next_event.device.os_build,
                sizeof(bugsnag_env->report_header.os_build));
  }

  const char *api_key = bsg_safe_get_string_utf_chars(env, _api_key);
  if (api_key != NULL) {
    bsg_strncpy(bugsnag_env->next_event.api_key, (char *)api_key,
                sizeof(bugsnag_env->next_event.api_key));
    bsg_safe_release_string_utf_chars(env, _api_key, api_key);
  }

  // clear the feature flag fields
  bugsnag_env->next_event.feature_flag_count = 0;
  bugsnag_env->next_event.feature_flags = NULL;

  bsg_global_env = bugsnag_env;
  bsg_update_next_run_info(bsg_global_env);
  BUGSNAG_LOG("Initialization complete!");
  return;

error:
  free(bugsnag_env);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_deliverReportAtPath(
    JNIEnv *env, jobject _this, jstring _report_path) {
  static pthread_mutex_t bsg_native_delivery_mutex = PTHREAD_MUTEX_INITIALIZER;
  pthread_mutex_lock(&bsg_native_delivery_mutex);

  const char *event_path = NULL;
  bugsnag_event *event = NULL;
  jbyteArray jpayload = NULL;
  jbyteArray jstage = NULL;
  char *payload = NULL;
  jstring japi_key = NULL;

  if (!bsg_jni_cache->initialized) {
    BUGSNAG_LOG("deliverReportAtPath failed: JNI cache not initialized.");
    goto exit;
  }

  event_path = bsg_safe_get_string_utf_chars(env, _report_path);
  if (event_path == NULL) {
    goto exit;
  }
  event = bsg_deserialize_event_from_file((char *)event_path);

  // remove persisted NDK struct early - this reduces the chance of crash loops
  // in delivery.
  remove(event_path);

  if (event == NULL) {
    BUGSNAG_LOG("Failed to read event at file: %s", event_path);
    goto exit;
  }

  payload = bsg_serialize_event_to_json_string(event);
  if (payload == NULL) {
    BUGSNAG_LOG("Failed to serialize event as JSON: %s", event_path);
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
  japi_key = bsg_safe_new_string_utf(env, event->api_key);
  if (japi_key != NULL) {
    bool is_launching = event->app.is_launching;
    bsg_safe_call_static_void_method(
        env, bsg_jni_cache->NativeInterface,
        bsg_jni_cache->NativeInterface_deliverReport, jstage, jpayload,
        japi_key, is_launching);
  }

exit:
  bsg_safe_release_string_utf_chars(env, _report_path, event_path);
  if (event != NULL) {
    bsg_safe_release_byte_array_elements(env, jstage,
                                         (jbyte *)event->app.release_stage);
    bsg_free_feature_flags(event);
    free(event);
  }
  bsg_safe_release_byte_array_elements(env, jpayload, (jbyte *)payload);
  free(payload);

  pthread_mutex_unlock(&bsg_native_delivery_mutex);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addHandledEvent(JNIEnv *env,
                                                          jobject _this) {
  if (bsg_global_env == NULL) {
    return;
  }
  request_env_write_lock();
  bugsnag_event *event = &bsg_global_env->next_event;

  if (bugsnag_event_has_session(event)) {
    event->handled_events++;
  }
  release_env_write_lock();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addUnhandledEvent(JNIEnv *env,
                                                            jobject _this) {
  if (bsg_global_env == NULL) {
    return;
  }
  request_env_write_lock();
  bugsnag_event *event = &bsg_global_env->next_event;

  if (bugsnag_event_has_session(event)) {
    event->unhandled_events++;
  }
  release_env_write_lock();
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_startedSession(
    JNIEnv *env, jobject _this, jstring session_id_, jstring start_date_,
    jint handled_count, jint unhandled_count) {
  if (bsg_global_env == NULL || session_id_ == NULL) {
    return;
  }
  char *session_id = (char *)bsg_safe_get_string_utf_chars(env, session_id_);
  char *started_at = (char *)bsg_safe_get_string_utf_chars(env, start_date_);
  if (session_id != NULL && started_at != NULL) {
    request_env_write_lock();
    bugsnag_event_start_session(&bsg_global_env->next_event, session_id,
                                started_at, handled_count, unhandled_count);
    release_env_write_lock();
  }
  bsg_safe_release_string_utf_chars(env, session_id_, session_id);
  bsg_safe_release_string_utf_chars(env, start_date_, started_at);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_pausedSession(
    JNIEnv *env, jobject _this) {
  if (bsg_global_env == NULL) {
    return;
  }
  request_env_write_lock();
  bugsnag_event *event = &bsg_global_env->next_event;
  memset(event->session_id, 0, bsg_strlen(event->session_id));
  memset(event->session_start, 0, bsg_strlen(event->session_start));
  event->handled_events = 0;
  event->unhandled_events = 0;
  release_env_write_lock();
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_addBreadcrumb(
    JNIEnv *env, jobject _this, jstring name_, jstring crumb_type,
    jstring timestamp_, jobject metadata) {

  if (!bsg_jni_cache->initialized) {
    BUGSNAG_LOG("addBreadcrumb failed: JNI cache not initialized.");
    return;
  }
  const char *name = bsg_safe_get_string_utf_chars(env, name_);
  const char *type = bsg_safe_get_string_utf_chars(env, crumb_type);
  const char *timestamp = bsg_safe_get_string_utf_chars(env, timestamp_);

  if (name != NULL && type != NULL && timestamp != NULL) {
    bugsnag_breadcrumb *crumb = calloc(1, sizeof(bugsnag_breadcrumb));
    bsg_strncpy(crumb->name, name, sizeof(crumb->name));
    bsg_strncpy(crumb->timestamp, timestamp, sizeof(crumb->timestamp));
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
    request_env_write_lock();
    bugsnag_event_add_breadcrumb(&bsg_global_env->next_event, crumb);
    release_env_write_lock();

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
  request_env_write_lock();
  bugsnag_app_set_version(&bsg_global_env->next_event, value);
  release_env_write_lock();
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
  request_env_write_lock();
  bugsnag_app_set_build_uuid(&bsg_global_env->next_event, value);
  release_env_write_lock();
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
  request_env_write_lock();
  bugsnag_event_set_context(&bsg_global_env->next_event, value);
  release_env_write_lock();
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
  request_env_write_lock();
  bool was_in_foreground = bsg_global_env->next_event.app.in_foreground;
  bsg_global_env->next_event.app.in_foreground = (bool)new_value;
  bsg_strncpy(bsg_global_env->next_event.app.active_screen, activity,
              sizeof(bsg_global_env->next_event.app.active_screen));
  if ((bool)new_value) {
    if (!was_in_foreground) {
      time(&bsg_global_env->foreground_start_time);
    }
  } else {
    bsg_global_env->foreground_start_time = 0;
    bsg_global_env->next_event.app.duration_in_foreground_ms_offset = 0;
  }
  release_env_write_lock();
  if (activity_ != NULL) {
    bsg_safe_release_string_utf_chars(env, activity_, activity);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateIsLaunching(
    JNIEnv *env, jobject _this, jboolean new_value) {
  if (bsg_global_env == NULL) {
    return;
  }
  request_env_write_lock();
  bugsnag_app_set_is_launching(&bsg_global_env->next_event, new_value);
  bsg_update_next_run_info(bsg_global_env);
  release_env_write_lock();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateLowMemory(
    JNIEnv *env, jobject _this, jboolean low_memory,
    jstring memory_trim_level_description) {
  if (bsg_global_env == NULL) {
    return;
  }

  char *memory_trim_level =
      (char *)bsg_safe_get_string_utf_chars(env, memory_trim_level_description);

  if (memory_trim_level == NULL) {
    return;
  }

  request_env_write_lock();
  bugsnag_event_add_metadata_bool(&bsg_global_env->next_event, "app",
                                  "lowMemory", (bool)low_memory);
  bugsnag_event_add_metadata_string(&bsg_global_env->next_event, "app",
                                    "memoryTrimLevel", memory_trim_level);
  release_env_write_lock();
  if (memory_trim_level_description != NULL) {
    bsg_safe_release_string_utf_chars(env, memory_trim_level_description,
                                      memory_trim_level);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateOrientation(JNIEnv *env,
                                                            jobject _this,
                                                            jstring new_value) {
  if (bsg_global_env == NULL) {
    return;
  }

  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }
  request_env_write_lock();
  bugsnag_device_set_orientation(&bsg_global_env->next_event, value);
  release_env_write_lock();
  if (new_value != NULL) {
    bsg_safe_release_string_utf_chars(env, new_value, value);
  }
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
  request_env_write_lock();
  bugsnag_app_set_release_stage(&bsg_global_env->next_event, value);
  release_env_write_lock();
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
  request_env_write_lock();
  bugsnag_event *event = &bsg_global_env->next_event;
  bugsnag_user user = bugsnag_event_get_user(event);
  bugsnag_event_set_user(event, value, user.email, user.name);
  release_env_write_lock();
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
  request_env_write_lock();
  bugsnag_event *event = &bsg_global_env->next_event;
  bugsnag_user user = bugsnag_event_get_user(event);
  bugsnag_event_set_user(event, user.id, user.email, value);
  release_env_write_lock();
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
  request_env_write_lock();
  bugsnag_event *event = &bsg_global_env->next_event;
  bugsnag_user user = bugsnag_event_get_user(event);
  bugsnag_event_set_user(event, user.id, value, user.name);
  release_env_write_lock();
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
    request_env_write_lock();
    bugsnag_event_add_metadata_string(&bsg_global_env->next_event, tab, key,
                                      value);
    release_env_write_lock();
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
    request_env_write_lock();
    bugsnag_event_add_metadata_double(&bsg_global_env->next_event, tab, key,
                                      (double)value_);
  }
  release_env_write_lock();
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
    request_env_write_lock();
    bugsnag_event_add_metadata_bool(&bsg_global_env->next_event, tab, key,
                                    (bool)value_);
    release_env_write_lock();
  }
  bsg_safe_release_string_utf_chars(env, tab_, tab);
  bsg_safe_release_string_utf_chars(env, key_, key);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addMetadataOpaque(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_, jstring value_) {
  if (bsg_global_env == NULL) {
    return;
  }
  char *tab = (char *)bsg_safe_get_string_utf_chars(env, tab_);
  char *key = (char *)bsg_safe_get_string_utf_chars(env, key_);
  char *value = (char *)bsg_safe_get_string_utf_chars(env, value_);
  if (tab != NULL && key != NULL) {
    request_env_write_lock();
    bsg_add_metadata_value_opaque(&bsg_global_env->next_event.metadata, tab,
                                  key, value);
    release_env_write_lock();
  }
  bsg_safe_release_string_utf_chars(env, tab_, tab);
  bsg_safe_release_string_utf_chars(env, key_, key);
  bsg_safe_release_string_utf_chars(env, value_, value);
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
  request_env_write_lock();
  bugsnag_event_clear_metadata_section(&bsg_global_env->next_event, tab);
  release_env_write_lock();
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
    request_env_write_lock();
    bugsnag_event_clear_metadata(&bsg_global_env->next_event, tab, key);
    release_env_write_lock();
  }

  bsg_safe_release_string_utf_chars(env, tab_, tab);
  bsg_safe_release_string_utf_chars(env, key_, key);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateMetadata(
    JNIEnv *env, jobject _this, jobject metadata) {
  if (bsg_global_env == NULL) {
    return;
  }

  if (!bsg_jni_cache->initialized) {
    BUGSNAG_LOG("updateMetadata failed: JNI cache not initialized.");
    return;
  }
  request_env_write_lock();
  bsg_populate_metadata(env, &bsg_global_env->next_event.metadata, metadata);
  release_env_write_lock();
}

JNIEXPORT jlong JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_getSignalUnwindStackFunction(
    JNIEnv *env, jobject thiz) {
  return (jlong)bsg_unwind_crash_stack;
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_addFeatureFlag(
    JNIEnv *env, jobject thiz, jstring name_, jstring variant_) {

  if (bsg_global_env == NULL) {
    return;
  }

  char *name = (char *)bsg_safe_get_string_utf_chars(env, name_);
  char *variant = (char *)bsg_safe_get_string_utf_chars(env, variant_);

  if (name != NULL) {
    request_env_write_lock();
    bsg_set_feature_flag(&bsg_global_env->next_event, name, variant);
    release_env_write_lock();
  }

  bsg_safe_release_string_utf_chars(env, name_, name);
  bsg_safe_release_string_utf_chars(env, variant_, variant);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_clearFeatureFlag(JNIEnv *env,
                                                           jobject thiz,
                                                           jstring name_) {

  if (bsg_global_env == NULL) {
    return;
  }

  char *name = (char *)bsg_safe_get_string_utf_chars(env, name_);

  if (name != NULL) {
    request_env_write_lock();
    bsg_clear_feature_flag(&bsg_global_env->next_event, name);
    release_env_write_lock();
  }

  bsg_safe_release_string_utf_chars(env, name_, name);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_clearFeatureFlags(JNIEnv *env,
                                                            jobject thiz) {
  if (bsg_global_env == NULL) {
    return;
  }

  request_env_write_lock();
  bsg_free_feature_flags(&bsg_global_env->next_event);
  release_env_write_lock();
}

#ifdef __cplusplus
}
#endif
