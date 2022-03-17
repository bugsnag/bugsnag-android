#include <jni.h>

static volatile int *the_value;

int __attribute__((optnone)) get_the_null_value() {
  // assume this function is very interesting
  return *the_value;
}

extern "C" {
JNIEXPORT jint JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXDereferenceNullScenario_crash(
    JNIEnv *env, jobject instance) {
  return get_the_null_value();
}
}
