#include <jni.h>
#include <stdlib.h>
#include <stdexcept>

extern "C" {

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

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXDelayedCrashScenario_activate(JNIEnv *env,
                                                                         jobject instance,
                                                                         jint value) {
  if (value > 0)
    __builtin_trap();
  return 12067 / value;
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
Java_com_bugsnag_android_mazerunner_scenarios_CXXJavaUserInfoNativeCrashScenario_crash(
    JNIEnv *env,
    jobject instance) {
  int x = 47;
  if (x > 0)
    __builtin_trap();
  return 12812;
}

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
Java_com_bugsnag_android_mazerunner_scenarios_CXXConfigurationMetadataNativeCrashScenario_activate(
    JNIEnv *env,
    jobject instance) {
  int x = 47;
  if (x > 0)
    __builtin_trap();
  return 12167;
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXFeatureFlagNativeCrashScenario_crash(JNIEnv *env,
                                                                                      jobject instance) {
  __builtin_trap();
}

}
