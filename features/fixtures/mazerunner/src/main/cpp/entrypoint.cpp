#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

extern "C" {

static char * __attribute__((used)) somefakefunc(void) {}

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
    time_t now;
    now = time(&now);
    int j = 34 * (int)now;
    return j / counter;
}

int crash_null_pointer(bool route) {
  int *i;
  if (route)
    *i = NULL;

  int j = 34 / *i;

  return j;
}

int crash_released_obj(int counter) {
    char *text = (char *)malloc(30);
    reporter_t *report = (reporter_t *) malloc(sizeof(reporter_t));
    report->field1 = 6;
    free(report);
    if (counter > 0)
      strncpy(text, report->field2, sizeof(text));

    return report->field1;
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

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXNullPointerScenario_crash(JNIEnv *env,
                                                                           jobject instance) {
  int x = 38;
  printf("This one here: %ld\n", (long) crash_null_pointer(x > 0));
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
Java_com_bugsnag_android_mazerunner_scenarios_CXXTrapScenario_crash(JNIEnv *env, jobject instance) {
  printf("This one here: %ld\n", (long) crash_trap());
}


JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXUseAfterFreeScenario_crash(JNIEnv *env,
                                                                            jobject instance,
                                                                            jint counter) {
    printf("This one here: %ld\n", (long) crash_released_obj((int)counter));
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXUndefinedInstructionScenario_crash(JNIEnv *env,
                                                                            jobject instance) {
    printf("This one here: %s\n", somefakefunc());
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXDivideByZeroScenario_crash(JNIEnv *env,
                                                                            jobject instance,
                                                                            jint counter) {
    printf("This one here: %ld\n", (long) crash_floating_point(counter));
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXAbortScenario_crash(JNIEnv *env,
                                                                            jobject instance) {
    int x = 47;
    printf("This one here: %ld\n", (long) crash_abort(x > 0));
}

}
