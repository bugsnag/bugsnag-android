#include <bugsnag.h>
#include <jni.h>
#include <stdlib.h>
#include <stdexcept>

extern "C" {


bool on_err_true(void *event_ptr) {
  bugsnag_event_set_context(event_ptr, (char *) "Some custom context");
  return true;
}

bool on_err_false(void *event_ptr) {
  printf("Received Bugsnag error report, logging callback!");
  return false;
}

bool __attribute__((noinline)) f_run_away(bool value) {
  if (value)
    throw new std::runtime_error("How about NO");
  return false;
}


bool __attribute__((noinline)) f_run_back(int value, int boundary) {
  printf("boundary: %d\n", boundary);
  if (value > -boundary)
    throw 42;
  return false;
}

int __attribute__((noinline)) f_throw_an_object(bool value, int boundary) {
  if (value) {
    printf("Now we know what they mean by 'advanced' tactical training: %d", boundary);
    return (int) f_run_back(value, boundary);
  }
  return boundary * 2;
}

int __attribute__((noinline)) f_trigger_an_exception(bool value) {
  printf("Shields up! Rrrrred alert!.\n");
  if (value)
    return (int) f_run_away(value);
  else
    return 405;
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXAutoContextScenario_activate(JNIEnv *env,
                                                                              jobject instance) {
  bugsnag_notify_env(env, (char *) "Hello hello",
                     (char *) "This is a new world", BSG_SEVERITY_INFO);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXNotifyScenario_activate(JNIEnv *env,
                                                                         jobject instance) {
  bugsnag_notify_env(env, (char *) "Vitamin C deficiency",
                     (char *) "9 out of 10 adults do not get their 5-a-day", BSG_SEVERITY_ERR);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXDelayedNotifyScenario_activate(JNIEnv *env,
                                                                                jobject instance) {
  bugsnag_notify_env(env, (char *) "Ferret Escape!", (char *) "oh no", BSG_SEVERITY_ERR);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXBreadcrumbScenario_activate(JNIEnv *env,
                                                                             jobject instance) {
  bugsnag_leave_breadcrumb_env(env, (char *) "Cold beans detected", BSG_CRUMB_LOG);
  bugsnag_notify_env(env, (char *) "Bean temperature loss",
                     (char *) "100% more microwave required", BSG_SEVERITY_INFO);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXUserInfoScenario_activate(JNIEnv *env,
                                                                           jobject instance) {
  bugsnag_set_user_env(env, (char *) "324523", NULL, (char *) "Jack Mill");
  bugsnag_notify_env(env, (char *) "Connection lost",
                     (char *) "No antenna detected", BSG_SEVERITY_INFO);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXNativeBreadcrumbJavaNotifyScenario_activate(
    JNIEnv *env,
    jobject instance) {
  bugsnag_leave_breadcrumb_env(env, (char *) "Rerun field analysis", BSG_CRUMB_PROCESS);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXNativeBreadcrumbJavaCrashScenario_activate(
    JNIEnv *env,
    jobject instance) {
  bugsnag_leave_breadcrumb_env(env, (char *) "Lack of cheese detected", BSG_CRUMB_LOG);
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXJavaBreadcrumbNativeBreadcrumbScenario_activate(
    JNIEnv *env,
    jobject instance) {
  bugsnag_leave_breadcrumb_env(env, (char *) "Warm beer detected", BSG_CRUMB_LOG);
  int x = 47;
  if (x > 0)
    __builtin_trap();
  return 12167;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXSignalOnErrorFalseScenario_crash(
    JNIEnv *env,
    jobject instance) {
  bugsnag_add_on_error(&on_err_false);
  int x = 47;
  if (x > 0)
    __builtin_trap();
  return 12633;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXSignalSmokeScenario_crash(JNIEnv *env,
                                                                           jobject instance,
                                                                           jint value) {
  bugsnag_leave_breadcrumb_env(env, (char *) "Substandard nacho error", BSG_CRUMB_REQUEST);
  bugsnag_add_on_error(&on_err_true);
  int x = 38;
  if (value > 0) {
    raise(SIGSEGV);
  }
  printf("That might've been one of the shortest assignments in the history of Starfleet.\n");
  return value / x / 8;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXExceptionOnErrorFalseScenario_crash(
    JNIEnv *env,
    jobject instance) {
  bugsnag_add_on_error(&on_err_false);
  int x = 61;
  printf("This one here: %ld\n", (long) f_trigger_an_exception(x > 0));
  printf("This one here: %ld\n", (long) f_throw_an_object(x > 0, x));
  return 22;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXExceptionSmokeScenario_crash(
    JNIEnv *env,
    jobject instance) {
  int x = 61;
  printf("This one here: %ld\n", (long) f_trigger_an_exception(x > 0));
  printf("This one here: %ld\n", (long) f_throw_an_object(x > 0, x));
  return 55;
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXStartScenario_activate(
    JNIEnv *env,
    jobject instance) {
  bugsnag_start(env);
  bugsnag_leave_breadcrumb((char *) "Start scenario crumb", BSG_CRUMB_LOG);
  bugsnag_notify((char *) "Start scenario",
                 (char *) "Testing env", BSG_SEVERITY_INFO);
}

bool override_user(void *event_ptr) {
  bugsnag_event_set_user(event_ptr, (char *) "callback", (char *) "call@back.cb", (char *) "");
  return true;
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXRemoveOnErrorScenario_activate(
    JNIEnv *env,
    jobject instance) {
  bugsnag_set_user_env(env, (char *) "default", (char *) "default@default.df", (char *) "default");
  bugsnag_add_on_error(&override_user);
  bugsnag_remove_on_error();
  int x = 47;
  if (x > 0)
    __builtin_trap();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXRemoveDataScenario_activate(JNIEnv *env,
                                                                             jobject instance) {
  bugsnag_notify_env(env, (char *) "RemoveDataScenario", (char *) "oh no", BSG_SEVERITY_ERR);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXNativeUserInfoJavaCrashScenario_activate(
    JNIEnv *env,
    jobject instance) {
  bugsnag_set_user_env(env, (char *) "24601", (char *) "test@test.test", (char *) "test user");
}

bool add_java_data(void *event_ptr) {
  bugsnag_event_add_metadata_string(event_ptr,
                                    (char *) "data",
                                    (char *) "context",
                                    bugsnag_event_get_context(event_ptr)
  );
  bugsnag_event_add_metadata_string(event_ptr,
                                    (char *) "data",
                                    (char *) "appVersion",
                                    bugsnag_app_get_version(event_ptr)
  );
  bugsnag_event_add_metadata_string(event_ptr,
                                    (char *) "data",
                                    (char *) "userName",
                                    bugsnag_event_get_user(event_ptr).name
  );
  bugsnag_event_add_metadata_string(event_ptr,
                                    (char *) "data",
                                    (char *) "userEmail",
                                    bugsnag_event_get_user(event_ptr).email
  );
  bugsnag_event_add_metadata_string(event_ptr,
                                    (char *) "data",
                                    (char *) "userId",
                                    bugsnag_event_get_user(event_ptr).id
  );
  bugsnag_event_add_metadata_string(event_ptr,
                                    (char *) "data",
                                    (char *) "metadata",
                                    bugsnag_event_get_metadata_string(event_ptr,
                                                                      (char *) "notData",
                                                                      (char *) "vals"
                                    )
  );
  bugsnag_event_add_metadata_string(event_ptr,
                                    (char *) "data",
                                    (char *) "device",
                                    bugsnag_device_get_model(event_ptr)
  );
  bugsnag_event_add_metadata_string(event_ptr,
                                    (char *) "data",
                                    (char *) "password",
                                    (char *) "Not telling you"
  );
  return true;
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXGetJavaDataScenario_activate(JNIEnv *env,
                                                                              jobject instance) {
  bugsnag_add_on_error(&add_java_data);
  int x = 47;
  if (x > 0)
    __builtin_trap();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXBackgroundNotifyScenario_activate(JNIEnv *env,
                                                                                   jobject instance) {
  bugsnag_notify_env(env, (char *)"Ferret Escape!", (char *)"oh no", BSG_SEVERITY_ERR);
}


JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXNotifySmokeScenario_activate(JNIEnv *env, jobject instance) {
  bugsnag_set_user_env(env, (char *)"324523", NULL, (char *)"Jack Mill");
  bugsnag_leave_breadcrumb_env(env, (char *)"Cold beans detected", BSG_CRUMB_LOG);
  bugsnag_notify_env(env, (char *)"CXXNotifySmokeScenario",
  (char *)"Smoke test scenario", BSG_SEVERITY_ERR);
}

bool override_unhandled(void *event_ptr) {
  if (bugsnag_event_is_unhandled(event_ptr)) {
    bugsnag_event_set_unhandled(event_ptr, false);
  }
  return !bugsnag_event_is_unhandled(event_ptr);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXHandledOverrideScenario_activate(JNIEnv *env,jobject instance) {
  bugsnag_add_on_error(&override_unhandled);
  abort();
}


JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_MultiProcessHandledCXXErrorScenario_activate(JNIEnv *env,
                                                                                           jobject instance) {
  bugsnag_notify_env(env, (char *)"activate",
                     (char *)"MultiProcessHandledCXXErrorScenario", BSG_SEVERITY_ERR);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_MultiProcessUnhandledCXXErrorScenario_user1(JNIEnv *env,
                                                                                          jobject instance) {
  bugsnag_set_user_env(env, (char *)"1", (char *)"1@test.com", (char *)"MultiProcessUnhandledCXXErrorScenario");
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_MultiProcessUnhandledCXXErrorScenario_user2(JNIEnv *env,
                                                                                          jobject instance) {
  bugsnag_set_user_env(env, (char *)"2", (char *)"2@example.com", (char *)"MultiProcessUnhandledCXXErrorScenario");
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_MultiProcessUnhandledCXXErrorScenario_activate(JNIEnv *env,
                                                                                             jobject instance) {
  abort();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXCrashLoopScenario_crash(JNIEnv *env,
                                                                         jobject instance) {
  abort();
}

}
