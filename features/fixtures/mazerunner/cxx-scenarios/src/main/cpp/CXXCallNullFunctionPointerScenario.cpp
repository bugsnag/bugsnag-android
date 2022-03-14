#include <jni.h>

void (*definitely_valid_func)(jobject) = 0;

namespace dispatch {
class Handler {
public:
  static void __attribute__((optnone)) handle(jobject obj) {
    definitely_valid_func(obj);
  }
};
} // namespace dispatch

extern "C" {
JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXCallNullFunctionPointerScenario_crash(
    JNIEnv *env, jobject instance) {
  dispatch::Handler::handle(instance);
}
}
