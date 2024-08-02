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
#include "internal_metrics.h"
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
 * crash) must first request the lock. Returns the bsg_environment that should
 * be used, may return NULL if there is no valid bsg_environment (no lock
 * will be held if this returns NULL)
 */
static bsg_environment *request_env_write_lock(void) {
  pthread_mutex_lock(&bsg_global_env_write_mutex);
  bsg_environment *local_env = bsg_global_env;
  if (local_env != NULL) {
    return local_env;
  } else {
    pthread_mutex_unlock(&bsg_global_env_write_mutex);
    return NULL;
  }
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
    bsg_notify_add_callback(&bsg_global_env->next_event, "ndkOnError");
  }
}

void bugsnag_remove_on_error() {
  if (bsg_global_env != NULL) {
    bsg_global_env->on_error = NULL;
    bsg_notify_remove_callback(&bsg_global_env->next_event, "ndkOnError");
  }
}

void bugsnag_refresh_symbol_table() { bsg_unwinder_refresh(); }

bool bsg_run_on_error() {
  bsg_on_error on_error = bsg_global_env->on_error;
  if (on_error != NULL) {
    return on_error(&bsg_global_env->next_event);
  }
  return true;
}

bool bsg_begin_handling_crash() {
  bool expected = false;
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
    jstring _last_run_info_path, jstring _event_uuid,
    jint consecutive_launch_crashes, jboolean auto_detect_ndk_crashes,
    jint _api_level, jboolean is32bit, jint send_threads,
    jint max_breadcrumbs) {

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

  bugsnag_env->next_event.max_crumb_count = max_breadcrumbs;
  bugsnag_env->next_event.breadcrumbs =
      calloc(max_breadcrumbs, sizeof(bugsnag_breadcrumb));

  if (bugsnag_env->next_event.breadcrumbs == NULL) {
    goto error;
  }

  // copy event path to env struct
  const char *event_path = bsg_safe_get_string_utf_chars(env, _event_path);
  if (event_path == NULL) {
    goto error;
  }
  bugsnag_env->event_path = strdup(event_path);
  bsg_safe_release_string_utf_chars(env, _event_path, event_path);

  // copy the event UUID to the env struct
  const char *event_uuid = bsg_safe_get_string_utf_chars(env, _event_uuid);
  if (event_uuid == NULL) {
    goto error;
  }
  bsg_strncpy(bugsnag_env->event_uuid, event_uuid,
              sizeof(bugsnag_env->event_uuid));
  bsg_safe_release_string_utf_chars(env, _event_uuid, event_uuid);

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

  atomic_init(&bugsnag_env->static_json_data, NULL);

  bsg_global_env = bugsnag_env;
  bsg_update_next_run_info(bsg_global_env);
  BUGSNAG_LOG("Initialization complete!");
  return;

error:
  free(bugsnag_env);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addHandledEvent(JNIEnv *env,
                                                          jobject _this) {
  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    return;
  }
  bugsnag_event *event = &bsg_env->next_event;

  if (bsg_event_has_session(event)) {
    event->handled_events++;
  }
  release_env_write_lock();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addUnhandledEvent(JNIEnv *env,
                                                            jobject _this) {
  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    return;
  }
  bugsnag_event *event = &bsg_env->next_event;

  if (bsg_event_has_session(event)) {
    event->unhandled_events++;
  }
  release_env_write_lock();
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_startedSession(
    JNIEnv *env, jobject _this, jstring session_id_, jstring start_date_,
    jint handled_count, jint unhandled_count) {
  if (session_id_ == NULL) {
    return;
  }
  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    return;
  }
  char *session_id = (char *)bsg_safe_get_string_utf_chars(env, session_id_);
  char *started_at = (char *)bsg_safe_get_string_utf_chars(env, start_date_);
  if (session_id != NULL && started_at != NULL) {
    bsg_event_start_session(&bsg_env->next_event, session_id, started_at,
                            handled_count, unhandled_count);
    release_env_write_lock();
  }
  bsg_safe_release_string_utf_chars(env, session_id_, session_id);
  bsg_safe_release_string_utf_chars(env, start_date_, started_at);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_pausedSession(
    JNIEnv *env, jobject _this) {
  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    return;
  }
  bugsnag_event *event = &bsg_env->next_event;
  memset(event->session_id, 0, bsg_strlen(event->session_id));
  memset(event->session_start, 0, bsg_strlen(event->session_start));
  event->handled_events = 0;
  event->unhandled_events = 0;
  release_env_write_lock();
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_addBreadcrumb(
    JNIEnv *env, jobject _this, jstring name_, jint crumb_type,
    jstring timestamp_, jobject metadata) {

  if (!bsg_jni_cache->initialized) {
    BUGSNAG_LOG("addBreadcrumb failed: JNI cache not initialized.");
    return;
  }
  const char *name = bsg_safe_get_string_utf_chars(env, name_);
  const char *timestamp = bsg_safe_get_string_utf_chars(env, timestamp_);

  if (name != NULL && timestamp != NULL) {
    bugsnag_breadcrumb *crumb = calloc(1, sizeof(bugsnag_breadcrumb));
    bsg_strncpy(crumb->name, name, sizeof(crumb->name));
    bsg_strncpy(crumb->timestamp, timestamp, sizeof(crumb->timestamp));

    // the values of crumb_type are defined in
    // NativeBridge.BreadcrumbType.toNativeValue()
    switch (crumb_type) {
    case 0:
      crumb->type = BSG_CRUMB_ERROR;
      break;
    case 1:
      crumb->type = BSG_CRUMB_LOG;
      break;
    case 2:
      crumb->type = BSG_CRUMB_MANUAL;
      break;
    case 3:
      crumb->type = BSG_CRUMB_NAVIGATION;
      break;
    case 4:
      crumb->type = BSG_CRUMB_PROCESS;
      break;
    case 5:
      crumb->type = BSG_CRUMB_REQUEST;
      break;
    case 6:
      crumb->type = BSG_CRUMB_STATE;
      break;
    case 7:
      crumb->type = BSG_CRUMB_USER;
      break;
    default:
      crumb->type = BSG_CRUMB_MANUAL;
    }

    bsg_populate_crumb_metadata(env, crumb, metadata);
    bsg_environment *bsg_env = request_env_write_lock();
    if (bsg_env == NULL) {
      goto end;
    }
    bsg_event_add_breadcrumb(&bsg_env->next_event, crumb);
    release_env_write_lock();

  end:
    free(crumb);
  }
  bsg_safe_release_string_utf_chars(env, name_, name);
  bsg_safe_release_string_utf_chars(env, timestamp_, timestamp);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateAppVersion(JNIEnv *env,
                                                           jobject _this,
                                                           jstring new_value) {
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }
  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    goto end;
  }
  bugsnag_app_set_version(&bsg_env->next_event, value);
  release_env_write_lock();
end:
  bsg_safe_release_string_utf_chars(env, new_value, value);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateBuildUUID(JNIEnv *env,
                                                          jobject _this,
                                                          jstring new_value) {
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }
  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    goto end;
  }
  bugsnag_app_set_build_uuid(&bsg_env->next_event, value);
end:
  bsg_safe_release_string_utf_chars(env, new_value, value);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateContext(
    JNIEnv *env, jobject _this, jstring new_value) {
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }
  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    goto end;
  }
  bugsnag_event_set_context(&bsg_env->next_event, value);
end:
  release_env_write_lock();
  if (new_value != NULL) {
    bsg_safe_release_string_utf_chars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateInForeground(
    JNIEnv *env, jobject _this, jboolean new_value, jstring activity_) {
  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    return;
  }
  char *activity = (char *)bsg_safe_get_string_utf_chars(env, activity_);
  bool was_in_foreground = bsg_env->next_event.app.in_foreground;
  bsg_env->next_event.app.in_foreground = (bool)new_value;
  bsg_strncpy(bsg_env->next_event.app.active_screen, activity,
              sizeof(bsg_env->next_event.app.active_screen));
  if ((bool)new_value) {
    if (!was_in_foreground) {
      time(&bsg_env->foreground_start_time);
    }
  } else {
    bsg_env->foreground_start_time = 0;
    bsg_env->next_event.app.duration_in_foreground_ms_offset = 0;
  }
  release_env_write_lock();
  if (activity_ != NULL) {
    bsg_safe_release_string_utf_chars(env, activity_, activity);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateIsLaunching(
    JNIEnv *env, jobject _this, jboolean new_value) {
  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    return;
  }
  bugsnag_app_set_is_launching(&bsg_env->next_event, new_value);
  bsg_update_next_run_info(bsg_env);
  release_env_write_lock();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateLowMemory(
    JNIEnv *env, jobject _this, jboolean low_memory,
    jstring memory_trim_level_description) {

  char *memory_trim_level =
      (char *)bsg_safe_get_string_utf_chars(env, memory_trim_level_description);

  if (memory_trim_level == NULL) {
    return;
  }

  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    goto end;
  }
  bugsnag_event_add_metadata_bool(&bsg_env->next_event, "app", "lowMemory",
                                  (bool)low_memory);
  bugsnag_event_add_metadata_string(&bsg_env->next_event, "app",
                                    "memoryTrimLevel", memory_trim_level);
  release_env_write_lock();
end:
  if (memory_trim_level_description != NULL) {
    bsg_safe_release_string_utf_chars(env, memory_trim_level_description,
                                      memory_trim_level);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateOrientation(JNIEnv *env,
                                                            jobject _this,
                                                            jstring new_value) {
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }

  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    goto end;
  }
  bugsnag_device_set_orientation(&bsg_env->next_event, value);
  release_env_write_lock();
end:
  if (new_value != NULL) {
    bsg_safe_release_string_utf_chars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateReleaseStage(
    JNIEnv *env, jobject _this, jstring new_value) {
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }
  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    goto end;
  }
  bugsnag_app_set_release_stage(&bsg_env->next_event, value);
  release_env_write_lock();
end:
  if (new_value != NULL) {
    bsg_safe_release_string_utf_chars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateUserId(
    JNIEnv *env, jobject _this, jstring new_value) {
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }

  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    goto end;
  }
  bugsnag_event *event = &bsg_env->next_event;
  bugsnag_user user = bugsnag_event_get_user(event);
  bugsnag_event_set_user(event, value, user.email, user.name);
  release_env_write_lock();
end:
  if (new_value != NULL) {
    bsg_safe_release_string_utf_chars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateUserName(
    JNIEnv *env, jobject _this, jstring new_value) {
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }

  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    goto end;
  }
  bugsnag_event *event = &bsg_env->next_event;
  bugsnag_user user = bugsnag_event_get_user(event);
  bugsnag_event_set_user(event, user.id, user.email, value);
  release_env_write_lock();
end:
  if (new_value != NULL) {
    bsg_safe_release_string_utf_chars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateUserEmail(JNIEnv *env,
                                                          jobject _this,
                                                          jstring new_value) {
  char *value = (char *)bsg_safe_get_string_utf_chars(env, new_value);
  if (value == NULL) {
    return;
  }

  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    goto end;
  }
  bugsnag_event *event = &bsg_env->next_event;
  bugsnag_user user = bugsnag_event_get_user(event);
  bugsnag_event_set_user(event, user.id, value, user.name);
  release_env_write_lock();
end:
  if (new_value != NULL) {
    bsg_safe_release_string_utf_chars(env, new_value, value);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addMetadataString(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_, jstring value_) {
  char *tab = (char *)bsg_safe_get_string_utf_chars(env, tab_);
  char *key = (char *)bsg_safe_get_string_utf_chars(env, key_);
  char *value = (char *)bsg_safe_get_string_utf_chars(env, value_);

  if (tab != NULL && key != NULL && value != NULL) {
    bsg_environment *bsg_env = request_env_write_lock();
    if (bsg_env == NULL) {
      goto end;
    }
    bugsnag_event_add_metadata_string(&bsg_env->next_event, tab, key, value);
    release_env_write_lock();
  }
end:
  bsg_safe_release_string_utf_chars(env, tab_, tab);
  bsg_safe_release_string_utf_chars(env, key_, key);
  bsg_safe_release_string_utf_chars(env, value_, value);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addMetadataDouble(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_, jdouble value_) {
  char *tab = (char *)bsg_safe_get_string_utf_chars(env, tab_);
  char *key = (char *)bsg_safe_get_string_utf_chars(env, key_);
  if (tab != NULL && key != NULL) {
    bsg_environment *bsg_env = request_env_write_lock();
    if (bsg_env == NULL) {
      goto end;
    }
    bugsnag_event_add_metadata_double(&bsg_env->next_event, tab, key,
                                      (double)value_);
  }
  release_env_write_lock();
end:
  bsg_safe_release_string_utf_chars(env, tab_, tab);
  bsg_safe_release_string_utf_chars(env, key_, key);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addMetadataBoolean(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_, jboolean value_) {
  char *tab = (char *)bsg_safe_get_string_utf_chars(env, tab_);
  char *key = (char *)bsg_safe_get_string_utf_chars(env, key_);
  if (tab != NULL && key != NULL) {
    bsg_environment *bsg_env = request_env_write_lock();
    if (bsg_env == NULL) {
      goto end;
    }
    bugsnag_event_add_metadata_bool(&bsg_env->next_event, tab, key,
                                    (bool)value_);
    release_env_write_lock();
  }
end:
  bsg_safe_release_string_utf_chars(env, tab_, tab);
  bsg_safe_release_string_utf_chars(env, key_, key);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addMetadataOpaque(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_, jstring value_) {
  char *tab = (char *)bsg_safe_get_string_utf_chars(env, tab_);
  char *key = (char *)bsg_safe_get_string_utf_chars(env, key_);
  char *value = (char *)bsg_safe_get_string_utf_chars(env, value_);
  if (tab != NULL && key != NULL) {
    bsg_environment *bsg_env = request_env_write_lock();
    if (bsg_env == NULL) {
      goto end;
    }
    bsg_add_metadata_value_opaque(&bsg_env->next_event.metadata, tab, key,
                                  value);
    release_env_write_lock();
  }
end:
  bsg_safe_release_string_utf_chars(env, tab_, tab);
  bsg_safe_release_string_utf_chars(env, key_, key);
  bsg_safe_release_string_utf_chars(env, value_, value);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_clearMetadataTab(JNIEnv *env,
                                                           jobject _this,
                                                           jstring tab_) {
  char *tab = (char *)bsg_safe_get_string_utf_chars(env, tab_);
  if (tab == NULL) {
    return;
  }
  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    goto end;
  }
  bugsnag_event_clear_metadata_section(&bsg_env->next_event, tab);
  release_env_write_lock();
end:
  bsg_safe_release_string_utf_chars(env, tab_, tab);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_removeMetadata(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_) {
  char *tab = (char *)bsg_safe_get_string_utf_chars(env, tab_);
  char *key = (char *)bsg_safe_get_string_utf_chars(env, key_);

  if (tab != NULL && key != NULL) {
    bsg_environment *bsg_env = request_env_write_lock();
    if (bsg_env == NULL) {
      goto end;
    }
    bugsnag_event_clear_metadata(&bsg_env->next_event, tab, key);
    release_env_write_lock();
  }

end:
  bsg_safe_release_string_utf_chars(env, tab_, tab);
  bsg_safe_release_string_utf_chars(env, key_, key);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateMetadata(
    JNIEnv *env, jobject _this, jobject metadata) {
  if (!bsg_jni_cache->initialized) {
    BUGSNAG_LOG("updateMetadata failed: JNI cache not initialized.");
    return;
  }

  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    return;
  }
  bsg_populate_metadata(env, &bsg_env->next_event.metadata, metadata);
  release_env_write_lock();
}

JNIEXPORT jlong JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_getSignalUnwindStackFunction(
    JNIEnv *env, jobject thiz) {
  return (jlong)bsg_unwind_crash_stack;
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_addFeatureFlag(
    JNIEnv *env, jobject thiz, jstring name_, jstring variant_) {

  char *name = (char *)bsg_safe_get_string_utf_chars(env, name_);
  char *variant = (char *)bsg_safe_get_string_utf_chars(env, variant_);

  if (name != NULL) {
    bsg_environment *bsg_env = request_env_write_lock();
    if (bsg_env == NULL) {
      goto end;
    }
    bsg_set_feature_flag(&bsg_env->next_event, name, variant);
    release_env_write_lock();
  }

end:
  bsg_safe_release_string_utf_chars(env, name_, name);
  bsg_safe_release_string_utf_chars(env, variant_, variant);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_clearFeatureFlag(JNIEnv *env,
                                                           jobject thiz,
                                                           jstring name_) {

  char *name = (char *)bsg_safe_get_string_utf_chars(env, name_);

  if (name != NULL) {
    bsg_environment *bsg_env = request_env_write_lock();
    if (bsg_env == NULL) {
      goto end;
    }
    bsg_clear_feature_flag(&bsg_env->next_event, name);
    release_env_write_lock();
  }

end:
  bsg_safe_release_string_utf_chars(env, name_, name);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_clearFeatureFlags(JNIEnv *env,
                                                            jobject thiz) {
  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    return;
  }
  bsg_free_feature_flags(&bsg_env->next_event);
  release_env_write_lock();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_refreshSymbolTable(JNIEnv *env,
                                                             jobject thiz) {
  bugsnag_refresh_symbol_table();
}

JNIEXPORT jobject JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_getCurrentCallbackSetCounts(
    JNIEnv *env, jobject thiz) {

  if (bsg_global_env == NULL || bsg_jni_cache == NULL) {
    return NULL;
  }
  static const int total_callbacks =
      sizeof(bsg_global_env->next_event.set_callback_counts) /
      sizeof(*bsg_global_env->next_event.set_callback_counts);

  jobject counts = bsg_safe_new_object(env, bsg_jni_cache->HashMap,
                                       bsg_jni_cache->HashMap_constructor);
  if (counts == NULL) {
    return NULL;
  }

  for (int i = 0; i < total_callbacks; i++) {
    jstring key = bsg_safe_new_string_utf(
        env, bsg_global_env->next_event.set_callback_counts[i].name);
    jobject value = bsg_safe_new_object(
        env, bsg_jni_cache->Int, bsg_jni_cache->Int_constructor,
        (jint)bsg_global_env->next_event.set_callback_counts[i].count);
    if (value == NULL) {
      return NULL;
    }
    bsg_safe_call_object_method(env, counts, bsg_jni_cache->HashMap_put, key,
                                value);
    bsg_safe_delete_local_ref(env, value);
  }

  return counts;
}

JNIEXPORT jobject JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_getCurrentNativeApiCallUsage(
    JNIEnv *env, jobject thiz) {
  if (bsg_global_env == NULL || bsg_jni_cache == NULL) {
    return NULL;
  }

  jobject map = bsg_safe_new_object(env, bsg_jni_cache->HashMap,
                                    bsg_jni_cache->HashMap_constructor);
  if (map == NULL) {
    return NULL;
  }

  jobject trueValue = bsg_safe_new_object(
      env, bsg_jni_cache->Boolean, bsg_jni_cache->Boolean_constructor, true);
  if (trueValue == NULL) {
    return NULL;
  }
  for (bsg_called_api i = 0; i < bsg_called_apis_count; i++) {
    if (bsg_was_api_called(&bsg_global_env->next_event, i)) {
      jstring key = bsg_safe_new_string_utf(env, bsg_called_api_names[i]);
      bsg_safe_call_object_method(env, map, bsg_jni_cache->HashMap_put, key,
                                  trueValue);
    }
  }
  bsg_safe_delete_local_ref(env, trueValue);

  return map;
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_setStaticJsonData(JNIEnv *env,
                                                            jobject thiz,
                                                            jstring data_) {
  if (bsg_global_env == NULL) {
    return;
  }

  const char *data = bsg_safe_get_string_utf_chars(env, data_);
  if (data == NULL) {
    return;
  }

  // strlen(data) == 0
  if (*data == 0) {
    goto done;
  }

  const char *new_data = strdup(data);
  if (!new_data) {
    goto done;
  }

  const char *data_old =
      atomic_exchange(&bsg_global_env->static_json_data, new_data);
  free((void *)data_old);

done:
  bsg_safe_release_string_utf_chars(env, data_, data);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_initCallbackCounts(JNIEnv *env,
                                                             jobject thiz,
                                                             jobject counts) {
  jobject entrySet =
      bsg_safe_call_object_method(env, counts, bsg_jni_cache->Map_entrySet);
  jobject iterator =
      bsg_safe_call_object_method(env, entrySet, bsg_jni_cache->Set_iterator);

  while (bsg_safe_call_boolean_method(env, iterator,
                                      bsg_jni_cache->Iterator_hasNext)) {
    jobject entry = bsg_safe_call_object_method(env, iterator,
                                                bsg_jni_cache->Iterator_next);
    jstring name =
        bsg_safe_call_object_method(env, entry, bsg_jni_cache->MapEntry_getKey);
    jobject value = bsg_safe_call_object_method(
        env, entry, bsg_jni_cache->MapEntry_getValue);
    const char *nameString = bsg_safe_get_string_utf_chars(env, name);
    jint intValue =
        (jint)bsg_safe_call_int_method(env, value, bsg_jni_cache->Int_intValue);
    bsg_set_callback_count(&bsg_global_env->next_event, nameString,
                           (int32_t)intValue);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_notifyAddCallback(JNIEnv *env,
                                                            jobject thiz,
                                                            jstring callback_) {
  const char *callback = bsg_safe_get_string_utf_chars(env, callback_);
  if (!callback) {
    return;
  }

  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    goto end;
  }
  bsg_notify_add_callback(&bsg_env->next_event, callback);
  release_env_write_lock();
end:
  bsg_safe_release_string_utf_chars(env, callback_, callback);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_notifyRemoveCallback(
    JNIEnv *env, jobject thiz, jstring callback_) {
  const char *callback = bsg_safe_get_string_utf_chars(env, callback_);
  if (!callback) {
    return;
  }

  bsg_environment *bsg_env = request_env_write_lock();
  if (bsg_env == NULL) {
    goto end;
  }
  bsg_notify_remove_callback(&bsg_env->next_event, callback);
  release_env_write_lock();
end:
  bsg_safe_release_string_utf_chars(env, callback_, callback);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_setInternalMetricsEnabled(
    JNIEnv *env, jobject thiz, jboolean enabled) {
  bsg_set_internal_metrics_enabled(enabled);
}

#ifdef __cplusplus
}
#endif
