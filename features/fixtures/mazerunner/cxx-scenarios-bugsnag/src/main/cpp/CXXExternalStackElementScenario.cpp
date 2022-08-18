#include <jni.h>
#include <cstdio>
#include <dlfcn.h>
#include <bugsnag.h>

extern "C" {
// defined in libs/[ABI]/libmonochrome.so
int (*something_innocuous)(int input);

JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXExternalStackElementScenario_crash(
    JNIEnv *env, jobject instance, jint counter) {
  void *monochrome = dlopen("libmonochrome.so", RTLD_GLOBAL);
  bugsnag_refresh_symbol_table();
  *(void**)(&something_innocuous) = dlsym(monochrome, "something_innocuous");

  printf("Captain, why are we out here chasing comets?\n%d\n", counter);
  int value = counter * 4;
  if (counter > 0) {
    value = something_innocuous(counter);
  }
  printf("Something innocuous this way comes: %d\n", value);
  return value;
}
}
