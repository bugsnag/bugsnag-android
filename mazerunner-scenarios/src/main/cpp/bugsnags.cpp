//#include <bugsnag.h>
#include <jni.h>
#include <stdlib.h>

extern "C" {
//JNIEXPORT void JNICALL
//Java_com_bugsnag_android_mazerunner_scenarios_CXXCustomMetadataNativeNotifyScenario_activate(JNIEnv *env,
//                                                                         jobject instance) {
//  bugsnag_notify_env(env, (char *)"Twitter Overdose",
//                     (char *)"Turn off the internet and go outside", BSG_SEVERITY_INFO);
//}
//
//JNIEXPORT void JNICALL
//Java_com_bugsnag_android_mazerunner_scenarios_CXXAutoContextScenario_activate(JNIEnv *env,
//                                                                         jobject instance) {
//  bugsnag_notify_env(env, (char *)"Hello hello",
//                     (char *)"This is a new world", BSG_SEVERITY_INFO);
//}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXStartSessionScenario_crash(JNIEnv *env,
                                                                            jobject instance,
                                                                            jint value) {
    int x = 22;
    if (x > 0)
        __builtin_trap();
    return 338;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXPausedSessionScenario_crash(JNIEnv *env,
                                                                           jobject instance,
                                                                           jint value) {
    int x = 22552;
    if (x > 0)
        __builtin_trap();
    return 555;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXUpdateContextCrashScenario_crash(JNIEnv *env,
                                                                         jobject instance,
                                                                         jint value) {
  if (value > 0)
    __builtin_trap();
  return 12067 / value;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXSessionInfoCrashScenario_crash(JNIEnv *env,
                                                                         jobject instance,
                                                                         jint value) {
  if (value > 0)
    __builtin_trap();
  return 48206 / value;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXExtraordinaryLongStringScenario_crash(JNIEnv *env,
                                                                         jobject instance,
                                                                         jint value) {
  if (value > 0)
    __builtin_trap();
  return 12062 / value;
}

//JNIEXPORT void JNICALL
//Java_com_bugsnag_android_mazerunner_scenarios_NativeNotifyBeforeSendScenario_activate(JNIEnv *env,
//                                                                         jobject instance) {
//  bugsnag_notify_env(env, (char *)"Ad-hoc",
//                     (char *)"Auto-generated issue", BSG_SEVERITY_INFO);
//}
//
//JNIEXPORT void JNICALL
//Java_com_bugsnag_android_mazerunner_scenarios_CXXNotifyScenario_activate(JNIEnv *env,
//                                                                         jobject instance) {
//  bugsnag_notify_env(env, (char *)"Vitamin C deficiency",
//                     (char *)"9 out of 10 adults do not get their 5-a-day", BSG_SEVERITY_ERR);
//}
//
//JNIEXPORT void JNICALL
//Java_com_bugsnag_android_mazerunner_scenarios_CXXDelayedNotifyScenario_activate(JNIEnv *env,
//                                                                         jobject instance) {
//  bugsnag_notify_env(env, (char *)"Ferret Escape!", (char *)"oh no", BSG_SEVERITY_ERR);
//}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXDelayedCrashScenario_activate(JNIEnv *env,
                                                                         jobject instance,
                                                                         jint value) {
  if (value > 0)
    __builtin_trap();
  return 12067 / value;
}

//JNIEXPORT void JNICALL
//Java_com_bugsnag_android_mazerunner_scenarios_CXXBreadcrumbScenario_activate(JNIEnv *env,
//                                                                             jobject instance) {
//  bugsnag_leave_breadcrumb_env(env, (char *)"Cold beans detected", BSG_CRUMB_LOG);
//  bugsnag_notify_env(env, (char *)"Bean temperature loss",
//                     (char *)"100% more microwave required", BSG_SEVERITY_INFO);
//}
//
//JNIEXPORT void JNICALL
//Java_com_bugsnag_android_mazerunner_scenarios_CXXUserInfoScenario_activate(JNIEnv *env,
//                                                                           jobject instance) {
//  bugsnag_set_user_env(env, (char *)"324523", NULL, (char *)"Jack Mill");
//  bugsnag_notify_env(env, (char *)"Connection lost",
//                     (char *)"No antenna detected", BSG_SEVERITY_INFO);
//}
//
//JNIEXPORT void JNICALL
//Java_com_bugsnag_android_mazerunner_scenarios_CXXJavaBreadcrumbNativeNotifyScenario_activate(
//    JNIEnv *env,
//    jobject instance) {
//  bugsnag_notify_env(env, (char *)"Failed instantiation",
//                     (char *)"Could not allocate", BSG_SEVERITY_ERR);
//}
//
//JNIEXPORT void JNICALL
//Java_com_bugsnag_android_mazerunner_scenarios_CXXNativeBreadcrumbJavaNotifyScenario_activate(
//    JNIEnv *env,
//    jobject instance) {
//  bugsnag_leave_breadcrumb_env(env, (char *)"Rerun field analysis", BSG_CRUMB_PROCESS);
//}
//
//JNIEXPORT int JNICALL
//Java_com_bugsnag_android_mazerunner_scenarios_CXXNativeBreadcrumbNativeCrashScenario_activate(
//    JNIEnv *env,
//    jobject instance) {
//  bugsnag_leave_breadcrumb_env(env, (char *)"Substandard nacho error", BSG_CRUMB_REQUEST);
//  int x = 47;
//  if (x > 0)
//    __builtin_trap();
//  return 308;
//}
//
//JNIEXPORT void JNICALL
//Java_com_bugsnag_android_mazerunner_scenarios_CXXNativeBreadcrumbJavaCrashScenario_activate(
//    JNIEnv *env,
//    jobject instance) {
//  bugsnag_leave_breadcrumb_env(env, (char *)"Lack of cheese detected", BSG_CRUMB_LOG);
//}

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
Java_com_bugsnag_android_mazerunner_scenarios_CXXJavaUserInfoNativeCrashScenario_crash(
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
//
//JNIEXPORT int JNICALL
//Java_com_bugsnag_android_mazerunner_scenarios_CXXJavaBreadcrumbNativeBreadcrumbScenario_activate(
//    JNIEnv *env,
//    jobject instance) {
//  bugsnag_leave_breadcrumb_env(env, (char *)"Warm beer detected", BSG_CRUMB_LOG);
//  int x = 47;
//  if (x > 0)
//    __builtin_trap();
//  return 12167;
//}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_AutoDetectNdkDisabledScenario_crash(
    JNIEnv *env,
jobject instance) {
int x = 47;
if (x > 0)
__builtin_trap();
return 512345;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_AutoDetectNdkEnabledScenario_crash(
    JNIEnv *env,
jobject instance) {
int x = 47;
if (x > 0)
__builtin_trap();
return 12633;
}

}
