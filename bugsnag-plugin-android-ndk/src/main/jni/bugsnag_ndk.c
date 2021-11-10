#include "bugsnag_ndk.h"

#include <arpa/inet.h>
#include <jni.h>
#include <pthread.h>
#include <stdlib.h>
#include <string.h>

#include "crashtime_journal.h"
#include "crashtime_journal_primitives.h"
#include "event_cache.h"
#include "handlers/cpp_handler.h"
#include "handlers/signal_handler.h"
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
    jstring _crashtime_journal_path, jstring _last_run_info_path,
    jint consecutive_launch_crashes, jboolean auto_detect_ndk_crashes,
    jint _api_level, jboolean is32bit, jint send_threads) {
  bsg_environment *bugsnag_env = calloc(1, sizeof(bsg_environment));
  bsg_set_unwind_types((int)_api_level, (bool)is32bit,
                       &bugsnag_env->signal_unwind_style,
                       &bugsnag_env->unwind_style);
  bugsnag_env->report_header.big_endian =
      htonl(47) == 47; // potentially too clever, see man 3 htonl
  bugsnag_env->report_header.version = BUGSNAG_EVENT_VERSION;
  bugsnag_env->consecutive_launch_crashes = consecutive_launch_crashes;
  bugsnag_env->send_threads = send_threads;

  // copy event path to env struct
  const char *event_path = bsg_safe_get_string_utf_chars(env, _event_path);
  if (event_path == NULL) {
    return;
  }
  sprintf(bugsnag_env->next_event_path, "%s", event_path);
  bsg_safe_release_string_utf_chars(env, _event_path, event_path);

  const char *crashtime_journal_path =
      bsg_safe_get_string_utf_chars(env, _crashtime_journal_path);
  if (crashtime_journal_path == NULL) {
    return;
  }
  bsg_ctj_init(crashtime_journal_path);
  bsg_safe_release_string_utf_chars(env, _crashtime_journal_path,
                                    crashtime_journal_path);

  // copy last run info path to env struct
  const char *last_run_info_path =
      bsg_safe_get_string_utf_chars(env, _last_run_info_path);
  if (last_run_info_path == NULL) {
    return;
  }
  bsg_strncpy_safe(bugsnag_env->last_run_info_path, last_run_info_path,
                   sizeof(bugsnag_env->last_run_info_path));
  bsg_safe_release_string_utf_chars(env, _last_run_info_path,
                                    last_run_info_path);

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

  const char *api_key = bsg_safe_get_string_utf_chars(env, _api_key);
  if (api_key != NULL) {
    bsg_strncpy_safe(bugsnag_env->next_event.api_key, (char *)api_key,
                     sizeof(bugsnag_env->next_event.api_key));
    bsg_safe_release_string_utf_chars(env, _api_key, api_key);
  }

  bsg_global_env = bugsnag_env;
  bsg_update_next_run_info(bsg_global_env);
  BUGSNAG_LOG("Initialization complete!");
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
  bsg_cache_set_app_version(&bsg_global_env->next_event, value);
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
  bsg_cache_set_app_build_uuid(&bsg_global_env->next_event, value);
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
  bsg_cache_set_event_context(&bsg_global_env->next_event, value);
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
    bsg_safe_release_string_utf_chars(env, activity_, activity);
  }
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateIsLaunching(
    JNIEnv *env, jobject _this, jboolean new_value) {
  if (bsg_global_env == NULL) {
    return;
  }
  bsg_request_env_write_lock();
  bsg_cache_set_app_is_launching(&bsg_global_env->next_event, new_value);
  bsg_update_next_run_info(bsg_global_env);
  bsg_release_env_write_lock();
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

  bsg_request_env_write_lock();
  bsg_cache_set_metadata_bool(&bsg_global_env->next_event.metadata, "app",
                              "lowMemory", (bool)low_memory);
  bsg_cache_set_metadata_string(&bsg_global_env->next_event.metadata, "app",
                                "memoryTrimLevel", memory_trim_level);
  bsg_release_env_write_lock();
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
  bsg_request_env_write_lock();
  bsg_cache_set_device_orientation(&bsg_global_env->next_event, value);
  bsg_release_env_write_lock();
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
  bsg_request_env_write_lock();
  bsg_cache_set_app_release_stage(&bsg_global_env->next_event, value);
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
  bugsnag_event *event = &bsg_global_env->next_event;
  bugsnag_user user = bsg_cache_get_event_user(event);
  bsg_cache_set_event_user(event, value, user.email, user.name);
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
  bugsnag_event *event = &bsg_global_env->next_event;
  bugsnag_user user = bsg_cache_get_event_user(event);
  bsg_cache_set_event_user(event, user.id, user.email, value);
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
  bugsnag_event *event = &bsg_global_env->next_event;
  bugsnag_user user = bsg_cache_get_event_user(event);
  bsg_cache_set_event_user(event, user.id, value, user.name);
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
    bsg_cache_set_metadata_string(&bsg_global_env->next_event.metadata, tab,
                                  key, value);
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
    bsg_cache_set_metadata_double(&bsg_global_env->next_event.metadata, tab,
                                  key, (double)value_);
  }
  bsg_release_env_write_lock();
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
    bsg_cache_set_metadata_bool(&bsg_global_env->next_event.metadata, tab, key,
                                (bool)value_);
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
  bsg_cache_clear_metadata_section(&bsg_global_env->next_event, tab);
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
    bsg_cache_clear_metadata(&bsg_global_env->next_event, tab, key);
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
  bsg_populate_metadata(env, &bsg_global_env->next_event.metadata, metadata);
  bsg_release_env_write_lock();
}

ssize_t
bsg_unwind_stack_default(bugsnag_stackframe stacktrace[BUGSNAG_FRAMES_MAX],
                         siginfo_t *info, void *user_context) __asyncsafe;

JNIEXPORT jlong JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_getUnwindStackFunction(JNIEnv *env,
                                                                 jobject thiz) {
  return (jlong)bsg_unwind_stack_default;
}

#ifdef __cplusplus
}
#endif
