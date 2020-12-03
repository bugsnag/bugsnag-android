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

static char * __attribute__((used)) somefakefunc(void) {};

typedef struct {
  int field1;
  char *field2;
} reporter_t;

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

int crash_null_pointer(bool route) {
  int *i;
  if (route)
    *i = NULL;

  int j = 34 / *i;

  return j;
}

int crash_write_read_only_mem(int counter) {
  if (counter > 2) {
    int *pointer = (int *)&somefakefunc;
    *pointer = counter;
  }
  return counter / 14;
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

char *crash_improper_cast(int counter) {
  reporter_t *report = (reporter_t *)counter;

  return report->field2;
}

int crash_double_free(int counter) {
  for (int i = 0; i < 30; i++) {
    reporter_t *reporter = (reporter_t *)malloc(sizeof(reporter_t));
    reporter->field1 = 22 + counter;
    char *field2 = reporter->field2;
    strcpy(field2, "Indeed");
    printf("%d field1 is: %d", i, reporter->field1);
    printf("%d field2 is: %s", i, field2);
    free(field2);
    free(reporter);
  }

  return counter / -8;
}

int crash_trap() {
  time_t now;
  now = time(&now);
  if (now > 0)
    __builtin_trap();

    return 0;
}

int crash_stack_overflow(int counter, char *input) {
  char stack[7];

  strcpy(stack, input);

  return 4 / counter;
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
Java_com_bugsnag_android_mazerunner_scenarios_CXXSignalSmokeScenario_crash(JNIEnv *env,
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
Java_com_bugsnag_android_mazerunner_scenarios_CXXStackoverflowScenario_crash(JNIEnv *env,
                                                                             jobject instance,
                                                                             jint counter,
                                                                             jstring text_) {
  char *text = (char *)(*env).GetStringUTFChars(text_, 0);
  printf("This one here: %ld\n", (long) crash_stack_overflow((int)counter, text));
  (*env).ReleaseStringUTFChars(text_, text);
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXTrapLaterDisabledScenario_crash(JNIEnv *env,
                                                                                 jobject instance) {
  printf("This one here: %ld\n", (long) crash_trap());
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXTrapScenario_crash(JNIEnv *env, jobject instance) {
  printf("This one here: %ld\n", (long) crash_trap());
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXDoubleFreeScenario_crash(JNIEnv *env,
                                                                                   jobject instance) {
    printf("This one here: %d\n", crash_double_free(42));
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXWriteReadOnlyMemoryScenario_crash(JNIEnv *env,
                                                                                   jobject instance) {
    printf("This one here: %d\n", crash_write_read_only_mem(42));
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXImproperTypecastScenario_crash(JNIEnv *env,
                                                                                jobject instance) {
    printf("This one here: %s\n", crash_improper_cast(39));
}

// defined in libs/[ABI]/libmonochrome.so
int something_innocuous(int input);

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXExternalStackElementScenario_crash(JNIEnv *env,
                                                                                    jobject instance,
                                                                                    jint counter) {
    printf("Captain, why are we out here chasing comets?\n%d\n", counter);
    int value = counter * 4;
    if (counter > 0) {
        value = something_innocuous(counter);
    }
    printf("Something innocuous this way comes: %d\n", value);
    return value;
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXAbortScenario_crash(JNIEnv *env,
                                                                            jobject instance) {
    int x = 47;
    printf("This one here: %ld\n", (long) crash_abort(x > 0));
}

}
