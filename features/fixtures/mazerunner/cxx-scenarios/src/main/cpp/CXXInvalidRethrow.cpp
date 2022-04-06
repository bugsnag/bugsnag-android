#include <cstdio>
#include <jni.h>
#include <stdexcept>

void __attribute__((optnone)) print_last_exception() {
  try {
    throw;
  } catch (std::exception *ex) {
    // should never get here, since there is no exception.
    printf("ex: %p", ex);
  }
}

extern "C" {
JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXInvalidRethrow_crash(
    JNIEnv *env, jobject instance) {
  print_last_exception();
}
}
