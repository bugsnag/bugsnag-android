#include <cstdio>
#include <cstdlib>
#include <ctime>
#include <jni.h>

int trap_it() {
  time_t now;
  now = time(&now);
  if (now > 0)
    __builtin_trap();

  return 0;
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXTrapScenario_crash(
    JNIEnv *env, jobject instance) {
  printf("This one here: %ld\n", (long)trap_it());
}
}
