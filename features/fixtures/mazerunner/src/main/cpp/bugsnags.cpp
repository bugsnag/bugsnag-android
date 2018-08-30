#include <bugsnag.h>
#include <jni.h>
#include <stdlib.h>

extern "C" {
JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXCustomMetadataNativeNotifyScenario_activate(JNIEnv *env,
                                                                         jobject instance) {
  bugsnag_notify_env(env, "Oh no", "The mill is down", BSG_SEVERITY_INFO);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXNotifyScenario_activate(JNIEnv *env,
                                                                         jobject instance) {
  bugsnag_notify_env(env, "Oh no", "The mill is down", BSG_SEVERITY_INFO);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXBreadcrumbScenario_activate(JNIEnv *env,
                                                                             jobject instance) {
  bugsnag_leave_breadcrumb_env(env, "Cold beans detected", BSG_CRUMB_LOG);
  bugsnag_notify_env(env, "Oh no", "The mill is down", BSG_SEVERITY_INFO);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXUserInfoScenario_activate(JNIEnv *env,
                                                                           jobject instance) {
  bugsnag_set_user_env(env, "324523", NULL, "Jack Mill");
  bugsnag_notify_env(env, "Oh no", "The mill is down", BSG_SEVERITY_INFO);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXJavaBreadcrumbNativeNotifyScenario_activate(
    JNIEnv *env,
    jobject instance) {
  bugsnag_notify_env(env, "Oh no", "The mill is down", BSG_SEVERITY_ERR);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXNativeBreadcrumbJavaNotifyScenario_activate(
    JNIEnv *env,
    jobject instance) {
  bugsnag_leave_breadcrumb_env(env, "Cold beans detected", BSG_CRUMB_LOG);
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXNativeBreadcrumbNativeCrashScenario_activate(
    JNIEnv *env,
    jobject instance) {
  bugsnag_leave_breadcrumb_env(env, "Cold beans detected", BSG_CRUMB_LOG);
  int x = 47;
  if (x > 0)
    __builtin_trap();
  return 308;
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXNativeBreadcrumbJavaCrashScenario_activate(
    JNIEnv *env,
    jobject instance) {
  bugsnag_leave_breadcrumb_env(env, "Cold beans detected", BSG_CRUMB_LOG);
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXJavaBreadcrumbNativeCrashScenario_activate(
    JNIEnv *env,
    jobject instance) {
  int x = 47;
  if (x > 0)
    __builtin_trap();
  return 670;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXJavaUserInfoNativeCrashScenario_activate(
    JNIEnv *env,
    jobject instance) {
  int x = 47;
  if (x > 0)
    __builtin_trap();
  return 12812;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXCustomMetadataNativeCrashScenario_activate(
    JNIEnv *env,
    jobject instance) {
  int x = 47;
  if (x > 0)
    __builtin_trap();
  return 12167;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXJavaBreadcrumbNativeBreadcrumbScenario_activate(
    JNIEnv *env,
    jobject instance) {
  bugsnag_leave_breadcrumb_env(env, "Cold beans detected", BSG_CRUMB_LOG);
  int x = 47;
  if (x > 0)
    __builtin_trap();
  return 12167;
}
}
