#include <jni.h>
#include <stdexcept>

namespace magicstacks {

class FatalProblem : std::runtime_error {
  using std::runtime_error::runtime_error;

  virtual ~FatalProblem() {}
};

void __attribute__((optnone)) top() {
  throw new FatalProblem("well there it is!");
}

void __attribute__((optnone)) middle() { top(); }

void __attribute__((optnone)) start() { middle(); }
} // namespace magicstacks

extern "C" {
JNIEXPORT int JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXExceptionSmokeScenario_crash(
    JNIEnv *env, jobject instance) {
  magicstacks::start();

  return 90;
}
}
