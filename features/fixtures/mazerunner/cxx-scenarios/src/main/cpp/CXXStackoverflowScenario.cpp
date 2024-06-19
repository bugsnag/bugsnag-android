#include <jni.h>
#include <cstring>
#include <cstdio>

extern "C" {
int __attribute__((optnone)) __attribute__((noinline)) crash_stack_overflow(int counter, char *input) {
  char stack[7];
  char *output = stack;

  while (*input) {
    *output = *input;
    input++;
    output++;
  }

  return 4 / counter;
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXStackoverflowScenario_crash(
    JNIEnv *env, jobject instance, jint counter, jstring text_) {
  char *text = (char *)(*env).GetStringUTFChars(text_, 0);
  printf("This one here: %ld\n",
         (long)crash_stack_overflow((int)counter, text));
  (*env).ReleaseStringUTFChars(text_, text);
}
}
