#include "bugsnag_ndk.h"

#include <arpa/inet.h>
#include <jni.h>
#include <pthread.h>
#include <stdlib.h>
#include <string.h>

#include "handlers/signal_handler.h"
#include "metadata.h"
#include "report.h"
#include "utils/serializer.h"

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

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_install(
    JNIEnv *env, jobject _this, jstring _report_path, jboolean auto_notify,
    jint _api_level) {
  bsg_populate_jni_cache(env);
  bsg_environment *bugsnag_env = calloc(1, sizeof(bsg_environment));
  bugsnag_env->unwind_style = bsg_get_unwind_type((int)_api_level);
  bugsnag_env->report_header.big_endian =
      htonl(47) == 47; // potentially too clever, see man 3 htonl
  bugsnag_env->report_header.version = BUGSNAG_REPORT_VERSION;
  const char *report_path = (*env)->GetStringUTFChars(env, _report_path, 0);
  sprintf(bugsnag_env->next_report_path, "%s", report_path);

  if ((bool)auto_notify) {
    bsg_handler_install_signal(bugsnag_env);
  }

  // populate metadata from Java layer
  bsg_populate_app_data(env, &bugsnag_env->next_report);
  bsg_populate_device_data(env, &bugsnag_env->next_report);
  bsg_populate_user_data(env, &bugsnag_env->next_report);
  bsg_populate_context(env, &bugsnag_env->next_report);
  bsg_populate_breadcrumbs(env, &bugsnag_env->next_report);
  bsg_populate_metadata(env, &bugsnag_env->next_report);

  // If set, save os build info to report info header
  if (strlen(bugsnag_env->next_report.device.os_build) > 0) {
    strcpy(&bugsnag_env->report_header.os_build,
           bugsnag_env->next_report.device.os_build);
  }

  (*env)->ReleaseStringUTFChars(env, _report_path, report_path);
  bsg_global_env = bugsnag_env;
  BUGSNAG_LOG("Initialization complete!");
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_deliverReportAtPath(
    JNIEnv *env, jobject _this, jstring _report_path) {
  const char *report_path = (*env)->GetStringUTFChars(env, _report_path, 0);
  bugsnag_report *report = bsg_deserialize_report_from_file((char *)report_path);

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
    }
    free(report);
  }
  remove(report_path);
  (*env)->ReleaseStringUTFChars(env, _report_path, report_path);
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
    JNIEnv *env, jstring session_id_, jlong start_date_) {
  if (bsg_global_env == NULL)
    return;
  const char *session_id = (*env)->GetStringUTFChars(env, session_id_, 0);
  bsg_request_env_write_lock();
  strcpy(bsg_global_env->next_report.session_id, session_id);
  bsg_global_env->next_report.session_start = (long)start_date_;
  bsg_global_env->next_report.handled_events = 0;
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, session_id_, session_id);
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
    jobject metadata) {
  if (bsg_global_env == NULL)
    return;
  const char *name = (*env)->GetStringUTFChars(env, name_, 0);
  const char *type = (*env)->GetStringUTFChars(env, crumb_type, 0);

  bsg_request_env_write_lock();
  bsg_release_env_write_lock();

  (*env)->ReleaseStringUTFChars(env, name_, name);
  (*env)->ReleaseStringUTFChars(env, crumb_type, type);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateAppVersion(JNIEnv *env,
                                                           jobject _this,
                                                           jstring new_value) {
  if (bsg_global_env == NULL)
    return;
  const char *value = (*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  if (value == NULL) {

  } else {
  }
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, new_value, value);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateBuildUUID(JNIEnv *env,
                                                          jobject _this,
                                                          jstring new_value) {
  if (bsg_global_env == NULL)
    return;
  const char *value = (*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, new_value, value);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateContext(
    JNIEnv *env, jobject _this, jstring new_value) {
  if (bsg_global_env == NULL)
    return;
  const char *value = (*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, new_value, value);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateInForeground(
    JNIEnv *env, jobject _this, jboolean new_value) {
  if (bsg_global_env == NULL)
    return;
  bsg_request_env_write_lock();
  bsg_global_env->next_report.app.in_foreground = (bool)new_value;
  bsg_release_env_write_lock();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateLowMemory(JNIEnv *env,
                                                          jobject _this,
                                                          jboolean new_value) {
  if (bsg_global_env == NULL)
    return;
  bsg_request_env_write_lock();
  bsg_release_env_write_lock();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateOrientation(JNIEnv *env,
                                                            jobject _this,
                                                            jstring new_value) {
  if (bsg_global_env == NULL)
    return;
  const char *value = (*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, new_value, value);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateReleaseStage(
    JNIEnv *env, jobject _this, jstring new_value) {
  if (bsg_global_env == NULL)
    return;
  const char *value = (*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, new_value, value);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateUserId(
    JNIEnv *env, jobject _this, jstring new_value) {
  if (bsg_global_env == NULL)
    return;
  const char *value = (*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  strncpy(bsg_global_env->next_report.user.id, value, sizeof(char) * 64);
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, new_value, value);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateUserName(
    JNIEnv *env, jobject _this, jstring new_value) {
  if (bsg_global_env == NULL)
    return;
  const char *value = (*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  strncpy(bsg_global_env->next_report.user.name, value, sizeof(char) * 64);
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, new_value, value);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_updateUserEmail(JNIEnv *env,
                                                          jobject _this,
                                                          jstring new_value) {
  if (bsg_global_env == NULL)
    return;
  const char *value = (*env)->GetStringUTFChars(env, new_value, 0);
  bsg_request_env_write_lock();
  strncpy(bsg_global_env->next_report.user.email, value, sizeof(char) * 64);
  bsg_release_env_write_lock();
  (*env)->ReleaseStringUTFChars(env, new_value, value);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_NativeBridge_addMetadataString(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_, jstring value_) {
  if (bsg_global_env == NULL)
    return;
  const char *tab = (*env)->GetStringUTFChars(env, tab_, 0);
  const char *key = (*env)->GetStringUTFChars(env, key_, 0);
  const char *value = (*env)->GetStringUTFChars(env, value_, 0);
  bsg_request_env_write_lock();
  bugsnag_report_add_metadata_string(&bsg_global_env->next_report, (char *)tab, (char *)key,
                                     (char *)value);
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
  const char *tab = (*env)->GetStringUTFChars(env, tab_, 0);
  const char *key = (*env)->GetStringUTFChars(env, key_, 0);
  bsg_request_env_write_lock();
  bugsnag_report_add_metadata_double(&bsg_global_env->next_report, (char *)tab, (char *)key,
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
  const char *tab = (*env)->GetStringUTFChars(env, tab_, 0);
  const char *key = (*env)->GetStringUTFChars(env, key_, 0);
  bsg_request_env_write_lock();
  bugsnag_report_add_metadata_bool(&bsg_global_env->next_report, (char *)tab, (char *)key,
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
  bsg_request_env_write_lock();
  bsg_release_env_write_lock();
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_removeMetadata(
    JNIEnv *env, jobject _this, jstring tab_, jstring key_) {
  if (bsg_global_env == NULL)
    return;
  const char *tab = (*env)->GetStringUTFChars(env, tab_, 0);
  const char *key = (*env)->GetStringUTFChars(env, key_, 0);

  bsg_request_env_write_lock();
  bugsnag_report_remove_metadata(&bsg_global_env->next_report, (char *)tab, (char *)key);
  bsg_release_env_write_lock();

  (*env)->ReleaseStringUTFChars(env, tab_, tab);
  (*env)->ReleaseStringUTFChars(env, key_, key);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_NativeBridge_updateMetadata(
    JNIEnv *env, jobject _this, jobject metadata) {
  if (bsg_global_env == NULL)
    return;
  bsg_request_env_write_lock();
  bsg_populate_metadata(env, &bsg_global_env->next_report);
  bsg_release_env_write_lock();
}

#ifdef __cplusplus
}
#endif
