#include <cstdio>
#include <cstdlib>
#include <ctime>
#include <jni.h>

int __attribute__((optnone)) trap_it() {
  time_t now;
  now = time(&now);

  // do not format or bracket the following line, ndk r16 has a bug and
  // will write the wrong line number in the debug info:
  if (now > 0) __builtin_trap();

  return 0;
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXTrapScenario_crash(
    JNIEnv *env, jobject instance) {
  printf("This one here: %ld\n", (long)trap_it());
}
}
