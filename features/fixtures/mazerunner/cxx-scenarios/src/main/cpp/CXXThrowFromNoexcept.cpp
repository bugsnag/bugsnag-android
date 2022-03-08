#include <cstdio>
#include <jni.h>
#include <stdexcept>

class FunkError : public std::runtime_error {
  using std::runtime_error::runtime_error;

  const char *toss_an_exception() const {
    throw new FunkError("you done it now!");
  }

public:
  virtual const char *what() const noexcept { return toss_an_exception(); }
};

extern "C" {
JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXThrowFromNoexcept_crash(
    JNIEnv *env, jobject instance) {
  auto err = new FunkError("mistakes were made");
  printf("what? %s", err->what());
}
}
