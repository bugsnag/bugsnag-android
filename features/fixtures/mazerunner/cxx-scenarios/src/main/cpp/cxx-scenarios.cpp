#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <time.h>

#include <stdexcept>

bool __attribute__((noinline)) run_away(bool value) {
  if (value)
    throw new std::runtime_error("How about NO");

  return false;
}

bool __attribute__((noinline)) run_back(int value, int boundary) {
  printf("boundary: %d\n", boundary);
  if (value > -boundary)
    throw 42;

  return false;
}

extern "C" {

int crash_abort(bool route) {
    if (route)
        abort();
    return 7;
}

int crash_floating_point(int counter) {
    time_t now; now = time(&now); int j = 34 * (int)now;
    for (int i = 0; i < 10; i++) { printf("Dividing by 0"); }
    if (counter < 4) {
        return j / counter;
    } else {
        return j;
    }
}

// Non-static so that it could in theory be modified in another module.
// This prevents optimizing the crash away due to UB.
int *int_ptr_null_94841ef2 = NULL;

// Make the compiler REALLY REALLY believe that int_ptr_null_94841ef2 could be modified.
void set_null_pointer_sgsdfg(int* new_value) {
  int_ptr_null_94841ef2 = new_value;
}

int crash_null_pointer(bool route) {
  int j = 34 / *int_ptr_null_94841ef2;

  return j;
}

volatile unsigned uint_f2wk124_dont_optimize_me_bro;

int crash_anr(bool route) {
    if (route)
        for(unsigned i = 0; ;i++) {
            uint_f2wk124_dont_optimize_me_bro = i;
        }
    return 7;
}

int __attribute__((noinline)) throw_an_object(bool value, int boundary) {
  if (value) {
    printf("Now we know what they mean by 'advanced' tactical training: %d", boundary);
    return (int)run_back(value, boundary);
  }
  return boundary * 2;
}

int __attribute__((noinline)) trigger_an_exception(bool value) {
  printf("Shields up! Rrrrred alert!.\n");
  if (value)
    return (int)run_away(value);
  else
    return 405;
}

int crash_trap() {
  time_t now;
  now = time(&now);
  if (now > 0)
    __builtin_trap();

    return 0;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXSigtrapScenario_crash(JNIEnv *env,
                                                                      jobject instance,
                                                                      jint value) {
  int x = 38;
  if (value > 0) {
    raise(SIGTRAP);
  }
  printf("Shields up! Rrrrred alert!.\n");
  return value / x;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXSigbusScenario_crash(JNIEnv *env,
                                                                      jobject instance,
                                                                      jint value) {
  int x = 38;
  if (value > 0) {
    raise(SIGBUS);
  }
  printf("A surprise party? Mr. Worf, I hate surprise parties. I would *never* do that to you.\n");
  return value / x / 8;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXSigabrtScenario_crash(JNIEnv *env,
                                                                        jobject instance,
                                                                        jint value) {
  int x = 38;
  if (value > 0) {
    raise(SIGABRT);
  }
  printf("Is it my imagination, or have tempers become a little frayed on the ship lately?\n");
  return value / x / 8;
}

JNIEXPORT jint JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXIgnoredSigabrtScenario_crash(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jint value) {
  int x = 38;
  if (value > 0) {
    raise(SIGABRT);
  }
  printf("Yeah, Steve, I remember. You said Wolf 359 was an inside job.\n");
  return value / x / 8;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXSigfpeScenario_crash(JNIEnv *env,
                                                                      jobject instance,
                                                                      jint value) {
  int x = 38;
  if (value > 0) {
    raise(SIGFPE);
  }
  printf("We know you're dealing in stolen ore.\n");
  return value / x / 8;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXSigsegvScenario_crash(JNIEnv *env,
                                                                       jobject instance,
                                                                       jint value) {
  int x = 38;
  if (value > 0) {
    raise(SIGSEGV);
  }
  printf("That might've been one of the shortest assignments in the history of Starfleet.\n");
  return value / x / 8;
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXSigillScenario_crash(JNIEnv *env,
                                                                      jobject instance,
                                                                      jint value) {
  int x = 38;
  if (value > 0) {
    raise(SIGILL);
  }
  printf("In all trust, there is the possibility for betrayal.\n");
  return value / x / 8;
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXNullPointerScenario_crash(JNIEnv *env,
                                                                           jobject instance) {
  int x = 38;
  printf("This one here: %ld\n", (long) crash_null_pointer(x > 0));
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXAnrScenario_crash(JNIEnv *env,
                                                                           jobject instance) {
  int x = 38;
  printf("This one here: %ld\n", (long) crash_anr(x > 0));
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXAnrNdkDisabledScenario_crash(JNIEnv *env,
                                                                              jobject instance) {
  int x = 38;
  printf("This one here: %ld\n", (long) crash_anr(x > 0));
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXExceptionScenario_crash(JNIEnv *env,
                                                                         jobject instance) {
  int x = 61;
  printf("This one here: %ld\n", (long) trigger_an_exception(x > 0));
  printf("This one here: %ld\n", (long) throw_an_object(x > 0, x));
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXThrowSomethingLaterDisabledScenario_crash(JNIEnv *env,
                                                                                           jobject instance,
                                                                                           jint num) {
  printf("This one here: %ld\n", (long) throw_an_object((num - 10) > 0, num));
  printf("This one here: %ld\n", (long) trigger_an_exception(num > 0));
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXThrowSomethingScenario_crash(JNIEnv *env,
                                                                              jobject instance,
                                                                              jint num) {
  printf("This one here: %ld\n", (long) throw_an_object((num - 10) > 0, num));
  printf("This one here: %ld\n", (long) trigger_an_exception(num > 0));
}


JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXThrowSomethingReenabledScenario_crash(JNIEnv *env,
                                                                                       jobject instance,
                                                                                       jint num) {
  printf("This one here: %ld\n", (long) throw_an_object((num - 10) > 0, num));
  printf("This one here: %ld\n", (long) trigger_an_exception(num > 0));
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXThrowSomethingOutsideReleaseStagesScenario_crash(JNIEnv *env,
                                                                                                  jobject instance,
                                                                                                  jint num) {
  printf("This one here: %ld\n", (long) throw_an_object((num - 10) > 0, num));
  printf("This one here: %ld\n", (long) trigger_an_exception(num > 0));
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXTrapOutsideReleaseStagesScenario_crash(JNIEnv *env,
                                                                                        jobject instance) {
  printf("This one here: %ld\n", (long) crash_trap());
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXTrapLaterDisabledScenario_crash(JNIEnv *env,
                                                                                 jobject instance) {
  printf("This one here: %ld\n", (long) crash_trap());
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXNaughtyStringsScenario_crash(JNIEnv *env, jobject instance) {
  abort();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXDelayedErrorScenario_crash(JNIEnv *env,
                                                                            jobject thiz) {
  abort();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXMarkLaunchCompletedScenario_crash(JNIEnv *env,
                                                                                   jobject thiz) {
  abort();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_UnhandledNdkAutoNotifyTrueScenario_crash(JNIEnv *env) {
  abort();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_UnhandledNdkAutoNotifyFalseScenario_crash(JNIEnv *env) {
  abort();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXMaxBreadcrumbCrashScenario_activate(JNIEnv *env,
                                                                                     jobject thiz) {
    abort();
}

}