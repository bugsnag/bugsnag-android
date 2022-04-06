#include <jni.h>
#include <stdlib.h>

namespace evictor {
void __attribute__((optnone)) exit_with_style() { abort(); }
} // namespace evictor

extern "C" {
JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXAbortScenario_crash(
    JNIEnv *env, jobject instance) {
  // added namespaces for variety between tests
  evictor::exit_with_style();
}
}
